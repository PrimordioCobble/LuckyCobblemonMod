package net.crulim.luckblockcobblemon.block.custom;

import com.google.gson.JsonObject;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerVanilla;
import net.crulim.luckblockcobblemon.handler.PocketLuckHandler; // Só se você quiser reaproveitar utilidades!
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

public class LuckBlockVanilla extends BaseDirectionalBlock {

    public LuckBlockVanilla(Properties settings) {
        super(settings);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        boolean isCreative = player.isCreative();
        // Use o getter que adicionamos acima
        if (isCreative && !LuckyBlockHandlerVanilla.isBreakCreativeAllowed()) {
            // Só quebra, não executa evento
            return super.playerWillDestroy(world, pos, state, player);
        }

        if (!world.isClientSide && world instanceof ServerLevel serverWorld) {
            LuckyBlockHandlerVanilla.handleLuckEvent(serverWorld, pos, 0, serverWorld.random);
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
