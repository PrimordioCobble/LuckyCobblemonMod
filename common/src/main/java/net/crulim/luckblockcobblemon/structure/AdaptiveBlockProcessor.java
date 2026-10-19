package net.crulim.luckblockcobblemon.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import java.util.HashMap;
import java.util.Map;

public class AdaptiveBlockProcessor extends StructureProcessor {
    public static final MapCodec<AdaptiveBlockProcessor> CODEC = MapCodec.unit(new AdaptiveBlockProcessor());


    public AdaptiveBlockProcessor() {}


    public StructureTemplate.StructureBlockInfo process(
            LevelReader world,
            BlockPos pos,
            BlockPos pivot,
            StructureTemplate.StructureBlockInfo original,
            StructureTemplate.StructureBlockInfo current
    ) {
        Block block = original.state().getBlock();
        if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
            // Desce até 20 blocos para tentar achar o solo real
            BlockPos scanPos = pos.below();
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
                scanPos = scanPos.below();
            }
            if (foundState != null) {
                return new StructureTemplate.StructureBlockInfo(current.pos(), foundState.getBlock().defaultBlockState(), current.nbt());
            }
        }
        return current;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return LuckBlockCobblemon.ADAPTIVE_BLOCK_PROCESSOR;
    }
}
