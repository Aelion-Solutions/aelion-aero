package com.aelion.aero.bukkit;

import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Tracks the live server-list MOTD (base {@code getMotd()} + {@link ServerListPingEvent} mutations).
 */
public final class MotdTracker implements Listener {

    private final AtomicReference<String> motd = new AtomicReference<String>("");

    public MotdTracker(JavaPlugin plugin) {
        refreshFromServer();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public String motd() {
        String current = motd.get();
        return current == null ? "" : current;
    }

    public void refreshFromServer() {
        try {
            String base = Bukkit.getServer().getMotd();
            if (base != null) {
                motd.set(base);
            }
        } catch (RuntimeException ignored) {
            // Some legacy forks may not expose getMotd safely
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPing(ServerListPingEvent event) {
        String pingMotd = event.getMotd();
        if (pingMotd != null) {
            motd.set(pingMotd);
        }
    }
}
