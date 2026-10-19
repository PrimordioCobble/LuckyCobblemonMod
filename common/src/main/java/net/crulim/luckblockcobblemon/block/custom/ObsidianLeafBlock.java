package net.crulim.luckblockcobblemon.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ObsidianLeafBlock extends Block {

    public ObsidianLeafBlock(Properties settings) {
        super(settings);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        ItemStack tool = player.getMainHandItem();
        if (tool.is(Items.DIAMOND_AXE) || tool.is(Items.NETHERITE_AXE)) {
            return super.getDestroyProgress(state, player, world, pos);
        }
        return 0.0F;
    }
}
