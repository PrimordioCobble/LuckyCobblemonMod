package net.crulim.luckblockcobblemon.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon;

import java.util.HashMap;
import java.util.Map;

public class AdaptiveBlockProcessor extends StructureProcessor {
    public static final MapCodec<AdaptiveBlockProcessor> CODEC = MapCodec.unit(new AdaptiveBlockProcessor());


    public AdaptiveBlockProcessor() {}


    public StructureTemplate.StructureBlockInfo process(
            WorldView world,
            BlockPos pos,
            BlockPos pivot,
            StructureTemplate.StructureBlockInfo original,
            StructureTemplate.StructureBlockInfo current
    ) {
        Block block = original.state().getBlock();
        if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.SAND || block == Blocks.STONE) {
            Map<Block, Integer> blockCount = new HashMap<>();
            for (BlockPos offset : BlockPos.iterate(
                    pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
                if (!offset.equals(pos)) {
                    BlockState state = world.getBlockState(offset);
                    Block neighbor = state.getBlock();
                    if (state.isAir() || !state.getFluidState().isEmpty()) continue;
                    blockCount.put(neighbor, blockCount.getOrDefault(neighbor, 0) + 1);
                }
            }

            Block dominant = null;
            int max = 0;
            for (Map.Entry<Block, Integer> entry : blockCount.entrySet()) {
                if (entry.getValue() > max) {
                    dominant = entry.getKey();
                    max = entry.getValue();
                }
            }

            if (dominant != null) {
                BlockState newState = dominant.getDefaultState();
                return new StructureTemplate.StructureBlockInfo(current.pos(), newState, current.nbt());
            }
        }

        return current;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return LuckBlockCobblemon.ADAPTIVE_BLOCK_PROCESSOR;
    }
}
