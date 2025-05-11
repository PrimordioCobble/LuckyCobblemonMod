package net.crulim.luckblockcobblemon.handler;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
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

public class LuckyBlockHandlerPocket {
    private static final List<LevelRangeWeight> weightedLevels = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()  // <- isso resolve!
            .create();
    private static final Random random = Random.create();
    private static final String CONFIG_PATH = "config/luckpocket_config.json";
    private static final Logger LOGGER = Logger.getLogger(LuckyBlockHandlerPocket.class.getName());

    private static final List<JsonObject> luckPool = new ArrayList<>();
    private static int minLevel = 5;
    private static int maxLevel = 30;
    private static float shinyChancePercent = 5.0F;

    public static void loadConfig() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                generateDefaultConfig(file);
            }

            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
            ).getAsJsonObject();

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

                // Quando há weighting, desativa min/max padrão
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

        if (PokemonSpecies.INSTANCE.getByName(speciesName) == null) {
            System.out.println("[LuckyBlockPocket] Pokémon inválido ignorado: " + speciesName);
            return;
        }


        System.out.println("[LuckyBlockPocket] Tentando pegar espécie: " + speciesName);
        Species species = PokemonSpecies.INSTANCE.getByName(speciesName);
        if (species == null) {
            System.out.println("[LuckyBlockPocket] ESPÉCIE NÃO ENCONTRADA: " + speciesName);
            return;
        }

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);

        int level;
        if (data.has("level")) {
            level = data.get("level").getAsInt();
            System.out.println("[LuckyBlockPocket] Level definido via JSON: " + level);
        } else {
            level = getWeightedRandomLevel();
            System.out.println("[LuckyBlockPocket] Level sorteado com peso: " + level);
        }

        pokemon.setLevel(level);
        System.out.println("[LuckyBlockPocket] Level definido: " + level);

        float localShinyChance = data.has("shinyChance") ? data.get("shinyChance").getAsFloat() : shinyChancePercent;
        boolean isShiny = forceShiny || (random.nextFloat() * 100F < localShinyChance);
        pokemon.setShiny(isShiny);
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
            String speciesName = species.getName().toLowerCase(Locale.ROOT).replace(" ", "_");

            int level;
            if (data.has("minLevel") && data.has("maxLevel")) {
                int minLevelLocal = data.get("minLevel").getAsInt();
                int maxLevelLocal = data.get("maxLevel").getAsInt();
                level = random.nextBetween(minLevelLocal, maxLevelLocal + 1);
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
            structureId = structures.get(random.nextInt(structures.size())).getAsString();
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
        BlockPos placementPos = pos.up().west(4); // 1 bloco acima do bloco quebrado e 4 blocos a direita

        template.place(world, placementPos, placementPos,
                new net.minecraft.structure.StructurePlacementData(),
                random, 3);
    }



    private static void generateDefaultConfig(File file) {
        try {
            JsonObject defaultConfig = new JsonObject();
            JsonArray pool = new JsonArray();

            // Item Vanilla
            JsonObject itemDrop = new JsonObject();
            itemDrop.addProperty("type", "item");
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

            JsonArray structures = new JsonArray();
            structures.add("luckblockcobblemon:eletric_boss");
            structures.add("luckblockcobblemon:fairy_boss");
            structures.add("luckblockcobblemon:fire_boss");
            structures.add("luckblockcobblemon:fly_boss");
            structures.add("luckblockcobblemon:grass_and_bug_boss");
            structures.add("luckblockcobblemon:ground_boss");
            structures.add("luckblockcobblemon:ice_and_water_boss");
            structures.add("luckblockcobblemon:luckornot");
            structures.add("luckblockcobblemon:stone_boss");


            structureSpawn.add("structures", structures);
            structureSpawn.addProperty("chance", 1F);

            pool.add(structureSpawn);

            // Itens do Cobblemon
            JsonObject cobbleItemDrop = new JsonObject();
            cobbleItemDrop.addProperty("type", "cobbleitem");

            JsonArray cobbleItems = new JsonArray();
            String[] pokeballsAndCandy = {
                    "cobblemon:pokeball", "cobblemon:great_ball", "cobblemon:ultra_ball", "cobblemon:quick_ball",
                    "cobblemon:dusk_ball", "cobblemon:repeat_ball", "cobblemon:timer_ball", "cobblemon:nest_ball",
                    "cobblemon:net_ball", "cobblemon:luxury_ball", "cobblemon:heal_ball", "cobblemon:friend_ball",
                    "cobblemon:level_ball", "cobblemon:moon_ball", "cobblemon:love_ball", "cobblemon:heavy_ball",
                    "cobblemon:fast_ball", "cobblemon:safari_ball", "cobblemon:rare_candy"
            };
            for (String item : pokeballsAndCandy) cobbleItems.add(item);

            cobbleItemDrop.add("items", cobbleItems);
            cobbleItemDrop.addProperty("min", 1);
            cobbleItemDrop.addProperty("max", 6);
            cobbleItemDrop.addProperty("chance", 4F);

            pool.add(cobbleItemDrop);


            // Cobblemon normal
            JsonObject cobblemonSpawn = new JsonObject();
            cobblemonSpawn.addProperty("type", "cobblemonp");
            JsonArray cobblemons = new JsonArray();
            String[] cobblemonList = {
                    "bulbasaur","ivysaur","venusaur","charmander","charmeleon","charizard","squirtle","wartortle","blastoise",
                    "caterpie","metapod","butterfree","weedle","kakuna","beedrill","pidgey","pidgeotto","pidgeot","rattata",
                    "raticate","spearow","fearow","ekans","arbok","pikachu","raichu","sandshrew","sandslash","nidoran_f",
                    "nidorina","nidoqueen","nidoran_m","nidorino","nidoking","clefairy","clefable","vulpix","ninetales",
                    "jigglypuff","wigglytuff","zubat","golbat","oddish","gloom","vileplume","paras","parasect","venonat",
                    "venomoth","diglett","dugtrio","meowth","persian","psyduck","golduck","mankey","primeape","growlithe",
                    "arcanine","poliwag","poliwhirl","poliwrath","abra","kadabra","alakazam","machop","machoke","machamp",
                    "bellsprout","weepinbell","victreebel","tentacool","tentacruel","geodude","graveler","golem","ponyta",
                    "rapidash","slowpoke","slowbro","magnemite","magneton","farfetchd","doduo","dodrio","seel","dewgong",
                    "grimer","muk","shellder","cloyster","gastly","haunter","gengar","onix","drowzee","hypno","krabby",
                    "kingler","voltorb","electrode","exeggcute","exeggutor","cubone","marowak","hitmonlee","hitmonchan",
                    "lickitung","koffing","weezing","rhyhorn","rhydon","chansey","tangela","kangaskhan","horsea","seadra",
                    "goldeen","seaking","staryu","starmie","mr_mime","scyther","jynx","electabuzz","magmar","pinsir","tauros",
                    "magikarp","gyarados","lapras","ditto","eevee","vaporeon","jolteon","flareon","porygon","omanyte","omastar",
                    "kabuto","kabutops","aerodactyl","snorlax","articuno","zapdos","moltres","dratini","dragonair","dragonite",
                    "mewtwo","mew"
            };
            for (String name : cobblemonList) cobblemons.add(name);
            cobblemonSpawn.add("cobblemons", cobblemons);
            cobblemonSpawn.addProperty("__nota_min_max_level", "Você pode adicionar 'minLevel' e 'maxLevel' neste evento para definir um nível aleatório personalizado. Exemplo: minLevel: 10, maxLevel: 50. Se não definir, o sistema usará 'levelWeighting'.");
            cobblemonSpawn.addProperty("chance", 75F);
            pool.add(cobblemonSpawn);

            // Shiny Cobblemon (reutiliza a mesma lista do evento anterior)
            JsonObject shinySpawn = new JsonObject();
            shinySpawn.addProperty("type", "shiny_cobblemonp");
            shinySpawn.add("cobblemons", cobblemons.deepCopy());
            shinySpawn.addProperty("__nota_min_max_level", "Você pode adicionar 'minLevel' e 'maxLevel' para controlar o nível dos Pokémon shiny. Exemplo: minLevel: 20, maxLevel: 60. Se omitido, será usado 'levelWeighting'.");
            shinySpawn.addProperty("chance", 1F);
            pool.add(shinySpawn);

            // Random Cobblemon
            JsonObject randomCobblemonSpawn = new JsonObject();
            randomCobblemonSpawn.addProperty("type", "random_cobblemonp");
            randomCobblemonSpawn.addProperty("chance", 10F); // porcentagem de ativação
            randomCobblemonSpawn.addProperty("__nota_min_max_level", "Este evento suporta 'minLevel' e 'maxLevel' para limitar o nível dos Pokémon aleatórios. Exemplo: minLevel: 30, maxLevel: 80.");
            randomCobblemonSpawn.addProperty("shinyChance", 0.02F);
            pool.add(randomCobblemonSpawn);

            // Múltiplos Cobblemon aleatórios
            JsonObject multiCobblemonSpawn = new JsonObject();
            multiCobblemonSpawn.addProperty("type", "multi_cobblemonp");
            multiCobblemonSpawn.addProperty("chance", 0.5F); // chance de ativação
            multiCobblemonSpawn.addProperty("min", 2);       // quantidade mínima
            multiCobblemonSpawn.addProperty("max", 5);       // quantidade máxima
            multiCobblemonSpawn.addProperty("__nota_min_max_level", "Você pode adicionar 'minLevel' e 'maxLevel' para definir o nível aleatório de cada Pokémon no spawn múltiplo. Exemplo: minLevel: 5, maxLevel: 100.");
            multiCobblemonSpawn.addProperty("shinyChance", 0.02F); // chance shiny por Cobblemon
            pool.add(multiCobblemonSpawn);


            defaultConfig.add("luckPool", pool);


            defaultConfig.addProperty("shinyChancePercent", 0.02F);

            // Todos itens
            JsonObject allItemsDrop = new JsonObject();
            allItemsDrop.addProperty("type", "cobblemon_allitems");

            JsonArray allItems = new JsonArray();
            String[] cobblemonItemList = {
                    "cobblemon:ability_capsule", "cobblemon:ability_patch", "cobblemon:ability_shield",
                    "cobblemon:absorb_bulb", "cobblemon:air_balloon", "cobblemon:ancient_pokeball",
                    "cobblemon:antidote", "cobblemon:apricorn", "cobblemon:assault_vest",
                    "cobblemon:auspicious_armor", "cobblemon:awakening", "cobblemon:berry",
                    "cobblemon:berry_juice", "cobblemon:big_root", "cobblemon:binding_band",
                    "cobblemon:black_augurite", "cobblemon:black_belt", "cobblemon:black_glasses",
                    "cobblemon:black_sludge", "cobblemon:blunder_policy", "cobblemon:braised_vivichoke",
                    "cobblemon:bright_powder", "cobblemon:burn_heal", "cobblemon:calcium",
                    "cobblemon:carbos", "cobblemon:cell_battery", "cobblemon:charcoal_stick",
                    "cobblemon:chipped_pot", "cobblemon:choice_band", "cobblemon:choice_scarf",
                    "cobblemon:choice_specs", "cobblemon:cleanse_tag", "cobblemon:covert_cloak",
                    "cobblemon:cracked_pot", "cobblemon:damp_rock", "cobblemon:dawn_stone",
                    "cobblemon:dawn_stone_shard", "cobblemon:deep_sea_scale", "cobblemon:deep_sea_tooth",
                    "cobblemon:destiny_knot", "cobblemon:dire_hit", "cobblemon:dragon_fang",
                    "cobblemon:dragon_scale", "cobblemon:dragon_scale_fragment", "cobblemon:dubious_disc",
                    "cobblemon:dubious_disc_fragment", "cobblemon:dusk_stone", "cobblemon:dusk_stone_shard",
                    "cobblemon:eject_button", "cobblemon:eject_pack", "cobblemon:electirizer",
                    "cobblemon:electirizer_fragment", "cobblemon:elixir", "cobblemon:ether",
                    "cobblemon:everstone", "cobblemon:everstone_fragment", "cobblemon:eviolite",
                    "cobblemon:evolution_stone", "cobblemon:exp_candy", "cobblemon:exp_candy_l",
                    "cobblemon:exp_candy_m", "cobblemon:exp_candy_s", "cobblemon:exp_candy_xl",
                    "cobblemon:exp_candy_xs", "cobblemon:exp_share", "cobblemon:expert_belt",
                    "cobblemon:fairy_feather", "cobblemon:feather", "cobblemon:fine_remedy",
                    "cobblemon:flame_orb", "cobblemon:float_stone", "cobblemon:focus_band",
                    "cobblemon:focus_sash", "cobblemon:fossil", "cobblemon:full_heal",
                    "cobblemon:full_restore", "cobblemon:fire_stone", "cobblemon:fire_stone_shard",
                    "cobblemon:ice_stone", "cobblemon:ice_stone_shard", "cobblemon:king_s_rock",
                    "cobblemon:king_s_rock_fragment", "cobblemon:leaf_stone", "cobblemon:leaf_stone_shard",
                    "cobblemon:magmarizer", "cobblemon:magmarizer_fragment", "cobblemon:metal_coat",
                    "cobblemon:metal_coat_fragment", "cobblemon:moon_stone", "cobblemon:moon_stone_shard",
                    "cobblemon:oval_stone", "cobblemon:prism_scale", "cobblemon:prism_scale_fragment",
                    "cobblemon:protector", "cobblemon:protector_fragment", "cobblemon:rare_candy",
                    "cobblemon:razor_claw", "cobblemon:razor_claw_fragment", "cobblemon:razor_fang",
                    "cobblemon:razor_fang_fragment", "cobblemon:reaper_cloth", "cobblemon:reaper_cloth_fragment",
                    "cobblemon:sachet", "cobblemon:sachet_fragment", "cobblemon:shiny_stone",
                    "cobblemon:shiny_stone_shard", "cobblemon:sun_stone", "cobblemon:sun_stone_shard",
                    "cobblemon:thunder_stone", "cobblemon:thunder_stone_shard", "cobblemon:upgrade",
                    "cobblemon:upgrade_fragment", "cobblemon:water_stone", "cobblemon:water_stone_shard",
                    "cobblemon:whipped_dream", "cobblemon:whipped_dream_fragment"
            };

            for (String item : cobblemonItemList) allItems.add(item);

            allItemsDrop.add("items", allItems);
            allItemsDrop.addProperty("min", 1);
            allItemsDrop.addProperty("max", 1);
            allItemsDrop.addProperty("chance", 4.5F);

            pool.add(allItemsDrop);

            // levelWeighting customizado
            JsonArray levelWeights = new JsonArray();

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
