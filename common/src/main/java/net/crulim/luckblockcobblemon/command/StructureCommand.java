package net.crulim.luckblockcobblemon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.crulim.luckblockcobblemon.util.StructureSpawner;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class StructureCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnluckornot")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    StructureSpawner.spawnLuckOrNot(source.getLevel(), source.getPlayer());
                    source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Tentando spawnar luckornot!"), false);
                    return 1;
                })
        );
    }
}
