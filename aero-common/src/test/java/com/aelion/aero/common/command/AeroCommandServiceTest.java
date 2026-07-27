package com.aelion.aero.common.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.BackendEntry;
import com.aelion.aero.common.control.ProxyBackendRole;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class AeroCommandServiceTest {

    @Test
    void helpIncludesPrefixAndSubcommands() {
        RecordingPlatform platform = new RecordingPlatform(false);
        AeroCommandService.execute(new String[0], platform);
        assertFalse(platform.lines.isEmpty());
        assertTrue(platform.lines.get(0).contains("Aero"));
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("/ae help")));
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("servers list")));
        assertFalse(platform.lines.stream().anyMatch(l -> l.contains("create-server")));
    }

    @Test
    void proxyHelpIncludesCreateAndBackends() {
        RecordingPlatform platform = new RecordingPlatform(true);
        AeroCommandService.execute(new String[] {"help"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("create-server")));
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("backends")));
    }

    @Test
    void unknownSubcommandIsStyledError() {
        RecordingPlatform platform = new RecordingPlatform(false);
        AeroCommandService.execute(new String[] {"nope"}, platform);
        assertTrue(platform.lines.get(0).contains("Unknown subcommand"));
        assertTrue(platform.lines.get(0).contains(AeroCommandStyle.PREFIX)
                || platform.lines.get(0).contains("["));
    }

    @Test
    void pingWithoutConfigRepliesImmediately() {
        RecordingPlatform platform = new RecordingPlatform(false);
        AeroCommandService.execute(new String[] {"ping"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("not configured")));
        assertFalse(platform.asyncRan.get());
    }

    @Test
    void createServerRejectedOnPaper() {
        RecordingPlatform platform = new RecordingPlatform(false);
        AeroCommandService.execute(
                new String[] {"create-server", "name=Lobby", "software=paper", "version=1.21.4"},
                platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("proxy")));
        assertFalse(platform.asyncRan.get());
    }

    @Test
    void createServerRequiresXorArgsOnProxy() {
        RecordingPlatform platform = new RecordingPlatform(true);
        AeroCommandService.execute(new String[] {"create-server", "name=Lobby"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("Usage") || l.contains("template")));
        assertFalse(platform.asyncRan.get());
    }

    @Test
    void createServerRejectsMixedTemplateAndSoftware() {
        RecordingPlatform platform = new RecordingPlatform(true);
        AeroCommandService.execute(
                new String[] {
                    "create-server",
                    "name=Lobby",
                    "template=Lobby",
                    "software=paper",
                    "version=1.21.4"
                },
                platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("not both")));
        assertFalse(platform.asyncRan.get());
    }

    @Test
    void backendsRejectedOnPaper() {
        RecordingPlatform platform = new RecordingPlatform(false);
        AeroCommandService.execute(new String[] {"backends"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("proxy")));
    }

    @Test
    void backendsListsSnapshotOnProxy() {
        RecordingPlatform platform = new RecordingPlatform(true);
        platform.backends.add(new BackendEntry("lobby", "127.0.0.1:25565", ProxyBackendRole.LOBBY));
        AeroCommandService.execute(new String[] {"backends"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("lobby")));
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("Showing 1 backend")));
    }

    @Test
    void serversListRequiresListSubcommand() {
        RecordingPlatform platform = new RecordingPlatform(false);
        AeroCommandService.execute(new String[] {"servers"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("servers list")));
    }

    @Test
    void tabCompleteProxyIncludesCreate() {
        List<String> paper = AeroCommandService.tabComplete(new String[] {""}, false);
        List<String> proxy = AeroCommandService.tabComplete(new String[] {""}, true);
        assertFalse(paper.contains("create-server"));
        assertTrue(proxy.contains("create-server"));
        assertTrue(proxy.contains("backends"));
    }

    private static final class RecordingPlatform implements AeroCommandService.Platform {
        private final List<String> lines = new ArrayList<>();
        private final AtomicBoolean asyncRan = new AtomicBoolean(false);
        private final boolean proxy;
        private final List<BackendEntry> backends = new ArrayList<>();

        private RecordingPlatform(boolean proxy) {
            this.proxy = proxy;
        }

        @Override
        public void send(String legacyLine) {
            lines.add(legacyLine);
        }

        @Override
        public void sendAll(List<String> legacyLines) {
            lines.addAll(legacyLines);
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public void runAsync(Runnable task) {
            asyncRan.set(true);
            task.run();
        }

        @Override
        public void runSync(Runnable task) {
            task.run();
        }

        @Override
        public AeroConfig config() {
            return new AeroConfig("", "", "", AeroConfig.ControlConfig.disabled());
        }

        @Override
        public void reloadConfig() {
        }

        @Override
        public boolean isProxy() {
            return proxy;
        }

        @Override
        public List<BackendEntry> backendsSnapshot() {
            return List.copyOf(backends);
        }
    }
}
