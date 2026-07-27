package com.aelion.aero.paper;

import com.aelion.aero.api.AeroFleetService;
import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.Permissions;
import com.aelion.aero.common.command.AeroCommandService;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.paper.fleet.PaperFleetService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper entry point for Aelion Aero.
 */
public final class AeroPaperPlugin extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private final AtomicReference<AeroConfig> aeroConfigRef =
            new AtomicReference<>(new AeroConfig("", "", "", AeroConfig.ControlConfig.disabled()));

    private PaperFleetService fleetService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadAeroConfig();
        fleetService = new PaperFleetService(this, aeroConfigRef);
        fleetService.start();
        getServer().getServicesManager().register(
                AeroFleetService.class,
                fleetService,
                this,
                ServicePriority.Normal);
        registerCommands();
        getLogger().info(AeroConstants.NAME + " enabled (fleet bridge registered)");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (fleetService != null) {
            fleetService.stop();
        }
        getLogger().info(AeroConstants.NAME + " disabled");
    }

    public AeroConfig aeroConfig() {
        return aeroConfigRef.get();
    }

    public AeroFleetService fleetService() {
        return fleetService;
    }

    public void reloadAeroConfig() {
        reloadConfig();
        aeroConfigRef.set(PaperConfigBridge.fromBukkit(getConfig()));
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            LiteralCommandNode<CommandSourceStack> root = Commands.literal(AeroConstants.COMMAND_PRIMARY)
                    .requires(stack -> stack.getSender().hasPermission(Permissions.INFO)
                            || stack.getSender().hasPermission(Permissions.ADMIN))
                    .executes(ctx -> {
                        dispatch(ctx.getSource().getSender(), new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.literal("help").executes(ctx -> {
                        dispatch(ctx.getSource().getSender(), new String[] {"help"});
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.literal("info").executes(ctx -> {
                        dispatch(ctx.getSource().getSender(), new String[] {"info"});
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.literal("reload").executes(ctx -> {
                        dispatch(ctx.getSource().getSender(), new String[] {"reload"});
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.literal("ping").executes(ctx -> {
                        dispatch(ctx.getSource().getSender(), new String[] {"ping"});
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.literal("servers")
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
                    .then(Commands.argument("subcommand", StringArgumentType.word()).executes(ctx -> {
                        String sub = StringArgumentType.getString(ctx, "subcommand");
                        dispatch(ctx.getSource().getSender(), new String[] {sub});
                        return Command.SINGLE_SUCCESS;
                    }))
                    .build();

            commands.register(
                    root,
                    "Aelion Aero admin commands",
                    List.of(AeroConstants.COMMAND_ALIAS));
        });
    }

    private void dispatch(CommandSender sender, String[] args) {
        AeroCommandService.execute(args, new PaperCommandPlatform(sender));
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
        public void reloadConfig() {
            reloadAeroConfig();
        }
    }
}
