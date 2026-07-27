package com.aelion.aero.bungee;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.Permissions;
import com.aelion.aero.common.api.HttpPanelClient;
import com.aelion.aero.common.api.PanelApiException;
import com.aelion.aero.common.api.PanelHealthResponse;
import com.aelion.aero.common.api.PanelNotConfiguredException;
import com.aelion.aero.common.api.ServerInfoResponse;
import com.aelion.aero.common.command.AeroCommandMessages;
import com.aelion.aero.common.config.AeroConfig;
import java.util.Locale;
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
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sender.sendMessage(TextComponent.fromLegacy(AeroCommandMessages.help()));
            case "info" -> {
                if (!sender.hasPermission(Permissions.INFO)) {
                    sender.sendMessage(TextComponent.fromLegacy("No permission."));
                    return;
                }
                sender.sendMessage(TextComponent.fromLegacy(AeroCommandMessages.info(plugin.aeroConfig())));
            }
            case "reload" -> {
                if (!sender.hasPermission(Permissions.ADMIN)) {
                    sender.sendMessage(TextComponent.fromLegacy("No permission."));
                    return;
                }
                try {
                    plugin.reloadAeroConfig();
                    sender.sendMessage(TextComponent.fromLegacy("Aelion Aero config reloaded."));
                } catch (Exception e) {
                    sender.sendMessage(TextComponent.fromLegacy("Reload failed: " + e.getMessage()));
                }
            }
            case "ping" -> {
                if (!sender.hasPermission(Permissions.INFO)) {
                    sender.sendMessage(TextComponent.fromLegacy("No permission."));
                    return;
                }
                ping(sender);
            }
            default -> sender.sendMessage(TextComponent.fromLegacy("Unknown subcommand. Try /ae help"));
        }
    }

    private void ping(CommandSender sender) {
        AeroConfig config = plugin.aeroConfig();
        if (!config.isPanelConfigured()) {
            sender.sendMessage(TextComponent.fromLegacy(
                    "Panel not configured (set panel-url, server-id, token)."));
            return;
        }
        HttpPanelClient client = new HttpPanelClient(config);
        try {
            PanelHealthResponse health = client.ping();
            sender.sendMessage(TextComponent.fromLegacy("Panel health ok=" + health.isOk()
                    + (health.getVersion() == null ? "" : " version=" + health.getVersion())));
        } catch (PanelNotConfiguredException e) {
            sender.sendMessage(TextComponent.fromLegacy("Panel not configured."));
        } catch (PanelApiException e) {
            try {
                ServerInfoResponse info = client.getServerInfo();
                sender.sendMessage(TextComponent.fromLegacy("Panel reachable. server="
                        + nullToDash(info.getName()) + " status=" + nullToDash(info.getStatus())));
            } catch (PanelApiException nested) {
                sender.sendMessage(TextComponent.fromLegacy(
                        "Panel error HTTP " + nested.statusCode() + " (routes may not be live yet)."));
            }
        }
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return java.util.List.of("help", "info", "reload", "ping").stream()
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        return java.util.List.of();
    }
}
