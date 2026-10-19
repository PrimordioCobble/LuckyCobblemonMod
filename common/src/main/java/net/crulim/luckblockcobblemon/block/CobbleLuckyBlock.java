package net.crulim.luckblockcobblemon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class CobbleLuckyBlock extends Block {
    public CobbleLuckyBlock() {
        super(Properties.ofFullCopy(Blocks.GOLD_BLOCK)
                .strength(0.5f)
                .sound(SoundType.AMETHYST));
    }

    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide && world instanceof ServerLevel serverWorld) {
            CobbleLuckyBlockHandler.handleLuck(serverWorld, pos);
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
