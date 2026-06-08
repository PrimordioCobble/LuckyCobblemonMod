package net.crulim.luckblockcobblemon.handler;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.crulim.luckblockcobblemon.config.LuckyBlockConfigManager;
import net.crulim.luckblockcobblemon.config.LuckyBlockSettingsConfig;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LuckyBlockHandlerVanilla {
    private static final Random random = Random.create();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final List<String> legendaryList = new ArrayList<>();
    private static float shinyChance = 2.0F;
    private static int minLevel = 50;
    private static int maxLevel = 70;
    private static final List<TimeLevelRange> timeLeveling = new ArrayList<>();
    private static boolean breakCreative = false;
    private static JsonObject loadedConfig = null;

    private static class TimeLevelRange {
        int minDays, maxDays, minLevel, maxLevel;

        TimeLevelRange(int minDays, int maxDays, int minLevel, int maxLevel) {
            this.minDays = minDays;
            this.maxDays = maxDays;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }
    }

    static {
        loadConfig();
    }

    public static void reloadConfig() {
        loadConfig();
        System.out.println("[LuckyBlockLegendary] Config reloaded successfully.");
    }

    public static void handleLuckEvent(ServerWorld world, BlockPos pos, int luck, Random _random) {
        if (legendaryList.isEmpty()) {
            System.out.println("[LuckyBlockLegendary] Legendary pool is empty.");
            return;
        }

        String speciesName = normalizeSpeciesName(legendaryList.get(random.nextInt(legendaryList.size())));
        Species species = PokemonSpecies.INSTANCE.getByName(speciesName);
        if (species == null) {
            System.out.println("[LuckyBlockLegendary] Invalid species: " + speciesName);
            return;
        }

        int level = getLevel(world, loadedConfig);
        boolean isShiny = random.nextFloat() * 100F < shinyChance;

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(level);
        pokemon.setShiny(isShiny);
        pokemon.getMoveSet().clear();

        int index = 0;
        for (MoveTemplate move : pokemon.getRelearnableMoves()) {
            if (index >= 4) break;
            if (move != null) {
                pokemon.getMoveSet().setMove(index++, new Move(move, move.getPp(), 0));
            }
        }

        Vec3d spawnPos = Vec3d.ofCenter(pos).add(0, 1, 0);
        PokemonEntity entity = pokemon.sendOut(world, spawnPos, null, e -> null);

        if (entity == null) {
            System.out.println("[LuckyBlockLegendary] Failed to spawn: " + speciesName);
        } else {
            System.out.println("[LuckyBlockLegendary] Spawned: " + speciesName + " lvl " + level + " shiny: " + isShiny);
        }
    }

    public static void loadConfig() {
        legendaryList.clear();
        timeLeveling.clear();
        breakCreative = LuckyBlockSettingsConfig.isLegendaryLuckyBlockBreakCreativeAllowed();
        loadedConfig = null;

        try {
            JsonObject json = LuckyBlockConfigManager.loadObjectWithLegacyMigration(
                    LuckyBlockConfigManager.legendaryPoolFile(),
                    LuckyBlockConfigManager.legacyLegendaryPoolFile(),
                    LuckyBlockHandlerVanilla::createDefaultConfig
            );
            loadedConfig = json;

            if (json.has("breakCreative")) {
                breakCreative = json.get("breakCreative").getAsBoolean();
            }

            JsonArray pool = json.has("legendaryPool") && json.get("legendaryPool").isJsonArray()
                    ? json.getAsJsonArray("legendaryPool")
                    : new JsonArray();

            for (JsonElement el : pool) {
                if (el != null && el.isJsonPrimitive()) {
                    String name = el.getAsString();
                    if (!name.isBlank()) {
                        legendaryList.add(name);
                    }
                }
            }

            minLevel = json.has("minLevel") ? json.get("minLevel").getAsInt() : 50;
            maxLevel = json.has("maxLevel") ? json.get("maxLevel").getAsInt() : 70;
            shinyChance = json.has("shinyChance") ? json.get("shinyChance").getAsFloat() : 2.0F;

            if (json.has("timeLeveling") && json.get("timeLeveling").isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray("timeLeveling")) {
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }
                    JsonObject obj = element.getAsJsonObject();
                    if (!obj.has("minDays") || !obj.has("maxDays") || !obj.has("minLevel") || !obj.has("maxLevel")) {
                        continue;
                    }
                    timeLeveling.add(new TimeLevelRange(
                            obj.get("minDays").getAsInt(),
                            obj.get("maxDays").getAsInt(),
                            obj.get("minLevel").getAsInt(),
                            obj.get("maxLevel").getAsInt()
                    ));
                }
            }

            System.out.println("[LuckyBlockLegendary] Loaded config/luckyblockcobblemon/pools/legendary.json with " + legendaryList.size() + " species.");
        } catch (Exception e) {
            legendaryList.clear();
            timeLeveling.clear();
            loadedConfig = new JsonObject();
            System.out.println("[LuckyBlockLegendary] Failed to load legendary pool. No fallback species will spawn: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isBreakCreativeAllowed() {
        return breakCreative;
    }

    private static int getLevel(ServerWorld world, JsonObject event) {
        if (event != null && event.has("minLevel") && event.has("maxLevel")) {
            int min = event.get("minLevel").getAsInt();
            int max = event.get("maxLevel").getAsInt();
            if (min > 0 && max > 0) {
                return randomLevelBetween(min, max);
            }
        }

        long days = world.getTimeOfDay() / 24000L;
        for (TimeLevelRange range : timeLeveling) {
            if (days >= range.minDays && days <= range.maxDays) {
                return randomLevelBetween(range.minLevel, range.maxLevel);
            }
        }

        return randomLevelBetween(minLevel, maxLevel);
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

    private static JsonObject createDefaultConfig() {
        JsonObject bundled = LuckyBlockConfigManager.loadBundledObject("luckyblockcobblemon/default_configs/pools/legendary.json");
        if (bundled != null) {
            return bundled;
        }

        JsonObject root = new JsonObject();
        root.addProperty("configVersion", LuckyBlockConfigManager.CONFIG_VERSION);
        root.addProperty("_comment", "Emergency fallback Legendary Lucky Block pool. The bundled default config was not found.");

        JsonArray pool = new JsonArray();
        pool.add("articuno");
        pool.add("zapdos");
        pool.add("moltres");
        pool.add("mewtwo");
        pool.add("mew");
        root.add("legendaryPool", pool);
        root.addProperty("minLevel", 0);
        root.addProperty("maxLevel", 0);
        root.addProperty("shinyChance", 0.02F);

        JsonArray timeLvl = new JsonArray();
        timeLvl.add(createTimeLevelRange(0, 20, 45, 60));
        timeLvl.add(createTimeLevelRange(21, 50, 61, 80));
        timeLvl.add(createTimeLevelRange(51, 99999, 81, 100));
        root.add("timeLeveling", timeLvl);
        return root;
    }

    private static JsonObject createTimeLevelRange(int minDays, int maxDays, int minLevel, int maxLevel) {
        JsonObject range = new JsonObject();
        range.addProperty("minDays", minDays);
        range.addProperty("maxDays", maxDays);
        range.addProperty("minLevel", minLevel);
        range.addProperty("maxLevel", maxLevel);
        return range;
    }
}
