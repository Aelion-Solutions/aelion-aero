package com.aelion.aero.paper;

import com.aelion.aero.common.Permissions;
import com.aelion.aero.common.api.HttpPanelClient;
import com.aelion.aero.common.api.PanelApiException;
import com.aelion.aero.common.api.PanelHealthResponse;
import com.aelion.aero.common.api.PanelNotConfiguredException;
import com.aelion.aero.common.api.ServerInfoResponse;
import com.aelion.aero.common.command.AeroCommandMessages;
import com.aelion.aero.common.config.AeroConfig;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

final class AeroCommandExecutor implements CommandExecutor, TabCompleter {

    private final AeroPaperPlugin plugin;

    AeroCommandExecutor(AeroPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "help" -> {
                sender.sendMessage(AeroCommandMessages.help());
                yield true;
            }
            case "info" -> {
                if (!sender.hasPermission(Permissions.INFO)) {
                    sender.sendMessage("No permission.");
                    yield true;
                }
                sender.sendMessage(AeroCommandMessages.info(plugin.aeroConfig()));
                yield true;
            }
            case "reload" -> {
                if (!sender.hasPermission(Permissions.ADMIN)) {
                    sender.sendMessage("No permission.");
                    yield true;
                }
                plugin.reloadAeroConfig();
                sender.sendMessage("Aelion Aero config reloaded.");
                yield true;
            }
            case "ping" -> {
                if (!sender.hasPermission(Permissions.INFO)) {
                    sender.sendMessage("No permission.");
                    yield true;
                }
                ping(sender);
                yield true;
            }
            default -> {
                sender.sendMessage("Unknown subcommand. Try /ae help");
                yield true;
            }
        };
    }

    private void ping(CommandSender sender) {
        AeroConfig config = plugin.aeroConfig();
        if (!config.isPanelConfigured()) {
            sender.sendMessage("Panel not configured (set panel-url, server-id, token).");
            return;
        }
        HttpPanelClient client = new HttpPanelClient(config);
        try {
            PanelHealthResponse health = client.ping();
            sender.sendMessage("Panel health ok=" + health.isOk()
                    + (health.getVersion() == null ? "" : " version=" + health.getVersion()));
        } catch (PanelNotConfiguredException e) {
            sender.sendMessage("Panel not configured.");
        } catch (PanelApiException e) {
            try {
                ServerInfoResponse info = client.getServerInfo();
                sender.sendMessage("Panel reachable. server=" + nullToDash(info.getName())
                        + " status=" + nullToDash(info.getStatus()));
            } catch (PanelApiException nested) {
                sender.sendMessage("Panel error HTTP " + nested.statusCode() + " (routes may not be live yet).");
            }
        }
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("help", "info", "reload", "ping").stream()
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
