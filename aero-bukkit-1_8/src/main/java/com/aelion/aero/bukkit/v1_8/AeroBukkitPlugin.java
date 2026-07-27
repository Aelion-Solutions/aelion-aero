package com.aelion.aero.bukkit.v1_8;

import com.aelion.aero.bukkit.BukkitAeroBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Aero backend for Minecraft 1.8–1.12.2 (Spigot API).
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
