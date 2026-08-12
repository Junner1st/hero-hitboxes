/*
 * SPDX-License-Identifier: Apache-2.0
 * Modifications Copyright (c) 2026 Junner
 */

package me.sootysplash.box;

import net.fabricmc.api.ClientModInitializer;

public class HeroHitboxesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HeroHitboxesKeybinds.register();
    }
}
