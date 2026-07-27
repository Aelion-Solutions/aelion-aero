package com.aelion.aero.velocity;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.Permissions;
import com.aelion.aero.common.command.AeroCommandService;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.BackendEntry;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
        return AeroCommandService.tabComplete(invocation.arguments(), true);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(suggest(invocation));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(Permissions.INFO)
                || invocation.source().hasPermission(Permissions.ADMIN)
                || invocation.source().hasPermission(Permissions.CREATE);
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
    }
}
