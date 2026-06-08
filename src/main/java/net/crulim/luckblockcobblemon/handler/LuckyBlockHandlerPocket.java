package net.crulim.luckblockcobblemon.handler;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.google.gson.*;
import net.crulim.luckblockcobblemon.config.LuckyBlockConfigManager;
import net.crulim.luckblockcobblemon.config.LuckyBlockSettingsConfig;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3i;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.minecraft.predicate.entity.LocationPredicate.Builder.createStructure;

public class LuckyBlockHandlerPocket {
    private static final Timer CELEBRATION_TIMER = new Timer("LuckyBlockPocket-Celebration", true);
    private static final List<LevelRangeWeight> weightedLevels = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()  // <- isso resolve!
            .create();
    private static final Random random = Random.create();
    private static final Logger LOGGER = Logger.getLogger(LuckyBlockHandlerPocket.class.getName());

    private static final List<JsonObject> luckPool = new ArrayList<>();
    private static int minLevel = 5;
    private static int maxLevel = 30;
    private static float shinyChancePercent = 5.0F;
    private static final List<TimeBasedLevelRange> timeBasedLeveling = new ArrayList<>();

    private static boolean breakCreative = false;

    public static boolean isBreakCreativeAllowed() {
        return breakCreative;
    }

    private static int randomLevelBetween(int min, int max) {
        if (max < min) {
            int temp = min;
            min = max;
            max = temp;
        }
        return min + random.nextInt(max - min + 1);
    }

    private static class TimeBasedLevelRange {
        int minDays;
        int maxDays;
        List<LevelRangeWeight> levels;

        TimeBasedLevelRange(int minDays, int maxDays, List<LevelRangeWeight> levels) {
            this.minDays = minDays;
            this.maxDays = maxDays;
            this.levels = levels;
        }
    }

    public static void loadConfig() {
        luckPool.clear();
        weightedLevels.clear();
        timeBasedLeveling.clear();
        breakCreative = LuckyBlockSettingsConfig.isDefaultLuckyBlockBreakCreativeAllowed();

        try {
            JsonObject json = LuckyBlockConfigManager.loadPoolObjectWithLegacyMigration(
                    LuckyBlockConfigManager.defaultPoolFile(),
                    LuckyBlockConfigManager.legacyDefaultPoolFile(),
                    LuckyBlockHandlerPocket::createDefaultPoolConfig
            );

            remove150kCelebrationEventIfPresent(LuckyBlockConfigManager.defaultPoolFile().toFile(), json);


            JsonArray poolArray = LuckyBlockConfigManager.getEvents(json);
            for (JsonElement element : poolArray) {
                if (element != null && element.isJsonObject()) {
                    luckPool.add(element.getAsJsonObject());
                }
            }

            shinyChancePercent = json.has("shinyChancePercent")
                    ? json.get("shinyChancePercent").getAsFloat()
                    : 5.0F;

            if (json.has("levelWeighting") && json.get("levelWeighting").isJsonArray()) {
                JsonArray levelArray = json.getAsJsonArray("levelWeighting");
                for (JsonElement el : levelArray) {
                    if (el == null || !el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    if (!obj.has("min") || !obj.has("max") || !obj.has("chance")) continue;
                    int min = obj.get("min").getAsInt();
                    int max = obj.get("max").getAsInt();
                    float chance = obj.get("chance").getAsFloat();
                    if (chance > 0F) {
                        weightedLevels.add(new LevelRangeWeight(min, max, chance));
                    }
                }

                minLevel = -1;
                maxLevel = -1;
            } else if (json.has("levelRange") && json.get("levelRange").isJsonObject()) {
                JsonObject levelRange = json.getAsJsonObject("levelRange");
                minLevel = levelRange.has("min") ? levelRange.get("min").getAsInt() : 5;
                maxLevel = levelRange.has("max") ? levelRange.get("max").getAsInt() : 30;
            } else {
                minLevel = 5;
                maxLevel = 30;
            }

            if (json.has("timeLeveling") && json.get("timeLeveling").isJsonArray()) {
                JsonArray timeArray = json.getAsJsonArray("timeLeveling");
                for (JsonElement timeElement : timeArray) {
                    if (timeElement == null || !timeElement.isJsonObject()) continue;
                    JsonObject timeObj = timeElement.getAsJsonObject();
                    if (!timeObj.has("minDays") || !timeObj.has("maxDays") || !timeObj.has("levels") || !timeObj.get("levels").isJsonArray()) continue;
                    int minDays = timeObj.get("minDays").getAsInt();
                    int maxDays = timeObj.get("maxDays").getAsInt();
                    List<LevelRangeWeight> timeWeights = new ArrayList<>();

                    JsonArray levels = timeObj.getAsJsonArray("levels");
                    for (JsonElement lvl : levels) {
                        if (lvl == null || !lvl.isJsonObject()) continue;
                        JsonObject obj = lvl.getAsJsonObject();
                        if (!obj.has("min") || !obj.has("max") || !obj.has("chance")) continue;
                        int min = obj.get("min").getAsInt();
                        int max = obj.get("max").getAsInt();
                        float chance = obj.get("chance").getAsFloat();
                        if (chance > 0F) {
                            timeWeights.add(new LevelRangeWeight(min, max, chance));
                        }
                    }

                    if (!timeWeights.isEmpty()) {
                        timeBasedLeveling.add(new TimeBasedLevelRange(minDays, maxDays, timeWeights));
                    }
                }
            }

            System.out.println("[LuckyBlockPocket] Loaded config/luckyblockcobblemon/pools/default.json with " + luckPool.size() + " configured events.");
        } catch (Exception e) {
            luckPool.clear();
            weightedLevels.clear();
            timeBasedLeveling.clear();
            System.out.println("[LuckyBlockPocket] Failed to load default pool. No fallback events will run: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Erro ao carregar configuração", e);
        }
    }



    private static void remove150kCelebrationEventIfPresent(File file, JsonObject json) {
        if (json == null) {
            return;
        }

        boolean changed = false;
        JsonArray poolArray;

        if (json.has("events") && json.get("events").isJsonArray()) {
            poolArray = json.getAsJsonArray("events");
        } else if (json.has("luckPool") && json.get("luckPool").isJsonArray()) {
            poolArray = json.getAsJsonArray("luckPool");
        } else {
            return;
        }

        for (int i = poolArray.size() - 1; i >= 0; i--) {
            JsonElement element = poolArray.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }

            JsonObject obj = element.getAsJsonObject();
            if (!obj.has("type") || !obj.has("structure")) {
                continue;
            }

            if ("structure".equals(obj.get("type").getAsString())
                    && "luckblockcobblemon:150k".equals(obj.get("structure").getAsString())) {
                poolArray.remove(i);
                changed = true;
            }
        }

        if (changed) {
            saveConfigJson(file, json);
        }
    }



    private static void saveConfigJson(File file, JsonObject json) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao salvar configuração com evento 150k", e);
        }
    }

    private static void appendConfiguredLegendarySpawn(JsonObject json) {
        if (json.has("legendarySpawn") && json.get("legendarySpawn").isJsonObject()) {
            JsonObject legendarySpawn = json.getAsJsonObject("legendarySpawn");
            if (!legendarySpawn.has("enabled") || !legendarySpawn.get("enabled").getAsBoolean()) {
                return;
            }

            JsonObject event = createLegendaryEvent(
                    legendarySpawn.has("species") && legendarySpawn.get("species").isJsonArray()
                            ? legendarySpawn.getAsJsonArray("species")
                            : null,
                    legendarySpawn.has("chance") ? legendarySpawn.get("chance").getAsFloat() : 0.0F,
                    legendarySpawn.has("minLevel") ? legendarySpawn.get("minLevel").getAsInt() : 50,
                    legendarySpawn.has("maxLevel") ? legendarySpawn.get("maxLevel").getAsInt() : 70,
                    legendarySpawn.has("shinyChance") ? legendarySpawn.get("shinyChance").getAsFloat() : 0.02F
            );

            if (event != null) {
                luckPool.add(event);
            }
            return;
        }

        if (json.has("legendaryPool") && json.get("legendaryPool").isJsonArray()) {
            JsonObject legacyEvent = createLegendaryEvent(
                    json.getAsJsonArray("legendaryPool"),
                    json.has("legendaryChance") ? json.get("legendaryChance").getAsFloat() : 0.0F,
                    json.has("legendaryMinLevel") ? json.get("legendaryMinLevel").getAsInt() : 50,
                    json.has("legendaryMaxLevel") ? json.get("legendaryMaxLevel").getAsInt() : 70,
                    json.has("legendaryShinyChance") ? json.get("legendaryShinyChance").getAsFloat() : 0.02F
            );

            if (legacyEvent != null) {
                luckPool.add(legacyEvent);
            }
        }
    }

    private static JsonObject createLegendaryEvent(JsonArray speciesArray, float chance, int minLegendaryLevel, int maxLegendaryLevel, float legendaryShinyChance) {
        if (speciesArray == null || speciesArray.isEmpty() || chance <= 0.0F) {
            return null;
        }

        JsonArray validSpecies = new JsonArray();
        for (JsonElement element : speciesArray) {
            if (element != null && element.isJsonPrimitive()) {
                String name = element.getAsString();
                if (!name.isBlank()) {
                    validSpecies.add(name);
                }
            }
        }

        if (validSpecies.isEmpty()) {
            return null;
        }

        JsonObject event = new JsonObject();
        event.addProperty("type", "cobblemonp");
        event.add("cobblemons", validSpecies);
        event.addProperty("minLevel", minLegendaryLevel);
        event.addProperty("maxLevel", maxLegendaryLevel);
        event.addProperty("shinyChance", legendaryShinyChance);
        event.addProperty("chance", chance);
        return event;
    }

    private static class LevelRangeWeight {
        int min;
        int max;
        float chance;

        LevelRangeWeight(int min, int max, float chance) {
            this.min = min;
            this.max = max;
            this.chance = chance;
        }
    }

    private static int getWeightedRandomLevel() {
        float total = 0F;
        for (LevelRangeWeight range : weightedLevels) {
            total += range.chance;
        }

        float roll = random.nextFloat() * total;
        float cumulative = 0F;

        for (LevelRangeWeight range : weightedLevels) {
            cumulative += range.chance;
            if (roll < cumulative) {
                return randomLevelBetween(range.min, range.max);
            }
        }

        // fallback se nada for sorteado
        return random.nextBetween(1, 101);
    }

    private static int getTimeBasedLevel(ServerWorld world) {
        long days = world.getTimeOfDay() / 24000L;

        for (TimeBasedLevelRange timeRange : timeBasedLeveling) {
            if (days >= timeRange.minDays && days <= timeRange.maxDays) {
                //System.out.println("[PocketLuckHandler] Tempo atual: " + days + " dias. Usando faixa de " + timeRange.minDays + " a " + timeRange.maxDays);
                float total = 0F;
                for (LevelRangeWeight range : timeRange.levels) {
                    total += range.chance;
                }

                float roll = random.nextFloat() * total;
                float cumulative = 0F;
                for (LevelRangeWeight range : timeRange.levels) {
                    cumulative += range.chance;
                    if (roll < cumulative) {
                        return randomLevelBetween(range.min, range.max);
                    }
                }
            }

        }

        return -1; // nenhum match
    }


    public static void reloadConfig() {
        loadConfig();
        //System.out.println("[LuckyBlockPocket] Config reloaded!");
    }

    public static void triggerLuckEvent(ServerWorld world, BlockPos pos) {
        triggerLuckEventInternal(world, pos, -1, -1);
    }

    public static void triggerLockedLuckEvent(ServerWorld world, BlockPos pos, int forcedMinLevel, int forcedMaxLevel) {
        triggerLuckEventInternal(world, pos, forcedMinLevel, forcedMaxLevel);
    }

    private static void triggerLuckEventInternal(ServerWorld world, BlockPos pos, int forcedMinLevel, int forcedMaxLevel) {
        if (luckPool.isEmpty()) {
            System.out.println("[LuckyBlockPocket] Warning: Luck pool is empty!");
            return;
        }

        List<JsonObject> validPool = filterValidEvents();
        if (validPool.isEmpty()) {
            System.out.println("[LuckyBlockPocket] No valid events to pick from!");
            return;
        }

        JsonObject selected = pickRandomLuck(validPool);
        if (selected == null) {
            System.out.println("[LuckyBlockPocket] No luck event selected!");
            return;
        }

        JsonObject eventToExecute = selected.deepCopy();
        if (forcedMinLevel > 0 && forcedMaxLevel > 0) {
            applyLockedLevelRange(eventToExecute, forcedMinLevel, forcedMaxLevel);
        }

        executeLuck(world, pos, eventToExecute);
    }

    private static void applyLockedLevelRange(JsonObject event, int minLevel, int maxLevel) {
        if (event == null || !event.has("type")) {
            return;
        }

        String type = event.get("type").getAsString();
        if ("cobblemonp".equals(type)
                || "random_cobblemonp".equals(type)
                || "shiny_cobblemonp".equals(type)
                || "multi_cobblemonp".equals(type)) {
            event.addProperty("minLevel", minLevel);
            event.addProperty("maxLevel", maxLevel);
        }
    }

    private static boolean isEventCurrentlyActive(JsonObject event) {
        if (event == null) {
            return false;
        }

        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

        try {
            if (event.has("startDate")) {
                LocalDate startDate = LocalDate.parse(event.get("startDate").getAsString());
                if (today.isBefore(startDate)) {
                    return false;
                }
            }

            if (event.has("endDate")) {
                LocalDate endDate = LocalDate.parse(event.get("endDate").getAsString());
                if (today.isAfter(endDate)) {
                    return false;
                }
            }

            if (event.has("expiresAt")) {
                LocalDate expiresAt = LocalDate.parse(event.get("expiresAt").getAsString());
                if (today.isAfter(expiresAt)) {
                    return false;
                }
            }
        } catch (DateTimeParseException e) {
            LOGGER.warning("[LuckyBlockPocket] Invalid event date format: " + event);
            return false;
        }

        return true;
    }

    private static List<JsonObject> filterValidEvents() {
        List<JsonObject> valid = new ArrayList<>();

        for (JsonObject event : luckPool) {
            if (event == null || event.isJsonNull()) continue;
            if (!event.has("type")) continue;
            if (!isEventCurrentlyActive(event)) continue;

            String type = event.get("type").getAsString();

            switch (type) {
                case "item", "cobbleitem" -> {
                    if (event.has("items") && !event.getAsJsonArray("items").isEmpty()) {
                        valid.add(event);
                    }
                }
                case "cobblemonp", "random_cobblemonp", "shiny_cobblemonp" -> valid.add(event);
                case "structure" -> {
                    if (!LuckyBlockSettingsConfig.areStructureEventsEnabled()) {
                        continue;
                    }
                    if ((event.has("structure") && !event.get("structure").getAsString().isEmpty()) ||
                            (event.has("structures") && !event.getAsJsonArray("structures").isEmpty())) {
                        valid.add(event);
                    }
                }
                case "cobblemon_allitems" -> {
                    if (event.has("items") && !event.getAsJsonArray("items").isEmpty()) {
                        valid.add(event);
                    }
                }
                case "multi_cobblemonp" -> valid.add(event);
                default -> System.out.println("[LuckyBlockPocket] Unknown type ignored in filter: " + type);
            }
        }

        return valid;
    }

    private static JsonObject pickRandomLuck(List<JsonObject> pool) {
        if (pool.isEmpty()) return null;

        List<JsonObject> validPool = new ArrayList<>();
        float totalChance = 0F;

        for (JsonObject obj : pool) {
            float chance = obj.has("chance") ? obj.get("chance").getAsFloat() : 1.0F;
            if (chance > 0F) {
                totalChance += chance;
                validPool.add(obj);
            }
        }

        if (totalChance <= 0F || validPool.isEmpty()) {
            System.out.println("[LuckyBlockPocket] No event selected because all configured chances are 0 or invalid.");
            return null;
        }

        float roll = random.nextFloat() * totalChance;
        float cumulative = 0F;

        for (JsonObject obj : validPool) {
            float chance = obj.has("chance") ? obj.get("chance").getAsFloat() : 1.0F;
            cumulative += chance;
            if (roll < cumulative) {
                return obj;
            }
        }

        // Fallback absoluto (nunca deve acontecer)
        return validPool.get(random.nextInt(validPool.size()));
    }



    private static void executeLuck(ServerWorld world, BlockPos pos, JsonObject data) {
        //System.out.println("[LuckyBlockPocket] Evento sorteado: " + data.toString());
        String type = data.get("type").getAsString();


        switch (type) {
            case "item" -> dropItem(world, pos, data);
            case "cobbleitem" -> dropCobbleItem(world, pos, data);
            case "cobblemonp" -> spawnCobblemon(world, pos, data, false);
            case "random_cobblemonp" -> spawnRandomCobblemon(world, pos, data);
            case "shiny_cobblemonp" -> spawnCobblemon(world, pos, data, true);
            case "structure" -> spawnStructure(world, pos, data);
            case "multi_cobblemonp" -> spawnMultipleCobblemons(world, pos, data);
            case "cobblemon_allitems" -> dropCobblemonAllItems(world, pos, data);
            default -> System.out.println("[LuckyBlockPocket] Unknown luck type: " + type);
        }
    }

    private static void dropItem(ServerWorld world, BlockPos pos, JsonObject data) {
        JsonArray items = data.getAsJsonArray("items");
        if (items == null || items.isEmpty()) return;

        String id = items.get(random.nextInt(items.size())).getAsString();
        int min = data.get("min").getAsInt();
        int max = data.get("max").getAsInt();
        Item item = Registries.ITEM.get(Identifier.of(id));
        if (item == null || item == net.minecraft.item.Items.AIR) {
            LOGGER.warning("[LuckyBlockPocket] Item não encontrado ou inválido: " + id);
            return;
        }
        ItemStack stack = new ItemStack(item, random.nextBetween(min, max + 1));
        Block.dropStack(world, pos.up(), stack);

    }

    private static void dropItemsFromList(ServerWorld world, BlockPos pos, JsonObject data) {
        JsonArray items = data.getAsJsonArray("items");
        if (items == null || items.isEmpty()) return;

        String id = items.get(random.nextInt(items.size())).getAsString();
        int min = data.get("min").getAsInt();
        int max = data.get("max").getAsInt();
        Item item = Registries.ITEM.get(Identifier.of(id));

        if (item == Items.AIR) return;
        ItemStack stack = new ItemStack(item, random.nextBetween(min, max));
        Block.dropStack(world, pos.up(), stack);
    }

    private static void dropCobbleItem(ServerWorld world, BlockPos pos, JsonObject data) {
        dropItemsFromList(world, pos, data);
    }

    private static void dropCobblemonAllItems(ServerWorld world, BlockPos pos, JsonObject data) {
        dropItemsFromList(world, pos, data);
    }

    private static PokemonEntity spawnCobblemonAt(ServerWorld world, Vec3d spawnPos, JsonObject data, boolean forceShiny) {
        String speciesName;
        if (data.has("species")) {
            speciesName = data.get("species").getAsString();
        } else if (data.has("cobblemons")) {
            JsonArray list = data.getAsJsonArray("cobblemons");
            if (list.isEmpty()) {
                return null;
            }
            speciesName = list.get(random.nextInt(list.size())).getAsString();
        } else {
            return null;
        }

        speciesName = normalizeSpeciesName(speciesName);

        Species species = PokemonSpecies.INSTANCE.getByName(speciesName);
        if (species == null) {
            return null;
        }

        int level;

        if (data.has("level") && data.get("level").getAsInt() > 0) {
            level = data.get("level").getAsInt();
        } else if (data.has("minLevel") && data.has("maxLevel")) {
            int minLevelLocal = data.get("minLevel").getAsInt();
            int maxLevelLocal = data.get("maxLevel").getAsInt();
            if (minLevelLocal > 0 && maxLevelLocal > 0) {
                level = randomLevelBetween(minLevelLocal, maxLevelLocal);
            } else if (!timeBasedLeveling.isEmpty()) {
                int timeBased = getTimeBasedLevel(world);
                level = (timeBased > 0) ? timeBased : getWeightedRandomLevel();
            } else {
                level = getWeightedRandomLevel();
            }
        } else if (!timeBasedLeveling.isEmpty()) {
            int timeBased = getTimeBasedLevel(world);
            level = (timeBased > 0) ? timeBased : getWeightedRandomLevel();
        } else {
            level = getWeightedRandomLevel();
        }

        float effectiveShinyChance = data.has("shinyChance") ? data.get("shinyChance").getAsFloat() : shinyChancePercent;
        boolean isShiny = forceShiny || (random.nextFloat() * 100F < effectiveShinyChance);

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(level);
        pokemon.setShiny(isShiny);

        pokemon.getMoveSet().clear();
        Iterable<MoveTemplate> relearnableMoves = pokemon.getRelearnableMoves();
        int index = 0;

        for (MoveTemplate template : relearnableMoves) {
            if (template != null && index < 4) {
                Move move = new Move(template, template.getPp(), 0);
                pokemon.getMoveSet().setMove(index, move);
                index++;
            }
        }

        PokemonEntity entity = pokemon.sendOut(world, spawnPos, null, e -> null);

        if (entity == null) {
            System.out.println("[LuckyBlockPocket] Failed to spawn Cobblemon: " + speciesName);
        } else {
            System.out.println("[LuckyBlockPocket] Cobblemon spawned successfully: " + speciesName);
        }

        return entity;
    }

    private static void spawnCobblemon(ServerWorld world, BlockPos pos, JsonObject data, boolean forceShiny) {
        spawnCobblemonAt(world, Vec3d.ofCenter(pos).add(0, 1, 0), data, forceShiny);
    }



    private static void spawnRandomCobblemon(ServerWorld world, BlockPos pos, JsonObject data) {
        List<Species> allSpecies = new ArrayList<>(PokemonSpecies.INSTANCE.getSpecies());
        if (allSpecies.isEmpty()) return;

        Species species = allSpecies.get(random.nextInt(allSpecies.size()));
        String speciesName = species.getName().toLowerCase(Locale.ROOT).replace(" ", "_");

        int level;
        if (data.has("minLevel") && data.has("maxLevel")) {
            int min = data.get("minLevel").getAsInt();
            int max = data.get("maxLevel").getAsInt();
            if (min > 0 && max > 0) {
                level = randomLevelBetween(min, max);
            } else if (!timeBasedLeveling.isEmpty()) {
                int timeBased = getTimeBasedLevel(world);
                level = (timeBased > 0) ? timeBased : getWeightedRandomLevel();
            } else {
                level = getWeightedRandomLevel();
            }
        } else if (!timeBasedLeveling.isEmpty()) {
            int timeBased = getTimeBasedLevel(world);
            level = (timeBased > 0) ? timeBased : getWeightedRandomLevel();
        } else {
            level = getWeightedRandomLevel();
        }

        float shinyChance = data.has("shinyChance") ? data.get("shinyChance").getAsFloat() : shinyChancePercent;

        JsonObject fakeData = new JsonObject();
        fakeData.addProperty("species", speciesName);
        fakeData.addProperty("level", level);
        fakeData.addProperty("shinyChance", shinyChance);
        spawnCobblemon(world, pos, fakeData, false);
    }

    private static void spawnMultipleCobblemons(ServerWorld world, BlockPos pos, JsonObject data) {
        int min = data.has("min") ? data.get("min").getAsInt() : 1;
        int max = data.has("max") ? data.get("max").getAsInt() : 3;
        int count = random.nextBetween(min, max + 1);

        float shinyChance = data.has("shinyChance") ? data.get("shinyChance").getAsFloat() : shinyChancePercent;

        List<Species> allSpecies = new ArrayList<>(PokemonSpecies.INSTANCE.getSpecies());
        if (allSpecies.isEmpty()) return;

        for (int i = 0; i < count; i++) {
            Species species = allSpecies.get(random.nextInt(allSpecies.size()));
            String speciesName = species.getName()
                    .toLowerCase(Locale.ROOT)
                    .replace(" ", "")
                    .replace("-", "")
                    .replace("_", "")
                    .replace("-o", "o")
                    .replace("'", "");

            int level;
            if (data.has("minLevel") && data.has("maxLevel")) {
                int minLevelLocal = data.get("minLevel").getAsInt();
                int maxLevelLocal = data.get("maxLevel").getAsInt();
                if (minLevelLocal > 0 && maxLevelLocal > 0) {
                    level = randomLevelBetween(minLevelLocal, maxLevelLocal);
                } else if (!timeBasedLeveling.isEmpty()) {
                    int timeBased = getTimeBasedLevel(world);
                    level = (timeBased > 0) ? timeBased : getWeightedRandomLevel();
                } else {
                    level = getWeightedRandomLevel();
                }
            } else if (!timeBasedLeveling.isEmpty()) {
                int timeBased = getTimeBasedLevel(world);
                level = (timeBased > 0) ? timeBased : getWeightedRandomLevel();
            } else {
                level = getWeightedRandomLevel();
            }

            JsonObject fakeData = new JsonObject();
            fakeData.addProperty("species", speciesName);
            fakeData.addProperty("level", level);
            fakeData.addProperty("shinyChance", shinyChance);

            spawnCobblemon(world, pos, fakeData, false);
        }
    }






    private static String normalizeSpeciesName(String input) {
        if (input == null || input.isBlank()) return "";

        return input
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("♀", "f")
                .replace("♂", "m")
                .replaceAll("[^a-z0-9\\-]", ""); // remove qualquer caractere especial
    }


    private static void spawnStructure(ServerWorld world, BlockPos pos, JsonObject data) {
        if (!LuckyBlockSettingsConfig.areStructureEventsEnabled()) {
            System.out.println("[LuckyBlockPocket] Structure event blocked by config/luckyblockcobblemon/settings.json (enableStructureEvents=false).");
            return;
        }

        String structureId;
        JsonObject selectedStructureData = null;

        if (data.has("structures")) {
            JsonArray structures = data.getAsJsonArray("structures");
            if (structures.isEmpty()) {
                System.out.println("[LuckyBlockPocket] Lista de estruturas vazia.");
                return;
            }

            List<JsonObject> weightedList = new ArrayList<>();

            for (JsonElement el : structures) {
                JsonObject obj = el.getAsJsonObject();
                int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 1;
                for (int i = 0; i < weight; i++) {
                    weightedList.add(obj);
                }
            }

            if (weightedList.isEmpty()) {
                System.out.println("[LuckyBlockPocket] Weighted structure list is empty.");
                return;
            }

            selectedStructureData = weightedList.get(random.nextInt(weightedList.size()));
            structureId = selectedStructureData.get("id").getAsString();
        } else if (data.has("structure")) {
            structureId = data.get("structure").getAsString();
        } else {
            System.out.println("[LuckyBlockPocket] No structure ID found in the config.");
            return;
        }

        Identifier id = Identifier.of(structureId);
        var templateManager = world.getServer().getStructureTemplateManager();
        var optional = templateManager.getTemplate(id);

        if (optional.isEmpty()) {
            System.out.println("[LuckyBlockPocket] Structure not found: " + id);
            return;
        }

        var template = optional.get();

        var nearestPlayer = world.getClosestPlayer(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                64.0D,
                false
        );

        Direction playerFacing = nearestPlayer != null ? nearestPlayer.getHorizontalFacing() : Direction.NORTH;
        BlockRotation rotation = getStructureRotationFacingPlayer(playerFacing);

        Vec3i originalSize = template.getSize();
        Vec3i rotatedSize = getRotatedSize(originalSize, rotation);

        int forwardDistance = Math.max(5, getIntSetting(data, selectedStructureData, "forwardDistance", 5));

        BlockPos anchorCenter;
        if (nearestPlayer != null) {
            anchorCenter = nearestPlayer.getBlockPos().offset(playerFacing, forwardDistance);
        } else {
            anchorCenter = pos.offset(playerFacing, forwardDistance);
        }

        anchorCenter = new BlockPos(anchorCenter.getX(), pos.getY(), anchorCenter.getZ());

        BlockPos placementPos = anchorCenter.add(
                -(rotatedSize.getX() / 2),
                0,
                -(rotatedSize.getZ() / 2)
        );

        placementPos = applyRotationOriginCompensation(placementPos, originalSize, rotation);

        if (structureId.equals("luckblockcobblemon:luckornot")) {
            placementPos = placementPos.down(4);
        }

        StructurePlacementData placementData = new StructurePlacementData();
        placementData.setRotation(rotation);

        template.place(
                world,
                placementPos,
                placementPos,
                placementData,
                random,
                3
        );

        if (getBooleanSetting(data, selectedStructureData, "goldenFireworksMoment", false)) {
            runGoldenFireworksMoment(world, anchorCenter, rotatedSize, data, selectedStructureData);
        }
    }

    private static BlockRotation getStructureRotationFacingPlayer(Direction playerFacing) {
        Direction targetFront = playerFacing.getOpposite();

        return switch (targetFront) {
            case SOUTH -> BlockRotation.NONE;
            case WEST -> BlockRotation.CLOCKWISE_90;
            case NORTH -> BlockRotation.CLOCKWISE_180;
            case EAST -> BlockRotation.COUNTERCLOCKWISE_90;
            default -> BlockRotation.NONE;
        };
    }

    private static Vec3i getRotatedSize(Vec3i originalSize, BlockRotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 ->
                    new Vec3i(originalSize.getZ(), originalSize.getY(), originalSize.getX());
            default ->
                    new Vec3i(originalSize.getX(), originalSize.getY(), originalSize.getZ());
        };
    }

    private static BlockPos applyRotationOriginCompensation(BlockPos placementPos, Vec3i originalSize, BlockRotation rotation) {
        return switch (rotation) {
            case NONE -> placementPos;
            case CLOCKWISE_90 -> placementPos.add(originalSize.getZ() - 1, 0, 0);
            case CLOCKWISE_180 -> placementPos.add(originalSize.getX() - 1, 0, originalSize.getZ() - 1);
            case COUNTERCLOCKWISE_90 -> placementPos.add(0, 0, originalSize.getX() - 1);
        };
    }

    private static boolean getBooleanSetting(JsonObject eventData, JsonObject selectedStructureData, String key, boolean fallback) {
        if (selectedStructureData != null && selectedStructureData.has(key)) {
            return selectedStructureData.get(key).getAsBoolean();
        }
        if (eventData != null && eventData.has(key)) {
            return eventData.get(key).getAsBoolean();
        }
        return fallback;
    }

    private static int getIntSetting(JsonObject eventData, JsonObject selectedStructureData, String key, int fallback) {
        if (selectedStructureData != null && selectedStructureData.has(key)) {
            return selectedStructureData.get(key).getAsInt();
        }
        if (eventData != null && eventData.has(key)) {
            return eventData.get(key).getAsInt();
        }
        return fallback;
    }

    private static float getFloatSetting(JsonObject eventData, JsonObject selectedStructureData, String key, float fallback) {
        if (selectedStructureData != null && selectedStructureData.has(key)) {
            return selectedStructureData.get(key).getAsFloat();
        }
        if (eventData != null && eventData.has(key)) {
            return eventData.get(key).getAsFloat();
        }
        return fallback;
    }

    private static String getStringSetting(JsonObject eventData, JsonObject selectedStructureData, String key, String fallback) {
        if (selectedStructureData != null && selectedStructureData.has(key)) {
            return selectedStructureData.get(key).getAsString();
        }
        if (eventData != null && eventData.has(key)) {
            return eventData.get(key).getAsString();
        }
        return fallback;
    }

    private static JsonArray getArraySetting(JsonObject eventData, JsonObject selectedStructureData, String key) {
        if (selectedStructureData != null && selectedStructureData.has(key) && selectedStructureData.get(key).isJsonArray()) {
            return selectedStructureData.getAsJsonArray(key);
        }
        if (eventData != null && eventData.has(key) && eventData.get(key).isJsonArray()) {
            return eventData.getAsJsonArray(key);
        }
        return null;
    }

    private static void runGoldenFireworksMoment(ServerWorld world, BlockPos anchorCenter, Vec3i structureSize, JsonObject eventData, JsonObject selectedStructureData) {
        double centerX = anchorCenter.getX() + 0.5D;
        double centerZ = anchorCenter.getZ() + 0.5D;
        double topY = anchorCenter.getY() + Math.max(1, structureSize.getY());
        double skyY = topY + getIntSetting(eventData, selectedStructureData, "skyHeight", 10);

        int fireworkCount = getIntSetting(eventData, selectedStructureData, "fireworkCount", 10);
        int particleBursts = getIntSetting(eventData, selectedStructureData, "particleBursts", 8);
        int itemRainCount = getIntSetting(eventData, selectedStructureData, "itemRainCount", 18);
        int pokemonRainCount = getIntSetting(eventData, selectedStructureData, "pokemonRainCount", 1);
        int messageRadius = getIntSetting(eventData, selectedStructureData, "messageRadius", 64);

        int secondWaveDelayMs = getIntSetting(eventData, selectedStructureData, "secondWaveDelayMs", 1200);
        int secondWaveFireworkCount = getIntSetting(eventData, selectedStructureData, "secondWaveFireworkCount", 10);
        int secondWaveParticleBursts = getIntSetting(eventData, selectedStructureData, "secondWaveParticleBursts", 12);

        int horizontalRadiusX = Math.max(1, getIntSetting(eventData, selectedStructureData, "effectRadiusX", 2));
        int horizontalRadiusZ = Math.max(1, getIntSetting(eventData, selectedStructureData, "effectRadiusZ", 2));

        double effectX = centerX + getIntSetting(eventData, selectedStructureData, "effectOffsetX", 0);
        double effectY = topY + getIntSetting(eventData, selectedStructureData, "effectOffsetY", 0);
        double effectZ = centerZ + getIntSetting(eventData, selectedStructureData, "effectOffsetZ", 0);

        double itemSkyY = skyY + getIntSetting(eventData, selectedStructureData, "effectOffsetY", 0);

        launchGoldenFireworks(world, effectX, effectY + 1.0D, effectZ, fireworkCount);
        spawnGoldenParticles(world, effectX, effectY + 1.0D, effectZ, horizontalRadiusX, horizontalRadiusZ, particleBursts);
        playGoldenCelebrationSounds(world, BlockPos.ofFloored(effectX, effectY, effectZ));
        spawnGoldenItemRain(world, effectX, itemSkyY, effectZ, horizontalRadiusX, horizontalRadiusZ, itemRainCount, eventData, selectedStructureData);
        spawnGoldenPokemonRain(world, effectX, itemSkyY, effectZ, horizontalRadiusX, horizontalRadiusZ, pokemonRainCount, eventData, selectedStructureData);

        if (getBooleanSetting(eventData, selectedStructureData, "showMessage", true)) {
            showGoldenCelebrationTitle(world, effectX, effectY, effectZ, messageRadius, eventData, selectedStructureData);
        }

        scheduleGoldenSecondWave(
                world,
                effectX,
                effectY + 1.0D,
                effectZ,
                horizontalRadiusX,
                horizontalRadiusZ,
                secondWaveDelayMs,
                secondWaveFireworkCount,
                secondWaveParticleBursts
        );
    }

    private static Vec3d getCelebrationCenter(BlockPos placementPos, Vec3i originalSize, Vec3i rotatedSize, BlockRotation rotation, JsonObject eventData, JsonObject selectedStructureData) {
        double baseX = placementPos.getX() + (rotatedSize.getX() / 2.0D) + 0.5D;
        double baseY = placementPos.getY() + Math.max(1, rotatedSize.getY());
        double baseZ = placementPos.getZ() + (rotatedSize.getZ() / 2.0D) + 0.5D;

        int offsetX = getIntSetting(eventData, selectedStructureData, "effectOffsetX", 0);
        int offsetY = getIntSetting(eventData, selectedStructureData, "effectOffsetY", 0);
        int offsetZ = getIntSetting(eventData, selectedStructureData, "effectOffsetZ", 0);

        Vec3i rotatedOffset = rotateLocalOffset(offsetX, offsetY, offsetZ, originalSize, rotation);

        return new Vec3d(
                baseX + rotatedOffset.getX(),
                baseY + rotatedOffset.getY(),
                baseZ + rotatedOffset.getZ()
        );
    }

    private static Vec3i rotateLocalOffset(int x, int y, int z, Vec3i originalSize, BlockRotation rotation) {
        return switch (rotation) {
            case NONE -> new Vec3i(x, y, z);
            case CLOCKWISE_90 -> new Vec3i(-z, y, x);
            case CLOCKWISE_180 -> new Vec3i(-x, y, -z);
            case COUNTERCLOCKWISE_90 -> new Vec3i(z, y, -x);
        };
    }

    private static void scheduleGoldenSecondWave(ServerWorld world, double centerX, double y, double centerZ, int radiusX, int radiusZ, int delayMs, int fireworkCount, int particleBursts) {
        CELEBRATION_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                if (world.getServer() == null) {
                    return;
                }

                world.getServer().execute(() -> {
                    BlockPos centerPos = BlockPos.ofFloored(centerX, y, centerZ);

                    launchGoldenFireworks(world, centerX, y + 0.5D, centerZ, fireworkCount);
                    spawnGoldenParticles(world, centerX, y + 0.5D, centerZ, radiusX, radiusZ, particleBursts);
                    world.playSound(null, centerPos, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 1.1F, 1.0F);
                    world.playSound(null, centerPos, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.0F, 1.0F);
                });
            }
        }, Math.max(0, delayMs));
    }

    private static void launchGoldenFireworks(ServerWorld world, double centerX, double y, double centerZ, int count) {
        for (int i = 0; i < count; i++) {
            double x = centerX + random.nextBetween(-3, 3);
            double z = centerZ + random.nextBetween(-3, 3);
            FireworkRocketEntity rocket = new FireworkRocketEntity(world, x, y, z, new ItemStack(Items.FIREWORK_ROCKET));
            world.spawnEntity(rocket);
        }
    }

    private static void spawnGoldenParticles(ServerWorld world, double centerX, double y, double centerZ, int radiusX, int radiusZ, int bursts) {
        for (int i = 0; i < bursts; i++) {
            double x = centerX + random.nextBetween(-radiusX, radiusX);
            double z = centerZ + random.nextBetween(-radiusZ, radiusZ);

            world.spawnParticles(ParticleTypes.FIREWORK, x, y + 0.4D, z, 18, 0.35D, 0.25D, 0.35D, 0.02D);
            world.spawnParticles(ParticleTypes.END_ROD, x, y + 0.8D, z, 12, 0.45D, 0.25D, 0.45D, 0.02D);
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.6D, z, 10, 0.30D, 0.20D, 0.30D, 0.02D);
        }
    }

    private static void playGoldenCelebrationSounds(ServerWorld world, BlockPos centerPos) {
        world.playSound(null, centerPos, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.2F, 1.0F);
        world.playSound(null, centerPos, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.0F, 1.0F);
        world.playSound(null, centerPos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8F, 1.0F);
    }

    private static void spawnGoldenItemRain(ServerWorld world, double centerX, double skyY, double centerZ, int radiusX, int radiusZ, int count, JsonObject eventData, JsonObject selectedStructureData) {
        JsonArray configuredItems = getArraySetting(eventData, selectedStructureData, "skyItems");

        List<String> pool = new ArrayList<>();
        if (configuredItems != null && !configuredItems.isEmpty()) {
            for (JsonElement element : configuredItems) {
                pool.add(element.getAsString());
            }
        } else {
            pool.add("minecraft:gold_ingot");
            pool.add("minecraft:gold_nugget");
            pool.add("cobblemon:rare_candy");
        }

        for (int i = 0; i < count; i++) {
            String chosenId = pool.get(random.nextInt(pool.size()));
            Item item = Registries.ITEM.get(Identifier.of(chosenId));
            if (item == Items.AIR) {
                continue;
            }

            int amount = getCelebrationItemAmount(chosenId);
            ItemStack stack = new ItemStack(item, amount);

            double x = centerX + random.nextBetween(-radiusX, radiusX) + (random.nextDouble() - 0.5D);
            double z = centerZ + random.nextBetween(-radiusZ, radiusZ) + (random.nextDouble() - 0.5D);

            ItemEntity entity = new ItemEntity(world, x, skyY, z, stack);
            entity.setPickupDelay(20);
            entity.setVelocity((random.nextDouble() - 0.5D) * 0.08D, -0.08D, (random.nextDouble() - 0.5D) * 0.08D);
            world.spawnEntity(entity);
        }
    }

    private static int getCelebrationItemAmount(String itemId) {
        return switch (itemId) {
            case "minecraft:gold_nugget" -> random.nextBetween(4, 10);
            case "minecraft:gold_ingot" -> random.nextBetween(1, 3);
            case "cobblemon:rare_candy" -> random.nextBetween(1, 2);
            default -> 1;
        };
    }

    private static void spawnGoldenPokemonRain(ServerWorld world, double centerX, double skyY, double centerZ, int radiusX, int radiusZ, int count, JsonObject eventData, JsonObject selectedStructureData) {
        JsonArray configuredPokemon = getArraySetting(eventData, selectedStructureData, "celebrationPokemon");

        List<String> pokemonPool = new ArrayList<>();
        if (configuredPokemon != null && !configuredPokemon.isEmpty()) {
            for (JsonElement element : configuredPokemon) {
                pokemonPool.add(element.getAsString());
            }
        } else {
            pokemonPool.add("mewtwo");
            pokemonPool.add("gholdengo");
            pokemonPool.add("zapdos");
            pokemonPool.add("charizard_shiny");
            pokemonPool.add("ditto");
        }

        int minLevelLocal = getIntSetting(eventData, selectedStructureData, "celebrationPokemonMinLevel", 70);
        int maxLevelLocal = getIntSetting(eventData, selectedStructureData, "celebrationPokemonMaxLevel", 70);
        float shinyChanceLocal = getFloatSetting(eventData, selectedStructureData, "celebrationPokemonShinyChance", 0.02F);

        for (int i = 0; i < count; i++) {
            String selectedPokemon = pokemonPool.get(random.nextInt(pokemonPool.size()));
            boolean forceShiny = "charizard_shiny".equalsIgnoreCase(selectedPokemon);
            String speciesName = forceShiny ? "charizard" : selectedPokemon;

            JsonObject pokemonEvent = new JsonObject();
            pokemonEvent.addProperty("species", normalizeSpeciesName(speciesName));
            pokemonEvent.addProperty("minLevel", minLevelLocal);
            pokemonEvent.addProperty("maxLevel", maxLevelLocal);
            pokemonEvent.addProperty("shinyChance", shinyChanceLocal);

            double x = centerX + random.nextBetween(-radiusX, radiusX);
            double z = centerZ + random.nextBetween(-radiusZ, radiusZ);

            PokemonEntity entity = spawnCobblemonAt(world, new Vec3d(x, skyY, z), pokemonEvent, forceShiny);
            if (entity != null) {
                entity.setVelocity((random.nextDouble() - 0.5D) * 0.05D, -0.15D, (random.nextDouble() - 0.5D) * 0.05D);
            }
        }
    }


    private static void showGoldenCelebrationTitle(ServerWorld world, double centerX, double y, double centerZ, int radius, JsonObject eventData, JsonObject selectedStructureData) {
        String title = getStringSetting(eventData, selectedStructureData, "messageTitle", "150K!");
        String subtitle = getStringSetting(eventData, selectedStructureData, "messageSubtitle", "Thank you!");
        String chatMessage = getStringSetting(eventData, selectedStructureData, "chatMessage", "The CobbleKanto team thanks you for being part of this journey!");

        Vec3d center = new Vec3d(centerX, y, centerZ);
        double maxDistanceSq = radius * radius;

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(center) > maxDistanceSq) {
                continue;
            }

            player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 50, 20));
            player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title).formatted(Formatting.GOLD, Formatting.BOLD)));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(subtitle).formatted(Formatting.YELLOW)));

            player.sendMessage(
                    Text.literal("[CobbleKanto] ").formatted(Formatting.GOLD, Formatting.BOLD)
                            .append(Text.literal(chatMessage).formatted(Formatting.YELLOW)),
                    false
            );
        }
    }

    private static JsonObject createLevelRange(int min, int max, float chance) {
        JsonObject obj = new JsonObject();
        obj.addProperty("min", min);
        obj.addProperty("max", max);
        obj.addProperty("chance", chance);
        return obj;
    }

    private static JsonObject createStructureJson(String id, int weight) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("weight", weight);
        return obj;
    }


    private static JsonObject createDefaultPoolConfig() {
        JsonObject bundled = LuckyBlockConfigManager.loadBundledObject("luckyblockcobblemon/default_configs/pools/default.json");
        if (bundled != null) {
            return bundled;
        }

        JsonObject fallback = new JsonObject();
        fallback.addProperty("configVersion", LuckyBlockConfigManager.CONFIG_VERSION);
        fallback.addProperty("_comment", "Emergency fallback default Lucky Block pool. The bundled default config was not found.");

        JsonArray events = new JsonArray();

        JsonObject itemDrop = new JsonObject();
        itemDrop.addProperty("type", "item");
        itemDrop.addProperty("chance", 2.5F);
        JsonArray items = new JsonArray();
        items.add("minecraft:diamond");
        items.add("minecraft:gold_ingot");
        items.add("minecraft:iron_ingot");
        itemDrop.add("items", items);
        itemDrop.addProperty("min", 1);
        itemDrop.addProperty("max", 6);
        events.add(itemDrop);

        JsonObject cobblemonSpawn = new JsonObject();
        cobblemonSpawn.addProperty("type", "cobblemonp");
        cobblemonSpawn.addProperty("chance", 10.98F);
        cobblemonSpawn.addProperty("minLevel", 5);
        cobblemonSpawn.addProperty("maxLevel", 30);
        JsonArray cobblemons = new JsonArray();
        cobblemons.add("bulbasaur");
        cobblemons.add("charmander");
        cobblemons.add("squirtle");
        cobblemons.add("pikachu");
        cobblemons.add("eevee");
        cobblemonSpawn.add("cobblemons", cobblemons);
        events.add(cobblemonSpawn);

        fallback.add("events", events);
        return fallback;
    }

    private static void generateDefaultConfig(File file) {
        try {
            JsonObject defaultConfig = new JsonObject();
            JsonArray pool = new JsonArray();
            JsonArray note = new JsonArray();
            note.add("Creative-mode activation is controlled by config/luckyblockcobblemon/settings.json.");
            note.add("This pool controls regular Lucky Block events only.");
            defaultConfig.add("_note", note);
            JsonObject legendarySpawn = new JsonObject();
            legendarySpawn.addProperty("enabled", false);
            legendarySpawn.addProperty("chance", 0.25F);
            legendarySpawn.addProperty("minLevel", 50);
            legendarySpawn.addProperty("maxLevel", 70);
            legendarySpawn.addProperty("shinyChance", 0.02F);
            JsonArray legendarySpecies = new JsonArray();
            legendarySpecies.add("articuno");
            legendarySpecies.add("zapdos");
            legendarySpecies.add("moltres");
            legendarySpecies.add("mewtwo");
            legendarySpecies.add("mew");
            legendarySpawn.add("species", legendarySpecies);
            defaultConfig.addProperty("__note_legendarySpawn", "Optional helper for regular lucky blocks. Set legendarySpawn.enabled to true to let the regular block spawn legendary Pokemon without manually creating a luckPool event.");
            defaultConfig.add("legendarySpawn", legendarySpawn);

            // Item Vanilla
            JsonObject itemDrop = new JsonObject();
            itemDrop.addProperty("type", "item");
            itemDrop.addProperty("__note", "Vanilla item drop. Customize the 'items' array with any Minecraft item ID. 'min' and 'max' control quantity. 'chance' is the chance percentage.");
            JsonArray items = new JsonArray();
            items.add("minecraft:diamond");
            items.add("minecraft:gold_ingot");
            items.add("minecraft:iron_ingot");
            itemDrop.add("items", items);
            itemDrop.addProperty("min", 1);
            itemDrop.addProperty("max", 6);
            itemDrop.addProperty("chance", 2.5F);
            pool.add(itemDrop);

            // Estrutura
            JsonObject structureSpawn = new JsonObject();
            structureSpawn.addProperty("type", "structure");
            structureSpawn.addProperty("__note", "Structure event. Add structures using 'id' and 'weight'. Higher weight = more frequent. Use this to spawn special buildings.");
            structureSpawn.addProperty("chance", 1F);


            JsonArray structures = new JsonArray();
            structures.add(createStructureJson("luckblockcobblemon:eletric_boss", 10));
            structures.add(createStructureJson("luckblockcobblemon:fairy_boss", 10));
            structures.add(createStructureJson("luckblockcobblemon:fire_boss", 10));
            structures.add(createStructureJson("luckblockcobblemon:fly_boss", 10));
            structures.add(createStructureJson("luckblockcobblemon:grass_and_bug_boss", 10));
            structures.add(createStructureJson("luckblockcobblemon:ground_boss", 10));
            structures.add(createStructureJson("luckblockcobblemon:ice_and_water_boss", 10));
            structures.add(createStructureJson("luckblockcobblemon:stone_boss", 10));
            structures.add(createStructureJson("luckblockcobblemon:luckornot", 3)); // mais raro

            structureSpawn.add("structures", structures);
            pool.add(structureSpawn);

            // Itens do Cobblemon
            JsonObject cobbleItemDrop = new JsonObject();
            cobbleItemDrop.addProperty("type", "cobbleitem");
            cobbleItemDrop.addProperty("__note", "Cobblemon item drop. Edit the 'items' array with IDs from the Cobblemon mod (e.g., Poké Balls, Rare Candy).");

            JsonArray cobbleItems = new JsonArray();
            String[] pokeballsAndCandy = {
                    "cobblemon:ancient_azure_ball", "cobblemon:ancient_citrine_ball", "cobblemon:ancient_feather_ball", "cobblemon:ancient_gigaton_ball", "cobblemon:ancient_great_ball", "cobblemon:ancient_heavy_ball", "cobblemon:ancient_ivory_ball", "cobblemon:ancient_jet_ball", "cobblemon:ancient_leaden_ball", "cobblemon:ancient_origin_ball", "cobblemon:ancient_poke_ball", "cobblemon:ancient_roseate_ball", "cobblemon:ancient_slate_ball", "cobblemon:ancient_ultra_ball", "cobblemon:ancient_verdant_ball", "cobblemon:ancient_wing_ball", "cobblemon:azure_ball", "cobblemon:beast_ball", "cobblemon:cherish_ball", "cobblemon:citrine_ball", "cobblemon:dive_ball", "cobblemon:dream_ball", "cobblemon:dusk_ball", "cobblemon:fast_ball", "cobblemon:friend_ball", "cobblemon:great_ball", "cobblemon:heal_ball", "cobblemon:heavy_ball", "cobblemon:level_ball", "cobblemon:love_ball", "cobblemon:lure_ball", "cobblemon:luxury_ball", "cobblemon:master_ball", "cobblemon:moon_ball", "cobblemon:nest_ball", "cobblemon:net_ball", "cobblemon:park_ball", "cobblemon:poke_ball", "cobblemon:premier_ball", "cobblemon:quick_ball", "cobblemon:repeat_ball", "cobblemon:roseate_ball", "cobblemon:safari_ball", "cobblemon:slate_ball", "cobblemon:sport_ball", "cobblemon:timer_ball", "cobblemon:ultra_ball", "cobblemon:verdant_ball", "cobblemon:vivichoke_seeds",
                    "cobblemon:exp_candy_l", "cobblemon:exp_candy_m", "cobblemon:exp_candy_s", "cobblemon:exp_candy_xl", "cobblemon:exp_candy_xs", "cobblemon:rare_candy"


            };
            for (String item : pokeballsAndCandy) cobbleItems.add(item);

            cobbleItemDrop.add("items", cobbleItems);
            cobbleItemDrop.addProperty("min", 1);
            cobbleItemDrop.addProperty("max", 6);
            cobbleItemDrop.addProperty("chance", 4F);

            pool.add(cobbleItemDrop);

            JsonArray levelingStrategyNote = new JsonArray();
            levelingStrategyNote.add("There are 3 ways to define Pokémon level for spawn events.");
            levelingStrategyNote.add("Applies only to: 'cobblemonp', 'shiny_cobblemonp', 'random_cobblemonp', and 'multi_cobblemonp'.");
            levelingStrategyNote.add("1. minLevel / maxLevel → Defined directly in the event.");
            levelingStrategyNote.add("   Example: minLevel: 10, maxLevel: 50.");
            levelingStrategyNote.add("   If present, this is always used.");
            levelingStrategyNote.add("2. timeLeveling → If min/max are missing, uses world day ranges to decide.");
            levelingStrategyNote.add("   Example: Days 0–20 = 90% chance for level 1–40, etc.");
            levelingStrategyNote.add("3. levelWeighting → Global fallback level table.");
            levelingStrategyNote.add("Priority:");
            levelingStrategyNote.add("   1) minLevel/maxLevel (per event)");
            levelingStrategyNote.add("   2) timeLeveling (based on world days)");
            levelingStrategyNote.add("   3) levelWeighting (default)");
            levelingStrategyNote.add("⚠️ Works only for specific Pokémon spawn types.");
            defaultConfig.add("__note_leveling_strategy", levelingStrategyNote);


            // Cobblemon normal
            JsonObject cobblemonSpawn = new JsonObject();
            cobblemonSpawn.addProperty("type", "cobblemonp");
            JsonArray cobblemons = new JsonArray();
            String[] cobblemonList = {
                    "bulbasaur","ivysaur","venusaur","charmander","charmeleon","charizard","squirtle","wartortle","blastoise"

            };
            for (String name : cobblemonList) cobblemons.add(name);
            cobblemonSpawn.add("cobblemons", cobblemons);
            cobblemonSpawn.addProperty("minLevel", 0);
            cobblemonSpawn.addProperty("maxLevel", 0);
            cobblemonSpawn.addProperty("chance", 10.98F);
            pool.add(cobblemonSpawn);


            // Shiny Cobblemon (reutiliza a mesma lista do evento anterior)
            JsonObject shinySpawn = new JsonObject();
            shinySpawn.addProperty("type", "shiny_cobblemonp");
            shinySpawn.add("cobblemons", cobblemons.deepCopy());
            shinySpawn.addProperty("minLevel", 0);
            shinySpawn.addProperty("maxLevel", 0);
            shinySpawn.addProperty("chance", 0.02F);
            pool.add(shinySpawn);

            // Random Cobblemon
            JsonObject randomCobblemonSpawn = new JsonObject();
            randomCobblemonSpawn.addProperty("type", "random_cobblemonp");
            randomCobblemonSpawn.addProperty("minLevel", 0);
            randomCobblemonSpawn.addProperty("maxLevel", 0);
            randomCobblemonSpawn.addProperty("chance", 75F);
            randomCobblemonSpawn.addProperty("shinyChance", 0.02F);
            pool.add(randomCobblemonSpawn);

            // Múltiplos Cobblemon aleatórios
            JsonObject multiCobblemonSpawn = new JsonObject();
            multiCobblemonSpawn.addProperty("type", "multi_cobblemonp");
            multiCobblemonSpawn.addProperty("min", 4);       // quantidade mínima
            multiCobblemonSpawn.addProperty("max", 8);       // quantidade máxima
            multiCobblemonSpawn.addProperty("chance", 3F);
            multiCobblemonSpawn.addProperty("minLevel", 0);
            multiCobblemonSpawn.addProperty("maxLevel", 0);
            multiCobblemonSpawn.addProperty("shinyChance", 0.02F);
            pool.add(multiCobblemonSpawn);
            defaultConfig.add("luckPool", pool);


            defaultConfig.addProperty("shinyChancePercent", 0.02F);

            // Todos itens
            JsonObject allItemsDrop = new JsonObject();
            allItemsDrop.addProperty("type", "cobblemon_allitems");

            JsonArray allItems = new JsonArray();
            String[] cobblemonItemList = {
                    "cobblemon:ability_shield", "cobblemon:absorb_bulb", "cobblemon:air_balloon", "cobblemon:amulet_coin", "cobblemon:assault_vest", "cobblemon:big_root", "cobblemon:binding_band", "cobblemon:black_belt", "cobblemon:black_glasses", "cobblemon:black_sludge", "cobblemon:blunder_policy", "cobblemon:bright_powder", "cobblemon:cell_battery", "cobblemon:charcoal", "cobblemon:charcoal_stick", "cobblemon:choice_band", "cobblemon:choice_scarf", "cobblemon:choice_specs", "cobblemon:cleanse_tag", "cobblemon:clear_amulet", "cobblemon:covert_cloak", "cobblemon:damp_rock", "cobblemon:destiny_knot", "cobblemon:dragon_fang", "cobblemon:eject_button", "cobblemon:eject_pack", "cobblemon:electric_seed", "cobblemon:everstone", "cobblemon:eviolite", "cobblemon:expert_belt", "cobblemon:exp_share", "cobblemon:fairy_feather", "cobblemon:flame_orb", "cobblemon:float_stone", "cobblemon:focus_band", "cobblemon:focus_sash", "cobblemon:grassy_seed", "cobblemon:grip_claw", "cobblemon:hard_stone", "cobblemon:heat_rock", "cobblemon:heavy_duty_boots", "cobblemon:icy_rock", "cobblemon:iron_ball", "cobblemon:lagging_tail", "cobblemon:leftovers", "cobblemon:life_orb", "cobblemon:light_ball", "cobblemon:light_clay", "cobblemon:loaded_dice", "cobblemon:lucky_egg", "cobblemon:luminous_moss", "cobblemon:magnet", "cobblemon:mental_herb", "cobblemon:metal_powder", "cobblemon:metronome", "cobblemon:miracle_seed", "cobblemon:mirror_herb", "cobblemon:misty_seed", "cobblemon:muscle_band", "cobblemon:mystic_water", "cobblemon:never_melt_ice", "cobblemon:poison_barb", "cobblemon:power_anklet", "cobblemon:power_band", "cobblemon:power_belt", "cobblemon:power_bracer", "cobblemon:power_herb", "cobblemon:power_lens", "cobblemon:power_weight", "cobblemon:protective_pads", "cobblemon:psychic_seed", "cobblemon:punching_glove", "cobblemon:quick_claw", "cobblemon:quick_powder", "cobblemon:red_card", "cobblemon:ring_target", "cobblemon:rocky_helmet", "cobblemon:room_service", "cobblemon:safety_goggles", "cobblemon:scope_lens", "cobblemon:sharp_beak", "cobblemon:shed_shell", "cobblemon:shell_bell", "cobblemon:silk_scarf", "cobblemon:silver_powder", "cobblemon:smoke_ball", "cobblemon:smooth_rock", "cobblemon:soft_sand", "cobblemon:soothe_bell", "cobblemon:spell_tag", "cobblemon:sticky_barb", "cobblemon:terrain_extender", "cobblemon:throat_spray", "cobblemon:toxic_orb", "cobblemon:twisted_spoon", "cobblemon:utility_umbrella", "cobblemon:weakness_policy", "cobblemon:white_herb", "cobblemon:wide_lens", "cobblemon:wise_glasses", "cobblemon:zoom_lens",
                    "cobblemon:auspicious_armor", "cobblemon:berry_sweet", "cobblemon:black_augurite", "cobblemon:chipped_pot", "cobblemon:clover_sweet", "cobblemon:cracked_pot", "cobblemon:dawn_stone", "cobblemon:deep_sea_scale", "cobblemon:deep_sea_tooth", "cobblemon:dragon_scale", "cobblemon:dubious_disc", "cobblemon:dusk_stone", "cobblemon:electirizer", "cobblemon:fire_stone", "cobblemon:flower_sweet", "cobblemon:galarica_cuff", "cobblemon:galarica_wreath", "cobblemon:ice_stone", "cobblemon:kings_rock", "cobblemon:leaf_stone", "cobblemon:link_cable", "cobblemon:love_sweet", "cobblemon:magmarizer", "cobblemon:malicious_armor", "cobblemon:masterpiece_teacup", "cobblemon:metal_alloy", "cobblemon:metal_coat", "cobblemon:moon_stone", "cobblemon:oval_stone", "cobblemon:peat_block", "cobblemon:prism_scale", "cobblemon:protector", "cobblemon:razor_claw", "cobblemon:razor_fang", "cobblemon:reaper_cloth", "cobblemon:ribbon_sweet", "cobblemon:sachet", "cobblemon:scroll_of_darkness", "cobblemon:scroll_of_waters", "cobblemon:shell_helmet", "cobblemon:shiny_stone", "cobblemon:star_sweet", "cobblemon:strawberry_sweet", "cobblemon:sun_stone", "cobblemon:sweet_apple", "cobblemon:syrupy_apple", "cobblemon:tart_apple", "cobblemon:thunder_stone", "cobblemon:unremarkable_teacup", "cobblemon:upgrade", "cobblemon:water_stone", "cobblemon:whipped_dream",
                    "cobblemon:ancient_azure_ball", "cobblemon:ancient_citrine_ball", "cobblemon:ancient_feather_ball", "cobblemon:ancient_gigaton_ball", "cobblemon:ancient_great_ball", "cobblemon:ancient_heavy_ball", "cobblemon:ancient_ivory_ball", "cobblemon:ancient_jet_ball", "cobblemon:ancient_leaden_ball", "cobblemon:ancient_origin_ball", "cobblemon:ancient_poke_ball", "cobblemon:ancient_roseate_ball", "cobblemon:ancient_slate_ball", "cobblemon:ancient_ultra_ball", "cobblemon:ancient_verdant_ball", "cobblemon:ancient_wing_ball", "cobblemon:azure_ball", "cobblemon:beast_ball", "cobblemon:cherish_ball", "cobblemon:citrine_ball", "cobblemon:dive_ball", "cobblemon:dream_ball", "cobblemon:dusk_ball", "cobblemon:fast_ball", "cobblemon:friend_ball", "cobblemon:great_ball", "cobblemon:heal_ball", "cobblemon:heavy_ball", "cobblemon:level_ball", "cobblemon:love_ball", "cobblemon:lure_ball", "cobblemon:luxury_ball", "cobblemon:master_ball", "cobblemon:moon_ball", "cobblemon:nest_ball", "cobblemon:net_ball", "cobblemon:park_ball", "cobblemon:poke_ball", "cobblemon:premier_ball", "cobblemon:quick_ball", "cobblemon:repeat_ball", "cobblemon:roseate_ball", "cobblemon:safari_ball", "cobblemon:slate_ball", "cobblemon:sport_ball", "cobblemon:timer_ball", "cobblemon:ultra_ball", "cobblemon:verdant_ball", "cobblemon:vivichoke_seeds",
                    "cobblemon:bug_gem", "cobblemon:dark_gem", "cobblemon:dragon_gem", "cobblemon:electric_gem", "cobblemon:fairy_gem", "cobblemon:fighting_gem", "cobblemon:fire_gem", "cobblemon:flying_gem", "cobblemon:ghost_gem", "cobblemon:grass_gem", "cobblemon:ground_gem", "cobblemon:ice_gem", "cobblemon:normal_gem", "cobblemon:poison_gem", "cobblemon:psychic_gem", "cobblemon:rock_gem", "cobblemon:steel_gem", "cobblemon:water_gem",
                    "cobblemon:antidote", "cobblemon:awakening", "cobblemon:burn_heal", "cobblemon:calcium", "cobblemon:carbos", "cobblemon:elixir", "cobblemon:energy_root", "cobblemon:ether", "cobblemon:fine_remedy", "cobblemon:full_heal", "cobblemon:full_restore", "cobblemon:heal_powder", "cobblemon:hp_up", "cobblemon:hyper_potion", "cobblemon:ice_heal", "cobblemon:iron", "cobblemon:max_elixir", "cobblemon:max_ether", "cobblemon:max_potion", "cobblemon:max_revive", "cobblemon:medicinal_brew", "cobblemon:medicinal_leek", "cobblemon:paralyze_heal", "cobblemon:potion", "cobblemon:pp_max", "cobblemon:pp_up", "cobblemon:protein", "cobblemon:remedy", "cobblemon:revive", "cobblemon:super_potion", "cobblemon:superb_remedy", "cobblemon:zinc"

            };

            for (String item : cobblemonItemList) allItems.add(item);

            allItemsDrop.add("items", allItems);
            allItemsDrop.addProperty("min", 1);
            allItemsDrop.addProperty("max", 1);
            allItemsDrop.addProperty("chance", 4.5F);

            pool.add(allItemsDrop);

            // levelWeighting customizado
            JsonArray levelWeights = new JsonArray();
            defaultConfig.addProperty("__note_levelWeighting", "This controls level probability when no minLevel/maxLevel is set. Total chances should ideally sum to 100.0.");

            JsonObject lv100 = new JsonObject();
            lv100.addProperty("min", 80);
            lv100.addProperty("max", 100);
            lv100.addProperty("chance", 1.0);
            levelWeights.add(lv100);

            JsonObject lv60to79 = new JsonObject();
            lv60to79.addProperty("min", 60);
            lv60to79.addProperty("max", 79);
            lv60to79.addProperty("chance", 5.0);
            levelWeights.add(lv60to79);

            JsonObject lv40to59 = new JsonObject();
            lv40to59.addProperty("min", 40);
            lv40to59.addProperty("max", 59);
            lv40to59.addProperty("chance", 15.0);
            levelWeights.add(lv40to59);

            JsonObject lv1to39 = new JsonObject();
            lv1to39.addProperty("min", 1);
            lv1to39.addProperty("max", 39);
            lv1to39.addProperty("chance", 79.0);
            levelWeights.add(lv1to39);

            defaultConfig.add("levelWeighting", levelWeights);

            // timeLeveling baseado nos dias do mundo
            JsonArray timeLeveling = new JsonArray();
            defaultConfig.addProperty("__note_timeLeveling", "Optional: Enables world-age-based scaling. The system will use these ranges based on how many days have passed in-game.");

            JsonObject days0to20 = new JsonObject();
            days0to20.addProperty("minDays", 0);
            days0to20.addProperty("maxDays", 25);
            JsonArray levels0to20 = new JsonArray();
            levels0to20.add(createLevelRange(1, 12, 90.0f));
            levels0to20.add(createLevelRange(13, 16, 9.0f));
            levels0to20.add(createLevelRange(17, 20, 1.0f));
            days0to20.add("levels", levels0to20);
            timeLeveling.add(days0to20);

            JsonObject days21to50 = new JsonObject();
            days21to50.addProperty("minDays", 26);
            days21to50.addProperty("maxDays", 50);
            JsonArray levels21to50 = new JsonArray();
            levels21to50.add(createLevelRange(1, 35, 60.0f));
            levels21to50.add(createLevelRange(36, 45, 30.0f));
            levels21to50.add(createLevelRange(46, 55, 10.0f));
            days21to50.add("levels", levels21to50);
            timeLeveling.add(days21to50);

            JsonObject days51to100 = new JsonObject();
            days51to100.addProperty("minDays", 51);
            days51to100.addProperty("maxDays", 100);
            JsonArray levels51to100 = new JsonArray();
            levels51to100.add(createLevelRange(30, 60, 50.0f));
            levels51to100.add(createLevelRange(61, 65, 35.0f));
            levels51to100.add(createLevelRange(66, 70, 15.0f));
            days51to100.add("levels", levels51to100);
            timeLeveling.add(days51to100);

            JsonObject days101plus = new JsonObject();
            days101plus.addProperty("minDays", 101);
            days101plus.addProperty("maxDays", 99999);
            JsonArray levels101plus = new JsonArray();
            levels101plus.add(createLevelRange(40, 80, 60.0f));
            levels101plus.add(createLevelRange(81, 85, 30.0f));
            levels101plus.add(createLevelRange(86, 90, 10.0f));
            days101plus.add("levels", levels101plus);
            timeLeveling.add(days101plus);

// adiciona ao config principal
            defaultConfig.add("timeLeveling", timeLeveling);
            // salva pool depois de adicionar todos eventos
            defaultConfig.add("luckPool", pool);

            File configDir = new File("./config");
            if (!configDir.exists()) configDir.mkdirs();

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(defaultConfig, writer);
            }

            System.out.println("[LuckyBlockPocket] Default config created.");

        } catch (Exception e) {
            System.out.println("[LuckyBlockPocket] Failed to create default config: " + e.getMessage());
            e.printStackTrace();
        }


    }

}
