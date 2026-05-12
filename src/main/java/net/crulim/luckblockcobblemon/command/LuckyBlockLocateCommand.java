package net.crulim.luckblockcobblemon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

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

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("luckyblocklocate")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> locateNearest(ctx.getSource(), 128))
                        .then(argument("radius", IntegerArgumentType.integer(16, 1024))
                                .executes(ctx -> locateNearest(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                )))
        );
    }

    private static int locateNearest(ServerCommandSource source, int radius) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos();

        BlockPos bestPos = null;
        Block bestBlock = null;
        double bestDistance = Double.MAX_VALUE;

        int minX = origin.getX() - radius;
        int maxX = origin.getX() + radius;
        int minZ = origin.getZ() - radius;
        int maxZ = origin.getZ() + radius;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                int topY = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

                for (int y = Math.max(world.getBottomY(), topY - 6); y <= Math.min(world.getTopY() - 1, topY + 2); y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    Block block = world.getBlockState(checkPos).getBlock();

                    if (!LUCKY_BLOCKS.contains(block)) {
                        continue;
                    }

                    double dist = checkPos.getSquaredDistance(origin);
                    if (dist < bestDistance) {
                        bestDistance = dist;
                        bestPos = checkPos.toImmutable();
                        bestBlock = block;
                    }
                }
            }
        }

        if (bestPos == null) {
            source.sendFeedback(
                    () -> Text.literal("No lucky block found within radius " + radius + " in loaded chunks.")
                            .formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        BlockPos finalBestPos = bestPos;
        Block finalBestBlock = bestBlock;
        int distance = (int) Math.sqrt(bestDistance);

        source.sendFeedback(
                () -> Text.literal("Nearest lucky block: ")
                        .formatted(Formatting.GOLD)
                        .append(Text.literal(net.minecraft.registry.Registries.BLOCK.getId(finalBestBlock).toString())
                                .formatted(Formatting.YELLOW))
                        .append(Text.literal(" at "))
                        .append(Text.literal(
                                finalBestPos.getX() + " " + finalBestPos.getY() + " " + finalBestPos.getZ()
                        ).formatted(Formatting.AQUA))
                        .append(Text.literal(" | distance: " + distance + " blocks").formatted(Formatting.GRAY)),
                false
        );

        return 1;
    }
}