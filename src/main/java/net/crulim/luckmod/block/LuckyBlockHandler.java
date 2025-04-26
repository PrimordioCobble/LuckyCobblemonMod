package net.crulim.luckmod.block;

import com.google.gson.*;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LuckyBlockHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Random random = Random.create();
    private static List<JsonObject> luckPool = new ArrayList<>();
    private static final String CONFIG_PATH = "config/luck_config.json";

    public static void loadConfig() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                generateDefaultConfig(file);
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray poolArray = json.getAsJsonArray("luckPool");

            luckPool.clear();
            for (JsonElement element : poolArray) {
                luckPool.add(element.getAsJsonObject());
            }

            System.out.println("[LuckyBlock] Config loaded successfully.");

        } catch (Exception e) {
            System.out.println("[LuckyBlock] Failed to load config: " + e.getMessage());
        }
    }

    public static void reloadConfig() {
        loadConfig();
        System.out.println("[LuckyBlock] Config reloaded!");
    }

    public static void handleLuck(ServerWorld world, BlockPos pos) {
        if (luckPool.isEmpty()) {
            System.out.println("[LuckyBlock] Warning: Luck pool is empty!");
            return;
        }

        List<JsonObject> validPool = filterValidEvents();

        if (validPool.isEmpty()) {
            System.out.println("[LuckyBlock] No valid events to pick from!");
            return;
        }

        JsonObject selected = pickRandomLuck(validPool);
        if (selected == null) {
            System.out.println("[LuckyBlock] No luck event selected!");
            return;
        }

        executeLuck(world, pos, selected);
    }

    private static List<JsonObject> filterValidEvents() {
        List<JsonObject> valid = new ArrayList<>();

        for (JsonObject event : luckPool) {
            String type = event.get("type").getAsString();
            switch (type) {
                case "item" -> {
                    JsonArray items = event.getAsJsonArray("items");
                    if (items != null && !items.isEmpty()) valid.add(event);
                }
                case "mob" -> {
                    JsonArray mobs = event.getAsJsonArray("mobs");
                    if (mobs != null && !mobs.isEmpty()) valid.add(event);
                }
                case "structure" -> {
                    JsonArray structures = event.getAsJsonArray("structures");
                    if (structures != null && !structures.isEmpty()) valid.add(event);
                }
                case "effect", "explosion" -> valid.add(event);
            }
        }
        return valid;
    }

    private static JsonObject pickRandomLuck(List<JsonObject> pool) {
        int totalChance = pool.stream()
                .mapToInt(obj -> obj.has("chance") ? obj.get("chance").getAsInt() : 1)
                .sum();

        if (totalChance == 0) return null;

        int roll = random.nextInt(totalChance);
        int cumulative = 0;

        for (JsonObject obj : pool) {
            int weight = obj.has("chance") ? obj.get("chance").getAsInt() : 1;
            cumulative += weight;
            if (roll < cumulative) {
                return obj;
            }
        }

        return null;
    }

    private static void executeLuck(ServerWorld world, BlockPos pos, JsonObject data) {
        String type = data.get("type").getAsString();

        switch (type) {
            case "item" -> dropRandomItem(world, pos, data);
            case "mob" -> spawnMob(world, pos, data);
            case "effect" -> applyRandomEffects(world, pos, data);
            case "structure" -> placeStructure(world, pos, data);
            case "explosion" -> createExplosion(world, pos, data);
            default -> System.out.println("[LuckyBlock] Unknown luck type: " + type);
        }
    }

    private static void dropRandomItem(ServerWorld world, BlockPos pos, JsonObject data) {
        JsonArray items = data.getAsJsonArray("items");
        if (items == null || items.isEmpty()) return;

        String id = items.get(random.nextInt(items.size())).getAsString();
        int min = data.get("min").getAsInt();
        int max = data.get("max").getAsInt();
        Item item = Registries.ITEM.get(Identifier.of(id));
        ItemStack stack = new ItemStack(item, random.nextBetween(min, max));
        Block.dropStack(world, pos.up(), stack);
    }

    private static void spawnMob(ServerWorld world, BlockPos pos, JsonObject data) {
        try {
            JsonArray mobs = data.getAsJsonArray("mobs");
            if (mobs == null || mobs.isEmpty()) return;

            String mobId = mobs.get(random.nextInt(mobs.size())).getAsString();
            Identifier id = Identifier.of(mobId);

            EntityType<?> entityType = Registries.ENTITY_TYPE.get(id);
            var entity = entityType.create(world);

            if (entity == null) {
                System.out.println("[LuckyBlock] Failed to create entity: " + mobId);
                return;
            }

            if (data.has("name")) {
                String customName = data.get("name").getAsString();
                entity.setCustomName(Text.literal(customName));
                entity.setCustomNameVisible(true);
            }

            entity.refreshPositionAndAngles(
                    pos.getX() + 0.5,
                    pos.getY() + 1,
                    pos.getZ() + 0.5,
                    world.getRandom().nextFloat() * 360F,
                    0
            );
            world.spawnEntity(entity);

        } catch (Exception e) {
            System.out.println("[LuckyBlock] Failed to spawn mob: " + e.getMessage());
        }
    }

    private static void applyRandomEffects(ServerWorld world, BlockPos pos, JsonObject data) {
        PlayerEntity player = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, false);
        if (player == null) return;

        JsonArray effects = data.getAsJsonArray("effects");
        boolean applyAll = data.has("applyAll") && data.get("applyAll").getAsBoolean();

        if (applyAll) {
            for (var elem : effects) {
                applyEffectToPlayer(player, elem.getAsJsonObject());
            }
        } else {
            JsonObject effect = effects.get(random.nextInt(effects.size())).getAsJsonObject();
            applyEffectToPlayer(player, effect);
        }
    }

    private static void applyEffectToPlayer(PlayerEntity player, JsonObject effectData) {
        var effect = Registries.STATUS_EFFECT.getEntry(Identifier.of(effectData.get("effect").getAsString()));
        if (effect.isEmpty()) return;

        int duration = effectData.get("duration").getAsInt();
        int amplifier = effectData.get("amplifier").getAsInt();

        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(effect.get(), duration, amplifier));
    }

    private static void placeStructure(ServerWorld world, BlockPos pos, JsonObject data) {
        try {
            JsonArray structures = data.getAsJsonArray("structures");
            if (structures == null || structures.isEmpty()) return;

            String structureId = structures.get(random.nextInt(structures.size())).getAsString();

            world.getServer().getCommandManager().executeWithPrefix(
                    world.getServer().getCommandSource()
                            .withLevel(4)
                            .withWorld(world)
                            .withPosition(Vec3d.ofCenter(pos.up())),
                    "place structure " + structureId
            );
        } catch (Exception e) {
            System.out.println("[LuckyBlock] Failed to place structure: " + e.getMessage());
        }
    }

    private static void createExplosion(ServerWorld world, BlockPos pos, JsonObject data) {
        float power = data.has("power") ? data.get("power").getAsFloat() : 4.0f;
        boolean fire = data.has("fire") && data.get("fire").getAsBoolean();

        world.createExplosion(
                null,
                world.getDamageSources().explosion(null),
                new ExplosionBehavior(),
                new Vec3d(pos.getX(), pos.getY(), pos.getZ()),
                power,
                fire,
                World.ExplosionSourceType.BLOCK
        );
    }
    private static void generateDefaultConfig(File file) {
        try {
            JsonObject defaultConfig = new JsonObject();
            JsonArray pool = new JsonArray();


            JsonObject itemDrop = new JsonObject();
            itemDrop.addProperty("type", "item");
            JsonArray items = new JsonArray();
            items.add("minecraft:diamond");
            items.add("minecraft:gold_ingot");
            items.add("minecraft:emerald");
            itemDrop.add("items", items);
            itemDrop.addProperty("min", 2);
            itemDrop.addProperty("max", 5);
            itemDrop.addProperty("chance", 30);
            pool.add(itemDrop);


            JsonObject mobSpawn = new JsonObject();
            mobSpawn.addProperty("type", "mob");
            JsonArray mobs = new JsonArray();
            mobs.add("minecraft:zombie");
            mobs.add("minecraft:skeleton");
            mobs.add("minecraft:creeper");
            mobSpawn.add("mobs", mobs);
            mobSpawn.addProperty("name", "HELLO 😁");
            mobSpawn.addProperty("chance", 25);
            pool.add(mobSpawn);


            JsonObject effectBuff = new JsonObject();
            effectBuff.addProperty("type", "effect");
            JsonArray effects = new JsonArray();

            JsonObject speed = new JsonObject();
            speed.addProperty("effect", "minecraft:speed");
            speed.addProperty("duration", 300);
            speed.addProperty("amplifier", 1);
            effects.add(speed);

            JsonObject strength = new JsonObject();
            strength.addProperty("effect", "minecraft:strength");
            strength.addProperty("duration", 300);
            strength.addProperty("amplifier", 1);
            effects.add(strength);

            JsonObject jumpBoost = new JsonObject();
            jumpBoost.addProperty("effect", "minecraft:jump_boost");
            jumpBoost.addProperty("duration", 300);
            jumpBoost.addProperty("amplifier", 1);
            effects.add(jumpBoost);

            JsonObject regeneration = new JsonObject();
            regeneration.addProperty("effect", "minecraft:regeneration");
            regeneration.addProperty("duration", 300);
            regeneration.addProperty("amplifier", 1);
            effects.add(regeneration);

            JsonObject haste = new JsonObject();
            haste.addProperty("effect", "minecraft:haste");
            haste.addProperty("duration", 300);
            haste.addProperty("amplifier", 1);
            effects.add(haste);

            effectBuff.add("effects", effects);
            effectBuff.addProperty("applyAll", false); // aplica só um efeito aleatório
            effectBuff.addProperty("chance", 20);
            pool.add(effectBuff);


            JsonObject structureSpawn = new JsonObject();
            structureSpawn.addProperty("type", "structure");
            JsonArray structures = new JsonArray();
            structures.add("minecraft:desert_pyramid");
            structures.add("minecraft:igloo");
            structures.add("minecraft:ruined_portal");
            structureSpawn.add("structures", structures);
            structureSpawn.addProperty("chance", 15);
            pool.add(structureSpawn);


            JsonObject explosion = new JsonObject();
            explosion.addProperty("type", "explosion");
            explosion.addProperty("power", 4.0);
            explosion.addProperty("fire", true);
            explosion.addProperty("chance", 10);
            pool.add(explosion);


            defaultConfig.add("luckPool", pool);

            File configDir = new File("./config");
            if (!configDir.exists()) configDir.mkdirs();

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(defaultConfig, writer);
            }

            System.out.println("[LuckyBlock] Default config created.");
        } catch (Exception e) {
            System.out.println("[LuckyBlock] Failed to create default config: " + e.getMessage());
        }
    }

}
