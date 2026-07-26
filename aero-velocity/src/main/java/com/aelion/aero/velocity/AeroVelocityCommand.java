package com.aelion.aero.velocity;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.Permissions;
import com.aelion.aero.common.api.HttpPanelClient;
import com.aelion.aero.common.api.PanelApiException;
import com.aelion.aero.common.api.PanelHealthResponse;
import com.aelion.aero.common.api.PanelNotConfiguredException;
import com.aelion.aero.common.api.ServerInfoResponse;
import com.aelion.aero.common.command.AeroCommandMessages;
import com.aelion.aero.common.config.AeroConfig;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;

final class AeroVelocityCommand implements SimpleCommand {

    private final AeroVelocityPlugin plugin;

    AeroVelocityCommand(AeroVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        var source = invocation.source();

        switch (sub) {
            case "help" -> source.sendMessage(Component.text(AeroCommandMessages.help()));
            case "info" -> {
                if (!source.hasPermission(Permissions.INFO)) {
                    source.sendMessage(Component.text("No permission."));
                    return;
                }
                source.sendMessage(Component.text(AeroCommandMessages.info(plugin.aeroConfig())));
            }
            case "reload" -> {
                if (!source.hasPermission(Permissions.ADMIN)) {
                    source.sendMessage(Component.text("No permission."));
                    return;
                }
                try {
                    plugin.reloadAeroConfig();
                    source.sendMessage(Component.text("Aelion Aero config reloaded."));
                } catch (Exception e) {
                    source.sendMessage(Component.text("Reload failed: " + e.getMessage()));
                }
            }
            case "ping" -> {
                if (!source.hasPermission(Permissions.INFO)) {
                    source.sendMessage(Component.text("No permission."));
                    return;
                }
                ping(source);
            }
            default -> source.sendMessage(Component.text("Unknown subcommand. Try /ae help"));
        }
    }

    private void ping(com.velocitypowered.api.command.CommandSource source) {
        AeroConfig config = plugin.aeroConfig();
        if (!config.isPanelConfigured()) {
            source.sendMessage(Component.text("Panel not configured (set panel-url, server-id, token)."));
            return;
        }
        HttpPanelClient client = new HttpPanelClient(config);
        try {
            PanelHealthResponse health = client.ping();
            source.sendMessage(Component.text("Panel health ok=" + health.isOk()
                    + (health.getVersion() == null ? "" : " version=" + health.getVersion())));
        } catch (PanelNotConfiguredException e) {
            source.sendMessage(Component.text("Panel not configured."));
        } catch (PanelApiException e) {
            try {
                ServerInfoResponse info = client.getServerInfo();
                source.sendMessage(Component.text("Panel reachable. server=" + nullToDash(info.getName())
                        + " status=" + nullToDash(info.getStatus())));
            } catch (PanelApiException nested) {
                source.sendMessage(Component.text(
                        "Panel error HTTP " + nested.statusCode() + " (routes may not be live yet)."));
            }
        }
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return List.of("help", "info", "reload", "ping").stream()
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(Permissions.INFO)
                || invocation.source().hasPermission(Permissions.ADMIN);
    }

    static void register(AeroVelocityPlugin plugin) {
        var meta = plugin.proxy().getCommandManager().metaBuilder(AeroConstants.COMMAND_PRIMARY)
                .aliases(AeroConstants.COMMAND_ALIAS)
                .plugin(plugin)
                .build();
        plugin.proxy().getCommandManager().register(meta, new AeroVelocityCommand(plugin));
    }
}
