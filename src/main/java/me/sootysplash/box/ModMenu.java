/*
 * SPDX-License-Identifier: Apache-2.0
 * Modifications Copyright (c) 2026 Junner
 */

package me.sootysplash.box;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.EmptyEntry;
import me.shedaniel.clothconfig2.gui.widget.SearchFieldEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ModMenu implements ModMenuApi {
    private static final String DEFAULT_NEW_ID = "minecraft:zombie";

    private String pendingId = DEFAULT_NEW_ID;
    private int pendingBaseColor = Color.WHITE.getRGB();
    private int pendingEyeColor = Color.RED.getRGB();
    private int pendingLookColor = Color.BLUE.getRGB();
    private int pendingTargetColor = Color.RED.getRGB();
    private int pendingHurtColor = Color.MAGENTA.getRGB();
    private int pendingDefaultBaseColor = Color.WHITE.getRGB();
    private int pendingDefaultEyeColor = Color.RED.getRGB();
    private int pendingDefaultLookColor = Color.BLUE.getRGB();
    private int pendingDefaultTargetColor = Color.RED.getRGB();
    private int pendingDefaultHurtColor = Color.MAGENTA.getRGB();
    private final List<Config.HitboxType> pendingDeletedHitboxTypes = new ArrayList<>();

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            Config config = Config.getInstance();
            if (!hasPendingChanges()) {
                resetPendingHitboxType(config);
            }

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.nullToEmpty("Config"))
                    .setSavingRunnable(() -> {
                        removePendingDeletedHitboxTypes(config);
                        addPendingHitboxType(config);
                        config.save();
                        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreenAndShow(getModConfigScreenFactory().create(parent)));
                    });
            builder.setGlobalized(false);
            builder.setAfterInitConsumer(ModMenu::removeSearchField);

            ConfigEntryBuilder cfgent = builder.entryBuilder();
            ConfigCategory behavior = builder.getOrCreateCategory(Component.nullToEmpty("Behavior"));


            behavior.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("Enabled"), config.enabled)
                    .setDefaultValue(true)
                    .setTooltip(Component.nullToEmpty("Modify hitbox rendering?"))
                    .setSaveConsumer(newValue -> config.enabled = newValue)
                    .build());


            behavior.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("Render Eye Height"), config.renderEyeHeight)
                    .setDefaultValue(true)
                    .setTooltip(Component.nullToEmpty("Render the red line at the entity's eye height"))
                    .setSaveConsumer(newValue -> config.renderEyeHeight = newValue)
                    .build());


            behavior.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("Render Look Direction"), config.renderLookDir)
                    .setDefaultValue(true)
                    .setTooltip(Component.nullToEmpty("Render the blue line indicating the direction the entity is facing"))
                    .setSaveConsumer(newValue -> config.renderLookDir = newValue)
                    .build());


            behavior.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("Target HitBox Color"), config.changeTargetColor)
                    .setDefaultValue(true)
                    .setTooltip(Component.nullToEmpty("Target hitbox color on targets?"))
                    .setSaveConsumer(newValue -> config.changeTargetColor = newValue)
                    .build());


            behavior.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("HitBox Hurt"), config.hitBoxHurt)
                    .setDefaultValue(false)
                    .setTooltip(Component.nullToEmpty("Hitbox hurt color when hurt?"))
                    .setSaveConsumer(newValue -> config.hitBoxHurt = newValue)
                    .build());

            behavior.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("Line Look Direction"), config.lineLookDir)
                    .setDefaultValue(true)
                    .setTooltip(Component.nullToEmpty("Instead of the new arrow, draw the entity's look direction as a line"))
                    .setSaveConsumer(newValue -> config.lineLookDir = newValue)
                    .build());

            behavior.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("Hide Stuck Arrows"), config.hideArrow)
                    .setDefaultValue(false)
                    .setTooltip(Component.nullToEmpty("Removes bee stingers and arrows visually from other players"))
                    .setSaveConsumer(newValue -> config.hideArrow = newValue)
                    .build());

            behavior.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("Skip Fireworks"), config.hideFireworks)
                    .setDefaultValue(false)
                    .setTooltip(Component.nullToEmpty("Skips rendering hitboxes for fireworks"))
                    .setSaveConsumer(newValue -> config.hideFireworks = newValue)
                    .build());

            behavior.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("Skip Items"), config.hideItems)
                    .setDefaultValue(false)
                    .setTooltip(Component.nullToEmpty("Skips rendering hitboxes for dropped items"))
                    .setSaveConsumer(newValue -> config.hideItems = newValue)
                    .build());


            ConfigCategory colors = builder.getOrCreateCategory(Component.nullToEmpty("Colors"));


            colors.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Base Color"), config.hitBoxColor)
                    .setDefaultValue(Color.WHITE.getRGB())
                    .setTooltip(Component.nullToEmpty("The base hitbox's color"))
                    .setSaveConsumer(newValue -> config.hitBoxColor = newValue)
                    .build());


            colors.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Eye Color"), config.eyeColor)
                    .setDefaultValue(Color.RED.getRGB())
                    .setTooltip(Component.nullToEmpty("The hitbox eye height color"))
                    .setSaveConsumer(newValue -> config.eyeColor = newValue)
                    .build());


            colors.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Look Direction Color"), config.lookColor)
                    .setDefaultValue(Color.BLUE.getRGB())
                    .setTooltip(Component.nullToEmpty("The hitbox's look direction color"))
                    .setSaveConsumer(newValue -> config.lookColor = newValue)
                    .build());


            colors.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Target Color"), config.targetBoxColor)
                    .setDefaultValue(Color.RED.getRGB())
                    .setTooltip(Component.nullToEmpty("The hitbox color when the entity is targeted"))
                    .setSaveConsumer(newValue -> config.targetBoxColor = newValue)
                    .build());


            colors.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Hurt Color"), config.hurtBoxColor)
                    .setDefaultValue(Color.MAGENTA.getRGB())
                    .setTooltip(Component.nullToEmpty("The hitbox color when the entity is on hurt tick"))
                    .setSaveConsumer(newValue -> config.hurtBoxColor = newValue)
                    .build());

            ConfigCategory hitboxTypes = builder.getOrCreateCategory(Component.nullToEmpty("Hitbox Types"));

            config.ensureHitboxTypes();
            if (config.hitboxTypes.isEmpty()) {
                hitboxTypes.addEntry(cfgent.startTextDescription(Component.nullToEmpty("No custom hitbox types yet. Use + Add Hitbox Type to create one."))
                        .build());
            } else {
                for (Config.HitboxType hitboxType : config.hitboxTypes) {
                    if (hitboxType == null) {
                        continue;
                    }

                    hitboxTypes.addEntry(cfgent.startTextDescription(Component.nullToEmpty(hitboxTypeHeader(hitboxType.normalizedId())))
                            .build());

                    hitboxTypes.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("Enabled"), hitboxType.enabled)
                            .setDefaultValue(true)
                            .setSaveConsumer(newValue -> hitboxType.enabled = newValue)
                            .build());

                    hitboxTypes.addEntry(cfgent.startStrField(Component.nullToEmpty("Entity ID"), hitboxType.normalizedId())
                            .setDefaultValue(DEFAULT_NEW_ID)
                            .setTooltip(Component.nullToEmpty("Example: minecraft:zombie, minecraft:player, minecraft:item"))
                            .setSaveConsumer(newValue -> hitboxType.id = Config.HitboxType.normalizeId(newValue))
                            .build());

                    hitboxTypes.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Base Color"), hitboxType.baseColor)
                            .setDefaultValue(config.hitBoxColor)
                            .setTooltip(Component.nullToEmpty("Color used for this entity type's normal hitbox"))
                            .setSaveConsumer(newValue -> hitboxType.baseColor = newValue)
                            .build());

                    hitboxTypes.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Eye Color"), hitboxType.eyeColor)
                            .setDefaultValue(config.eyeColor)
                            .setTooltip(Component.nullToEmpty("Color used for this entity type's eye height"))
                            .setSaveConsumer(newValue -> hitboxType.eyeColor = newValue)
                            .build());

                    hitboxTypes.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Look Direction Color"), hitboxType.lookColor)
                            .setDefaultValue(config.lookColor)
                            .setTooltip(Component.nullToEmpty("Color used for this entity type's look direction"))
                            .setSaveConsumer(newValue -> hitboxType.lookColor = newValue)
                            .build());

                    hitboxTypes.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Target Color"), hitboxType.targetColor)
                            .setDefaultValue(config.targetBoxColor)
                            .setTooltip(Component.nullToEmpty("Color used when this entity type is targeted"))
                            .setSaveConsumer(newValue -> hitboxType.targetColor = newValue)
                            .build());

                    hitboxTypes.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Hurt Color"), hitboxType.hurtColor)
                            .setDefaultValue(config.hurtBoxColor)
                            .setTooltip(Component.nullToEmpty("Color used when this entity type is on hurt tick"))
                            .setSaveConsumer(newValue -> hitboxType.hurtColor = newValue)
                            .build());

                    hitboxTypes.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("Delete"), false)
                            .setDefaultValue(false)
                            .setTooltip(Component.nullToEmpty("Remove this hitbox type when saving"))
                            .setSaveConsumer(newValue -> {
                                if (newValue) {
                                    pendingDeletedHitboxTypes.add(hitboxType);
                                }
                            })
                            .build());
                }
            }

            ConfigCategory addHitboxType = builder.getOrCreateCategory(Component.nullToEmpty("+ Add Hitbox Type"));

            addHitboxType.addEntry(cfgent.startTextDescription(Component.nullToEmpty("Fill in the fields and click Save to create the hitbox type."))
                    .build());

            addHitboxType.addEntry(cfgent.startStrField(Component.nullToEmpty("Entity ID"), pendingId)
                    .setDefaultValue(DEFAULT_NEW_ID)
                    .setTooltip(Component.nullToEmpty("Example: minecraft:zombie, minecraft:player, minecraft:item"))
                    .setSaveConsumer(newValue -> pendingId = Config.HitboxType.normalizeId(newValue))
                    .build());

            addHitboxType.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Base Color"), pendingBaseColor)
                    .setDefaultValue(config.hitBoxColor)
                    .setTooltip(Component.nullToEmpty("Color used for this entity type's normal hitbox"))
                    .setSaveConsumer(newValue -> pendingBaseColor = newValue)
                    .build());

            addHitboxType.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Eye Color"), pendingEyeColor)
                    .setDefaultValue(config.eyeColor)
                    .setTooltip(Component.nullToEmpty("Color used for this entity type's eye height"))
                    .setSaveConsumer(newValue -> pendingEyeColor = newValue)
                    .build());

            addHitboxType.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Look Direction Color"), pendingLookColor)
                    .setDefaultValue(config.lookColor)
                    .setTooltip(Component.nullToEmpty("Color used for this entity type's look direction"))
                    .setSaveConsumer(newValue -> pendingLookColor = newValue)
                    .build());

            addHitboxType.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Target Color"), pendingTargetColor)
                    .setDefaultValue(config.targetBoxColor)
                    .setTooltip(Component.nullToEmpty("Color used when this entity type is targeted"))
                    .setSaveConsumer(newValue -> pendingTargetColor = newValue)
                    .build());

            addHitboxType.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Hurt Color"), pendingHurtColor)
                    .setDefaultValue(config.hurtBoxColor)
                    .setTooltip(Component.nullToEmpty("Color used when this entity type is on hurt tick"))
                    .setSaveConsumer(newValue -> pendingHurtColor = newValue)
                    .build());


            ConfigCategory linesWidths = builder.getOrCreateCategory(Component.nullToEmpty("Line Width"));


            linesWidths.addEntry(cfgent.startFloatField(Component.nullToEmpty("Line Width 1"), config.line1)
                    .setMin(0)
                    .setMax(25f)
                    .setDefaultValue(2.5f)
                    .setTooltip(Component.nullToEmpty("The width of the hitbox lines"))
                    .setSaveConsumer(newValue -> config.line1 = newValue)
                    .build());

            linesWidths.addEntry(cfgent.startDoubleField(Component.nullToEmpty("Distance for width 2"), config.distFor2)
                    .setMin(0)
                    .setMax(256)
                    .setDefaultValue(32)
                    .setTooltip(Component.nullToEmpty("The distance for Line Width 2 to be used"))
                    .setSaveConsumer(newValue -> config.distFor2 = newValue)
                    .build());

            linesWidths.addEntry(cfgent.startFloatField(Component.nullToEmpty("Line Width 2"), config.line2)
                    .setMin(0)
                    .setMax(25f)
                    .setDefaultValue(2.5f)
                    .setTooltip(Component.nullToEmpty("The width of the hitbox lines beyond the set distance"))
                    .setSaveConsumer(newValue -> config.line2 = newValue)
                    .build());

            ConfigCategory outline = builder.getOrCreateCategory(Component.nullToEmpty("Outline"));

            outline.addEntry(cfgent.startBooleanToggle(Component.nullToEmpty("Outline Enabled"), config.outlineEnabled)
                    .setDefaultValue(false)
                    .setTooltip(Component.nullToEmpty("Enable hitbox outlines"))
                    .setSaveConsumer(newValue -> config.outlineEnabled = newValue)
                    .build());

            outline.addEntry(cfgent.startAlphaColorField(Component.nullToEmpty("Outline Color"), config.outlineColor)
                    .setDefaultValue(Color.BLACK.getRGB())
                    .setTooltip(Component.nullToEmpty("The hitbox's outline color"))
                    .setSaveConsumer(newValue -> config.outlineColor = newValue)
                    .build());

            outline.addEntry(cfgent.startFloatField(Component.nullToEmpty("Outline Size Multiplier"), config.outlineMultiplier)
                    .setDefaultValue(2)
                    .setMin(Math.nextUp(1))
                    .setMax(10F)
                    .setTooltip(Component.nullToEmpty("How much the hitbox's line width will be multiplied by for the outline"))
                    .setSaveConsumer(newValue -> config.outlineMultiplier = newValue)
                    .build());

            return builder.build();
        };
    }

    private void addPendingHitboxType(Config config) {
        String normalizedPendingId = Config.HitboxType.normalizeId(pendingId);
        if (!hasPendingChanges() || normalizedPendingId.isBlank()) {
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
        resetPendingHitboxType(config);
    }

    private void removePendingDeletedHitboxTypes(Config config) {
        if (pendingDeletedHitboxTypes.isEmpty()) {
            return;
        }

        config.hitboxTypes.removeAll(pendingDeletedHitboxTypes);
        pendingDeletedHitboxTypes.clear();
    }

    private boolean hasPendingChanges() {
        return !DEFAULT_NEW_ID.equals(Config.HitboxType.normalizeId(pendingId))
                || pendingBaseColor != pendingDefaultBaseColor
                || pendingEyeColor != pendingDefaultEyeColor
                || pendingLookColor != pendingDefaultLookColor
                || pendingTargetColor != pendingDefaultTargetColor
                || pendingHurtColor != pendingDefaultHurtColor;
    }

    private void resetPendingHitboxType(Config config) {
        pendingId = DEFAULT_NEW_ID;
        pendingBaseColor = config.hitBoxColor;
        pendingEyeColor = config.eyeColor;
        pendingLookColor = config.lookColor;
        pendingTargetColor = config.targetBoxColor;
        pendingHurtColor = config.hurtBoxColor;
        pendingDefaultBaseColor = pendingBaseColor;
        pendingDefaultEyeColor = pendingEyeColor;
        pendingDefaultLookColor = pendingLookColor;
        pendingDefaultTargetColor = pendingTargetColor;
        pendingDefaultHurtColor = pendingHurtColor;
    }

    private static String hitboxTypeHeader(String id) {
        return "§6── §r" + displayNameFromId(id) + " §6──";
    }

    private static String displayNameFromId(String id) {
        String value = Config.HitboxType.normalizeId(id);
        int namespaceSeparator = value.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < value.length()) {
            value = value.substring(namespaceSeparator + 1);
        }

        String[] words = value.split("_+");
        StringBuilder displayName = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!displayName.isEmpty()) {
                displayName.append(' ');
            }
            displayName.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                displayName.append(word.substring(1).toLowerCase());
            }
        }

        return displayName.isEmpty() ? value : displayName.toString();
    }

    private static void removeSearchField(Screen screen) {
        if (!(screen instanceof ClothConfigScreen clothConfigScreen)) {
            return;
        }

        List<?> entries = clothConfigScreen.listWidget.children();
        for (int index = 0; index < entries.size(); index++) {
            if (!(entries.get(index) instanceof SearchFieldEntry)) {
                continue;
            }

            entries.remove(index);
            if (index > 0 && entries.get(index - 1) instanceof EmptyEntry) {
                entries.remove(index - 1);
                index--;
            }
            if (index < entries.size() && entries.get(index) instanceof EmptyEntry) {
                entries.remove(index);
            }
            return;
        }
    }
}
