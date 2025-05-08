package net.crulim.luckblockcobblemon.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LuckClass extends Block {
    public LuckClass(Settings settings) {
        super(settings);
    }

    @Override

    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            LuckyBlockHandler.handleLuck(serverWorld, pos);
        }


        return super.onBreak(world, pos, state, player);
    }

}