package com.aelion.aero.bungee;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.Permissions;
import com.aelion.aero.common.command.AeroCommandService;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.BackendEntry;
import java.util.List;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

final class AeroBungeeCommand extends Command implements TabExecutor {

    private final AeroBungeePlugin plugin;

    AeroBungeeCommand(AeroBungeePlugin plugin) {
        super(AeroConstants.COMMAND_PRIMARY, Permissions.INFO, AeroConstants.COMMAND_ALIAS);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        AeroCommandService.execute(args, new BungeePlatform(sender));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return AeroCommandService.tabComplete(args, true);
    }

    private final class BungeePlatform implements AeroCommandService.Platform {
        private final CommandSender sender;

        private BungeePlatform(CommandSender sender) {
            this.sender = sender;
        }

        @Override
        public void send(String legacyLine) {
            sender.sendMessage(TextComponent.fromLegacy(legacyLine));
        }

        @Override
        public void sendAll(List<String> legacyLines) {
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
            plugin.getProxy().getScheduler().runAsync(plugin, task);
        }

        @Override
        public void runSync(Runnable task) {
            task.run();
        }

        @Override
        public AeroConfig config() {
            return plugin.aeroConfig();
        }

        @Override
        public void reloadConfig() throws Exception {
            plugin.reloadAeroConfig();
        }

        @Override
        public boolean isProxy() {
            return true;
        }

        @Override
        public List<BackendEntry> backendsSnapshot() {
            BackendRegistryService registry = plugin.registryService();
            if (registry == null) {
                return List.of();
            }
            return registry.snapshot().validBackends();
        }
    }
}
