package net.crulim.luckblockcobblemon.block;

import net.crulim.luckblockcobblemon.block.custom.BaseDirectionalBlock;
import net.crulim.luckblockcobblemon.config.LockedLuckyBlockTierConfig;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerPocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

public class LuckBlockPocket extends BaseDirectionalBlock {

    public LuckBlockPocket(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        boolean isCreative = player.isCreative();

        if (isCreative && !LuckyBlockHandlerPocket.isBreakCreativeAllowed()) {
            return super.playerWillDestroy(world, pos, state, player);
        }

        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            Vec3 center = Vec3.atCenterOf(pos);

            serverWorld.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 10, 0.7, 0.8, 0.7, 0.2);
            serverWorld.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 10, 0.6, 0.6, 0.6, 0.3);
            serverWorld.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 10, 1.0, 1.0, 1.0, 0.4);
            serverWorld.sendParticles(ParticleTypes.SOUL, center.x, center.y, center.z, 10, 0.5, 0.5, 0.5, 0.2);

            serverWorld.playSound(
                    null,
                    pos,
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.BLOCKS,
                    1.0f,
                    1.0f + world.getRandom().nextFloat() * 0.2f
            );

            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            Optional<LockedLuckyBlockTierConfig.LockedBlockInfo> lockedInfo = LockedLuckyBlockTierConfig.resolveLockedBlock(blockId);

            if (lockedInfo.isPresent() && lockedInfo.get().defaultType()) {
                LockedLuckyBlockTierConfig.LevelRange range = lockedInfo.get().range();
                LuckyBlockHandlerPocket.triggerLockedLuckEvent(serverWorld, pos, range.minLevel(), range.maxLevel());
            } else {
                LuckyBlockHandlerPocket.triggerLuckEvent(serverWorld, pos);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
