package net.crulim.luckmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CobbleLuckyBlock extends Block {
    public CobbleLuckyBlock() {
        super(Settings.copy(Blocks.GOLD_BLOCK)
                .strength(0.5f)
                .sounds(BlockSoundGroup.AMETHYST_BLOCK));
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            CobbleLuckyBlockHandler.handleLuck(serverWorld, pos);
        }
        return super.onBreak(world, pos, state, player);
    }
}
