package net.crulim.luckmod.block;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LuckyBlockHandler {

    private static List<JsonObject> luckPool = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_PATH = "./config/luck_config.json";

    public static void loadConfig() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                generateDefaultConfig(file);
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)).getAsJsonObject();
            luckPool.clear();

            JsonArray poolArray = json.getAsJsonArray("luckPool");
            for (var element : poolArray) {
                luckPool.add(element.getAsJsonObject());
            }

            System.out.println("[LuckMod] Config loaded successfully.");
        } catch (Exception e) {
            System.out.println("[LuckMod] Failed to load config: " + e.getMessage());
        }
    }

    public static void reloadConfig() {
        loadConfig();
        System.out.println("[LuckMod] Config reloaded!");
    }

    private static void generateDefaultConfig(File file) {
        try {
            JsonObject defaultConfig = new JsonObject();

            defaultConfig.addProperty("_info_luckPool", "Lista de efeitos possíveis. type pode ser: item, mob, explosion, effect, structure.");

            JsonArray luckPool = new JsonArray();

            JsonObject itemDrop = new JsonObject();
            itemDrop.addProperty("type", "item");
            itemDrop.addProperty("id", "minecraft:diamond");
            itemDrop.addProperty("min", 1);
            itemDrop.addProperty("max", 3);
            luckPool.add(itemDrop);

            JsonObject mobSpawn = new JsonObject();
            mobSpawn.addProperty("type", "mob");
            mobSpawn.addProperty("mob", "minecraft:zombie");
            mobSpawn.addProperty("name", "Chefão Zumbi");
            luckPool.add(mobSpawn);

            JsonObject explosion = new JsonObject();
            explosion.addProperty("type", "explosion");
            explosion.addProperty("power", 4.0);
            luckPool.add(explosion);

            JsonObject effect = new JsonObject();
            effect.addProperty("type", "effect");
            effect.addProperty("effect", "minecraft:speed");
            effect.addProperty("duration", 200);
            effect.addProperty("amplifier", 2);
            luckPool.add(effect);

            JsonObject structure = new JsonObject();
            structure.addProperty("type", "structure");
            structure.addProperty("structure", "minecraft:desert_pyramid");
            luckPool.add(structure);

            defaultConfig.add("luckPool", luckPool);

            File configDir = new File("./config");
            if (!configDir.exists()) configDir.mkdirs();

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(defaultConfig, writer);
            }

            System.out.println("[LuckMod] Default config created.");
        } catch (Exception e) {
            System.out.println("[LuckMod] Failed to create default config: " + e.getMessage());
        }
    }

    public static void handleLuck(ServerWorld world, BlockPos pos) {
        if (luckPool.isEmpty()) {
            System.out.println("[LuckMod] Warning: Luck pool is empty!");
            return;
        }

        Random random = world.getRandom();
        JsonObject choice = luckPool.get(random.nextInt(luckPool.size()));
        String type = choice.get("type").getAsString();

        switch (type) {
            case "item" -> spawnItem(world, pos, choice);
            case "mob" -> spawnMob(world, pos, choice);
            case "explosion" -> createExplosion(world, pos, choice);
            case "effect" -> applyEffect(world, pos, choice);
            case "structure" -> generateStructure(world, pos, choice);
            default -> System.out.println("[LuckMod] Unknown type: " + type);
        }
    }

    private static void spawnItem(ServerWorld world, BlockPos pos, JsonObject data) {
        String id = data.get("id").getAsString();
        int min = data.get("min").getAsInt();
        int max = data.get("max").getAsInt();

        Item item = Registries.ITEM.get(Identifier.of(id));
        if (item == null) {
            System.out.println("[LuckMod] Unknown item: " + id);
            return;
        }

        Random random = world.getRandom();
        ItemStack stack = new ItemStack(item, random.nextBetween(min, max));
        Block.dropStack(world, pos, stack);
    }

    private static void spawnMob(ServerWorld world, BlockPos pos, JsonObject data) {
        String mobId = data.get("mob").getAsString();
        String name = data.get("name").getAsString();

        EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(mobId));
        if (type == null) {
            System.out.println("[LuckMod] Unknown mob: " + mobId);
            return;
        }

        var entity = type.create(world);
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
            livingEntity.setCustomName(net.minecraft.text.Text.literal(name));
            world.spawnEntity(livingEntity);
        }
    }

    private static void createExplosion(ServerWorld world, BlockPos pos, JsonObject data) {
        float power = data.get("power").getAsFloat();
        world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), power, World.ExplosionSourceType.NONE); // NÃO destrói blocos
    }

    private static void applyEffect(ServerWorld world, BlockPos pos, JsonObject data) {
        var nearestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, false);
        if (nearestPlayer == null) return;

        String effectId = data.get("effect").getAsString();
        int duration = data.get("duration").getAsInt();
        int amplifier = data.get("amplifier").getAsInt();

        var effect = Registries.STATUS_EFFECT.getEntry(Identifier.of(effectId));

        if (effect.isEmpty()) {
            System.out.println("[LuckMod] Unknown effect: " + effectId);
            return;
        }

        nearestPlayer.addStatusEffect(new StatusEffectInstance(effect.get(), duration, amplifier));
    }

    private static void generateStructure(ServerWorld world, BlockPos pos, JsonObject data) {
        try {
            String structureId = data.get("structure").getAsString();
            world.getServer().getCommandManager().executeWithPrefix(
                    world.getServer().getCommandSource()
                            .withLevel(4)
                            .withWorld(world)
                            .withPosition(Vec3d.ofCenter(pos)),
                    "place structure " + structureId
            );
            System.out.println("[LuckMod] Structure placed: " + structureId);
        } catch (Exception e) {
            System.out.println("[LuckMod] Failed to place structure: " + e.getMessage());
        }
    }
}
