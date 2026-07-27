package com.aelion.aero.bukkit;

import com.aelion.aero.api.AeroFleetService;
import com.aelion.aero.common.command.AeroCommandService;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.util.Strings;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Classic Bukkit {@link CommandExecutor} / {@link TabCompleter} for {@code /aes}.
 */
public final class BukkitAeroCommandExecutor implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final AeroConfigSupplier configSupplier;
    private final AeroReloadAction reloadAction;
    private final AeroFleetService fleetService;

    public BukkitAeroCommandExecutor(
            JavaPlugin plugin,
            AeroConfigSupplier configSupplier,
            AeroReloadAction reloadAction,
            AeroFleetService fleetService
    ) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
        this.reloadAction = reloadAction;
        this.fleetService = fleetService;
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
        return AeroCommandService.tabComplete(args, false, onlineNames());
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

    private static List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
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
        public void reloadConfig() throws Exception {
            reloadAction.run();
        }

        @Override
        public boolean kickPlayer(String playerName, String message) {
            if (fleetService == null || Strings.isBlank(playerName)) {
                return false;
            }
            Player player = Bukkit.getPlayerExact(playerName.trim());
            if (player == null || !player.isOnline()) {
                return false;
            }
            return fleetService.kickPlayer(player.getUniqueId(), message);
        }

        @Override
        public boolean transferPlayer(String playerName, String serverKey, String groupKey) {
            if (fleetService == null || Strings.isBlank(playerName)) {
                return false;
            }
            Player player = Bukkit.getPlayerExact(playerName.trim());
            if (player == null || !player.isOnline()) {
                return false;
            }
            if (Strings.isNotBlank(serverKey)) {
                return fleetService.transferToServer(player.getUniqueId(), serverKey);
            }
            if (Strings.isNotBlank(groupKey)) {
                return fleetService.transferToGroup(player.getUniqueId(), groupKey);
            }
            return false;
        }

        @Override
        public List<String> onlinePlayerNames() {
            return onlineNames();
        }
    }

    @FunctionalInterface
    public interface AeroConfigSupplier {
        AeroConfig get();
    }

    /**
     * Reload action that may throw checked exceptions (e.g. when the loopback
     * control API fails to bind on the requested port); the throwing signature
     * is required so {@code /aes reload} surfaces failures to the sender
     * instead of silently swallowing them.
     */
    @FunctionalInterface
    public interface AeroReloadAction {
        void run() throws Exception;
    }
}
