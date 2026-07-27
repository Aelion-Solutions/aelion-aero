package com.aelion.aero.bukkit;

import com.aelion.aero.common.command.AeroCommandService;
import com.aelion.aero.common.config.AeroConfig;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Classic Bukkit {@link CommandExecutor} / {@link TabCompleter} for {@code /aes}.
 */
public final class BukkitAeroCommandExecutor implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final AeroConfigSupplier configSupplier;
    private final Runnable reloadAction;

    public BukkitAeroCommandExecutor(
            JavaPlugin plugin,
            AeroConfigSupplier configSupplier,
            Runnable reloadAction
    ) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        AeroCommandService.execute(args == null ? new String[0] : args, new BukkitPlatform(sender));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args == null) {
            return Collections.emptyList();
        }
        return AeroCommandService.tabComplete(args, false);
    }

    /**
     * Registers the backend command name against plugin.yml.
     */
    public static void register(JavaPlugin plugin, BukkitAeroCommandExecutor executor, String name) {
        if (plugin.getCommand(name) != null) {
            plugin.getCommand(name).setExecutor(executor);
            plugin.getCommand(name).setTabCompleter(executor);
        }
    }

    private final class BukkitPlatform implements AeroCommandService.Platform {
        private final CommandSender sender;

        private BukkitPlatform(CommandSender sender) {
            this.sender = sender;
        }

        @Override
        public void send(String legacyLine) {
            // AeroCommandStyle already emits section-sign (§) codes.
            sender.sendMessage(legacyLine == null ? "" : legacyLine);
        }

        @Override
        public void sendAll(List<String> legacyLines) {
            if (legacyLines == null) {
                return;
            }
            for (String line : legacyLines) {
                send(line);
            }
        }

        @Override
        public boolean hasPermission(String permission) {
            return sender.hasPermission(permission);
        }

        @Override
        public void runAsync(Runnable task) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }

        @Override
        public void runSync(Runnable task) {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }

        @Override
        public AeroConfig config() {
            return configSupplier.get();
        }

        @Override
        public void reloadConfig() {
            reloadAction.run();
        }
    }

    @FunctionalInterface
    public interface AeroConfigSupplier {
        AeroConfig get();
    }
}
