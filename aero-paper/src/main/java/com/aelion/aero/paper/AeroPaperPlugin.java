package com.aelion.aero.paper;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.config.AeroConfig;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper entry point for Aelion Aero.
 */
public final class AeroPaperPlugin extends JavaPlugin {

    private AeroConfig aeroConfig = new AeroConfig("", "", "", AeroConfig.ControlConfig.disabled());

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadAeroConfig();

        AeroCommandExecutor executor = new AeroCommandExecutor(this);
        PluginCommand ae = Objects.requireNonNull(getCommand("ae"), "ae command missing from plugin.yml");
        ae.setExecutor(executor);
        ae.setTabCompleter(executor);

        getLogger().info(AeroConstants.NAME + " enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info(AeroConstants.NAME + " disabled");
    }

    public AeroConfig aeroConfig() {
        return aeroConfig;
    }

    public void reloadAeroConfig() {
        reloadConfig();
        aeroConfig = PaperConfigBridge.fromBukkit(getConfig());
    }
}
