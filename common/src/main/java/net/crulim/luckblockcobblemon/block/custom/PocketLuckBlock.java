package net.crulim.luckblockcobblemon.block.custom;

import net.crulim.luckblockcobblemon.config.LockedLuckyBlockTierConfig;
import net.crulim.luckblockcobblemon.handler.PocketLuckHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

public class PocketLuckBlock extends Block {
    public PocketLuckBlock(Properties settings) {
        super(settings);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (player.isCreative() && !PocketLuckHandler.isBreakCreativeAllowed()) {
            return super.playerWillDestroy(world, pos, state, player);
        }

        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            Optional<LockedLuckyBlockTierConfig.LockedBlockInfo> lockedInfo = LockedLuckyBlockTierConfig.resolveLockedBlock(blockId);

            if (lockedInfo.isPresent() && !lockedInfo.get().defaultType()) {
                LockedLuckyBlockTierConfig.LockedBlockInfo info = lockedInfo.get();
                LockedLuckyBlockTierConfig.LevelRange range = info.range();
                PocketLuckHandler.triggerLocked(serverWorld, pos, info.basePoolKey(), range.minLevel(), range.maxLevel());
            } else {
                PocketLuckHandler.trigger(serverWorld, pos, blockId);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
