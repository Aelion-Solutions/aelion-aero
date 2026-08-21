package com.aelion.aero.paper.v26;

import com.aelion.aero.api.AeroFleetService;
import com.aelion.aero.bukkit.BukkitAeroBootstrap;
import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.Permissions;
import com.aelion.aero.common.command.AeroCommandService;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.api.PanelClient;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper 26.x entry point (Brigadier command registration).
 */
public final class AeroPaperPlugin extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private BukkitAeroBootstrap bootstrap;

    @Override
    public void onEnable() {
        bootstrap = new BukkitAeroBootstrap(this);
        bootstrap.enableFleetOnly();
        registerCommands();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.disable();
        }
    }

    public AeroConfig aeroConfig() {
        return bootstrap.config();
    }

    public AeroFleetService fleetService() {
        return bootstrap.fleetService();
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            LiteralCommandNode<CommandSourceStack> root = Commands.literal(AeroConstants.COMMAND_BACKEND_PRIMARY)
                    .requires(stack -> Permissions.allowsAny(stack.getSender()::hasPermission))
                    .executes(ctx -> {
                        dispatch(ctx.getSource().getSender(), new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.literal("help")
                            .requires(stack -> Permissions.allowsInfo(stack.getSender()::hasPermission))
                            .executes(ctx -> {
                                dispatch(ctx.getSource().getSender(), new String[] {"help"});
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(Commands.literal("info")
                            .requires(stack -> Permissions.allowsInfo(stack.getSender()::hasPermission))
                            .executes(ctx -> {
                                dispatch(ctx.getSource().getSender(), new String[] {"info"});
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(Commands.literal("reload")
                            .requires(stack -> Permissions.allowsAdmin(stack.getSender()::hasPermission))
                            .executes(ctx -> {
                                dispatch(ctx.getSource().getSender(), new String[] {"reload"});
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(Commands.literal("ping")
                            .requires(stack -> Permissions.allowsInfo(stack.getSender()::hasPermission))
                            .executes(ctx -> {
                                dispatch(ctx.getSource().getSender(), new String[] {"ping"});
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(Commands.literal("servers")
                            .requires(stack -> Permissions.allowsInfo(stack.getSender()::hasPermission))
                            .then(Commands.literal("list")
                                    .then(Commands.literal("--names").executes(ctx -> {
                                        dispatch(ctx.getSource().getSender(),
                                                new String[] {"servers", "list", "--names"});
                                        return Command.SINGLE_SUCCESS;
                                    }))
                                    .executes(ctx -> {
                                        dispatch(ctx.getSource().getSender(),
                                                new String[] {"servers", "list"});
                                        return Command.SINGLE_SUCCESS;
                                    }))
                            .executes(ctx -> {
                                dispatch(ctx.getSource().getSender(), new String[] {"servers"});
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(Commands.literal("kick")
                            .requires(stack -> Permissions.allowsAdmin(stack.getSender()::hasPermission))
                            .then(Commands.argument("args", StringArgumentType.greedyString())
                                    .executes(ctx -> dispatchVerb(
                                            ctx.getSource().getSender(),
                                            "kick",
                                            StringArgumentType.getString(ctx, "args"))))
                            .executes(ctx -> {
                                dispatch(ctx.getSource().getSender(), new String[] {"kick"});
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(Commands.literal("transfer")
                            .requires(stack -> Permissions.allowsAdmin(stack.getSender()::hasPermission))
                            .then(Commands.argument("args", StringArgumentType.greedyString())
                                    .executes(ctx -> dispatchVerb(
                                            ctx.getSource().getSender(),
                                            "transfer",
                                            StringArgumentType.getString(ctx, "args"))))
                            .executes(ctx -> {
                                dispatch(ctx.getSource().getSender(), new String[] {"transfer"});
                                return Command.SINGLE_SUCCESS;
                            }))
                    // Fallback: forward unknown / proxy-only verbs to AeroCommandService for styled
                    // messages (parity with classic /aes). Empty suggests so tab only shows literals.
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> builder.buildFuture())
                            .executes(ctx -> {
                                String raw = StringArgumentType.getString(ctx, "args").trim();
                                String[] args = raw.isEmpty()
                                        ? new String[0]
                                        : raw.split("\\s+");
                                dispatch(ctx.getSource().getSender(), args);
                                return Command.SINGLE_SUCCESS;
                            }))
                    .build();

            commands.register(
                    root,
                    "Aelion Aero server commands",
                    List.of());
        });
    }

    private void dispatch(CommandSender sender, String[] args) {
        AeroCommandService.execute(args, new PaperCommandPlatform(sender));
    }

    private int dispatchVerb(CommandSender sender, String verb, String rawArgs) {
        String raw = rawArgs == null ? "" : rawArgs.trim();
        String[] rest = raw.isEmpty() ? new String[0] : raw.split("\\s+");
        String[] args = new String[rest.length + 1];
        args[0] = verb;
        System.arraycopy(rest, 0, args, 1, rest.length);
        dispatch(sender, args);
        return Command.SINGLE_SUCCESS;
    }

    private final class PaperCommandPlatform implements AeroCommandService.Platform {
        private final CommandSender sender;

        private PaperCommandPlatform(CommandSender sender) {
            this.sender = sender;
        }

        @Override
        public void send(String legacyLine) {
            sender.sendMessage(LEGACY.deserialize(legacyLine));
        }

        @Override
        public void sendAll(List<String> legacyLines) {
            for (String line : legacyLines) {
                send(line);
            }
        }

        @Override
        public boolean hasPermission(String permission) {
            return sender.hasPermission(permission);
        }

        @Override
        public void runAsync(Runnable task) {
            getServer().getScheduler().runTaskAsynchronously(AeroPaperPlugin.this, task);
        }

        @Override
        public void runSync(Runnable task) {
            getServer().getScheduler().runTask(AeroPaperPlugin.this, task);
        }

        @Override
        public AeroConfig config() {
            return aeroConfig();
        }

        @Override
        public void reloadConfig() throws Exception {
            bootstrap.reloadAeroConfig();
        }

        @Override
        public PanelClient panelClient() {
            return bootstrap.panelClient();
        }

        @Override
        public boolean kickPlayer(String playerName, String message) {
            AeroFleetService fleet = fleetService();
            if (fleet == null || playerName == null || playerName.isBlank()) {
                return false;
            }
            org.bukkit.entity.Player player = getServer().getPlayerExact(playerName.trim());
            if (player == null || !player.isOnline()) {
                return false;
            }
            return fleet.kickPlayer(player.getUniqueId(), message);
        }

        @Override
        public boolean transferPlayer(String playerName, String serverKey, String groupKey) {
            AeroFleetService fleet = fleetService();
            if (fleet == null || playerName == null || playerName.isBlank()) {
                return false;
            }
            org.bukkit.entity.Player player = getServer().getPlayerExact(playerName.trim());
            if (player == null || !player.isOnline()) {
                return false;
            }
            if (serverKey != null && !serverKey.isBlank()) {
                return fleet.transferToServer(player.getUniqueId(), serverKey);
            }
            if (groupKey != null && !groupKey.isBlank()) {
                return fleet.transferToGroup(player.getUniqueId(), groupKey);
            }
            return false;
        }

        @Override
        public List<String> onlinePlayerNames() {
            return getServer().getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName)
                    .toList();
        }
    }
}
