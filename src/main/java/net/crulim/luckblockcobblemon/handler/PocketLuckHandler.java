package net.crulim.luckblockcobblemon.handler;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.crulim.luckblockcobblemon.config.LuckyBlockConfigManager;
import net.crulim.luckblockcobblemon.config.LuckyBlockSettingsConfig;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PocketLuckHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Random random = Random.create();

    private static final Set<String> EXPECTED_TYPES = Set.of(
            "fire", "water", "grass", "ground", "fly", "fairy", "eletric", "steel"
    );

    private static final Map<String, JsonObject> POOL_CONFIGS = new HashMap<>();
    private static final Map<String, List<JsonObject>> POOLS = new HashMap<>();
    private static boolean breakCreative = false;

    public static void loadConfig() {
        POOL_CONFIGS.clear();
        POOLS.clear();
        breakCreative = LuckyBlockSettingsConfig.isThemedLuckyBlockBreakCreativeAllowed();

        for (String type : EXPECTED_TYPES) {
            JsonObject root = loadOrCreateRoot(type);
            POOL_CONFIGS.put(type, root);
            POOLS.put(type, readValidPool(root));
        }

        System.out.println("[PocketLuckHandler] Loaded themed pools from config/luckyblockcobblemon/pools/themed/.");
    }

    public static void reloadConfig() {
        loadConfig();
        System.out.println("[PocketLuckHandler] Config reloaded successfully.");
    }

    public static boolean isBreakCreativeAllowed() {
        return breakCreative;
    }

    public static void trigger(ServerWorld world, BlockPos pos, Identifier blockId) {
        String type = LuckyBlockConfigManager.toThemedType(blockId.getPath());
        triggerFromPool(world, pos, type, -1, -1);
    }

    public static void triggerLocked(ServerWorld world, BlockPos pos, String basePoolKey, int forcedMinLevel, int forcedMaxLevel) {
        String type = LuckyBlockConfigManager.toThemedType(basePoolKey);
        triggerFromPool(world, pos, type, forcedMinLevel, forcedMaxLevel);
    }

    private static void triggerFromPool(ServerWorld world, BlockPos pos, String type, int forcedMinLevel, int forcedMaxLevel) {
        if (!EXPECTED_TYPES.contains(type)) {
            System.out.println("[PocketLuckHandler] Unknown themed pool ignored: " + type);
            return;
        }

        List<JsonObject> pool = POOLS.getOrDefault(type, Collections.emptyList());
        if (pool.isEmpty()) {
            System.out.println("[PocketLuckHandler] Themed pool is empty: " + type);
            return;
        }

        JsonObject chosen = pick(pool);
        if (chosen == null) {
            System.out.println("[PocketLuckHandler] No event selected for themed pool: " + type);
            return;
        }

        JsonObject eventToExecute = chosen.deepCopy();
        if (forcedMinLevel > 0 && forcedMaxLevel > 0) {
            applyLockedLevelRange(eventToExecute, forcedMinLevel, forcedMaxLevel);
        }

        String eventType = eventToExecute.get("type").getAsString();
        switch (eventType) {
            case "item" -> dropItems(world, pos, eventToExecute);
            case "pokemon" -> spawnPokemon(world, pos, eventToExecute, type);
            default -> System.out.println("[PocketLuckHandler] Unknown event type ignored: " + eventType);
        }
    }

    private static JsonObject loadOrCreateRoot(String type) {
        return LuckyBlockConfigManager.loadPoolObjectWithLegacyMigration(
                LuckyBlockConfigManager.themedPoolFile(type),
                LuckyBlockConfigManager.legacyThemedPoolFile(type),
                () -> createDefaultPool(type)
        );
    }

    private static List<JsonObject> readValidPool(JsonObject root) {
        JsonArray events = LuckyBlockConfigManager.getEvents(root);
        List<JsonObject> pool = new ArrayList<>();

        for (JsonElement element : events) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }

            JsonObject event = element.getAsJsonObject();
            if (isValidEvent(event)) {
                pool.add(event);
            }
        }

        return pool;
    }

    private static boolean isValidEvent(JsonObject event) {
        if (event == null || !event.has("type")) {
            return false;
        }

        float chance = event.has("chance") ? event.get("chance").getAsFloat() : 1.0F;
        if (chance <= 0F) {
            return false;
        }

        String type = event.get("type").getAsString();
        return switch (type) {
            case "item" -> event.has("items") && event.get("items").isJsonArray() && !event.getAsJsonArray("items").isEmpty();
            case "pokemon" -> getPokemonArray(event) != null && !getPokemonArray(event).isEmpty();
            default -> false;
        };
    }

    private static JsonObject pick(List<JsonObject> pool) {
        float total = 0F;
        for (JsonObject event : pool) {
            float chance = event.has("chance") ? event.get("chance").getAsFloat() : 1.0F;
            if (chance > 0F) {
                total += chance;
            }
        }

        if (total <= 0F) {
            return null;
        }

        float roll = random.nextFloat() * total;
        float cumulative = 0F;

        for (JsonObject event : pool) {
            float chance = event.has("chance") ? event.get("chance").getAsFloat() : 1.0F;
            if (chance <= 0F) {
                continue;
            }
            cumulative += chance;
            if (roll < cumulative) {
                return event;
            }
        }

        return null;
    }

    private static void applyLockedLevelRange(JsonObject event, int minLevel, int maxLevel) {
        if (event != null && event.has("type") && "pokemon".equals(event.get("type").getAsString())) {
            event.addProperty("minLevel", minLevel);
            event.addProperty("maxLevel", maxLevel);
        }
    }

    private static void dropItems(ServerWorld world, BlockPos pos, JsonObject data) {
        JsonArray items = data.getAsJsonArray("items");
        if (items == null || items.isEmpty()) {
            return;
        }

        String id = items.get(random.nextInt(items.size())).getAsString();
        int min = data.has("min") ? data.get("min").getAsInt() : 1;
        int max = data.has("max") ? data.get("max").getAsInt() : min;
        if (max < min) {
            int temp = min;
            min = max;
            max = temp;
        }

        Item item = Registries.ITEM.get(Identifier.of(id));
        if (item == null || item == net.minecraft.item.Items.AIR) {
            System.out.println("[PocketLuckHandler] Invalid item id: " + id);
            return;
        }

        Block.dropStack(world, pos.up(), new ItemStack(item, randomLevelBetween(min, max)));
    }

    private static void spawnPokemon(ServerWorld world, BlockPos pos, JsonObject data, String themedType) {
        JsonArray pokemons = getPokemonArray(data);
        if (pokemons == null || pokemons.isEmpty()) {
            return;
        }

        String speciesName = normalizeSpeciesName(pokemons.get(random.nextInt(pokemons.size())).getAsString());
        Species species = PokemonSpecies.INSTANCE.getByName(speciesName);
        if (species == null) {
            System.out.println("[PocketLuckHandler] Species not found: " + speciesName);
            return;
        }

        int level = getEffectiveLevel(world, themedType, data);
        float shinyChance = data.has("shinyChance") ? data.get("shinyChance").getAsFloat() : 0.02F;

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(level);
        pokemon.setShiny(random.nextFloat() * 100F < shinyChance);

        Vec3d spawnPos = Vec3d.ofCenter(pos).add(0, 1, 0);
        PokemonEntity entity = pokemon.sendOut(world, spawnPos, null, e -> null);
        if (entity == null) {
            System.out.println("[PocketLuckHandler] Failed to spawn Pokémon: " + speciesName);
        }
    }

    private static JsonArray getPokemonArray(JsonObject event) {
        if (event.has("pokemons") && event.get("pokemons").isJsonArray()) {
            return event.getAsJsonArray("pokemons");
        }
        if (event.has("pokemon") && event.get("pokemon").isJsonArray()) {
            return event.getAsJsonArray("pokemon");
        }
        return null;
    }

    private static int getEffectiveLevel(ServerWorld world, String themedType, JsonObject event) {
        if (event.has("level") && event.get("level").getAsInt() > 0) {
            return event.get("level").getAsInt();
        }

        if (event.has("minLevel") && event.has("maxLevel")) {
            int min = event.get("minLevel").getAsInt();
            int max = event.get("maxLevel").getAsInt();
            if (min > 0 && max > 0) {
                return randomLevelBetween(min, max);
            }
        }

        JsonObject root = POOL_CONFIGS.get(themedType);
        int fromPoolConfig = getLevelFromPoolRoot(world, themedType, root);
        if (fromPoolConfig > 0) {
            return fromPoolConfig;
        }

        int fromLegacy = getLevelFromLegacyConfig(world, themedType);
        if (fromLegacy > 0) {
            return fromLegacy;
        }

        return randomLevelBetween(5, 15);
    }

    private static int getLevelFromPoolRoot(ServerWorld world, String themedType, JsonObject root) {
        if (root == null) {
            return -1;
        }

        if (root.has("levelRange") && root.get("levelRange").isJsonObject()) {
            JsonObject range = root.getAsJsonObject("levelRange");
            int min = range.has("min") ? range.get("min").getAsInt() : -1;
            int max = range.has("max") ? range.get("max").getAsInt() : -1;
            if (min > 0 && max > 0) {
                return randomLevelBetween(min, max);
            }
        }

        int time = getTimeBasedLevel(world, root.getAsJsonArray("timeLeveling"));
        if (time > 0) {
            return time;
        }

        if (root.has("levelWeighting") && root.get("levelWeighting").isJsonArray()) {
            int weighted = getWeightedRandomLevel(root.getAsJsonArray("levelWeighting"));
            if (weighted > 0) {
                return weighted;
            }
        }

        String typeKey = "levelWeighting_" + themedType;
        if (root.has(typeKey) && root.get(typeKey).isJsonArray()) {
            int weighted = getWeightedRandomLevel(root.getAsJsonArray(typeKey));
            if (weighted > 0) {
                return weighted;
            }
        }

        return -1;
    }

    private static int getLevelFromLegacyConfig(ServerWorld world, String themedType) {
        int fromTypes = readLegacyTypeLevelConfig(world, themedType);
        if (fromTypes > 0) {
            return fromTypes;
        }

        if (!Files.exists(LuckyBlockConfigManager.legacyThemedLevelConfigFile())) {
            return -1;
        }

        try (Reader reader = Files.newBufferedReader(LuckyBlockConfigManager.legacyThemedLevelConfigFile(), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("levelWeighting") && json.get("levelWeighting").isJsonArray()) {
                return getWeightedRandomLevel(json.getAsJsonArray("levelWeighting"));
            }
        } catch (Exception e) {
            System.out.println("[PocketLuckHandler] Failed to read legacy level_config.json: " + e.getMessage());
        }

        return -1;
    }

    private static int readLegacyTypeLevelConfig(ServerWorld world, String themedType) {
        if (!Files.exists(LuckyBlockConfigManager.legacyThemedTypeLevelConfigFile())) {
            return -1;
        }

        try (Reader reader = Files.newBufferedReader(LuckyBlockConfigManager.legacyThemedTypeLevelConfigFile(), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            if (json.has("minLevel") && json.has("maxLevel")) {
                int min = json.get("minLevel").getAsInt();
                int max = json.get("maxLevel").getAsInt();
                if (min > 0 && max > 0) {
                    return randomLevelBetween(min, max);
                }
            }

            String timeKey = "timeLeveling_" + themedType;
            if (json.has(timeKey) && json.get(timeKey).isJsonArray()) {
                int time = getTimeBasedLevel(world, json.getAsJsonArray(timeKey));
                if (time > 0) {
                    return time;
                }
            }

            String weightKey = "levelWeighting_" + themedType;
            if (json.has(weightKey) && json.get(weightKey).isJsonArray()) {
                return getWeightedRandomLevel(json.getAsJsonArray(weightKey));
            }
        } catch (Exception e) {
            System.out.println("[PocketLuckHandler] Failed to read legacy lvlconfig_types.json: " + e.getMessage());
        }

        return -1;
    }

    private static int getTimeBasedLevel(ServerWorld world, JsonArray timeLeveling) {
        if (timeLeveling == null || timeLeveling.isEmpty()) {
            return -1;
        }

        long days = (world.getTimeOfDay() / 24000L) + 20;
        for (JsonElement element : timeLeveling) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            if (!obj.has("minDays") || !obj.has("maxDays") || !obj.has("levels")) {
                continue;
            }
            long minDays = obj.get("minDays").getAsLong();
            long maxDays = obj.get("maxDays").getAsLong();
            if (days >= minDays && days <= maxDays && obj.get("levels").isJsonArray()) {
                return getWeightedRandomLevel(obj.getAsJsonArray("levels"));
            }
        }
        return -1;
    }

    private static int getWeightedRandomLevel(JsonArray weights) {
        if (weights == null || weights.isEmpty()) {
            return -1;
        }

        float total = 0F;
        List<JsonObject> valid = new ArrayList<>();
        for (JsonElement element : weights) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            if (!obj.has("min") || !obj.has("max") || !obj.has("chance")) {
                continue;
            }
            float chance = obj.get("chance").getAsFloat();
            if (chance > 0F) {
                total += chance;
                valid.add(obj);
            }
        }

        if (total <= 0F || valid.isEmpty()) {
            return -1;
        }

        float roll = random.nextFloat() * total;
        float cumulative = 0F;
        for (JsonObject obj : valid) {
            cumulative += obj.get("chance").getAsFloat();
            if (roll < cumulative) {
                return randomLevelBetween(obj.get("min").getAsInt(), obj.get("max").getAsInt());
            }
        }

        return -1;
    }

    private static int randomLevelBetween(int min, int max) {
        if (max < min) {
            int temp = min;
            min = max;
            max = temp;
        }
        return min + random.nextInt(max - min + 1);
    }

    private static String normalizeSpeciesName(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        return input
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("♀", "f")
                .replace("♂", "m")
                .replaceAll("[^a-z0-9\\-]", "");
    }

    private static JsonObject createDefaultPool(String type) {
        String normalizedType = LuckyBlockConfigManager.toThemedType(type);
        JsonObject bundled = LuckyBlockConfigManager.loadBundledObject("luckyblockcobblemon/default_configs/pools/themed/" + normalizedType + ".json");
        if (bundled != null) {
            return bundled;
        }

        JsonObject root = new JsonObject();
        root.addProperty("configVersion", LuckyBlockConfigManager.CONFIG_VERSION);
        root.addProperty("_comment", "Emergency fallback themed Lucky Block pool for type: " + normalizedType + ". The bundled default config was not found.");

        JsonArray events = new JsonArray();

        JsonObject pokemon = new JsonObject();
        pokemon.addProperty("type", "pokemon");
        pokemon.addProperty("chance", 85F);
        pokemon.addProperty("shinyChance", 10F);
        JsonArray species = new JsonArray();
        for (String name : defaultSpeciesByType().getOrDefault(normalizedType, List.of("eevee"))) {
            species.add(name);
        }
        pokemon.add("pokemons", species);
        events.add(pokemon);

        JsonObject item = new JsonObject();
        item.addProperty("type", "item");
        item.addProperty("chance", 15F);
        item.addProperty("min", 1);
        item.addProperty("max", 1);
        JsonArray items = new JsonArray();
        for (String id : defaultItemsByType().getOrDefault(normalizedType, List.of("cobblemon:poke_ball"))) {
            items.add(id);
        }
        item.add("items", items);
        events.add(item);

        root.add("events", events);
        return root;
    }

    private static Map<String, List<String>> defaultSpeciesByType() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("fire", List.of("charmander", "charmeleon", "charizard", "vulpix", "ninetales", "growlithe", "arcanine", "ponyta", "rapidash"));
        map.put("water", List.of("squirtle", "wartortle", "blastoise", "psyduck", "golduck", "poliwag", "poliwhirl", "poliwrath", "tentacool", "tentacruel", "slowpoke", "slowbro", "seel", "dewgong", "shellder", "cloyster", "krabby", "kingler", "horsea", "seadra", "goldeen", "seaking", "staryu", "starmie", "magikarp", "gyarados", "lapras", "vaporeon"));
        map.put("grass", List.of("bulbasaur", "ivysaur", "venusaur", "oddish", "gloom", "vileplume", "paras", "parasect", "bellsprout", "weepinbell", "victreebel", "exeggcute", "exeggutor", "tangela"));
        map.put("ground", List.of("sandshrew", "sandslash", "diglett", "dugtrio", "geodude", "graveler", "golem", "onix", "cubone", "marowak", "rhyhorn", "rhydon"));
        map.put("fly", List.of("charizard", "butterfree", "pidgey", "pidgeotto", "pidgeot", "spearow", "fearow", "zubat", "golbat", "farfetchd", "doduo", "dodrio", "scyther", "gyarados", "aerodactyl", "dragonite"));
        map.put("steel", List.of("magnemite", "magneton"));
        map.put("eletric", List.of("pikachu", "raichu", "magnemite", "magneton", "voltorb", "electrode", "electabuzz", "jolteon"));
        map.put("fairy", List.of("clefairy", "clefable", "jigglypuff", "wigglytuff", "mrMime"));
        return map;
    }

    private static Map<String, List<String>> defaultItemsByType() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("fire", List.of("cobblemon:fire_stone", "cobblemon:charcoal"));
        map.put("water", List.of("cobblemon:water_stone", "cobblemon:mystic_water"));
        map.put("grass", List.of("cobblemon:leaf_stone", "cobblemon:miracle_seed"));
        map.put("ground", List.of("cobblemon:soft_sand", "cobblemon:hard_stone"));
        map.put("fly", List.of("cobblemon:sharp_beak", "cobblemon:ancient_feather_ball"));
        map.put("steel", List.of("cobblemon:metal_coat", "cobblemon:iron_ball"));
        map.put("eletric", List.of("cobblemon:thunder_stone", "cobblemon:magnet"));
        map.put("fairy", List.of("cobblemon:moon_stone", "cobblemon:fairy_feather"));
        return map;
    }
}
