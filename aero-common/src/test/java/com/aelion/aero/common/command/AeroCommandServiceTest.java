package com.aelion.aero.common.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.BackendEntry;
import com.aelion.aero.common.control.ProxyBackendRole;
import java.util.ArrayList;
import java.util.Collections;
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
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("/aes help")));
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("servers list")));
        assertFalse(platform.lines.stream().anyMatch(l -> l.contains("create-server")));
    }

    @Test
    void proxyHelpIncludesCreateAndBackends() {
        RecordingPlatform platform = new RecordingPlatform(true);
        AeroCommandService.execute(new String[] {"help"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("/ae help")));
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("create-server")));
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("backends")));
    }

    @Test
    void unknownSubcommandIsStyledError() {
        RecordingPlatform platform = new RecordingPlatform(false);
        AeroCommandService.execute(new String[] {"nope"}, platform);
        assertTrue(platform.lines.get(0).contains("Unknown subcommand"));
        assertTrue(platform.lines.get(0).contains("/aes help"));
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
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("/aes servers list")));
    }

    @Test
    void helpIncludesKickAndTransfer() {
        RecordingPlatform platform = new RecordingPlatform(false);
        AeroCommandService.execute(new String[] {"help"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("kick")));
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("transfer")));
    }

    @Test
    void kickRequiresPlayerArg() {
        RecordingPlatform platform = new RecordingPlatform(false);
        AeroCommandService.execute(new String[] {"kick"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("Usage") && l.contains("kick")));
    }

    @Test
    void transferRequiresXorTarget() {
        RecordingPlatform platform = new RecordingPlatform(false);
        AeroCommandService.execute(new String[] {"transfer", "Steve"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("Usage") && l.contains("transfer")));
    }

    @Test
    void tabCompleteIncludesKickTransfer() {
        List<String> paper = AeroCommandService.tabComplete(new String[] {""}, false);
        assertTrue(paper.contains("kick"));
        assertTrue(paper.contains("transfer"));
    }

    @Test
    void helpIncludesNotifyOnProxyOnly() {
        RecordingPlatform paper = new RecordingPlatform(false);
        AeroCommandService.execute(new String[] {"help"}, paper);
        assertFalse(paper.lines.stream().anyMatch(l -> l.contains("notify")));

        RecordingPlatform proxy = new RecordingPlatform(true);
        AeroCommandService.execute(new String[] {"help"}, proxy);
        assertTrue(proxy.lines.stream().anyMatch(l -> l.contains("notify")));
    }

    @Test
    void notifyRejectedOnBackend() {
        RecordingPlatform platform = new RecordingPlatform(false);
        platform.supportsNotify = true;
        platform.senderUuid = java.util.UUID.randomUUID();
        AeroCommandService.execute(new String[] {"notify"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("proxy")));
    }

    @Test
    void notifyRequiresPlayer() {
        RecordingPlatform platform = new RecordingPlatform(true);
        platform.supportsNotify = true;
        AeroCommandService.execute(new String[] {"notify"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("players") || l.contains("console")));
    }

    @Test
    void notifyTogglesForPlayer() {
        RecordingPlatform platform = new RecordingPlatform(true);
        platform.supportsNotify = true;
        platform.senderUuid = java.util.UUID.randomUUID();
        AeroCommandService.execute(new String[] {"notify"}, platform);
        assertTrue(platform.notifyEnabled);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("on")));
        AeroCommandService.execute(new String[] {"notify", "off"}, platform);
        assertFalse(platform.notifyEnabled);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("off")));
    }

    @Test
    void tabCompleteIncludesNotifyOnProxyOnly() {
        List<String> paper = AeroCommandService.tabComplete(new String[] {""}, false);
        assertFalse(paper.contains("notify"));
        List<String> proxy = AeroCommandService.tabComplete(new String[] {""}, true);
        assertTrue(proxy.contains("notify"));
        List<String> modes = AeroCommandService.tabComplete(new String[] {"notify", ""}, true);
        assertTrue(modes.contains("on"));
        assertTrue(modes.contains("off"));
    }

    @Test
    void helpDeniedWithoutPermissions() {
        RecordingPlatform platform = new RecordingPlatform(false);
        platform.permissions.clear();
        AeroCommandService.execute(new String[] {"help"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("permission")));
    }

    @Test
    void reloadDeniedWithInfoOnly() {
        RecordingPlatform platform = new RecordingPlatform(false);
        platform.grantInfoOnly();
        AeroCommandService.execute(new String[] {"reload"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("permission")));
        assertFalse(platform.lines.stream().anyMatch(l -> l.contains("reloaded") || l.contains("Reloaded")));
    }

    @Test
    void kickDeniedWithInfoOnly() {
        RecordingPlatform platform = new RecordingPlatform(false);
        platform.grantInfoOnly();
        AeroCommandService.execute(new String[] {"kick", "Steve"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("permission")));
    }

    @Test
    void adminOnlyCanRunInfo() {
        RecordingPlatform platform = new RecordingPlatform(false);
        platform.grantAdminOnly();
        AeroCommandService.execute(new String[] {"info"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("server-id")));
        assertFalse(platform.lines.stream().anyMatch(l -> l.contains("permission")));
    }

    @Test
    void helpFiltersAdminVerbsForInfoOnly() {
        RecordingPlatform platform = new RecordingPlatform(false);
        platform.grantInfoOnly();
        AeroCommandService.execute(new String[] {"help"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("info")));
        assertFalse(platform.lines.stream().anyMatch(l -> l.contains("reload")));
        assertFalse(platform.lines.stream().anyMatch(l -> l.contains("kick")));
        assertFalse(platform.lines.stream().anyMatch(l -> l.contains("transfer")));
    }

    @Test
    void createDeniedWithoutCreate() {
        RecordingPlatform platform = new RecordingPlatform(true);
        platform.grantInfoOnly();
        AeroCommandService.execute(
                new String[] {"create-server", "name=Lobby", "software=paper", "version=1.21.4"},
                platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("permission")));
        assertFalse(platform.asyncRan.get());
    }

    @Test
    void createAllowedWithCreateOnly() {
        RecordingPlatform platform = new RecordingPlatform(true);
        platform.grantCreateOnly();
        AeroCommandService.execute(new String[] {"create-server", "name=Lobby"}, platform);
        assertTrue(platform.lines.stream().anyMatch(l -> l.contains("Usage") || l.contains("template")));
        assertFalse(platform.lines.stream().anyMatch(l -> l.contains("permission")));
    }

    @Test
    void tabCompleteFiltersByPermission() {
        List<String> infoOnly = AeroCommandService.tabComplete(
                new String[] {""}, false, Collections.emptyList(), true, false, false);
        assertTrue(infoOnly.contains("info"));
        assertFalse(infoOnly.contains("reload"));
        assertFalse(infoOnly.contains("kick"));

        List<String> admin = AeroCommandService.tabComplete(
                new String[] {""}, false, Collections.emptyList(), true, true, false);
        assertTrue(admin.contains("reload"));
        assertTrue(admin.contains("kick"));

        List<String> createProxy = AeroCommandService.tabComplete(
                new String[] {""}, true, Collections.emptyList(), true, false, true);
        assertTrue(createProxy.contains("create-server"));
        assertFalse(createProxy.contains("reload"));
    }

    private static final class RecordingPlatform implements AeroCommandService.Platform {
        private final List<String> lines = new ArrayList<>();
        private final AtomicBoolean asyncRan = new AtomicBoolean(false);
        private final boolean proxy;
        private final List<BackendEntry> backends = new ArrayList<>();
        private final java.util.Set<String> permissions = new java.util.HashSet<>();
        private boolean supportsNotify;
        private java.util.UUID senderUuid;
        private boolean notifyEnabled;

        private RecordingPlatform(boolean proxy) {
            this.proxy = proxy;
            grantAll();
        }

        private void grantAll() {
            permissions.clear();
            permissions.add(com.aelion.aero.common.Permissions.INFO);
            permissions.add(com.aelion.aero.common.Permissions.ADMIN);
            permissions.add(com.aelion.aero.common.Permissions.CREATE);
        }

        private void grantInfoOnly() {
            permissions.clear();
            permissions.add(com.aelion.aero.common.Permissions.INFO);
        }

        private void grantAdminOnly() {
            permissions.clear();
            permissions.add(com.aelion.aero.common.Permissions.ADMIN);
        }

        private void grantCreateOnly() {
            permissions.clear();
            permissions.add(com.aelion.aero.common.Permissions.CREATE);
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
            return permissions.contains(permission);
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
            return new ArrayList<>(backends);
        }

        @Override
        public java.util.UUID senderId() {
            return senderUuid;
        }

        @Override
        public boolean supportsNotify() {
            return supportsNotify;
        }

        @Override
        public boolean isNotifyEnabled() {
            return notifyEnabled;
        }

        @Override
        public boolean setNotifyEnabled(boolean enabled) {
            notifyEnabled = enabled;
            return notifyEnabled;
        }
    }
}
