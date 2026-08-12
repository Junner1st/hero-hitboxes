/*
 * SPDX-License-Identifier: Apache-2.0
 * Modifications Copyright (c) 2026 Junner
 */

package me.sootysplash.box;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class HeroHitboxesKeybinds {
    private static boolean wasOpenColorsConfigPressed;
    private static boolean wasOpenHitboxTypesConfigPressed;

    private HeroHitboxesKeybinds() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Config config = Config.getInstance();
            boolean openColorsConfigPressed = InputConstants.isKeyDown(client.getWindow(), config.openColorsConfigKey);
            boolean openHitboxTypesConfigPressed = InputConstants.isKeyDown(client.getWindow(), config.openHitboxTypesConfigKey);

            if (client.screen == null && openColorsConfigPressed && !wasOpenColorsConfigPressed) {
                ModMenu.openColorsConfigScreen();
            }
            if (client.screen == null && openHitboxTypesConfigPressed && !wasOpenHitboxTypesConfigPressed) {
                ModMenu.openHitboxTypesConfigScreen();
            }

            wasOpenColorsConfigPressed = openColorsConfigPressed;
            wasOpenHitboxTypesConfigPressed = openHitboxTypesConfigPressed;
        });
    }

    public static void updateFromConfig() {
        wasOpenColorsConfigPressed = false;
        wasOpenHitboxTypesConfigPressed = false;
    }
}
