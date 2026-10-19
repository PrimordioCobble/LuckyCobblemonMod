package net.crulim.luckblockcobblemon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import java.util.HashSet;
import java.util.Set;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class LuckyBlockLocateCommand {

    private static final Set<Block> LUCKY_BLOCKS = new HashSet<>();

    static {
        LUCKY_BLOCKS.add(ModBlocks.LUCK_BLOCK_POCKET);
        LUCKY_BLOCKS.add(ModBlocks.LUCK_BLOCK_POCKET_FIRE);
        LUCKY_BLOCKS.add(ModBlocks.LUCK_BLOCK_POCKET_WATER);
        LUCKY_BLOCKS.add(ModBlocks.LUCK_BLOCK_POCKET_GRASS);
        LUCKY_BLOCKS.add(ModBlocks.LUCK_BLOCK_POCKET_GROUND);
        LUCKY_BLOCKS.add(ModBlocks.LUCK_BLOCK_POCKET_FLY);
        LUCKY_BLOCKS.add(ModBlocks.LUCK_BLOCK_POCKET_STEEL);
        LUCKY_BLOCKS.add(ModBlocks.LUCK_BLOCK_POCKET_ELETRIC);
        LUCKY_BLOCKS.add(ModBlocks.LUCK_BLOCK_POCKET_FAIRY);
        LUCKY_BLOCKS.add(ModBlocks.LUCK_BLOCK_VANILLA);
        LUCKY_BLOCKS.addAll(ModBlocks.getAllLockedBlocks());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("luckyblocklocate")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> locateNearest(ctx.getSource(), 128))
                        .then(argument("radius", IntegerArgumentType.integer(16, 1024))
                                .executes(ctx -> locateNearest(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                )))
        );
    }

    private static int locateNearest(CommandSourceStack source, int radius) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel world = player.serverLevel();
        BlockPos origin = player.blockPosition();

        BlockPos bestPos = null;
        Block bestBlock = null;
        double bestDistance = Double.MAX_VALUE;

        int minX = origin.getX() - radius;
        int maxX = origin.getX() + radius;
        int minZ = origin.getZ() - radius;
        int maxZ = origin.getZ() + radius;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!world.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }

                int topY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                for (int y = Math.max(world.getMinBuildHeight(), topY - 6); y <= Math.min(world.getMaxBuildHeight() - 1, topY + 2); y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    Block block = world.getBlockState(checkPos).getBlock();

                    if (!LUCKY_BLOCKS.contains(block)) {
                        continue;
                    }

                    double dist = checkPos.distSqr(origin);
                    if (dist < bestDistance) {
                        bestDistance = dist;
                        bestPos = checkPos.immutable();
                        bestBlock = block;
                    }
                }
            }
        }

        if (bestPos == null) {
            source.sendSuccess(
                    () -> Component.literal("No lucky block found within radius " + radius + " in loaded chunks.")
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return 0;
        }

        BlockPos finalBestPos = bestPos;
        Block finalBestBlock = bestBlock;
        int distance = (int) Math.sqrt(bestDistance);

        source.sendSuccess(
                () -> Component.literal("Nearest lucky block: ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(finalBestBlock).toString())
                                .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" at "))
                        .append(Component.literal(
                                finalBestPos.getX() + " " + finalBestPos.getY() + " " + finalBestPos.getZ()
                        ).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" | distance: " + distance + " blocks").withStyle(ChatFormatting.GRAY)),
                false
        );

        return 1;
    }
}