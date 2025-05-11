package net.crulim.luckblockcobblemon.block.custom;

import net.crulim.luckblockcobblemon.block.LuckyBlockHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class LuckBlockVanilla extends BaseDirectionalBlock {

    public LuckBlockVanilla(Settings settings) {
        super(settings);
    }

    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            LuckyBlockHandler.handleLuck(serverWorld, pos);
        }

        return super.onBreak(world, pos, state, player);
    }
}
