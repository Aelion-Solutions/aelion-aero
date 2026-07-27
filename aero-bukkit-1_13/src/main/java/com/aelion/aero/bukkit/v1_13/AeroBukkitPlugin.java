package com.aelion.aero.bukkit.v1_13;

import com.aelion.aero.bukkit.BukkitAeroBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Aero backend for Minecraft 1.13–1.16.5 (Spigot API).
 */
public final class AeroBukkitPlugin extends JavaPlugin {

    private BukkitAeroBootstrap bootstrap;

    @Override
    public void onEnable() {
        bootstrap = new BukkitAeroBootstrap(this);
        bootstrap.enableWithClassicCommands();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.disable();
        }
    }
}
