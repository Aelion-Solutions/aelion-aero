package com.aelion.aero.bukkit;

import com.aelion.aero.common.control.ControlPlayerEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Join/quit roster updated on the primary thread; readable from any thread.
 */
final class OnlineRoster implements Listener {

    private final ConcurrentHashMap<UUID, String> players = new ConcurrentHashMap<UUID, String>();
    private final AtomicInteger maxPlayers = new AtomicInteger();

    OnlineRoster(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        seed();
    }

    private void seed() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null) {
                players.put(player.getUniqueId(), player.getName());
            }
        }
        maxPlayers.set(Bukkit.getMaxPlayers());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player != null) {
            players.put(player.getUniqueId(), player.getName());
        }
        maxPlayers.set(Bukkit.getMaxPlayers());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player != null) {
            players.remove(player.getUniqueId());
        }
        maxPlayers.set(Bukkit.getMaxPlayers());
    }

    int size() {
        return players.size();
    }

    int maxPlayers() {
        return maxPlayers.get();
    }

    List<ControlPlayerEntry> snapshot() {
        List<ControlPlayerEntry> out = new ArrayList<ControlPlayerEntry>(players.size());
        for (Map.Entry<UUID, String> entry : players.entrySet()) {
            out.add(new ControlPlayerEntry(entry.getKey().toString(), entry.getValue()));
        }
        return out;
    }
}
