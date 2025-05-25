package net.crulim.luckblockcobblemon.handler;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.google.gson.*;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.minecraft.predicate.entity.LocationPredicate.Builder.createStructure;

public class LuckyBlockHandlerPocket {
    private static final List<LevelRangeWeight> weightedLevels = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()  // <- isso resolve!
            .create();
    private static final Random random = Random.create();
    private static final String CONFIG_PATH = "config/luckyblock_config.json";
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
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                generateDefaultConfig(file);
            }

            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
            ).getAsJsonObject();

            breakCreative = json.has("breakCreative") && json.get("breakCreative").getAsBoolean();

            // Limpa os pools anteriores
            luckPool.clear();

            // Carrega os eventos da sorte
            JsonArray poolArray = json.getAsJsonArray("luckPool");
            for (JsonElement element : poolArray) {
                luckPool.add(element.getAsJsonObject());
            }

            // Shiny chance
            shinyChancePercent = json.has("shinyChancePercent")
                    ? json.get("shinyChancePercent").getAsFloat()
                    : 5.0F;

            // Carrega levelWeighting (prioritário)
            if (json.has("levelWeighting")) {
                weightedLevels.clear();
                JsonArray levelArray = json.getAsJsonArray("levelWeighting");
                for (JsonElement el : levelArray) {
                    JsonObject obj = el.getAsJsonObject();
                    int min = obj.get("min").getAsInt();
                    int max = obj.get("max").getAsInt();
                    float chance = obj.get("chance").getAsFloat();
                    weightedLevels.add(new LevelRangeWeight(min, max, chance));
                }

                minLevel = -1;
                maxLevel = -1;

            } else if (json.has("levelRange")) {
                // Só usa levelRange se não houver weighting
                JsonObject levelRange = json.getAsJsonObject("levelRange");
                minLevel = levelRange.has("min") ? levelRange.get("min").getAsInt() : 5;
                maxLevel = levelRange.has("max") ? levelRange.get("max").getAsInt() : 30;

            } else {
                // fallback final se nenhum dos dois existir
                minLevel = 5;
                maxLevel = 30;
            }
            if (json.has("timeLeveling")) {
                timeBasedLeveling.clear();
                JsonArray timeArray = json.getAsJsonArray("timeLeveling");
                for (JsonElement timeElement : timeArray) {
                    JsonObject timeObj = timeElement.getAsJsonObject();
                    int minDays = timeObj.get("minDays").getAsInt();
                    int maxDays = timeObj.get("maxDays").getAsInt();
                    List<LevelRangeWeight> timeWeights = new ArrayList<>();

                    JsonArray levels = timeObj.getAsJsonArray("levels");
                    for (JsonElement lvl : levels) {
                        JsonObject obj = lvl.getAsJsonObject();
                        int min = obj.get("min").getAsInt();
                        int max = obj.get("max").getAsInt();
                        float chance = obj.get("chance").getAsFloat();
                        timeWeights.add(new LevelRangeWeight(min, max, chance));
                    }

                    timeBasedLeveling.add(new TimeBasedLevelRange(minDays, maxDays, timeWeights));
                }
            }

            System.out.println("[LuckyBlockPocket] Config loaded successfully.");

        } catch (Exception e) {
            System.out.println("[LuckyBlockPocket] Failed to load config: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Erro ao carregar configuração", e);
        }
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
                return random.nextBetween(range.min, range.max + 1);
            }
        }

        // fallback se nada for sorteado
        return random.nextBetween(1, 101);
    }

    private static int getTimeBasedLevel(ServerWorld world) {
        long days = world.getTimeOfDay() / 24000L;

        for (TimeBasedLevelRange timeRange : timeBasedLeveling) {
            if (days >= timeRange.minDays && days <= timeRange.maxDays) {
                System.out.println("[PocketLuckHandler] Tempo atual: " + days + " dias. Usando faixa de " + timeRange.minDays + " a " + timeRange.maxDays);
                float total = 0F;
                for (LevelRangeWeight range : timeRange.levels) {
                    total += range.chance;
                }

                float roll = random.nextFloat() * total;
                float cumulative = 0F;
                for (LevelRangeWeight range : timeRange.levels) {
                    cumulative += range.chance;
                    if (roll < cumulative) {
                        return random.nextBetween(range.min, range.max + 1);
                    }
                }
            }

        }

        return -1; // nenhum match
    }


    public static void reloadConfig() {
        loadConfig();
        System.out.println("[LuckyBlockPocket] Config reloaded!");
    }

    public static void triggerLuckEvent(ServerWorld world, BlockPos pos) {
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

        executeLuck(world, pos, selected);
    }

    private static List<JsonObject> filterValidEvents() {
        List<JsonObject> valid = new ArrayList<>();

        for (JsonObject event : luckPool) {
            if (event == null || event.isJsonNull()) continue;
            if (!event.has("type")) continue;

            String type = event.get("type").getAsString();

            switch (type) {
                case "item", "cobbleitem" -> {
                    if (event.has("items") && !event.getAsJsonArray("items").isEmpty()) {
                        valid.add(event);
                    }
                }
                case "cobblemonp", "random_cobblemonp", "shiny_cobblemonp" -> valid.add(event);
                case "structure" -> {
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

        // Se todas as chances forem 0, usar a lista original como fallback
        if (totalChance <= 0F || validPool.isEmpty()) {
            System.out.println("[LuckyBlockPocket] Todas as chances são 0. Escolhendo evento aleatório da lista.");
            return pool.get(random.nextInt(pool.size()));
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
        System.out.println("[LuckyBlockPocket] Evento sorteado: " + data.toString());
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

    private static void spawnCobblemon(ServerWorld world, BlockPos pos, JsonObject data, boolean forceShiny) {
        System.out.println("[LuckyBlockPocket] Iniciando spawnCobblemon com dados: " + data.toString());

        String speciesName;
        if (data.has("species")) {
            speciesName = data.get("species").getAsString();
        } else if (data.has("cobblemons")) {
            JsonArray list = data.getAsJsonArray("cobblemons");
            if (list.isEmpty()) {
                System.out.println("[LuckyBlockPocket] Lista de cobblemons vazia.");
                return;
            }
            speciesName = list.get(random.nextInt(list.size())).getAsString();
        } else {
            System.out.println("[LuckyBlockPocket] Nenhuma espécie ou lista de cobblemons definida.");
            return;
        }

        speciesName = normalizeSpeciesName(speciesName);

        Species species = PokemonSpecies.INSTANCE.getByName(speciesName);
        if (species == null) {
            System.out.println("[LuckyBlockPocket] ESPÉCIE NÃO ENCONTRADA: " + speciesName);
            return;
        }

        int level;
        if (data.has("level")) {
            level = data.get("level").getAsInt();
        } else if (!timeBasedLeveling.isEmpty()) {
            int timeBased = getTimeBasedLevel(world);
            level = (timeBased > 0) ? timeBased : getWeightedRandomLevel();
        } else {
            level = getWeightedRandomLevel();
        }

        boolean isShiny = forceShiny || (random.nextFloat() * 100F < shinyChancePercent);

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(level);
        pokemon.setShiny(isShiny);

        // ✅ Garante ataques funcionais com PP
        pokemon.getMoveSet().clear();
        Iterable<MoveTemplate> relearnableMoves = pokemon.getRelearnableMoves();
        int index = 0;

        for (MoveTemplate template : relearnableMoves) {
            if (template != null && index < 4) {
                int maxPp = template.getMaxPp();
                Move move = new Move(template, template.getPp(), 0);
                pokemon.getMoveSet().setMove(index, move);
                index++;
            }
        }

        System.out.println("[LuckyBlockPocket] Level definido: " + level);
        System.out.println("[LuckyBlockPocket] Shiny: " + isShiny);

        Vec3d spawnPos = Vec3d.ofCenter(pos).add(0, 1, 0);
        PokemonEntity entity = pokemon.sendOut(world, spawnPos, null, e -> null);

        if (entity == null) {
            System.out.println("[LuckyBlockPocket] Falha ao spawnar Pokémon: " + speciesName);
        } else {
            System.out.println("[LuckyBlockPocket] Pokémon spawnado com sucesso: " + speciesName);
        }
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
            level = random.nextBetween(min, max + 1);
        } else {
            if (data.has("minLevel") && data.has("maxLevel")) {
                int min = data.get("minLevel").getAsInt();
                int max = data.get("maxLevel").getAsInt();
                level = random.nextBetween(min, max + 1);
            } else if (!timeBasedLeveling.isEmpty()) {
                int timeBased = getTimeBasedLevel(world);
                level = (timeBased > 0) ? timeBased : getWeightedRandomLevel();
            } else {
                level = getWeightedRandomLevel();
            }
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
            String speciesName = species.getName().toLowerCase(Locale.ROOT).replace(" ", "_");

            int level;
            if (data.has("minLevel") && data.has("maxLevel")) {
                int minLevelLocal = data.get("minLevel").getAsInt();
                int maxLevelLocal = data.get("maxLevel").getAsInt();
                level = random.nextBetween(minLevelLocal, maxLevelLocal + 1);
            } else {
                if (data.has("minLevel") && data.has("maxLevel")) {
                    min = data.get("minLevel").getAsInt();
                    max = data.get("maxLevel").getAsInt();
                    level = random.nextBetween(min, max + 1);
                } else if (!timeBasedLeveling.isEmpty()) {
                    int timeBased = getTimeBasedLevel(world);
                    level = (timeBased > 0) ? timeBased : getWeightedRandomLevel();
                } else {
                    level = getWeightedRandomLevel();
                }
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
                .replace(" ", "-")
                .replace("_", "-")
                .replace("♀", "-f")
                .replace("♂", "-m")
                .replaceAll("[^a-z0-9\\-]", ""); // remove qualquer caractere especial
    }


    private static void spawnStructure(ServerWorld world, BlockPos pos, JsonObject data) {
        String structureId;

        // Verifica se há uma lista de estruturas
        if (data.has("structures")) {
            JsonArray structures = data.getAsJsonArray("structures");
            if (structures.isEmpty()) {
                System.out.println("[LuckyBlockPocket] Lista de estruturas vazia.");
                return;
            }
            List<JsonObject> weightedList = new ArrayList<>();
            int totalWeight = 0;

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

            JsonObject selected = weightedList.get(random.nextInt(weightedList.size()));
            structureId = selected.get("id").getAsString();
        } else if (data.has("structure")) {
            structureId = data.get("structure").getAsString();
        } else {
            System.out.println("[LuckyBlockPocket] Nenhum ID de estrutura encontrado no config.");
            return;
        }

        Identifier id = Identifier.of(structureId);
        var templateManager = world.getServer().getStructureTemplateManager();
        var optional = templateManager.getTemplate(id);

        if (optional.isEmpty()) {
            System.out.println("[LuckyBlockPocket] Estrutura não encontrada: " + id);
            return;
        }

        var template = optional.get();
        BlockPos placementPos = pos.west(4);

        if (structureId.equals("luckblockcobblemon:luckornot")) {
            placementPos = placementPos.down(4);
        }
        template.place(world, placementPos, placementPos,
                new net.minecraft.structure.StructurePlacementData(),
                random, 3);
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

    private static void generateDefaultConfig(File file) {
        try {
            JsonObject defaultConfig = new JsonObject();
            JsonArray pool = new JsonArray();
            defaultConfig.addProperty("breakCreative", false);

            JsonArray note = new JsonArray();
            note.add("INFO: The 'breakCreative' property controls whether Lucky Blocks can be broken in creative mode.");
            note.add("If set to true, players in creative mode will be able to break and activate Lucky Blocks.");
            note.add("If set to false, Lucky Blocks cannot be activated in creative mode.");
            note.add("This config is GLOBAL and only read from lvlconfig_types.json (or level_config.json depending on your system).");

            defaultConfig.add("_note", note);

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
            cobblemonSpawn.addProperty("chance", 10F);
            pool.add(cobblemonSpawn);


            // Shiny Cobblemon (reutiliza a mesma lista do evento anterior)
            JsonObject shinySpawn = new JsonObject();
            shinySpawn.addProperty("type", "shiny_cobblemonp");
            shinySpawn.add("cobblemons", cobblemons.deepCopy());
            shinySpawn.addProperty("chance", 1F);
            pool.add(shinySpawn);

            // Random Cobblemon
            JsonObject randomCobblemonSpawn = new JsonObject();
            randomCobblemonSpawn.addProperty("type", "random_cobblemonp");
            randomCobblemonSpawn.addProperty("chance", 75F);
            randomCobblemonSpawn.addProperty("shinyChance", 0.02F);
            pool.add(randomCobblemonSpawn);

            // Múltiplos Cobblemon aleatórios
            JsonObject multiCobblemonSpawn = new JsonObject();
            multiCobblemonSpawn.addProperty("type", "multi_cobblemonp");
            multiCobblemonSpawn.addProperty("min", 4);       // quantidade mínima
            multiCobblemonSpawn.addProperty("max", 8);       // quantidade máxima
            multiCobblemonSpawn.addProperty("chance", 3F);
            multiCobblemonSpawn.addProperty("shinyChance", 0.02F);
            pool.add(multiCobblemonSpawn);


            defaultConfig.add("luckPool", pool);


            defaultConfig.addProperty("shinyChancePercent", 0.02F);

            // Todos itens
            JsonObject allItemsDrop = new JsonObject();
            allItemsDrop.addProperty("type", "cobblemon_allitems");
            allItemsDrop.addProperty("__note", "Drops a random item from the full list of Cobblemon items.");

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
            days0to20.addProperty("maxDays", 20);
            JsonArray levels0to20 = new JsonArray();
            levels0to20.add(createLevelRange(1, 40, 90.0f));
            levels0to20.add(createLevelRange(41, 60, 9.0f));
            levels0to20.add(createLevelRange(61, 100, 1.0f));
            days0to20.add("levels", levels0to20);
            timeLeveling.add(days0to20);

            JsonObject days21to50 = new JsonObject();
            days21to50.addProperty("minDays", 21);
            days21to50.addProperty("maxDays", 50);
            JsonArray levels21to50 = new JsonArray();
            levels21to50.add(createLevelRange(1, 40, 60.0f));
            levels21to50.add(createLevelRange(41, 60, 30.0f));
            levels21to50.add(createLevelRange(61, 100, 10.0f));
            days21to50.add("levels", levels21to50);
            timeLeveling.add(days21to50);

            JsonObject days51to100 = new JsonObject();
            days51to100.addProperty("minDays", 51);
            days51to100.addProperty("maxDays", 100);
            JsonArray levels51to100 = new JsonArray();
            levels51to100.add(createLevelRange(30, 60, 50.0f));
            levels51to100.add(createLevelRange(61, 85, 35.0f));
            levels51to100.add(createLevelRange(86, 100, 15.0f));
            days51to100.add("levels", levels51to100);
            timeLeveling.add(days51to100);

            JsonObject days101plus = new JsonObject();
            days101plus.addProperty("minDays", 101);
            days101plus.addProperty("maxDays", 99999);
            JsonArray levels101plus = new JsonArray();
            levels101plus.add(createLevelRange(60, 85, 60.0f));
            levels101plus.add(createLevelRange(86, 95, 30.0f));
            levels101plus.add(createLevelRange(96, 100, 10.0f));
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
