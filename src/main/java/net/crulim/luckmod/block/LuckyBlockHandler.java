package net.crulim.luckmod.block;

import net.crulim.luckmod.config.LuckConfig.LuckEntry;
import net.crulim.luckmod.config.LuckConfig;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;
import java.util.stream.Collectors;

public class LuckyBlockHandler {

    public static void handleLuck(ServerWorld world, BlockPos pos) {
        Random random = world.getRandom();
        MinecraftServer server = world.getServer();

        // Filtra os que passaram na chance
        List<LuckEntry> pool = LuckConfig.luckPool.stream()
                .filter(entry -> random.nextDouble() <= entry.chance)
                .collect(Collectors.toList());

        // Se sobrou algum, escolhe 1 aleatoriamente
        if (!pool.isEmpty()) {
            LuckEntry entry = pool.get(random.nextInt(pool.size()));

            switch (entry.type) {
                case "item" -> {
                    Identifier id = Identifier.tryParse(entry.item);
                    ItemStack stack = new ItemStack(Registries.ITEM.get(id), entry.amount);
                    BlockPos spawnPos = pos.up();
                    world.spawnEntity(new ItemEntity(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), stack));
                }
                case "command" -> {
                    ServerCommandSource source = new ServerCommandSource(
                            server,
                            pos.toCenterPos(),
                            Vec2f.ZERO,
                            world,
                            4,
                            "LuckyBlock",
                            Text.literal("LuckyBlock"),
                            server,
                            null
                    );
                    server.getCommandManager().executeWithPrefix(source.withSilent(), entry.command);
                }
                case "explosion" -> {
                    float power = (float) entry.power;
                    world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), power, World.ExplosionSourceType.BLOCK);
                }
                case "effect_random" -> {
                    if (entry.effects != null && !entry.effects.isEmpty()) {
                        String effectId = entry.effects.get(random.nextInt(entry.effects.size()));
                        var effectEntry = Registries.STATUS_EFFECT.getEntry(Identifier.tryParse(effectId)).orElse(null);
                        if (effectEntry != null) {
                            var instance = new StatusEffectInstance(effectEntry, entry.duration * 20, entry.amplifier);
                            var player = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, false);
                            if (player != null) {
                                player.addStatusEffect(instance);
                            }
                        }
                    }
                }
            }
        }
    }
}
