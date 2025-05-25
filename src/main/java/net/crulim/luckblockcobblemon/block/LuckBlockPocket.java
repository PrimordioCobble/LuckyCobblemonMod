package net.crulim.luckblockcobblemon.block;

import net.crulim.luckblockcobblemon.block.custom.BaseDirectionalBlock;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerPocket;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LuckBlockPocket extends BaseDirectionalBlock {

    public LuckBlockPocket(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        boolean isCreative = player.isCreative();

        if (isCreative && !LuckyBlockHandlerPocket.isBreakCreativeAllowed()) {
            return super.onBreak(world, pos, state, player);
        }

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            Vec3d center = Vec3d.ofCenter(pos);

            serverWorld.spawnParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 10, 0.7, 0.8, 0.7, 0.2);
            serverWorld.spawnParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 10, 0.6, 0.6, 0.6, 0.3);
            serverWorld.spawnParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 10, 1.0, 1.0, 1.0, 0.4);
            serverWorld.spawnParticles(ParticleTypes.SOUL, center.x, center.y, center.z, 10, 0.5, 0.5, 0.5, 0.2);

            serverWorld.playSound(
                    null,
                    pos,
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                    SoundCategory.BLOCKS,
                    1.0f,
                    1.0f + world.getRandom().nextFloat() * 0.2f
            );

            LuckyBlockHandlerPocket.triggerLuckEvent(serverWorld, pos);
        }
        return super.onBreak(world, pos, state, player);
    }
}
