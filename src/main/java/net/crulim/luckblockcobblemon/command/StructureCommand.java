package net.crulim.luckblockcobblemon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.crulim.luckblockcobblemon.util.StructureSpawner;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class StructureCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("spawnluckornot")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    StructureSpawner.spawnLuckOrNot(source.getWorld(), source.getPlayer());
                    source.sendFeedback(() -> net.minecraft.text.Text.literal("Tentando spawnar luckornot!"), false);
                    return 1;
                })
        );
    }
}
