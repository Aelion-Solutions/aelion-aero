package com.aelion.aero.common.control;

import com.aelion.aero.common.util.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

/**
 * Pure connection-tactic selection for proxy initial join (and related picks).
 * Mirrors panel pickJoinableMember: fill_first, balance, random, round_robin
 * (lowest_ping maps to balance).
 */
public final class ConnectionTactics {

    private ConnectionTactics() {
    }

    /**
     * One join candidate with group CT metadata and live-ish player counts.
     */
    public static final class Candidate {
        private final String name;
        private final String groupId;
        private final String playerDistribution;
        private final int currentPlayers;
        private final int maxPlayers;
        private final boolean joinable;
        private final boolean hasJoinable;
        private final ProxyBackendRole role;

        public Candidate(
                String name,
                String groupId,
                String playerDistribution,
                int currentPlayers,
                int maxPlayers,
                boolean joinable,
                boolean hasJoinable,
                ProxyBackendRole role
        ) {
            this.name = name == null ? "" : name;
            this.groupId = groupId == null ? "" : groupId.trim();
            this.playerDistribution = playerDistribution == null ? "" : playerDistribution.trim();
            this.role = role == null ? ProxyBackendRole.BACKEND : role;
            this.maxPlayers = maxPlayers < 1 ? 1 : maxPlayers;
            this.currentPlayers = currentPlayers < 0 ? 0 : currentPlayers;
            this.joinable = joinable;
            this.hasJoinable = hasJoinable;
        }

        public String name() {
            return name;
        }

        public String groupId() {
            return groupId;
        }

        public String playerDistribution() {
            return playerDistribution;
        }

        public int currentPlayers() {
            return currentPlayers;
        }

        public int maxPlayers() {
            return maxPlayers;
        }

        public boolean joinable() {
            return joinable;
        }

        public boolean hasJoinable() {
            return hasJoinable;
        }

        public ProxyBackendRole role() {
            return role;
        }
    }

    /**
     * In-memory round-robin counters keyed by group id (or strategy scope).
     */
    public static final class RoundRobinState {
        private final Map<String, AtomicInteger> counters = new HashMap<String, AtomicInteger>();

        public synchronized int nextIndex(String key, int size) {
            if (size <= 0) {
                return 0;
            }
            String scope = Strings.isBlank(key) ? "_" : key;
            AtomicInteger counter = counters.get(scope);
            if (counter == null) {
                counter = new AtomicInteger();
                counters.put(scope, counter);
            }
            int value = counter.getAndIncrement();
            int mod = value % size;
            return mod < 0 ? mod + size : mod;
        }
    }

    /**
     * Pick the best lobby/try server name using group CT when metadata is present.
     *
     * @param candidates registry backends that are currently registered on the proxy
     * @param excludeName sanitized name to skip (evacuating server), or blank
     * @param rr round-robin state (may be null → balance fallback for RR)
     * @return chosen backend name, or null if none
     */
    public static String pickInitialServer(
            List<Candidate> candidates,
            String excludeName,
            RoundRobinState rr
    ) {
        return pickInitialServer(candidates, excludeName, rr, ThreadLocalRandom.current());
    }

    static String pickInitialServer(
            List<Candidate> candidates,
            String excludeName,
            RoundRobinState rr,
            Random random
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        String exclude = sanitize(excludeName);

        List<Candidate> lobbies = filterRole(candidates, ProxyBackendRole.LOBBY, exclude);
        String picked = pickAmongGrouped(lobbies, rr, random);
        if (picked != null) {
            return picked;
        }

        List<Candidate> tryRole = filterRole(candidates, ProxyBackendRole.TRY, exclude);
        picked = pickAmongGrouped(tryRole, rr, random);
        if (picked != null) {
            return picked;
        }

        if (!lobbies.isEmpty()) {
            return lobbies.get(0).name();
        }
        if (!tryRole.isEmpty()) {
            return tryRole.get(0).name();
        }
        for (Candidate c : candidates) {
            if (c == null) {
                continue;
            }
            String name = sanitize(c.name());
            if (!name.isEmpty() && !name.equals(exclude)) {
                return c.name();
            }
        }
        return null;
    }

    private static String pickAmongGrouped(List<Candidate> roleCandidates, RoundRobinState rr, Random random) {
        if (roleCandidates.isEmpty()) {
            return null;
        }
        Map<String, List<Candidate>> byGroup = new HashMap<String, List<Candidate>>();
        for (Candidate c : roleCandidates) {
            if (Strings.isBlank(c.groupId())) {
                continue;
            }
            List<Candidate> bucket = byGroup.get(c.groupId());
            if (bucket == null) {
                bucket = new ArrayList<Candidate>();
                byGroup.put(c.groupId(), bucket);
            }
            bucket.add(c);
        }
        if (byGroup.isEmpty()) {
            return null;
        }
        List<String> groupIds = new ArrayList<String>(byGroup.keySet());
        Collections.sort(groupIds);
        String groupId = groupIds.get(0);
        List<Candidate> members = byGroup.get(groupId);
        if (members == null || members.isEmpty()) {
            return null;
        }
        String strategy = "balance";
        for (Candidate m : members) {
            if (Strings.isNotBlank(m.playerDistribution())) {
                strategy = m.playerDistribution();
                break;
            }
        }
        Candidate chosen = pickMember(members, strategy, groupId, rr, random);
        return chosen == null ? null : chosen.name();
    }

    /**
     * Select one member with the given strategy (same rules as panel pickJoinableMember).
     */
    public static Candidate pickMember(
            List<Candidate> members,
            String strategy,
            String groupId,
            RoundRobinState rr,
            Random random
    ) {
        if (members == null || members.isEmpty()) {
            return null;
        }
        List<Candidate> joinable = new ArrayList<Candidate>();
        for (Candidate m : members) {
            if (m == null) {
                continue;
            }
            if (!m.hasJoinable() || m.joinable()) {
                joinable.add(m);
            }
        }
        if (joinable.isEmpty()) {
            joinable = new ArrayList<Candidate>(members);
        }
        String tactic = strategy == null ? "" : strategy.trim().toLowerCase(Locale.ROOT);
        if ("fill_first".equals(tactic)) {
            Collections.sort(joinable, new Comparator<Candidate>() {
                @Override
                public int compare(Candidate a, Candidate b) {
                    int byPlayers = Integer.compare(b.currentPlayers(), a.currentPlayers());
                    if (byPlayers != 0) {
                        return byPlayers;
                    }
                    return String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name());
                }
            });
            return joinable.get(0);
        }
        if ("random".equals(tactic)) {
            return joinable.get(random.nextInt(joinable.size()));
        }
        if ("round_robin".equals(tactic) && rr != null) {
            List<Candidate> ordered = new ArrayList<Candidate>(joinable);
            Collections.sort(ordered, new Comparator<Candidate>() {
                @Override
                public int compare(Candidate a, Candidate b) {
                    return sanitize(a.name()).compareTo(sanitize(b.name()));
                }
            });
            return ordered.get(rr.nextIndex(groupId, ordered.size()));
        }
        Collections.sort(joinable, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                int byPlayers = Integer.compare(a.currentPlayers(), b.currentPlayers());
                if (byPlayers != 0) {
                    return byPlayers;
                }
                return String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name());
            }
        });
        return joinable.get(0);
    }

    /**
     * Build candidates from registry entries, overlaying live player counts when provided.
     */
    public static List<Candidate> candidatesFrom(
            List<BackendEntry> entries,
            ToIntFunction<String> livePlayerCount
    ) {
        List<Candidate> out = new ArrayList<Candidate>();
        if (entries == null) {
            return out;
        }
        for (BackendEntry entry : entries) {
            if (entry == null || !entry.isValid()) {
                continue;
            }
            String name = entry.getName();
            int live = livePlayerCount == null ? -1 : livePlayerCount.applyAsInt(name);
            int current = live >= 0 ? live : entry.getCurrentPlayers();
            int max = entry.getMaxPlayers() > 0 ? entry.getMaxPlayers() : 1;
            boolean hasJoinable = entry.hasJoinable();
            boolean joinable = hasJoinable ? entry.isJoinable() : current < max;
            out.add(new Candidate(
                    name,
                    entry.getGroupId(),
                    entry.getPlayerDistribution(),
                    current,
                    max,
                    joinable,
                    hasJoinable || live >= 0 || entry.getMaxPlayers() > 0,
                    entry.getRole()
            ));
        }
        return out;
    }

    private static List<Candidate> filterRole(List<Candidate> all, ProxyBackendRole role, String exclude) {
        List<Candidate> out = new ArrayList<Candidate>();
        for (Candidate c : all) {
            if (c == null || c.role() != role) {
                continue;
            }
            String name = sanitize(c.name());
            if (name.isEmpty() || name.equals(exclude)) {
                continue;
            }
            out.add(c);
        }
        return out;
    }

    private static String sanitize(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "");
    }
}
