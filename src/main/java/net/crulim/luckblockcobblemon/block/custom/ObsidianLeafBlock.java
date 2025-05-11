package net.crulim.luckblockcobblemon.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public class ObsidianLeafBlock extends Block {

    public ObsidianLeafBlock(Settings settings) {
        super(settings);
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        ItemStack tool = player.getMainHandStack();
        if (tool.isOf(Items.DIAMOND_AXE) || tool.isOf(Items.NETHERITE_AXE)) {
            return super.calcBlockBreakingDelta(state, player, world, pos);
        }
        return 0.0F;
    }
}
