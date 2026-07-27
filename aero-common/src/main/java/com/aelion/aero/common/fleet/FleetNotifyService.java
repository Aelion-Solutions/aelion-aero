package com.aelion.aero.common.fleet;

import com.aelion.aero.common.command.AeroCommandStyle;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.ControlFleetNotifyRequest;
import com.aelion.aero.common.util.Strings;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Session-scoped fleet change notifications for opted-in players on a proxy.
 *
 * <p>Events arrive via daemon {@code POST /v1/fleet-notify}; there is no panel poll.
 */
public final class FleetNotifyService {

    private final Supplier<AeroConfig> configSupplier;
    private final Delivery delivery;
    private final Set<UUID> subscribers = ConcurrentHashMap.newKeySet();

    public interface Delivery {
        /** Deliver one legacy (§) chat line to an online player. May be called off the main thread. */
        void deliver(UUID playerId, String legacyLine);
    }

    public FleetNotifyService(Supplier<AeroConfig> configSupplier, Delivery delivery) {
        this.configSupplier = configSupplier;
        this.delivery = delivery;
    }

    public boolean isEnabled(UUID playerId) {
        return playerId != null && subscribers.contains(playerId);
    }

    public boolean hasSubscribers() {
        return !subscribers.isEmpty();
    }

    /**
     * @return {@code true} if notify is now enabled for this player
     */
    public boolean setEnabled(UUID playerId, boolean enabled) {
        if (playerId == null) {
            return false;
        }
        if (enabled) {
            subscribers.add(playerId);
            return true;
        }
        removeSubscriber(playerId);
        return false;
    }

    public void removeSubscriber(UUID playerId) {
        if (playerId == null) {
            return;
        }
        subscribers.remove(playerId);
    }

    public void clear() {
        subscribers.clear();
    }

    /**
     * Formats and delivers push events to current subscribers.
     *
     * @return number of chat lines delivered (events × subscribers, after skip-self)
     */
    public int deliverPushed(List<ControlFleetNotifyRequest.ControlFleetNotifyEvent> events) {
        if (events == null || events.isEmpty() || !hasSubscribers()) {
            return 0;
        }
        AeroConfig config = configSupplier.get();
        String skip = config == null ? "" : config.serverId();
        if (skip == null) {
            skip = "";
        }

        Set<UUID> targets = new LinkedHashSet<>(subscribers);
        int delivered = 0;
        for (ControlFleetNotifyRequest.ControlFleetNotifyEvent event : events) {
            if (event == null) {
                continue;
            }
            String id = event.getId() == null ? "" : event.getId().trim();
            if (!skip.isEmpty() && skip.equals(id)) {
                continue;
            }
            String body = formatEvent(event);
            if (Strings.isBlank(body)) {
                continue;
            }
            String line = AeroCommandStyle.info(body);
            for (UUID playerId : targets) {
                delivery.deliver(playerId, line);
                delivered++;
            }
        }
        return delivered;
    }

    static String formatEvent(ControlFleetNotifyRequest.ControlFleetNotifyEvent event) {
        String status = FleetNotifySnapshot.normalizeStatus(event.getStatus());
        String verb;
        if ("removed".equals(status) || "deleted".equals(status)) {
            verb = "removed";
        } else {
            verb = FleetNotifyDiff.verbForStatus(status, false);
        }
        if (verb == null) {
            return null;
        }
        String name = Strings.isNotBlank(event.getName())
                ? event.getName().trim()
                : (Strings.isBlank(event.getId()) ? "-" : event.getId().trim());
        boolean scaled = Strings.isNotBlank(event.getGroupId()) || Strings.isNotBlank(event.getGroupName());
        String groupName = Strings.isNotBlank(event.getGroupName())
                ? event.getGroupName().trim()
                : (Strings.isBlank(event.getGroupId()) ? "" : event.getGroupId().trim());
        return FleetNotifyDiff.formatLine(name, verb, scaled, groupName);
    }
}
