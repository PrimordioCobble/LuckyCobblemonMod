package net.crulim.luckblockcobblemon.block;

import com.google.gson.*;
import net.crulim.luckblockcobblemon.block.custom.LuckyBlockRandomizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LuckyBlockHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final RandomSource random = RandomSource.create();
    private static final String CONFIG_PATH = "config/luck_config.json";

    private static List<JsonObject> luckPool = new ArrayList<>();
    private static LuckyBlockRandomizer<JsonObject> luckPoolRandomizer;
/*
    public static void loadConfig() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists() || file.length() == 0) {
                System.out.println("[LuckyBlock] ⚠ Arquivo inexistente ou vazio. Gerando default...");
                generateDefaultConfig(file);
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)).getAsJsonObject();

            if (!json.has("luckPool")) {
                System.out.println("[LuckyBlock] ⚠ Arquivo inválido (sem luckPool). Gerando default...");
                generateDefaultConfig(file);
            }

            JsonArray poolArray = json.getAsJsonArray("luckPool");

            luckPool.clear();
            for (JsonElement element : poolArray) {
                luckPool.add(element.getAsJsonObject());
            }

            luckPoolRandomizer = new LuckyBlockRandomizer<>(luckPool);
            System.out.println("[LuckyBlock] Config loaded successfully.");

        } catch (Exception e) {
            System.out.println("[LuckyBlock] Failed to load config: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void reloadConfig() {
        loadConfig();
        System.out.println("[LuckyBlock] Config reloaded!");
    }
*/
    public static void handleLuck(ServerLevel world, BlockPos pos) {
        if (luckPool.isEmpty() || luckPoolRandomizer == null) {
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
            if (event == null || event.isJsonNull()) continue;
            if (!event.has("type")) continue;

            String type = event.get("type").getAsString();

            switch (type) {
                case "item" -> {
                    if (event.has("items")) {
                        JsonArray items = event.getAsJsonArray("items");
                        if (items != null && !items.isEmpty()) valid.add(event);
                    }
                }
                case "mob" -> {
                    if (event.has("mobs")) {
                        JsonArray mobs = event.getAsJsonArray("mobs");
                        if (mobs != null && !mobs.isEmpty()) valid.add(event);
                    }
                }
                case "structure" -> {
                    if (event.has("structure") || (event.has("structures") && !event.getAsJsonArray("structures").isEmpty())) {
                        valid.add(event);
                    }
                }
                case "effect" -> {
                    if (event.has("effects")) {
                        JsonArray effects = event.getAsJsonArray("effects");
                        if (effects != null && !effects.isEmpty()) valid.add(event);
                    }
                }
                case "explosion" -> valid.add(event);
                default -> System.out.println("[LuckyBlock] Unknown type ignored in filter: " + type);
            }
        }

        return valid;
    }

    private static JsonObject pickRandomLuck(List<JsonObject> pool) {
        // Agora usando o Randomizer diretamente para sorteio mais natural
        if (pool.isEmpty()) return null;

        LuckyBlockRandomizer<JsonObject> localRandomizer = new LuckyBlockRandomizer<>(pool);
        return localRandomizer.getRandom();
    }

    private static void executeLuck(ServerLevel world, BlockPos pos, JsonObject data) {
        String type = data.get("type").getAsString();

        switch (type) {
            case "item" -> dropRandomItem(world, pos, data);
            case "mob" -> spawnMob(world, pos, data);
            case "effect" -> applyRandomEffects(world, pos, data);
            case "structure" -> generateStructure(world, pos, data);
            case "explosion" -> createExplosion(world, pos, data);
            default -> System.out.println("[LuckyBlock] Unknown luck type: " + type);
        }
    }

    private static void dropRandomItem(ServerLevel world, BlockPos pos, JsonObject data) {
        JsonArray items = data.getAsJsonArray("items");
        if (items == null || items.isEmpty()) return;

        String id = items.get(random.nextInt(items.size())).getAsString();
        int min = data.get("min").getAsInt();
        int max = data.get("max").getAsInt();
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        ItemStack stack = new ItemStack(item, random.nextIntBetweenInclusive(min, max));
        Block.popResource(world, pos.above(), stack);
    }

    private static void spawnMob(ServerLevel world, BlockPos pos, JsonObject data) {
        try {
            JsonArray mobs = data.getAsJsonArray("mobs");
            if (mobs == null || mobs.isEmpty()) return;

            String mobId = mobs.get(random.nextInt(mobs.size())).getAsString();
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(mobId));
            var entity = entityType.create(world);

            if (entity == null) {
                System.out.println("[LuckyBlock] Failed to create entity: " + mobId);
                return;
            }

            if (data.has("name")) {
                String customName = data.get("name").getAsString();
                entity.setCustomName(Component.literal(customName));
                entity.setCustomNameVisible(true);
            }

            entity.moveTo(
                    pos.getX() + 0.5,
                    pos.getY() + 1,
                    pos.getZ() + 0.5,
                    world.getRandom().nextFloat() * 360F,
                    0
            );
            world.addFreshEntity(entity);

        } catch (Exception e) {
            System.out.println("[LuckyBlock] Failed to spawn mob: " + e.getMessage());
        }
    }

    private static void applyRandomEffects(ServerLevel world, BlockPos pos, JsonObject data) {
        Player player = world.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, false);
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

    private static void applyEffectToPlayer(Player player, JsonObject effectData) {
        var effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(effectData.get("effect").getAsString()));
        if (effect.isEmpty()) return;

        int duration = effectData.get("duration").getAsInt();
        int amplifier = effectData.get("amplifier").getAsInt();

        player.addEffect(new MobEffectInstance(effect.get(), duration, amplifier));
    }

    private static void generateStructure(ServerLevel world, BlockPos pos, JsonObject data) {
        try {
            String structureId;
            if (data.has("structure")) {
                structureId = data.get("structure").getAsString();
            } else if (data.has("structures")) {
                JsonArray structures = data.getAsJsonArray("structures");
                if (structures.isEmpty()) {
                    System.out.println("[LuckyBlock] Structures list is empty!");
                    return;
                }
                int randomIndex = random.nextInt(structures.size());
                structureId = structures.get(randomIndex).getAsString();
            } else {
                System.out.println("[LuckyBlock] No structure or structures field found!");
                return;
            }

            if (structureId == null || structureId.isEmpty()) {
                System.out.println("[LuckyBlock] Structure ID is empty!");
                return;
            }

            // 🔵 Ajuste da posição dependendo da estrutura
            Vec3 placementPos = structureId.equals("minecraft:desert_pyramid")
                    ? Vec3.atCenterOf(pos.below(8))
                    : Vec3.atCenterOf(pos);

            world.getServer().getCommands().performPrefixedCommand(
                    world.getServer()
                            .createCommandSourceStack()
                            .withPermission(4)
                            .withPosition(placementPos),
                    "place structure " + structureId
            );

            System.out.println("[LuckyBlock] Structure placed: " + structureId);

        } catch (Exception e) {
            System.out.println("[LuckyBlock] Failed to place structure: " + e.getMessage());
        }
    }

    private static void createExplosion(ServerLevel world, BlockPos pos, JsonObject data) {
        float power = data.has("power") ? data.get("power").getAsFloat() : 4.0f;
        boolean fire = data.has("fire") && data.get("fire").getAsBoolean();

        world.explode(
                null,
                world.damageSources().explosion(null),
                new ExplosionDamageCalculator(),
                new Vec3(pos.getX(), pos.getY(), pos.getZ()),
                power,
                fire,
                Level.ExplosionInteraction.BLOCK
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
            effectBuff.addProperty("applyAll", false);
            effectBuff.addProperty("chance", 20);
            pool.add(effectBuff);


            JsonObject structureSpawn = new JsonObject();
            structureSpawn.addProperty("type", "structure");
            JsonArray structures = new JsonArray();
            structures.add("minecraft:desert_pyramid");
            structures.add("minecraft:pillager_outpost");
            structures.add("minecraft:bastion_remnant");
            structures.add("luckblockcobblemon:luckornot");
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
