package com.aelion.aero.paper.v1_17;

import com.aelion.aero.bukkit.BukkitAeroBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Aero backend for Minecraft 1.17–1.20.x (Paper API; classic commands).
 */
public final class AeroPaperPlugin extends JavaPlugin {

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
