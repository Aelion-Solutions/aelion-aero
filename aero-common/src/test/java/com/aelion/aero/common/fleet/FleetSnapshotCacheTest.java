package com.aelion.aero.common.fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aelion.aero.api.FleetServerSnapshot;
import com.aelion.aero.common.api.CreateGroupRequest;
import com.aelion.aero.common.api.CreateGroupResponse;
import com.aelion.aero.common.api.CreateServerRequest;
import com.aelion.aero.common.api.CreateServerResponse;
import com.aelion.aero.common.api.GroupInfoResponse;
import com.aelion.aero.common.api.PanelClient;
import com.aelion.aero.common.api.PanelHealthResponse;
import com.aelion.aero.common.api.SelfStatusRequest;
import com.aelion.aero.common.api.ServerInfoResponse;
import com.aelion.aero.common.config.AeroConfig;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FleetSnapshotCacheTest {

    @Test
    void listsAreEmptyUntilFirstSuccessfulRefresh() {
        RecordingClient client = new RecordingClient();
        client.servers = Collections.singletonList(server("s1", "alpha"));
        FleetSnapshotCache cache = cacheOf(client);

        assertTrue(cache.listServers().isEmpty());
        assertTrue(cache.listGroups().isEmpty());

        cache.refresh();

        assertEquals(1, cache.listServers().size());
        assertEquals("s1", cache.listServers().get(0).id());
        assertEquals(1, client.listServerCalls.get());
    }

    @Test
    void listReturnsLastSnapshotWhileRefreshIsBlocked() throws Exception {
        RecordingClient client = new RecordingClient();
        client.servers = Collections.singletonList(server("s1", "alpha"));
        FleetSnapshotCache cache = cacheOf(client);
        cache.refresh();
        assertEquals("s1", cache.listServers().get(0).id());

        client.servers = Collections.singletonList(server("s2", "beta"));
        client.block = true;
        Thread refresh = new Thread(cache::refresh, "fleet-refresh");
        refresh.start();
        assertTrue(client.entered.await(2, TimeUnit.SECONDS));

        List<FleetServerSnapshot> snapshot = cache.listServers();
        assertEquals(1, snapshot.size());
        assertEquals("s1", snapshot.get(0).id());

        client.release.countDown();
        refresh.join(2_000L);
        assertEquals("s2", cache.listServers().get(0).id());
    }

    @Test
    void secondRefreshIsSingleFlight() throws Exception {
        RecordingClient client = new RecordingClient();
        client.servers = Collections.singletonList(server("s1", "alpha"));
        client.block = true;
        FleetSnapshotCache cache = cacheOf(client);

        Thread first = new Thread(cache::refresh, "fleet-refresh-1");
        first.start();
        assertTrue(client.entered.await(2, TimeUnit.SECONDS));

        cache.refresh();
        assertEquals(1, client.listServerCalls.get());
        assertTrue(cache.listServers().isEmpty());

        client.release.countDown();
        first.join(2_000L);
        assertEquals(1, client.listServerCalls.get());
        assertEquals("s1", cache.listServers().get(0).id());
    }

    private static FleetSnapshotCache cacheOf(RecordingClient client) {
        return new FleetSnapshotCache(
                FleetSnapshotCacheTest::configured,
                config -> client,
                (message, thrown) -> {
                });
    }

    private static AeroConfig configured() {
        return new AeroConfig("https://panel.example", "sid", "token", AeroConfig.ControlConfig.disabled());
    }

    private static ServerInfoResponse server(String id, String name) {
        ServerInfoResponse response = new ServerInfoResponse();
        response.setId(id);
        response.setName(name);
        response.setProxyName(name);
        return response;
    }

    private static final class RecordingClient implements PanelClient {
        private volatile List<ServerInfoResponse> servers = Collections.emptyList();
        private volatile boolean block;
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger listServerCalls = new AtomicInteger();

        @Override
        public PanelHealthResponse ping() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ServerInfoResponse getServerInfo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ServerInfoResponse> listServers() {
            listServerCalls.incrementAndGet();
            waitIfBlocked();
            return servers;
        }

        @Override
        public List<GroupInfoResponse> listGroups() {
            waitIfBlocked();
            return Collections.emptyList();
        }

        @Override
        public CreateServerResponse createServer(CreateServerRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CreateGroupResponse createGroup(CreateGroupRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void postSelfStatus(SelfStatusRequest request) {
            throw new UnsupportedOperationException();
        }

        private void waitIfBlocked() {
            if (!block) {
                return;
            }
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("release timed out");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", e);
            }
        }
    }
}
