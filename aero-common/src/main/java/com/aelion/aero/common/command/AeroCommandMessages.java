package com.aelion.aero.common.command;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.AeroVersion;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.BackendEntry;
import com.aelion.aero.common.util.Strings;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared operator-facing strings for /ae (proxy) and /aes (backend) commands (legacy § colors + prefix).
 */
public final class AeroCommandMessages {

    private AeroCommandMessages() {
    }

    public static List<String> help(boolean proxy) {
        String cmd = AeroConstants.commandRoot(proxy);
        List<String> lines = new ArrayList<>();
        lines.add(AeroCommandStyle.success(AeroConstants.NAME + " §fv" + AeroVersion.VERSION));
        lines.add(AeroCommandStyle.info("/" + cmd + " help §8— §7show this help"));
        lines.add(AeroCommandStyle.info("/" + cmd + " info §8— §7plugin and panel status"));
        lines.add(AeroCommandStyle.info("/" + cmd + " reload §8— §7reload config"));
        lines.add(AeroCommandStyle.info("/" + cmd + " ping §8— §7ping the Aelion Cloud panel"));
        lines.add(AeroCommandStyle.info("/" + cmd + " servers list [--names] §8— §7list panel servers"));
        if (proxy) {
            lines.add(AeroCommandStyle.info("/" + cmd + " backends §8— §7list live proxy backends"));
            lines.add(AeroCommandStyle.info(
                    "/" + cmd + " create-server name=<n> template=<tpl> §8— §7or software=+version="));
        }
        return lines;
    }

    public static List<String> info(AeroConfig config) {
        List<String> lines = new ArrayList<>();
        lines.add(AeroCommandStyle.success(AeroConstants.NAME + " §fv" + AeroVersion.VERSION));
        lines.add(AeroCommandStyle.label(
                "server-id",
                config.serverId().isEmpty() ? "(not set)" : config.serverId()));
        lines.add(AeroCommandStyle.label(
                "panel",
                config.panelHostForDisplay().isEmpty() ? "(not set)" : config.panelHostForDisplay()));
        lines.add(AeroCommandStyle.label("panel configured", String.valueOf(config.isPanelConfigured())));
        lines.add(AeroCommandStyle.label(
                "panel insecure ssl", String.valueOf(config.panelInsecureSsl())));
        lines.add(AeroCommandStyle.label(
                "control",
                config.control().enabled()
                        ? "enabled (" + config.control().bind() + ":" + config.control().port() + ")"
                        : "disabled"));
        return lines;
    }

    public static String noPermission() {
        return AeroCommandStyle.error("You do not have permission for that.");
    }

    public static String unknownSubcommand(String sub, boolean proxy) {
        return AeroCommandStyle.error(
                "Unknown subcommand §f" + sub + "§c. Try §f/"
                        + AeroConstants.commandRoot(proxy) + " help§c.");
    }

    public static String proxyOnly(String feature) {
        return AeroCommandStyle.warn(feature + " is only available on proxy servers.");
    }

    public static String reloaded() {
        return AeroCommandStyle.success("Config reloaded.");
    }

    public static String reloadFailed(String detail) {
        return AeroCommandStyle.error("Reload failed: " + nullToMessage(detail));
    }

    public static String panelNotConfigured() {
        return AeroCommandStyle.warn(
                "Panel not configured. Set §fpanel-url§e, §fserver-id§e, and §ftoken§e in config.yml.");
    }

    public static String pingOk(boolean ok, String version) {
        StringBuilder sb = new StringBuilder("Panel health ok=").append(ok);
        if (Strings.isNotBlank(version)) {
            sb.append(" version=").append(version);
        }
        return AeroCommandStyle.success(sb.toString());
    }

    public static String pingReachable(String name, String status) {
        return AeroCommandStyle.success(
                "Panel reachable. server=" + nullToDash(name) + " status=" + nullToDash(status));
    }

    public static String pingHttpError(int statusCode, String bodySummary) {
        String detail = Strings.isBlank(bodySummary)
                ? ""
                : " — " + truncate(bodySummary, 120);
        return AeroCommandStyle.error("Panel error HTTP " + statusCode + detail);
    }

    public static String pingFailed(String detail) {
        return AeroCommandStyle.error("Panel ping failed: " + nullToMessage(detail));
    }

    public static String createUsageServer() {
        return AeroCommandStyle.warn(
                "Usage: /" + AeroConstants.COMMAND_PROXY_PRIMARY
                        + " create-server name=<name> template=<template>"
                        + " §8|§e name=<name> software=<sw> version=<ver> [memory=] [role=backend]");
    }

    public static String createServerOk(String id, String name, String status) {
        return AeroCommandStyle.success("Created server §f" + name + "§a (" + id + ") status=" + status);
    }

    public static String serversUsage(boolean proxy) {
        return AeroCommandStyle.warn(
                "Usage: /" + AeroConstants.commandRoot(proxy) + " servers list [--names]");
    }

    public static String backendsUsage() {
        return AeroCommandStyle.warn(
                "Usage: /" + AeroConstants.COMMAND_PROXY_PRIMARY + " backends [list]");
    }

    public static List<String> formatServerList(
            List<com.aelion.aero.common.api.ServerInfoResponse> servers, boolean namesOnly) {
        List<String> lines = new ArrayList<>();
        if (servers == null || servers.isEmpty()) {
            lines.add(AeroCommandStyle.info("No servers found."));
            lines.add(AeroCommandStyle.info("Showing 0 server(s)"));
            return lines;
        }
        for (com.aelion.aero.common.api.ServerInfoResponse server : servers) {
            String name = nullToDash(server.getName());
            String id = nullToDash(server.getId());
            if (namesOnly) {
                lines.add(AeroCommandStyle.info(name + " §8|§7 " + id));
            } else {
                String status = nullToDash(server.getLiveStatus() != null ? server.getLiveStatus() : server.getStatus());
                String software = nullToDash(server.getSoftware());
                lines.add(AeroCommandStyle.info(
                        name + " §8|§7 " + status + " §8|§7 " + software + " §8|§7 " + id));
            }
        }
        lines.add(AeroCommandStyle.info("Showing " + servers.size() + " server(s)"));
        return lines;
    }

    public static List<String> formatBackendList(List<BackendEntry> backends) {
        List<String> lines = new ArrayList<>();
        if (backends == null || backends.isEmpty()) {
            lines.add(AeroCommandStyle.info("No live backends."));
            lines.add(AeroCommandStyle.info("Showing 0 backend(s)"));
            return lines;
        }
        for (BackendEntry entry : backends) {
            String role = entry.getRole() == null ? "backend" : entry.getRole().wire();
            lines.add(AeroCommandStyle.info(
                    nullToDash(entry.getName())
                            + " §8|§7 "
                            + nullToDash(entry.getAddress())
                            + " §8|§7 "
                            + role));
        }
        lines.add(AeroCommandStyle.info("Showing " + backends.size() + " backend(s)"));
        return lines;
    }

    private static String nullToDash(String value) {
        return Strings.isBlank(value) ? "-" : value;
    }

    private static String nullToMessage(String detail) {
        return Strings.isBlank(detail) ? "unknown error" : detail;
    }

    private static String truncate(String body, int max) {
        String trimmed = body.replace('\n', ' ').trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
    }
}
