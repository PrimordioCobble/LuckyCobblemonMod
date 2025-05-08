package net.crulim.luckblockcobblemon.handler;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.*;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LuckyBlockHandlerPocket {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Random random = Random.create();
    private static final String CONFIG_PATH = "config/luckpocket_config.json";

    private static List<JsonObject> luckPool = new ArrayList<>();
    private static int minLevel = 5;
    private static int maxLevel = 30;
    private static float shinyChancePercent = 5.0F;

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

            JsonObject levelRange = json.getAsJsonObject("levelRange");
            minLevel = levelRange.get("min").getAsInt();
            maxLevel = levelRange.get("max").getAsInt();
            shinyChancePercent = json.get("shinyChancePercent").getAsFloat();

            System.out.println("[LuckyBlockPocket] Config loaded successfully.");

        } catch (Exception e) {
            System.out.println("[LuckyBlockPocket] Failed to load config: " + e.getMessage());
            e.printStackTrace();
        }
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
                case "pokemon", "random_pokemon", "shiny_pokemon" -> valid.add(event);
                case "structure" -> {
                    if ((event.has("structure") && !event.get("structure").getAsString().isEmpty()) ||
                            (event.has("structures") && event.getAsJsonArray("structures").size() > 0)) {
                        valid.add(event);
                    }
                }
                case "multi_pokemon" -> valid.add(event);
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
            case "pokemon" -> spawnPokemon(world, pos, data, false);
            case "random_pokemon" -> spawnRandomPokemon(world, pos, data);
            case "shiny_pokemon" -> spawnPokemon(world, pos, data, true);
            case "structure" -> spawnStructure(world, pos, data);
            case "multi_pokemon" -> spawnMultiplePokemons(world, pos, data);
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
        ItemStack stack = new ItemStack(item, random.nextBetween(min, max));
        Block.dropStack(world, pos.up(), stack);
    }

    private static void dropCobbleItem(ServerWorld world, BlockPos pos, JsonObject data) {
        dropItem(world, pos, data); // Pode ser separado se quiser depois
    }

    private static void spawnPokemon(ServerWorld world, BlockPos pos, JsonObject data, boolean forceShiny) {
        System.out.println("[LuckyBlockPocket] Iniciando spawnPokemon com dados: " + data.toString());

        String speciesName;
        if (data.has("species")) {
            speciesName = data.get("species").getAsString();
        } else if (data.has("pokemons")) {
            JsonArray list = data.getAsJsonArray("pokemons");
            if (list.isEmpty()) {
                System.out.println("[LuckyBlockPocket] Lista de pokemons vazia.");
                return;
            }
            speciesName = list.get(random.nextInt(list.size())).getAsString();
        } else {
            System.out.println("[LuckyBlockPocket] Nenhuma espécie ou lista de pokemons definida.");
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

        int level = data.has("level") ? data.get("level").getAsInt()
                : (data.has("minLevel") && data.has("maxLevel") ?
                random.nextBetween(data.get("minLevel").getAsInt(), data.get("maxLevel").getAsInt())
                : random.nextBetween(minLevel, maxLevel));

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




    private static void spawnRandomPokemon(ServerWorld world, BlockPos pos, JsonObject data) {
        List<Species> allSpecies = new ArrayList<>(PokemonSpecies.INSTANCE.getSpecies());
        if (allSpecies.isEmpty()) return;

        Species species = allSpecies.get(random.nextInt(allSpecies.size()));
        String speciesName = species.getName().toLowerCase(Locale.ROOT).replace(" ", "_");

        int min = data.has("minLevel") ? data.get("minLevel").getAsInt() : minLevel;
        int max = data.has("maxLevel") ? data.get("maxLevel").getAsInt() : maxLevel;
        float shinyChance = data.has("shinyChance") ? data.get("shinyChance").getAsFloat() : shinyChancePercent;

        JsonObject fakeData = new JsonObject();
        fakeData.addProperty("species", speciesName);
        fakeData.addProperty("level", random.nextBetween(min, max));
        fakeData.addProperty("shinyChance", shinyChance);
        spawnPokemon(world, pos, fakeData, false);
    }

    private static void spawnMultiplePokemons(ServerWorld world, BlockPos pos, JsonObject data) {
        int min = data.has("min") ? data.get("min").getAsInt() : 1;
        int max = data.has("max") ? data.get("max").getAsInt() : 3;
        int count = random.nextBetween(min, max + 1);

        int minLevel = data.has("minLevel") ? data.get("minLevel").getAsInt() : LuckyBlockHandlerPocket.minLevel;
        int maxLevel = data.has("maxLevel") ? data.get("maxLevel").getAsInt() : LuckyBlockHandlerPocket.maxLevel;
        float shinyChance = data.has("shinyChance") ? data.get("shinyChance").getAsFloat() : shinyChancePercent;

        List<Species> allSpecies = new ArrayList<>(PokemonSpecies.INSTANCE.getSpecies());
        if (allSpecies.isEmpty()) return;

        for (int i = 0; i < count; i++) {
            Species species = allSpecies.get(random.nextInt(allSpecies.size()));
            String speciesName = species.getName().toLowerCase(Locale.ROOT).replace(" ", "_");

            JsonObject fakeData = new JsonObject();
            fakeData.addProperty("species", speciesName);
            fakeData.addProperty("level", random.nextBetween(minLevel, maxLevel));
            fakeData.addProperty("shinyChance", shinyChance);

            spawnPokemon(world, pos, fakeData, false);
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
            itemDrop.addProperty("chance", 3F);
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
            cobbleItems.add("cobblemon:pokeball");
            cobbleItems.add("cobblemon:great_ball");
            cobbleItems.add("cobblemon:ultra_ball");
            cobbleItems.add("cobblemon:quick_ball");
            cobbleItems.add("cobblemon:dusk_ball");
            cobbleItems.add("cobblemon:repeat_ball");
            cobbleItems.add("cobblemon:timer_ball");
            cobbleItems.add("cobblemon:nest_ball");
            cobbleItems.add("cobblemon:net_ball");
            cobbleItems.add("cobblemon:luxury_ball");
            cobbleItems.add("cobblemon:heal_ball");
            cobbleItems.add("cobblemon:friend_ball");
            cobbleItems.add("cobblemon:level_ball");
            cobbleItems.add("cobblemon:moon_ball");
            cobbleItems.add("cobblemon:love_ball");
            cobbleItems.add("cobblemon:heavy_ball");
            cobbleItems.add("cobblemon:fast_ball");
            cobbleItems.add("cobblemon:safari_ball");
            cobbleItemDrop.add("items", cobbleItems);
            cobbleItemDrop.addProperty("min", 1);
            cobbleItemDrop.addProperty("max", 5);
            cobbleItemDrop.addProperty("chance", 5F);
            pool.add(cobbleItemDrop);

            // Pokémon normal
            JsonObject pokemonSpawn = new JsonObject();
            pokemonSpawn.addProperty("type", "pokemon");
            JsonArray pokemons = new JsonArray();
            String[] pokemonList = {
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
            for (String name : pokemonList) pokemons.add(name);
            pokemonSpawn.add("pokemons", pokemons);
            pokemonSpawn.addProperty("minLevel", 5);
            pokemonSpawn.addProperty("maxLevel", 30);
            pokemonSpawn.addProperty("chance", 80F);
            pool.add(pokemonSpawn);

            // Shiny Pokémon
            JsonObject shinySpawn = new JsonObject();
            shinySpawn.addProperty("type", "shiny_pokemon");
            JsonArray shinyList = new JsonArray();
            for (String name : pokemonList) shinyList.add(name);
            shinySpawn.add("pokemons", shinyList);
            shinySpawn.addProperty("minLevel", 15);
            shinySpawn.addProperty("maxLevel", 45);
            shinySpawn.addProperty("chance", 1F);
            pool.add(shinySpawn);

            JsonObject randomPokemonSpawn = new JsonObject();
            randomPokemonSpawn.addProperty("type", "random_pokemon");
            randomPokemonSpawn.addProperty("chance", 10F); // porcentagem de ativação
            randomPokemonSpawn.addProperty("minLevel", 10);  // nível mínimo
            randomPokemonSpawn.addProperty("maxLevel", 35);  // nível máximo
            randomPokemonSpawn.addProperty("shinyChance", 0.02F);
            pool.add(randomPokemonSpawn);

            // Múltiplos Pokémons aleatórios
            JsonObject multiPokemonSpawn = new JsonObject();
            multiPokemonSpawn.addProperty("type", "multi_pokemon");
            multiPokemonSpawn.addProperty("chance", 0.5F); // chance de ativação
            multiPokemonSpawn.addProperty("min", 2);       // quantidade mínima
            multiPokemonSpawn.addProperty("max", 5);       // quantidade máxima
            multiPokemonSpawn.addProperty("minLevel", 10); // nível mínimo
            multiPokemonSpawn.addProperty("maxLevel", 35); // nível máximo
            multiPokemonSpawn.addProperty("shinyChance", 0.02F); // chance shiny por Pokémon
            pool.add(multiPokemonSpawn);



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
