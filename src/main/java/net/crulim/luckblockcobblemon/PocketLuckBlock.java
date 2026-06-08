package net.crulim.luckblockcobblemon.block.custom;

import net.crulim.luckblockcobblemon.config.LockedLuckyBlockTierConfig;
import net.crulim.luckblockcobblemon.handler.PocketLuckHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

public class PocketLuckBlock extends Block {
    public PocketLuckBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (player.isCreative() && !PocketLuckHandler.isBreakCreativeAllowed()) {
            return super.onBreak(world, pos, state, player);
        }

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            Identifier blockId = Registries.BLOCK.getId(state.getBlock());
            Optional<LockedLuckyBlockTierConfig.LockedBlockInfo> lockedInfo = LockedLuckyBlockTierConfig.resolveLockedBlock(blockId);

            if (lockedInfo.isPresent() && !lockedInfo.get().defaultType()) {
                LockedLuckyBlockTierConfig.LockedBlockInfo info = lockedInfo.get();
                LockedLuckyBlockTierConfig.LevelRange range = info.range();
                PocketLuckHandler.triggerLocked(serverWorld, pos, info.basePoolKey(), range.minLevel(), range.maxLevel());
            } else {
                PocketLuckHandler.trigger(serverWorld, pos, blockId);
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
