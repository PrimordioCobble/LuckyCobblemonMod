package net.crulim.luckblockcobblemon.block.custom;

import com.google.gson.JsonObject;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerVanilla;
import net.crulim.luckblockcobblemon.handler.PocketLuckHandler; // Só se você quiser reaproveitar utilidades!
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class LuckBlockVanilla extends BaseDirectionalBlock {

    public LuckBlockVanilla(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        boolean isCreative = player.isCreative();
        // Use o getter que adicionamos acima
        if (isCreative && !LuckyBlockHandlerVanilla.isBreakCreativeAllowed()) {
            // Só quebra, não executa evento
            return super.onBreak(world, pos, state, player);
        }

        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            LuckyBlockHandlerVanilla.handleLuckEvent(serverWorld, pos, 0, serverWorld.random);
        }
        return super.onBreak(world, pos, state, player);
    }
}
