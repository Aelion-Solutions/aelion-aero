package com.aelion.aero.velocity;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.Permissions;
import com.aelion.aero.common.command.AeroCommandService;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.BackendEntry;
import com.aelion.aero.common.control.ControlTransferRequest;
import com.aelion.aero.common.control.ControlTransferResolver;
import com.aelion.aero.common.fleet.FleetNotifyService;
import com.aelion.aero.common.util.Strings;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

final class AeroVelocityCommand implements SimpleCommand {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private final AeroVelocityPlugin plugin;

    AeroVelocityCommand(AeroVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        AeroCommandService.execute(invocation.arguments(), new VelocityPlatform(invocation));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        VelocityPlatform platform = new VelocityPlatform(invocation);
        if (!Permissions.allowsAny(platform::hasPermission)) {
            return List.of();
        }
        List<String> names = plugin.proxy().getAllPlayers().stream()
                .map(Player::getUsername)
                .collect(Collectors.toList());
        return AeroCommandService.tabComplete(invocation.arguments(), platform, names);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(suggest(invocation));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return Permissions.allowsAny(invocation.source()::hasPermission);
    }

    static void register(AeroVelocityPlugin plugin) {
        var meta = plugin.proxy().getCommandManager().metaBuilder(AeroConstants.COMMAND_PROXY_PRIMARY)
                .aliases(AeroConstants.COMMAND_PROXY_ALIAS)
                .plugin(plugin)
                .build();
        plugin.proxy().getCommandManager().register(meta, new AeroVelocityCommand(plugin));
    }

    private final class VelocityPlatform implements AeroCommandService.Platform {
        private final Invocation invocation;

        private VelocityPlatform(Invocation invocation) {
            this.invocation = invocation;
        }

        @Override
        public void send(String legacyLine) {
            invocation.source().sendMessage(LEGACY.deserialize(legacyLine));
        }

        @Override
        public void sendAll(List<String> legacyLines) {
            for (String line : legacyLines) {
                send(line);
            }
        }

        @Override
        public boolean hasPermission(String permission) {
            return invocation.source().hasPermission(permission);
        }

        @Override
        public void runAsync(Runnable task) {
            plugin.proxy().getScheduler().buildTask(plugin, task).schedule();
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
            Optional<Player> player = plugin.proxy().getPlayer(playerName.trim());
            if (!player.isPresent()) {
                return false;
            }
            String reason = Strings.isBlank(message) ? "Kicked by Aelion Aero." : message;
            player.get().disconnect(Component.text(reason));
            return true;
        }

        @Override
        public boolean transferPlayer(String playerName, String serverKey, String groupKey) {
            if (Strings.isBlank(playerName)) {
                return false;
            }
            Optional<Player> player = plugin.proxy().getPlayer(playerName.trim());
            if (!player.isPresent()) {
                return false;
            }
            ControlTransferRequest req = new ControlTransferRequest();
            req.setUuid(player.get().getUniqueId().toString());
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
            Optional<RegisteredServer> target = plugin.proxy().getServer(resolved.proxyServerName());
            if (!target.isPresent()) {
                return false;
            }
            player.get().createConnectionRequest(target.get()).fireAndForget();
            return true;
        }

        @Override
        public List<String> onlinePlayerNames() {
            return plugin.proxy().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .collect(Collectors.toList());
        }

        @Override
        public UUID senderId() {
            if (invocation.source() instanceof Player) {
                return ((Player) invocation.source()).getUniqueId();
            }
            return null;
        }

        @Override
        public boolean supportsNotify() {
            return plugin.notifyService() != null;
        }

        @Override
        public boolean isNotifyEnabled() {
            FleetNotifyService notify = plugin.notifyService();
            UUID id = senderId();
            return notify != null && id != null && notify.isEnabled(id);
        }

        @Override
        public boolean setNotifyEnabled(boolean enabled) {
            FleetNotifyService notify = plugin.notifyService();
            UUID id = senderId();
            if (notify == null || id == null) {
                throw new UnsupportedOperationException("notify");
            }
            return notify.setEnabled(id, enabled);
        }
    }
}
