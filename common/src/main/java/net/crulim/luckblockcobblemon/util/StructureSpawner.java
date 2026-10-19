package net.crulim.luckblockcobblemon.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class StructureSpawner {

    public static void spawnLuckOrNot(ServerLevel world, ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();
        Direction facing = player.getDirection(); // Direção que o jogador está olhando

        // Calculando deslocamentos:
        BlockPos spawnPos = playerPos
                .relative(facing, 9) // 9 blocos para frente
                .relative(facing.getClockWise(), 5) // 5 blocos para a direita
                .below(4); // 3 blocos para baixo

        StructureTemplate template = world.getStructureManager()
                .get(ResourceLocation.fromNamespaceAndPath("luckblockcobblemon", "luckornot"))
                .orElse(null);

        if (template == null) {
            System.out.println("[LuckyBlock] Falhou ao carregar a estrutura: luckblockcobblemon:luckornot");
            return;
        }

        boolean success = template.placeInWorld(
                world,
                spawnPos,
                spawnPos,
                new StructurePlaceSettings(),
                world.getRandom(),
                2 // FLAG 2: Colocar blocos e entidades
        );

        if (success) {
            System.out.println("[LuckyBlock] Estrutura spawnada em " + spawnPos);
        } else {
            System.out.println("[LuckyBlock] Falhou ao spawnar a estrutura.");
        }
    }
}
