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
        if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
            // Desce até 20 blocos para tentar achar o solo real
            BlockPos scanPos = pos.down();
            int maxDepth = 20;
            BlockState foundState = null;
            for (int i = 0; i < maxDepth; i++) {
                BlockState state = world.getBlockState(scanPos);
                if (!state.isAir() && state.getFluidState().isEmpty()
                        && state.getBlock() != Blocks.DIRT
                        && state.getBlock() != Blocks.GRASS_BLOCK) {
                    foundState = state;
                    break;
                }
                scanPos = scanPos.down();
            }
            if (foundState != null) {
                return new StructureTemplate.StructureBlockInfo(current.pos(), foundState.getBlock().getDefaultState(), current.nbt());
            }
        }
        return current;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return LuckBlockCobblemon.ADAPTIVE_BLOCK_PROCESSOR;
    }
}
