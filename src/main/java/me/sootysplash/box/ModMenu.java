/*
 * SPDX-License-Identifier: Apache-2.0
 * Modifications Copyright (c) 2026 Junner
 */

package me.sootysplash.box;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.mojang.blaze3d.platform.InputConstants;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.gui.widgets.WidgetColorIndicator;
import fi.dy.masa.malilib.gui.wrappers.TextFieldType;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class ModMenu implements ModMenuApi {
    private static final String DEFAULT_NEW_ID = "minecraft:zombie";
    private static final int TAB_X = 10;
    private static final int TAB_Y = 26;
    private static final int TAB_WIDTH = 102;
    private static final int TAB_GAP = 2;
    private static final int CONTENT_START_Y = 52;
    private static final int ROW_X = 16;
    private static final int LABEL_WIDTH = 150;
    private static final int CONTROL_X = 174;
    private static final int ROW_HEIGHT = 22;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LABEL_Y_OFFSET = 6;
    private static final int TEXT_FIELD_WIDTH = 180;
    private static final int COLOR_PREVIEW_SIZE = 16;
    private static final int COLOR_FIELD_X = CONTROL_X + COLOR_PREVIEW_SIZE + 6;
    private static final int COLOR_FIELD_WIDTH = 84;
    private static final int SMALL_BUTTON_WIDTH = 60;
    private static final int DELETE_BUTTON_WIDTH = 58;
    private static final int ADD_TYPE_BUTTON_WIDTH = 118;
    private static final int TEXT_FIELD_MAX_LENGTH = 256;
    private static final int COLOR_FIELD_MAX_LENGTH = 9;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MalilibConfigScreen::new;
    }

    public static void openColorsConfigScreen() {
        openConfigScreen(MalilibConfigScreen.Tab.COLORS);
    }

    public static void openHitboxTypesConfigScreen() {
        openConfigScreen(MalilibConfigScreen.Tab.HITBOX_TYPES);
    }

    private static void openConfigScreen(MalilibConfigScreen.Tab tab) {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new MalilibConfigScreen(client.screen, tab));
    }

    private static class MalilibConfigScreen extends GuiBase {
        private final Screen parent;
        private final Config config = Config.getInstance();
        private Tab selectedTab;
        private int contentY;
        private int scrollOffset;
        private String typeSearch = "";
        private boolean refocusSearchAfterRebuild;
        private int searchCursorPosition;
        private String activeKeybindLabel;
        private Consumer<Integer> activeKeybindConsumer;
        private final Set<String> expandedTypeIds = new HashSet<>();
        private final Set<Config.HitboxType> pendingDeletedHitboxTypes = new HashSet<>();

        private MalilibConfigScreen(Screen parent) {
            this(parent, Tab.BEHAVIOR);
        }

        private MalilibConfigScreen(Screen parent, Tab selectedTab) {
            this.parent = parent;
            this.selectedTab = selectedTab;
            this.setParent(parent);
            this.setTitle("Hero Hitboxes Configs - " + modVersion());
        }

        @Override
        public void initGui() {
            super.initGui();
            clearElements();

            int x = TAB_X;
            for (Tab tab : Tab.values()) {
                ButtonGeneric button = new ButtonGeneric(x, TAB_Y, TAB_WIDTH, BUTTON_HEIGHT, tab.label);
                button.setEnabled(tab != selectedTab);
                addButton(button, (pressedButton, mouseButton) -> {
                    selectedTab = tab;
                    scrollOffset = 0;
                    initGui();
                });
                x += TAB_WIDTH + TAB_GAP;
            }

            addButton(new ButtonGeneric(width - 136, height - 26, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT, "Save"), (button, mouseButton) -> {
                removePendingDeletedHitboxTypes();
                config.save();
                Minecraft.getInstance().setScreen(parent);
            });
            addButton(new ButtonGeneric(width - 72, height - 26, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT, "Cancel"), (button, mouseButton) -> {
                Minecraft.getInstance().setScreen(parent);
            });

            contentY = CONTENT_START_Y - scrollOffset;
            switch (selectedTab) {
                case BEHAVIOR -> buildBehavior();
                case COLORS -> buildColors();
                case HITBOX_TYPES -> buildHitboxTypes();
                case KEYBINDS -> buildKeybinds();
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
        public boolean onKeyTyped(KeyEvent event) {
            if (activeKeybindConsumer != null) {
                if (event.key() != InputConstants.KEY_ESCAPE) {
                    activeKeybindConsumer.accept(event.key());
                    HeroHitboxesKeybinds.updateFromConfig();
                }
                activeKeybindLabel = null;
                activeKeybindConsumer = null;
                initGui();
                return true;
            }

            return super.onKeyTyped(event);
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

        private void buildKeybinds() {
            addKeybindRow("Open Colors Config", config.openColorsConfigKey, value -> config.openColorsConfigKey = value);
            addKeybindRow("Open Hitbox Types Config", config.openHitboxTypesConfigKey, value -> config.openHitboxTypesConfigKey = value);
        }

        private void buildHitboxTypes() {
            addButton(new ButtonGeneric(width - ADD_TYPE_BUTTON_WIDTH - 16, CONTENT_START_Y - 2, ADD_TYPE_BUTTON_WIDTH, BUTTON_HEIGHT, "Add Hitbox Type"), (button, mouseButton) -> {
                Minecraft.getInstance().setScreen(new AddHitboxTypeScreen(this));
            });

            addSearchRow();

            config.ensureHitboxTypes();
            int visibleTypes = 0;
            String normalizedSearch = normalizeSearch(typeSearch);
            for (Config.HitboxType hitboxType : config.hitboxTypes) {
                if (hitboxType == null || pendingDeletedHitboxTypes.contains(hitboxType) || !matchesTypeSearch(hitboxType, normalizedSearch)) {
                    continue;
                }
                addHitboxTypeBlock(hitboxType);
                visibleTypes++;
            }

            if (visibleTypes == 0) {
                String message = config.hitboxTypes.isEmpty()
                        ? "No custom hitbox types yet."
                        : "No hitbox types match the current search.";
                addLabel(ROW_X, nextY(), 260, 12, 0xFFA0A0A0, message);
            }
        }

        private void addHitboxTypeBlock(Config.HitboxType hitboxType) {
            String id = hitboxType.normalizedId();
            boolean expanded = expandedTypeIds.contains(id);
            int y = nextY();

            addButton(new ButtonGeneric(ROW_X, y, BUTTON_HEIGHT, BUTTON_HEIGHT, expanded ? "v" : ">"), (button, mouseButton) -> {
                if (expanded) {
                    expandedTypeIds.remove(id);
                } else {
                    expandedTypeIds.add(id);
                }
                initGui();
            });
            addLabel(44, y + LABEL_Y_OFFSET, LABEL_WIDTH, 12, 0xFFFFFFFF, displayNameFromId(id));
            addWidget(new ColorIndicatorWidget(CONTROL_X, y + 2, COLOR_PREVIEW_SIZE, COLOR_PREVIEW_SIZE, hitboxType.baseColor, newValue -> {
                hitboxType.baseColor = newValue;
                initGui();
            }));
            addButton(new ButtonGeneric(COLOR_FIELD_X, y, DELETE_BUTTON_WIDTH, BUTTON_HEIGHT, "Delete"), (button, mouseButton) -> deleteHitboxType(hitboxType));

            if (!expanded) {
                return;
            }

            contentY += 4;
            addToggleRow("Enabled", hitboxType.enabled, value -> hitboxType.enabled = value);
            addHitboxTypeIdRow(hitboxType);
            addColorRow("Base Color", hitboxType.baseColor, value -> hitboxType.baseColor = value);
            addColorRow("Eye Color", hitboxType.eyeColor, value -> hitboxType.eyeColor = value);
            addColorRow("Look Direction Color", hitboxType.lookColor, value -> hitboxType.lookColor = value);
            addColorRow("Target Color", hitboxType.targetColor, value -> hitboxType.targetColor = value);
            addColorRow("Hurt Color", hitboxType.hurtColor, value -> hitboxType.hurtColor = value);
            contentY += 4;
        }

        private void addHitboxTypeIdRow(Config.HitboxType hitboxType) {
            int y = nextY();
            addLabel(ROW_X, y + LABEL_Y_OFFSET, LABEL_WIDTH, 12, 0xFFFFFFFF, "Entity ID");
            GuiTextFieldGeneric field = new GuiTextFieldGeneric(CONTROL_X, y, TEXT_FIELD_WIDTH, BUTTON_HEIGHT, font);
            field.setValueWrapper(hitboxType.normalizedId());
            field.setMaxLengthWrapper(TEXT_FIELD_MAX_LENGTH);
            addTextField(field, new ChangeListener(value -> hitboxType.id = value), TextFieldType.STRING);
        }

        private void deleteHitboxType(Config.HitboxType hitboxType) {
            expandedTypeIds.remove(hitboxType.normalizedId());
            pendingDeletedHitboxTypes.add(hitboxType);
            initGui();
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

        private void addToggleRow(String label, boolean value, Consumer<Boolean> consumer) {
            int y = nextY();
            addLabel(ROW_X, y + LABEL_Y_OFFSET, LABEL_WIDTH, 12, 0xFFFFFFFF, label);
            addButton(new ToggleButton(CONTROL_X, y, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT, value), (button, mouseButton) -> {
                consumer.accept(!value);
                initGui();
            });
        }

        private void addKeybindRow(String label, int keyCode, Consumer<Integer> consumer) {
            int y = nextY();
            addLabel(ROW_X, y + LABEL_Y_OFFSET, LABEL_WIDTH, 12, 0xFFFFFFFF, label);
            String buttonLabel = label.equals(activeKeybindLabel) ? "Press key..." : keyDisplayName(keyCode);
            addButton(new ButtonGeneric(CONTROL_X, y, TEXT_FIELD_WIDTH, BUTTON_HEIGHT, buttonLabel), (button, mouseButton) -> {
                activeKeybindLabel = label;
                activeKeybindConsumer = consumer;
                initGui();
            });
        }

        private void addTextRow(String label, String value, Consumer<String> consumer) {
            int y = nextY();
            addLabel(ROW_X, y + LABEL_Y_OFFSET, LABEL_WIDTH, 12, 0xFFFFFFFF, label);
            GuiTextFieldGeneric field = new GuiTextFieldGeneric(CONTROL_X, y, TEXT_FIELD_WIDTH, BUTTON_HEIGHT, font);
            field.setValueWrapper(value);
            field.setMaxLengthWrapper(TEXT_FIELD_MAX_LENGTH);
            addTextField(field, new ChangeListener(consumer), TextFieldType.STRING);
        }

        private void addSearchRow() {
            int y = nextY();
            addLabel(ROW_X, y + LABEL_Y_OFFSET, LABEL_WIDTH, 12, 0xFFFFFFFF, "Search");
            GuiTextFieldGeneric field = new GuiTextFieldGeneric(CONTROL_X, y, TEXT_FIELD_WIDTH, BUTTON_HEIGHT, font);
            field.setValueWrapper(typeSearch);
            field.setMaxLengthWrapper(TEXT_FIELD_MAX_LENGTH);
            addTextField(field, new SearchChangeListener(), TextFieldType.STRING);

            if (refocusSearchAfterRebuild) {
                refocusSearchAfterRebuild = false;
                field.setFocusedWrapper(true);
                field.setCursorPosition(Math.min(searchCursorPosition, typeSearch.length()));
                setFocused(field);
            }
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
            addLabel(ROW_X, y + LABEL_Y_OFFSET, LABEL_WIDTH, 12, 0xFFFFFFFF, label);
            ColorIndicatorWidget indicator = new ColorIndicatorWidget(CONTROL_X, y + 2, COLOR_PREVIEW_SIZE, COLOR_PREVIEW_SIZE, value, newValue -> {
                consumer.accept(newValue);
                initGui();
            });
            addWidget(indicator);
            GuiTextFieldGeneric field = new ColorTextField(COLOR_FIELD_X, y, COLOR_FIELD_WIDTH, BUTTON_HEIGHT, font, indicator, consumer);
            field.setValueWrapper(hexColor(value));
            field.setMaxLengthWrapper(TEXT_FIELD_MAX_LENGTH);
            addTextField(field, new ChangeListener(text -> {
            }), TextFieldType.STRING);
            field.setMaxLengthWrapper(COLOR_FIELD_MAX_LENGTH);
        }

        private int nextY() {
            int y = contentY;
            contentY += ROW_HEIGHT;
            return y;
        }

        private void removePendingDeletedHitboxTypes() {
            if (pendingDeletedHitboxTypes.isEmpty()) {
                return;
            }
            config.hitboxTypes.removeAll(pendingDeletedHitboxTypes);
            pendingDeletedHitboxTypes.clear();
        }

        private static String hexColor(int color) {
            return String.format(Locale.ROOT, "#%08X", color);
        }

        private static String keyDisplayName(int keyCode) {
            return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
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

        private static String modVersion() {
            return FabricLoader.getInstance()
                    .getModContainer("hero-hitboxes")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        }

        private void refreshAfterChildScreen() {
            initGui();
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
            OUTLINE("Outline"),
            KEYBINDS("Keybinds");

            private final String label;

            Tab(String label) {
                this.label = label;
            }
        }

        private class SearchChangeListener implements ITextFieldListener<GuiTextFieldGeneric> {
            @Override
            public boolean onTextChange(GuiTextFieldGeneric textField) {
                typeSearch = textField.getValueWrapper();
                searchCursorPosition = textField.getCursorPosition();
                refocusSearchAfterRebuild = true;
                initGui();
                return true;
            }
        }
    }

    private static class AddHitboxTypeScreen extends GuiBase {
        private final MalilibConfigScreen parent;
        private final Config config = Config.getInstance();
        private int contentY;

        private String id = DEFAULT_NEW_ID;
        private int baseColor;
        private int eyeColor;
        private int lookColor;
        private int targetColor;
        private int hurtColor;

        private AddHitboxTypeScreen(MalilibConfigScreen parent) {
            this.parent = parent;
            this.setParent(parent);
            this.setTitle("Configure Hitbox Type - " + MalilibConfigScreen.modVersion());
            this.baseColor = config.hitBoxColor;
            this.eyeColor = config.eyeColor;
            this.lookColor = config.lookColor;
            this.targetColor = config.targetBoxColor;
            this.hurtColor = config.hurtBoxColor;
        }

        @Override
        public void initGui() {
            super.initGui();
            clearElements();

            addButton(new ButtonGeneric(width - 136, height - 26, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT, "Save"), (button, mouseButton) -> {
                saveHitboxType();
                parent.refreshAfterChildScreen();
                Minecraft.getInstance().setScreen(parent);
            });
            addButton(new ButtonGeneric(width - 72, height - 26, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT, "Cancel"), (button, mouseButton) -> {
                parent.refreshAfterChildScreen();
                Minecraft.getInstance().setScreen(parent);
            });

            contentY = CONTENT_START_Y;
            addEntityIdRow();
            addColorRow("Base Color", baseColor, value -> baseColor = value);
            addColorRow("Eye Color", eyeColor, value -> eyeColor = value);
            addColorRow("Look Direction Color", lookColor, value -> lookColor = value);
            addColorRow("Target Color", targetColor, value -> targetColor = value);
            addColorRow("Hurt Color", hurtColor, value -> hurtColor = value);
        }

        private void saveHitboxType() {
            String normalizedId = Config.HitboxType.normalizeId(id);
            if (normalizedId.isBlank()) {
                return;
            }

            config.addHitboxType(normalizedId, baseColor, eyeColor, lookColor, targetColor, hurtColor);
            config.save();
        }

        private void addTextRow(String label, String value, Consumer<String> consumer) {
            int y = nextY();
            addLabel(ROW_X, y + LABEL_Y_OFFSET, LABEL_WIDTH, 12, 0xFFFFFFFF, label);
            GuiTextFieldGeneric field = new GuiTextFieldGeneric(CONTROL_X, y, TEXT_FIELD_WIDTH, BUTTON_HEIGHT, font);
            field.setValueWrapper(value);
            field.setMaxLengthWrapper(TEXT_FIELD_MAX_LENGTH);
            addTextField(field, new ChangeListener(consumer), TextFieldType.STRING);
        }

        private void addEntityIdRow() {
            int y = nextY();
            addLabel(ROW_X, y + LABEL_Y_OFFSET, LABEL_WIDTH, 12, 0xFFFFFFFF, "Entity ID");
            GuiTextFieldGeneric field = new GuiTextFieldGeneric(CONTROL_X, y, TEXT_FIELD_WIDTH, BUTTON_HEIGHT, font);
            field.setValueWrapper(id);
            field.setMaxLengthWrapper(TEXT_FIELD_MAX_LENGTH);
            addTextField(field, new ChangeListener(value -> id = value), TextFieldType.STRING);
        }

        private void addColorRow(String label, int value, Consumer<Integer> consumer) {
            int y = nextY();
            addLabel(ROW_X, y + LABEL_Y_OFFSET, LABEL_WIDTH, 12, 0xFFFFFFFF, label);
            ColorIndicatorWidget indicator = new ColorIndicatorWidget(CONTROL_X, y + 2, COLOR_PREVIEW_SIZE, COLOR_PREVIEW_SIZE, value, newValue -> {
                consumer.accept(newValue);
                initGui();
            });
            addWidget(indicator);
            GuiTextFieldGeneric field = new ColorTextField(COLOR_FIELD_X, y, COLOR_FIELD_WIDTH, BUTTON_HEIGHT, font, indicator, consumer);
            field.setValueWrapper(MalilibConfigScreen.hexColor(value));
            field.setMaxLengthWrapper(TEXT_FIELD_MAX_LENGTH);
            addTextField(field, new ChangeListener(text -> {
            }), TextFieldType.STRING);
            field.setMaxLengthWrapper(COLOR_FIELD_MAX_LENGTH);
        }

        private int nextY() {
            int y = contentY;
            contentY += ROW_HEIGHT;
            return y;
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

    private static class ColorIndicatorWidget extends WidgetColorIndicator {
        private final ColorCallback callback;
        private boolean suppressCallback;

        private ColorIndicatorWidget(int x, int y, int width, int height, int color, java.util.function.IntConsumer consumer) {
            this(x, y, width, height, color, new ColorCallback(consumer));
        }

        private ColorIndicatorWidget(int x, int y, int width, int height, int color, ColorCallback callback) {
            super(x, y, width, height, Color4f.fromColor(color), callback);
            this.callback = callback;
            this.callback.widget = this;
        }

        private void setColor(int color) {
            suppressCallback = true;
            try {
                config.setIntegerValue(color);
            } finally {
                suppressCallback = false;
            }
        }

        private int currentColor() {
            return config.getIntegerValue();
        }

        private static class ColorCallback implements java.util.function.IntConsumer {
            private final java.util.function.IntConsumer consumer;
            private ColorIndicatorWidget widget;

            private ColorCallback(java.util.function.IntConsumer consumer) {
                this.consumer = consumer;
            }

            @Override
            public void accept(int value) {
                if (widget == null || !widget.suppressCallback) {
                    consumer.accept(value);
                }
            }
        }
    }

    private static class ToggleButton extends ButtonGeneric {
        private static final int ON_COLOR = 0xFF00AA00;
        private static final int OFF_COLOR = 0xFFAA0000;
        private final boolean value;

        private ToggleButton(int x, int y, int width, int height, boolean value) {
            super(x, y, width, height, value ? "ON" : "OFF");
            this.value = value;
        }

        @Override
        public void render(fi.dy.masa.malilib.render.GuiContext context, int mouseX, int mouseY, boolean selected) {
            String label = displayString;
            displayString = "";
            super.render(context, mouseX, mouseY, selected);
            displayString = label;

            if (visible) {
                int textY = y + (height - 8) / 2;
                drawCenteredStringWithShadow(context, x + width / 2, textY, value ? ON_COLOR : OFF_COLOR, label);
            }
        }
    }

    private static class ColorTextField extends GuiTextFieldGeneric {
        private final ColorIndicatorWidget indicator;
        private final Consumer<Integer> consumer;

        private ColorTextField(int x, int y, int width, int height, net.minecraft.client.gui.Font font, ColorIndicatorWidget indicator, Consumer<Integer> consumer) {
            super(x, y, width, height, font);
            this.indicator = indicator;
            this.consumer = consumer;
        }

        @Override
        public void setFocused(boolean focused) {
            boolean wasFocused = isFocused();
            super.setFocused(focused);
            if (wasFocused && !focused) {
                applyColor();
            }
        }

        private void applyColor() {
            String normalized = getValueWrapper().trim();
            if (normalized.startsWith("#")) {
                normalized = normalized.substring(1);
            }

            if (normalized.matches("[0-9a-fA-F]{8}")) {
                int color = (int) Long.parseLong(normalized, 16);
                indicator.setColor(color);
                consumer.accept(color);
                setValueWrapper(MalilibConfigScreen.hexColor(color));
            } else {
                setValueWrapper(MalilibConfigScreen.hexColor(indicator.currentColor()));
            }
        }
    }
}
