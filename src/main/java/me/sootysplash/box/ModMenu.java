/*
 * SPDX-License-Identifier: Apache-2.0
 * Modifications Copyright (c) 2026 Junner
 */

package me.sootysplash.box;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.component.SmallCheckboxComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;

public class ModMenu implements ModMenuApi {
    private static final String DEFAULT_NEW_ID = "minecraft:zombie";

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OwoConfigScreen::new;
    }

    private static class OwoConfigScreen extends BaseOwoScreen<FlowLayout> {
        private final Screen parent;
        private final Config config = Config.getInstance();
        private Tab selectedTab;

        private String pendingId = DEFAULT_NEW_ID;
        private int pendingBaseColor;
        private int pendingEyeColor;
        private int pendingLookColor;
        private int pendingTargetColor;
        private int pendingHurtColor;

        private OwoConfigScreen(Screen parent) {
            this(parent, Tab.BEHAVIOR);
        }

        private OwoConfigScreen(Screen parent, Tab selectedTab) {
            super(Component.nullToEmpty("Hero Hitboxes Config"));
            this.parent = parent;
            this.selectedTab = selectedTab;
            resetPendingHitboxType();
        }

        @Override
        protected OwoUIAdapter<FlowLayout> createAdapter() {
            return OwoUIAdapter.create(this, UIContainers::verticalFlow);
        }

        @Override
        protected void build(FlowLayout root) {
            root.surface(Surface.optionsBackground());
            root.padding(Insets.of(12));
            root.gap(8);
            root.horizontalAlignment(HorizontalAlignment.CENTER);

            root.child(UIComponents.label(Component.nullToEmpty("Hero Hitboxes"))
                    .color(Color.WHITE)
                    .shadow(true)
                    .margins(Insets.bottom(2)));

            root.child(tabBar());

            FlowLayout content = UIContainers.verticalFlow(Sizing.fill(), Sizing.content());
            content.gap(6);
            content.padding(Insets.of(4));
            buildSelectedTab(content);

            ScrollContainer<FlowLayout> scroll = UIContainers.verticalScroll(Sizing.fill(82), Sizing.fill(82), content)
                    .scrollbarThiccness(6)
                    .scrollStep(24);
            root.child(scroll);

            root.child(actionBar());
        }

        private FlowLayout tabBar() {
            FlowLayout tabs = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
            tabs.gap(4);
            tabs.horizontalAlignment(HorizontalAlignment.CENTER);
            for (Tab tab : Tab.values()) {
                tabs.child(UIComponents.button(Component.nullToEmpty(tab.label), button -> {
                    Minecraft.getInstance().setScreen(new OwoConfigScreen(parent, tab));
                }).active(tab != selectedTab));
            }
            return tabs;
        }

        private FlowLayout actionBar() {
            return UIContainers.horizontalFlow(Sizing.content(), Sizing.content())
                    .gap(6)
                    .child(UIComponents.button(Component.nullToEmpty("Save"), button -> {
                        config.save();
                        Minecraft.getInstance().setScreen(parent);
                    }))
                    .child(UIComponents.button(Component.nullToEmpty("Cancel"), button -> Minecraft.getInstance().setScreen(parent)));
        }

        private void buildSelectedTab(FlowLayout content) {
            switch (selectedTab) {
                case BEHAVIOR -> buildBehavior(content);
                case COLORS -> buildColors(content);
                case HITBOX_TYPES -> buildHitboxTypes(content);
                case LINE_WIDTH -> buildLineWidth(content);
                case OUTLINE -> buildOutline(content);
            }
        }

        private void buildBehavior(FlowLayout content) {
            content.child(toggleRow("Enabled", "Modify hitbox rendering?", config.enabled, value -> config.enabled = value));
            content.child(toggleRow("Render Eye Height", "Render the red line at the entity's eye height", config.renderEyeHeight, value -> config.renderEyeHeight = value));
            content.child(toggleRow("Render Look Direction", "Render the blue line indicating the direction the entity is facing", config.renderLookDir, value -> config.renderLookDir = value));
            content.child(toggleRow("Target HitBox Color", "Target hitbox color on targets?", config.changeTargetColor, value -> config.changeTargetColor = value));
            content.child(toggleRow("HitBox Hurt", "Hitbox hurt color when hurt?", config.hitBoxHurt, value -> config.hitBoxHurt = value));
            content.child(toggleRow("Line Look Direction", "Instead of the new arrow, draw the entity's look direction as a line", config.lineLookDir, value -> config.lineLookDir = value));
            content.child(toggleRow("Hide Stuck Arrows", "Removes bee stingers and arrows visually from other players", config.hideArrow, value -> config.hideArrow = value));
            content.child(toggleRow("Skip Fireworks", "Skips rendering hitboxes for fireworks", config.hideFireworks, value -> config.hideFireworks = value));
            content.child(toggleRow("Skip Items", "Skips rendering hitboxes for dropped items", config.hideItems, value -> config.hideItems = value));
        }

        private void buildColors(FlowLayout content) {
            content.child(colorRow("Base Color", "The base hitbox's color", config.hitBoxColor, value -> config.hitBoxColor = value));
            content.child(colorRow("Eye Color", "The hitbox eye height color", config.eyeColor, value -> config.eyeColor = value));
            content.child(colorRow("Look Direction Color", "The hitbox's look direction color", config.lookColor, value -> config.lookColor = value));
            content.child(colorRow("Target Color", "The hitbox color when the entity is targeted", config.targetBoxColor, value -> config.targetBoxColor = value));
            content.child(colorRow("Hurt Color", "The hitbox color when the entity is on hurt tick", config.hurtBoxColor, value -> config.hurtBoxColor = value));
        }

        private void buildHitboxTypes(FlowLayout content) {
            config.ensureHitboxTypes();

            content.child(section("Add Hitbox Type"));
            content.child(textRow("Entity ID", pendingId, "Example: minecraft:zombie, minecraft:player, minecraft:item", value -> pendingId = Config.HitboxType.normalizeId(value)));
            content.child(colorRow("Base Color", "Color used for this entity type's normal hitbox", pendingBaseColor, value -> pendingBaseColor = value));
            content.child(colorRow("Eye Color", "Color used for this entity type's eye height", pendingEyeColor, value -> pendingEyeColor = value));
            content.child(colorRow("Look Direction Color", "Color used for this entity type's look direction", pendingLookColor, value -> pendingLookColor = value));
            content.child(colorRow("Target Color", "Color used when this entity type is targeted", pendingTargetColor, value -> pendingTargetColor = value));
            content.child(colorRow("Hurt Color", "Color used when this entity type is on hurt tick", pendingHurtColor, value -> pendingHurtColor = value));
            content.child(UIComponents.button(Component.nullToEmpty("Add Hitbox Type"), button -> {
                addPendingHitboxType();
                Minecraft.getInstance().setScreen(new OwoConfigScreen(parent, Tab.HITBOX_TYPES));
            }));

            content.child(section("Hitbox Types"));
            if (config.hitboxTypes.isEmpty()) {
                content.child(UIComponents.label(Component.nullToEmpty("No custom hitbox types yet."))
                        .color(Color.ofRgb(0xA0A0A0)));
                return;
            }

            for (Config.HitboxType hitboxType : config.hitboxTypes) {
                if (hitboxType == null) {
                    continue;
                }
                content.child(hitboxTypeBlock(hitboxType));
            }
        }

        private FlowLayout hitboxTypeBlock(Config.HitboxType hitboxType) {
            FlowLayout block = UIContainers.verticalFlow(Sizing.fill(), Sizing.content());
            block.surface(Surface.PANEL);
            block.padding(Insets.of(6));
            block.gap(4);
            block.child(UIComponents.label(Component.nullToEmpty(displayNameFromId(hitboxType.normalizedId())))
                    .shadow(true)
                    .color(Color.ofRgb(0xFFD37A)));
            block.child(toggleRow("Enabled", "", hitboxType.enabled, value -> hitboxType.enabled = value));
            block.child(textRow("Entity ID", hitboxType.normalizedId(), "Example: minecraft:zombie, minecraft:player, minecraft:item", value -> hitboxType.id = Config.HitboxType.normalizeId(value)));
            block.child(colorRow("Base Color", "Color used for this entity type's normal hitbox", hitboxType.baseColor, value -> hitboxType.baseColor = value));
            block.child(colorRow("Eye Color", "Color used for this entity type's eye height", hitboxType.eyeColor, value -> hitboxType.eyeColor = value));
            block.child(colorRow("Look Direction Color", "Color used for this entity type's look direction", hitboxType.lookColor, value -> hitboxType.lookColor = value));
            block.child(colorRow("Target Color", "Color used when this entity type is targeted", hitboxType.targetColor, value -> hitboxType.targetColor = value));
            block.child(colorRow("Hurt Color", "Color used when this entity type is on hurt tick", hitboxType.hurtColor, value -> hitboxType.hurtColor = value));
            block.child(UIComponents.button(Component.nullToEmpty("Delete"), button -> {
                config.hitboxTypes.remove(hitboxType);
                Minecraft.getInstance().setScreen(new OwoConfigScreen(parent, Tab.HITBOX_TYPES));
            }));
            return block;
        }

        private void buildLineWidth(FlowLayout content) {
            content.child(floatRow("Line Width 1", config.line1, 0, 25, value -> config.line1 = value));
            content.child(doubleRow("Distance for width 2", config.distFor2, 0, 256, value -> config.distFor2 = value));
            content.child(floatRow("Line Width 2", config.line2, 0, 25, value -> config.line2 = value));
        }

        private void buildOutline(FlowLayout content) {
            content.child(toggleRow("Outline Enabled", "Enable hitbox outlines", config.outlineEnabled, value -> config.outlineEnabled = value));
            content.child(colorRow("Outline Color", "The hitbox's outline color", config.outlineColor, value -> config.outlineColor = value));
            content.child(floatRow("Outline Size Multiplier", config.outlineMultiplier, Math.nextUp(1f), 10, value -> config.outlineMultiplier = value));
        }

        private FlowLayout toggleRow(String label, String tooltip, boolean initialValue, Consumer<Boolean> consumer) {
            SmallCheckboxComponent checkbox = UIComponents.smallCheckbox(Component.nullToEmpty(label)).checked(initialValue);
            checkbox.onChanged().subscribe(consumer::accept);
            if (!tooltip.isBlank()) {
                checkbox.tooltip(Component.nullToEmpty(tooltip));
            }
            return row().child(checkbox);
        }

        private FlowLayout textRow(String label, String value, String tooltip, Consumer<String> consumer) {
            TextBoxComponent textBox = UIComponents.textBox(Sizing.fixed(180), value);
            textBox.onChanged().subscribe(consumer::accept);
            if (!tooltip.isBlank()) {
                textBox.tooltip(Component.nullToEmpty(tooltip));
            }

            return labeledRow(label).child(textBox);
        }

        private FlowLayout floatRow(String label, float value, float min, float max, Consumer<Float> consumer) {
            return textRow(label, Float.toString(value), "Range: " + min + " - " + max, text -> {
                try {
                    consumer.accept(clamp(Float.parseFloat(text), min, max));
                } catch (NumberFormatException ignored) {
                }
            });
        }

        private FlowLayout doubleRow(String label, double value, double min, double max, Consumer<Double> consumer) {
            return textRow(label, Double.toString(value), "Range: " + min + " - " + max, text -> {
                try {
                    consumer.accept(clamp(Double.parseDouble(text), min, max));
                } catch (NumberFormatException ignored) {
                }
            });
        }

        private FlowLayout colorRow(String label, String tooltip, int value, Consumer<Integer> consumer) {
            ColorPickerComponent picker = new ColorPickerComponent();
            picker.sizing(Sizing.fixed(160), Sizing.fixed(90));
            picker.showAlpha(true);
            picker.selectedColor(Color.ofArgb(value));
            picker.onChanged().subscribe(color -> consumer.accept(color.argb()));
            if (!tooltip.isBlank()) {
                picker.tooltip(Component.nullToEmpty(tooltip));
            }

            FlowLayout layout = UIContainers.verticalFlow(Sizing.fill(), Sizing.content());
            layout.surface(Surface.PANEL);
            layout.padding(Insets.of(6));
            layout.gap(4);
            layout.child(UIComponents.label(Component.nullToEmpty(label)).shadow(true));
            layout.child(picker);
            return layout;
        }

        private FlowLayout labeledRow(String label) {
            FlowLayout layout = row();
            layout.child(UIComponents.label(Component.nullToEmpty(label))
                    .horizontalSizing(Sizing.fixed(150))
                    .verticalSizing(Sizing.content()));
            return layout;
        }

        private FlowLayout row() {
            FlowLayout layout = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
            layout.surface(Surface.PANEL);
            layout.padding(Insets.of(6));
            layout.gap(6);
            layout.verticalAlignment(VerticalAlignment.CENTER);
            return layout;
        }

        private UIComponent section(String label) {
            var component = UIComponents.label(Component.nullToEmpty(label))
                    .shadow(true)
                    .color(Color.ofRgb(0xFFD37A));
            component.margins(Insets.top(4));
            return component;
        }

        private void addPendingHitboxType() {
            String normalizedPendingId = Config.HitboxType.normalizeId(pendingId);
            if (normalizedPendingId.isBlank()) {
                return;
            }

            config.addHitboxType(
                    normalizedPendingId,
                    pendingBaseColor,
                    pendingEyeColor,
                    pendingLookColor,
                    pendingTargetColor,
                    pendingHurtColor
            );
            resetPendingHitboxType();
        }

        private void resetPendingHitboxType() {
            pendingId = DEFAULT_NEW_ID;
            pendingBaseColor = config.hitBoxColor;
            pendingEyeColor = config.eyeColor;
            pendingLookColor = config.lookColor;
            pendingTargetColor = config.targetBoxColor;
            pendingHurtColor = config.hurtBoxColor;
        }

        private static String displayNameFromId(String id) {
            String value = Config.HitboxType.normalizeId(id);
            int namespaceSeparator = value.indexOf(':');
            if (namespaceSeparator >= 0 && namespaceSeparator + 1 < value.length()) {
                value = value.substring(namespaceSeparator + 1);
            }
            return value.replace('_', ' ');
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }

        private enum Tab {
            BEHAVIOR("Behavior"),
            COLORS("Colors"),
            HITBOX_TYPES("Hitbox Types"),
            LINE_WIDTH("Line Width"),
            OUTLINE("Outline");

            private final String label;

            Tab(String label) {
                this.label = label.toUpperCase(Locale.ROOT);
            }
        }
    }
}
