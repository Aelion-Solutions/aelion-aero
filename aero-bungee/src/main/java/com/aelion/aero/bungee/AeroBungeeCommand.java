package com.aelion.aero.bungee;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.Permissions;
import com.aelion.aero.common.command.AeroCommandService;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.BackendEntry;
import com.aelion.aero.common.control.ControlTransferRequest;
import com.aelion.aero.common.control.ControlTransferResolver;
import com.aelion.aero.common.util.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

final class AeroBungeeCommand extends Command implements TabExecutor {

    private final AeroBungeePlugin plugin;

    AeroBungeeCommand(AeroBungeePlugin plugin) {
        super(AeroConstants.COMMAND_PROXY_PRIMARY, Permissions.INFO, AeroConstants.COMMAND_PROXY_ALIAS);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        AeroCommandService.execute(args, new BungeePlatform(sender));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> names = plugin.getProxy().getPlayers().stream()
                .map(ProxiedPlayer::getName)
                .collect(Collectors.toList());
        return AeroCommandService.tabComplete(args, true, names);
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

        @Override
        public boolean kickPlayer(String playerName, String message) {
            if (Strings.isBlank(playerName)) {
                return false;
            }
            ProxiedPlayer player = plugin.getProxy().getPlayer(playerName.trim());
            if (player == null) {
                return false;
            }
            String reason = Strings.isBlank(message) ? "Kicked by Aelion Aero." : message;
            player.disconnect(TextComponent.fromLegacy(reason));
            return true;
        }

        @Override
        public boolean transferPlayer(String playerName, String serverKey, String groupKey) {
            if (Strings.isBlank(playerName)) {
                return false;
            }
            ProxiedPlayer player = plugin.getProxy().getPlayer(playerName.trim());
            if (player == null) {
                return false;
            }
            ControlTransferRequest req = new ControlTransferRequest();
            req.setUuid(player.getUniqueId().toString());
            if (Strings.isNotBlank(serverKey)) {
                req.setServerId(serverKey);
                req.setServerName(serverKey);
            }
            if (Strings.isNotBlank(groupKey)) {
                req.setGroupId(groupKey);
                req.setGroupName(groupKey);
            }
            List<String> registryNames = new ArrayList<>();
            BackendRegistryService registry = plugin.registryService();
            if (registry != null) {
                for (BackendEntry entry : registry.snapshot().getBackends()) {
                    if (entry != null && Strings.isNotBlank(entry.getName())) {
                        registryNames.add(entry.getName());
                    }
                }
            }
            ControlTransferResolver.Result resolved = ControlTransferResolver.resolve(
                    req,
                    plugin.aeroConfig(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    registryNames);
            if (!resolved.isOk()) {
                return false;
            }
            ServerInfo target = plugin.getProxy().getServerInfo(resolved.proxyServerName());
            if (target == null) {
                return false;
            }
            player.connect(target);
            return true;
        }

        @Override
        public List<String> onlinePlayerNames() {
            return plugin.getProxy().getPlayers().stream()
                    .map(ProxiedPlayer::getName)
                    .collect(Collectors.toList());
        }
    }
}
