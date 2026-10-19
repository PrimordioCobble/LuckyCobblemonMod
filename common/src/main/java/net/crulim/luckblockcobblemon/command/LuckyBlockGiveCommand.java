package net.crulim.luckblockcobblemon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.config.LockedLuckyBlockTierConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class LuckyBlockGiveCommand {
    private static final DynamicCommandExceptionType INVALID_TYPE = new DynamicCommandExceptionType(
            type -> Component.literal("Invalid lucky block type: " + type + ". Use default, fire, water, grass, ground, fly, steel, eletric or fairy.")
    );

    private static final DynamicCommandExceptionType INVALID_TIER = new DynamicCommandExceptionType(
            tier -> Component.literal("Invalid lucky block tier: " + tier + ". Use a tier from 1 to 10.")
    );

    private LuckyBlockGiveCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("luckyblockgive")
                        .requires(source -> source.hasPermission(2))
                        .then(argument("target", EntityArgument.players())
                                .then(argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                LockedLuckyBlockTierConfig.getCommandSuggestions(),
                                                builder
                                        ))
                                        .then(argument("tier", IntegerArgumentType.integer(1, 10))
                                                .executes(context -> execute(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "target"),
                                                        StringArgumentType.getString(context, "type"),
                                                        IntegerArgumentType.getInteger(context, "tier"),
                                                        1
                                                ))
                                                .then(argument("amount", IntegerArgumentType.integer(1, 64))
                                                        .executes(context -> execute(
                                                                context.getSource(),
                                                                EntityArgument.getPlayers(context, "target"),
                                                                StringArgumentType.getString(context, "type"),
                                                                IntegerArgumentType.getInteger(context, "tier"),
                                                                IntegerArgumentType.getInteger(context, "amount")
                                                        ))))))
        );
    }

    private static int execute(CommandSourceStack source, Collection<ServerPlayer> targets, String rawType, int tier, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String type = LockedLuckyBlockTierConfig.canonicalizeType(rawType);
        if (type == null) {
            throw INVALID_TYPE.create(rawType);
        }

        if (!LockedLuckyBlockTierConfig.isValidTier(tier)) {
            throw INVALID_TIER.create(tier);
        }

        String blockPath = LockedLuckyBlockTierConfig.getLockedBlockPath(type, tier);
        if (blockPath == null) {
            throw INVALID_TYPE.create(rawType);
        }

        ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(LuckBlockCobblemon.MOD_ID, blockPath);
        if (!BuiltInRegistries.BLOCK.containsKey(blockId)) {
            source.sendFailure(Component.literal("Locked lucky block is not registered: " + blockId));
            return 0;
        }

        Block block = BuiltInRegistries.BLOCK.get(blockId);
        ItemStack baseStack = new ItemStack(block.asItem(), amount);

        for (ServerPlayer player : targets) {
            ItemStack stack = baseStack.copy();
            boolean inserted = player.getInventory().add(stack);
            if (!inserted || !stack.isEmpty()) {
                player.drop(stack, false);
            }
        }

        String officialType = "default".equals(type) ? "default" : type;
        source.sendSuccess(
                () -> Component.literal("Gave ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(amount + "x ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(officialType).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" locked lucky block tier "))
                        .append(Component.literal(String.valueOf(tier)).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" to "))
                        .append(Component.literal(String.valueOf(targets.size())).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(targets.size() == 1 ? " player." : " players.")),
                true
        );

        return targets.size();
    }
}
