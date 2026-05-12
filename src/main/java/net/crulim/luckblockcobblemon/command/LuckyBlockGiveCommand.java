package net.crulim.luckblockcobblemon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.config.LockedLuckyBlockTierConfig;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class LuckyBlockGiveCommand {
    private static final DynamicCommandExceptionType INVALID_TYPE = new DynamicCommandExceptionType(
            type -> Text.literal("Invalid lucky block type: " + type + ". Use default, fire, water, grass, ground, fly, steel, eletric or fairy.")
    );

    private static final DynamicCommandExceptionType INVALID_TIER = new DynamicCommandExceptionType(
            tier -> Text.literal("Invalid lucky block tier: " + tier + ". Use a tier from 1 to 10.")
    );

    private LuckyBlockGiveCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("luckyblockgive")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(argument("target", EntityArgumentType.players())
                                .then(argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                LockedLuckyBlockTierConfig.getCommandSuggestions(),
                                                builder
                                        ))
                                        .then(argument("tier", IntegerArgumentType.integer(1, 10))
                                                .executes(context -> execute(
                                                        context.getSource(),
                                                        EntityArgumentType.getPlayers(context, "target"),
                                                        StringArgumentType.getString(context, "type"),
                                                        IntegerArgumentType.getInteger(context, "tier"),
                                                        1
                                                ))
                                                .then(argument("amount", IntegerArgumentType.integer(1, 64))
                                                        .executes(context -> execute(
                                                                context.getSource(),
                                                                EntityArgumentType.getPlayers(context, "target"),
                                                                StringArgumentType.getString(context, "type"),
                                                                IntegerArgumentType.getInteger(context, "tier"),
                                                                IntegerArgumentType.getInteger(context, "amount")
                                                        ))))))
        );
    }

    private static int execute(ServerCommandSource source, Collection<ServerPlayerEntity> targets, String rawType, int tier, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
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

        Identifier blockId = Identifier.of(LuckBlockCobblemon.MOD_ID, blockPath);
        if (!Registries.BLOCK.containsId(blockId)) {
            source.sendError(Text.literal("Locked lucky block is not registered: " + blockId));
            return 0;
        }

        Block block = Registries.BLOCK.get(blockId);
        ItemStack baseStack = new ItemStack(block.asItem(), amount);

        for (ServerPlayerEntity player : targets) {
            ItemStack stack = baseStack.copy();
            boolean inserted = player.getInventory().insertStack(stack);
            if (!inserted || !stack.isEmpty()) {
                player.dropItem(stack, false);
            }
        }

        String officialType = "default".equals(type) ? "default" : type;
        source.sendFeedback(
                () -> Text.literal("Gave ")
                        .formatted(Formatting.GREEN)
                        .append(Text.literal(amount + "x ").formatted(Formatting.YELLOW))
                        .append(Text.literal(officialType).formatted(Formatting.AQUA))
                        .append(Text.literal(" locked lucky block tier "))
                        .append(Text.literal(String.valueOf(tier)).formatted(Formatting.GOLD))
                        .append(Text.literal(" to "))
                        .append(Text.literal(String.valueOf(targets.size())).formatted(Formatting.YELLOW))
                        .append(Text.literal(targets.size() == 1 ? " player." : " players.")),
                true
        );

        return targets.size();
    }
}
