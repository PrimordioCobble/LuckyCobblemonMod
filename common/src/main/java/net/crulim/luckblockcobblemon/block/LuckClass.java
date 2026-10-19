package net.crulim.luckblockcobblemon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class LuckClass extends Block {
    public LuckClass(Properties settings) {
        super(settings);
    }

    @Override

    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide && world instanceof ServerLevel serverWorld) {
            LuckyBlockHandler.handleLuck(serverWorld, pos);
        }


        return super.playerWillDestroy(world, pos, state, player);
    }

}