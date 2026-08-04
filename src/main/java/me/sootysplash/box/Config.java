/*
 * SPDX-License-Identifier: Apache-2.0
 * Modifications Copyright (c) 2026 Junner
 */

package me.sootysplash.box;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {

    //Andy is the goat https://github.com/AndyRusso/pvplegacyutils/blob/main/src/main/java/io/github/andyrusso/pvplegacyutils/PvPLegacyUtilsConfig.java

    private static final Path file = FabricLoader.getInstance().getConfigDir().resolve("hero-hitboxes.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance;

    public boolean enabled = true;
    public boolean hideArrow = false;
    public boolean hideFireworks = false;
    public boolean hideItems = false;
    public boolean changeTargetColor = true;
    public int eyeColor = Color.RED.getRGB();
    public int lookColor = Color.BLUE.getRGB();
    public int hitBoxColor = Color.WHITE.getRGB();
    public int targetBoxColor = Color.RED.getRGB();
    public boolean hitBoxHurt = false;
    public boolean renderEyeHeight = true;
    public boolean renderLookDir = true;
    public boolean lineLookDir = true;
    public int hurtBoxColor = Color.MAGENTA.getRGB();
    public float line1 = 2.5f;
    public double distFor2 = 32;
    public float line2 = 2.5f;
    public boolean outlineEnabled = false;
    public int outlineColor = Color.BLACK.getRGB();
    public float outlineMultiplier = 2;
    public List<HitboxType> hitboxTypes = new ArrayList<>();

    public void save() {
        ensureHitboxTypes();
        try {
            Files.writeString(file, GSON.toJson(this));
        } catch (IOException e) {
            Main.LOGGER.error("CombatHitboxes could not save the config.");
            throw new RuntimeException(e);
        }
    }

    public static Config getInstance() {
        if (instance == null) {
            try {
                instance = GSON.fromJson(Files.readString(file), Config.class);
            } catch (IOException exception) {
                Main.LOGGER.warn("CombatHitboxes couldn't load the config, using defaults.");
                instance = new Config();
            }
            instance.ensureHitboxTypes();
        }

        return instance;
    }

    public HitboxType getHitboxType(String entityId) {
        ensureHitboxTypes();
        if (entityId == null || entityId.isBlank()) {
            return null;
        }

        for (HitboxType hitboxType : hitboxTypes) {
            if (hitboxType != null && hitboxType.enabled && entityId.equals(hitboxType.normalizedId())) {
                return hitboxType;
            }
        }

        return null;
    }

    public void addHitboxType(String id, int baseColor, int eyeColor, int lookColor, int targetColor, int hurtColor) {
        ensureHitboxTypes();
        hitboxTypes.add(new HitboxType(id, baseColor, eyeColor, lookColor, targetColor, hurtColor));
    }

    public void ensureHitboxTypes() {
        if (hitboxTypes == null) {
            hitboxTypes = new ArrayList<>();
        }
        for (HitboxType hitboxType : hitboxTypes) {
            if (hitboxType != null) {
                hitboxType.ensureColors(this);
            }
        }
    }

    public static class HitboxType {
        public String id;
        public Integer baseColor;
        public Integer eyeColor;
        public Integer lookColor;
        public Integer targetColor;
        public Integer hurtColor;
        public boolean enabled;

        public HitboxType() {
            this(
                    "minecraft:zombie",
                    Color.WHITE.getRGB(),
                    Color.RED.getRGB(),
                    Color.BLUE.getRGB(),
                    Color.RED.getRGB(),
                    Color.MAGENTA.getRGB()
            );
        }

        public HitboxType(String id, int baseColor, int eyeColor, int lookColor, int targetColor, int hurtColor) {
            this.id = id;
            this.baseColor = baseColor;
            this.eyeColor = eyeColor;
            this.lookColor = lookColor;
            this.targetColor = targetColor;
            this.hurtColor = hurtColor;
            this.enabled = true;
        }

        public void ensureColors(Config config) {
            if (baseColor == null) {
                baseColor = config.hitBoxColor;
            }
            if (eyeColor == null) {
                eyeColor = config.eyeColor;
            }
            if (lookColor == null) {
                lookColor = config.lookColor;
            }
            if (targetColor == null) {
                targetColor = config.targetBoxColor;
            }
            if (hurtColor == null) {
                hurtColor = config.hurtBoxColor;
            }
        }

        public String normalizedId() {
            return normalizeId(id);
        }

        public static String normalizeId(String value) {
            if (value == null) {
                return "";
            }
            String trimmed = value.trim().toLowerCase();
            return trimmed.contains(":") ? trimmed : "minecraft:" + trimmed;
        }
    }
}
