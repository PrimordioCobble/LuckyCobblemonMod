package net.crulim.luckblockcobblemon.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.registry.Registries;

import net.crulim.luckblockcobblemon.handler.PocketLuckHandler;

public class PocketLuckBlock extends Block {
    public PocketLuckBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (player.isCreative() && !PocketLuckHandler.isBreakCreativeAllowed()) {
            // Se não pode quebrar em criativo, só retorna o super e não executa evento nenhum.
            return super.onBreak(world, pos, state, player);
        }

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            Identifier blockId = Registries.BLOCK.getId(state.getBlock());
            PocketLuckHandler.trigger(serverWorld, pos, blockId);
        }
        return super.onBreak(world, pos, state, player);
    }
}
