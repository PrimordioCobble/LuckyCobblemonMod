package net.crulim.luckblockcobblemon.util;

import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.server.network.ServerPlayerEntity;

public class StructureSpawner {

    public static void spawnLuckOrNot(ServerWorld world, ServerPlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        Direction facing = player.getHorizontalFacing(); // Direção que o jogador está olhando

        // Calculando deslocamentos:
        BlockPos spawnPos = playerPos
                .offset(facing, 9) // 9 blocos para frente
                .offset(facing.rotateYClockwise(), 5) // 5 blocos para a direita
                .down(4); // 3 blocos para baixo

        StructureTemplate template = world.getStructureTemplateManager()
                .getTemplate(Identifier.of("luckblockcobblemon", "luckornot"))
                .orElse(null);

        if (template == null) {
            System.out.println("[LuckyBlock] Falhou ao carregar a estrutura: luckblockcobblemon:luckornot");
            return;
        }

        boolean success = template.place(
                world,
                spawnPos,
                spawnPos,
                new StructurePlacementData(),
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
