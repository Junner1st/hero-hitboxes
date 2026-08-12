/*
 * SPDX-License-Identifier: Apache-2.0
 * Modifications Copyright (c) 2026 Junner
 */

package me.sootysplash.box;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.awt.Color;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class ModMenu implements ModMenuApi {
    private static final String DEFAULT_NEW_ID = "minecraft:zombie";

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MalilibConfigScreen::new;
    }

    private static class MalilibConfigScreen extends GuiBase {
        private final Screen parent;
        private final Config config = Config.getInstance();
        private Tab selectedTab;
        private int contentY;
        private int scrollOffset;
        private String typeSearch = "";
        private final Set<String> expandedTypeIds = new HashSet<>();

        private String pendingId = DEFAULT_NEW_ID;
        private int pendingBaseColor;
        private int pendingEyeColor;
        private int pendingLookColor;
        private int pendingTargetColor;
        private int pendingHurtColor;

        private MalilibConfigScreen(Screen parent) {
            this(parent, Tab.BEHAVIOR);
        }

        private MalilibConfigScreen(Screen parent, Tab selectedTab) {
            this.parent = parent;
            this.selectedTab = selectedTab;
            this.setParent(parent);
            this.setTitle("Hero Hitboxes Config");
            resetPendingHitboxType();
        }

        @Override
        public void initGui() {
            super.initGui();
            clearElements();

            int x = 16;
            int y = 28;
            for (Tab tab : Tab.values()) {
                ButtonGeneric button = new ButtonGeneric(x, y, 104, 20, tab.label);
                button.setEnabled(tab != selectedTab);
                addButton(button, (pressedButton, mouseButton) -> {
                    selectedTab = tab;
                    scrollOffset = 0;
                    initGui();
                });
                x += 108;
            }

            addButton(new ButtonGeneric(width - 168, height - 28, 72, 20, "Save"), (button, mouseButton) -> {
                config.save();
                Minecraft.getInstance().setScreen(parent);
            });
            addButton(new ButtonGeneric(width - 88, height - 28, 72, 20, "Cancel"), (button, mouseButton) -> {
                Minecraft.getInstance().setScreen(parent);
            });

            contentY = 58 - scrollOffset;
            switch (selectedTab) {
                case BEHAVIOR -> buildBehavior();
                case COLORS -> buildColors();
                case HITBOX_TYPES -> buildHitboxTypes();
                case LINE_WIDTH -> buildLineWidth();
                case OUTLINE -> buildOutline();
            }
        }

        @Override
        public boolean onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            int oldOffset = scrollOffset;
            scrollOffset = Math.max(0, scrollOffset - (int) (verticalAmount * 18));
            if (oldOffset != scrollOffset) {
                initGui();
                return true;
            }
            return super.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        @Override
        protected void drawContents(GuiContext context, int mouseX, int mouseY, float partialTicks) {
            super.drawContents(context, mouseX, mouseY, partialTicks);
            drawString(context, "Hero Hitboxes", 16, 10, 0xFFFFFFFF);
        }

        private void buildBehavior() {
            addToggleRow("Enabled", config.enabled, value -> config.enabled = value);
            addToggleRow("Render Eye Height", config.renderEyeHeight, value -> config.renderEyeHeight = value);
            addToggleRow("Render Look Direction", config.renderLookDir, value -> config.renderLookDir = value);
            addToggleRow("Target HitBox Color", config.changeTargetColor, value -> config.changeTargetColor = value);
            addToggleRow("HitBox Hurt", config.hitBoxHurt, value -> config.hitBoxHurt = value);
            addToggleRow("Line Look Direction", config.lineLookDir, value -> config.lineLookDir = value);
            addToggleRow("Hide Stuck Arrows", config.hideArrow, value -> config.hideArrow = value);
            addToggleRow("Skip Fireworks", config.hideFireworks, value -> config.hideFireworks = value);
            addToggleRow("Skip Items", config.hideItems, value -> config.hideItems = value);
        }

        private void buildColors() {
            addColorRow("Base Color", config.hitBoxColor, value -> config.hitBoxColor = value);
            addColorRow("Eye Color", config.eyeColor, value -> config.eyeColor = value);
            addColorRow("Look Direction Color", config.lookColor, value -> config.lookColor = value);
            addColorRow("Target Color", config.targetBoxColor, value -> config.targetBoxColor = value);
            addColorRow("Hurt Color", config.hurtBoxColor, value -> config.hurtBoxColor = value);
        }

        private void buildHitboxTypes() {
            addLabel(16, nextY() + 4, 280, 14, 0xFFFFD37A, "Add Hitbox Type");
            addTextRow("Entity ID", pendingId, value -> pendingId = Config.HitboxType.normalizeId(value));
            addColorRow("Base Color", pendingBaseColor, value -> pendingBaseColor = value);
            addColorRow("Eye Color", pendingEyeColor, value -> pendingEyeColor = value);
            addColorRow("Look Direction Color", pendingLookColor, value -> pendingLookColor = value);
            addColorRow("Target Color", pendingTargetColor, value -> pendingTargetColor = value);
            addColorRow("Hurt Color", pendingHurtColor, value -> pendingHurtColor = value);
            addButton(new ButtonGeneric(176, nextY(), 136, 20, "Add Hitbox Type"), (button, mouseButton) -> {
                addPendingHitboxType();
                initGui();
            });

            contentY += 8;
            addTextRow("Search", typeSearch, value -> {
                typeSearch = value;
                initGui();
            });

            config.ensureHitboxTypes();
            int visibleTypes = 0;
            String normalizedSearch = normalizeSearch(typeSearch);
            for (Config.HitboxType hitboxType : config.hitboxTypes) {
                if (hitboxType == null || !matchesTypeSearch(hitboxType, normalizedSearch)) {
                    continue;
                }
                addHitboxTypeBlock(hitboxType);
                visibleTypes++;
            }

            if (visibleTypes == 0) {
                String message = config.hitboxTypes.isEmpty()
                        ? "No custom hitbox types yet."
                        : "No hitbox types match the current search.";
                addLabel(16, nextY(), 260, 12, 0xFFA0A0A0, message);
            }
        }

        private void addHitboxTypeBlock(Config.HitboxType hitboxType) {
            String id = hitboxType.normalizedId();
            boolean expanded = expandedTypeIds.contains(id);
            int y = nextY();

            addButton(new ButtonGeneric(16, y, 20, 20, expanded ? "v" : ">"), (button, mouseButton) -> {
                if (expanded) {
                    expandedTypeIds.remove(id);
                } else {
                    expandedTypeIds.add(id);
                }
                initGui();
            });
            addLabel(44, y + 6, 150, 12, 0xFFFFFFFF, displayNameFromId(id));
            addWidget(new ColorPreviewWidget(176, y + 2, hitboxType.baseColor));

            if (!expanded) {
                return;
            }

            contentY += 4;
            addToggleRow("Enabled", hitboxType.enabled, value -> hitboxType.enabled = value);
            addTextRow("Entity ID", hitboxType.normalizedId(), value -> hitboxType.id = Config.HitboxType.normalizeId(value));
            addColorRow("Base Color", hitboxType.baseColor, value -> hitboxType.baseColor = value);
            addColorRow("Eye Color", hitboxType.eyeColor, value -> hitboxType.eyeColor = value);
            addColorRow("Look Direction Color", hitboxType.lookColor, value -> hitboxType.lookColor = value);
            addColorRow("Target Color", hitboxType.targetColor, value -> hitboxType.targetColor = value);
            addColorRow("Hurt Color", hitboxType.hurtColor, value -> hitboxType.hurtColor = value);
            contentY += 4;
        }

        private void buildLineWidth() {
            addFloatRow("Line Width 1", config.line1, 0, 25, value -> config.line1 = value);
            addDoubleRow("Distance for width 2", config.distFor2, 0, 256, value -> config.distFor2 = value);
            addFloatRow("Line Width 2", config.line2, 0, 25, value -> config.line2 = value);
        }

        private void buildOutline() {
            addToggleRow("Outline Enabled", config.outlineEnabled, value -> config.outlineEnabled = value);
            addColorRow("Outline Color", config.outlineColor, value -> config.outlineColor = value);
            addFloatRow("Outline Size Multiplier", config.outlineMultiplier, Math.nextUp(1f), 10, value -> config.outlineMultiplier = value);
        }

        private void addSection(String label) {
            addLabel(16, nextY() + 4, 280, 14, 0xFFFFD37A, label);
            contentY += 8;
        }

        private void addToggleRow(String label, boolean value, Consumer<Boolean> consumer) {
            int y = nextY();
            addLabel(16, y + 6, 150, 12, 0xFFFFFFFF, label);
            addButton(new ButtonGeneric(176, y, 64, 20, value ? "ON" : "OFF"), (button, mouseButton) -> {
                consumer.accept(!value);
                initGui();
            });
        }

        private void addTextRow(String label, String value, Consumer<String> consumer) {
            int y = nextY();
            addLabel(16, y + 6, 150, 12, 0xFFFFFFFF, label);
            GuiTextFieldGeneric field = new GuiTextFieldGeneric(176, y, 180, 20, font);
            field.setValueWrapper(value);
            addTextField(field, new ChangeListener(consumer));
        }

        private void addFloatRow(String label, float value, float min, float max, Consumer<Float> consumer) {
            addTextRow(label, Float.toString(value), text -> {
                try {
                    consumer.accept(clamp(Float.parseFloat(text), min, max));
                } catch (NumberFormatException ignored) {
                }
            });
        }

        private void addDoubleRow(String label, double value, double min, double max, Consumer<Double> consumer) {
            addTextRow(label, Double.toString(value), text -> {
                try {
                    consumer.accept(clamp(Double.parseDouble(text), min, max));
                } catch (NumberFormatException ignored) {
                }
            });
        }

        private void addColorRow(String label, int value, Consumer<Integer> consumer) {
            int y = nextY();
            addLabel(16, y + 6, 150, 12, 0xFFFFFFFF, label);
            addWidget(new ColorPreviewWidget(176, y + 2, value));
            GuiTextFieldGeneric field = new GuiTextFieldGeneric(202, y, 96, 20, font);
            field.setMaxLengthWrapper(8);
            field.setValueWrapper(hexColor(value));
            addTextField(field, new ChangeListener(text -> parseHexColor(text, value, consumer)));
        }

        private int nextY() {
            int y = contentY;
            contentY += 26;
            return y;
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

        private static void parseHexColor(String text, int fallback, Consumer<Integer> consumer) {
            String normalized = text.trim();
            if (normalized.startsWith("#")) {
                normalized = normalized.substring(1);
            }
            if (!normalized.matches("[0-9a-fA-F]{8}")) {
                consumer.accept(fallback);
                return;
            }
            consumer.accept((int) Long.parseLong(normalized, 16));
        }

        private static String hexColor(int color) {
            return String.format(Locale.ROOT, "%08X", color);
        }

        private static String displayNameFromId(String id) {
            String value = Config.HitboxType.normalizeId(id);
            int namespaceSeparator = value.indexOf(':');
            if (namespaceSeparator >= 0 && namespaceSeparator + 1 < value.length()) {
                value = value.substring(namespaceSeparator + 1);
            }
            return value.replace('_', ' ');
        }

        private static boolean matchesTypeSearch(Config.HitboxType hitboxType, String search) {
            if (search.isBlank()) {
                return true;
            }

            return displayNameFromId(hitboxType.normalizedId()).toLowerCase(Locale.ROOT).contains(search);
        }

        private static String normalizeSearch(String search) {
            return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
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
                this.label = label;
            }
        }

        private static class ChangeListener implements ITextFieldListener<GuiTextFieldGeneric> {
            private final Consumer<String> consumer;

            private ChangeListener(Consumer<String> consumer) {
                this.consumer = consumer;
            }

            @Override
            public boolean onTextChange(GuiTextFieldGeneric textField) {
                consumer.accept(textField.getValueWrapper());
                return true;
            }
        }

        private static class ColorPreviewWidget extends fi.dy.masa.malilib.gui.widgets.WidgetBase {
            private final int color;

            private ColorPreviewWidget(int x, int y, int color) {
                super(x, y, 18, 16);
                this.color = color;
            }

            @Override
            public void render(GuiContext context, int mouseX, int mouseY, boolean selected) {
                RenderUtils.drawOutlinedBox(context, x, y, width, height, 0xFF000000, 0xFF909090);
                RenderUtils.drawRect(context, x + 2, y + 2, width - 4, height - 4, color);
            }
        }
    }
}
