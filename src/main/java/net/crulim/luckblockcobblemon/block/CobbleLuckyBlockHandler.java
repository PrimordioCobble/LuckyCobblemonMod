package net.crulim.luckblockcobblemon.block;

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

public class CobbleLuckyBlockHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Random random = Random.create();
    private static final String CONFIG_PATH = "config/luck_cobble_config.json";

    private static List<JsonObject> luckPool = new ArrayList<>();
    private static int minLevel = 5;
    private static int maxLevel = 30;
    private static int shinyChancePercent = 5;


    /*
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
            shinyChancePercent = json.get("shinyChancePercent").getAsInt();

            System.out.println("[CobbleLuckyBlock] Config loaded successfully.");

        } catch (Exception e) {
            System.out.println("[CobbleLuckyBlock] Failed to load config: " + e.getMessage());
        }
    }

    public static void reloadConfig() {
        loadConfig();
        System.out.println("[CobbleLuckyBlock] Config reloaded!");
    }
*/
    public static void handleLuck(ServerWorld world, BlockPos pos) {
        if (luckPool.isEmpty()) {
            System.out.println("[CobbleLuckyBlock] Warning: Luck pool is empty!");
            return;
        }

        List<JsonObject> validPool = filterValidEvents();

        if (validPool.isEmpty()) {
            System.out.println("[CobbleLuckyBlock] No valid events to pick from!");
            return;
        }

        JsonObject selected = pickRandomLuck(validPool);
        if (selected == null) {
            System.out.println("[CobbleLuckyBlock] No luck event selected!");
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
                case "pokemon" -> {
                    if (event.has("pokemons") && !event.getAsJsonArray("pokemons").isEmpty()) {
                        valid.add(event);
                    }
                }
                default -> System.out.println("[CobbleLuckyBlock] Unknown type ignored in filter: " + type);
            }
        }

        return valid;
    }

    private static JsonObject pickRandomLuck(List<JsonObject> pool) {
        if (pool.isEmpty()) return null;

        int totalChance = pool.stream()
                .mapToInt(obj -> obj.has("chance") ? obj.get("chance").getAsInt() : 1)
                .sum();

        if (totalChance <= 0) return null;

        int roll = random.nextInt(totalChance);
        int cumulative = 0;

        for (JsonObject obj : pool) {
            int chance = obj.has("chance") ? obj.get("chance").getAsInt() : 1;
            cumulative += chance;
            if (roll < cumulative) {
                return obj;
            }
        }

        return null;
    }

    private static void executeLuck(ServerWorld world, BlockPos pos, JsonObject data) {
        String type = data.get("type").getAsString();

        switch (type) {
            case "item" -> dropItem(world, pos, data);
            case "cobbleitem" -> dropCobbleItem(world, pos, data);
            case "pokemon" -> spawnPokemon(world, pos, data);
            default -> System.out.println("[CobbleLuckyBlock] Unknown luck type: " + type);
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
        JsonArray items = data.getAsJsonArray("items");
        if (items == null || items.isEmpty()) return;

        String id = items.get(random.nextInt(items.size())).getAsString();
        int min = data.get("min").getAsInt();
        int max = data.get("max").getAsInt();
        Item item = Registries.ITEM.get(Identifier.of(id));
        ItemStack stack = new ItemStack(item, random.nextBetween(min, max));
        Block.dropStack(world, pos.up(), stack);
    }

    private static void spawnPokemon(ServerWorld world, BlockPos pos, JsonObject data) {
        JsonArray pokemons = data.getAsJsonArray("pokemons");
        if (pokemons == null || pokemons.isEmpty()) return;

        String speciesName = pokemons.get(random.nextInt(pokemons.size())).getAsString();
        Species species = PokemonSpecies.INSTANCE.getByName(speciesName);
        if (species == null) {
            System.out.println("[CobbleLuckyBlock] Species not found: " + speciesName);
            return;
        }

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(random.nextBetween(minLevel, maxLevel));

        if (random.nextInt(100) < shinyChancePercent) {
            pokemon.setShiny(true);
        }

        Vec3d spawnPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        PokemonEntity entity = pokemon.sendOut(world, spawnPos, null, e -> null);

        if (entity == null) {
            System.out.println("[CobbleLuckyBlock] Failed to spawn Pokémon: " + speciesName);
        }
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
            itemDrop.addProperty("chance", 40);
            pool.add(itemDrop);

            JsonObject cobbleItemDrop = new JsonObject();
            cobbleItemDrop.addProperty("type", "cobbleitem");
            JsonArray cobbleItems = new JsonArray();
            cobbleItems.add("cobblemon:pokeball");
            cobbleItems.add("cobblemon:great_ball");
            cobbleItemDrop.add("items", cobbleItems);
            cobbleItemDrop.addProperty("min", 2);
            cobbleItemDrop.addProperty("max", 5);
            cobbleItemDrop.addProperty("chance", 30);
            pool.add(cobbleItemDrop);

            JsonObject pokemonSpawn = new JsonObject();
            pokemonSpawn.addProperty("type", "pokemon");
            JsonArray pokemons = new JsonArray();
            pokemons.add("bulbasaur");
            pokemons.add("ivysaur");
            pokemons.add("venusaur");
            pokemons.add("charmander");
            pokemons.add("charmeleon");
            pokemons.add("charizard");
            pokemons.add("squirtle");
            pokemons.add("wartortle");
            pokemons.add("blastoise");
            pokemons.add("caterpie");
            pokemons.add("metapod");
            pokemons.add("butterfree");
            pokemons.add("weedle");
            pokemons.add("kakuna");
            pokemons.add("beedrill");
            pokemons.add("pidgey");
            pokemons.add("pidgeotto");
            pokemons.add("pidgeot");
            pokemons.add("rattata");
            pokemons.add("raticate");
            pokemons.add("spearow");
            pokemons.add("fearow");
            pokemons.add("ekans");
            pokemons.add("arbok");
            pokemons.add("pikachu");
            pokemons.add("raichu");
            pokemons.add("sandshrew");
            pokemons.add("sandslash");
            pokemons.add("nidoran_f");
            pokemons.add("nidorina");
            pokemons.add("nidoqueen");
            pokemons.add("nidoran_m");
            pokemons.add("nidorino");
            pokemons.add("nidoking");
            pokemons.add("clefairy");
            pokemons.add("clefable");
            pokemons.add("vulpix");
            pokemons.add("ninetales");
            pokemons.add("jigglypuff");
            pokemons.add("wigglytuff");
            pokemons.add("zubat");
            pokemons.add("golbat");
            pokemons.add("oddish");
            pokemons.add("gloom");
            pokemons.add("vileplume");
            pokemons.add("paras");
            pokemons.add("parasect");
            pokemons.add("venonat");
            pokemons.add("venomoth");
            pokemons.add("diglett");
            pokemons.add("dugtrio");
            pokemons.add("meowth");
            pokemons.add("persian");
            pokemons.add("psyduck");
            pokemons.add("golduck");
            pokemons.add("mankey");
            pokemons.add("primeape");
            pokemons.add("growlithe");
            pokemons.add("arcanine");
            pokemons.add("poliwag");
            pokemons.add("poliwhirl");
            pokemons.add("poliwrath");
            pokemons.add("abra");
            pokemons.add("kadabra");
            pokemons.add("alakazam");
            pokemons.add("machop");
            pokemons.add("machoke");
            pokemons.add("machamp");
            pokemons.add("bellsprout");
            pokemons.add("weepinbell");
            pokemons.add("victreebel");
            pokemons.add("tentacool");
            pokemons.add("tentacruel");
            pokemons.add("geodude");
            pokemons.add("graveler");
            pokemons.add("golem");
            pokemons.add("ponyta");
            pokemons.add("rapidash");
            pokemons.add("slowpoke");
            pokemons.add("slowbro");
            pokemons.add("magnemite");
            pokemons.add("magneton");
            pokemons.add("farfetchd");
            pokemons.add("doduo");
            pokemons.add("dodrio");
            pokemons.add("seel");
            pokemons.add("dewgong");
            pokemons.add("grimer");
            pokemons.add("muk");
            pokemons.add("shellder");
            pokemons.add("cloyster");
            pokemons.add("gastly");
            pokemons.add("haunter");
            pokemons.add("gengar");
            pokemons.add("onix");
            pokemons.add("drowzee");
            pokemons.add("hypno");
            pokemons.add("krabby");
            pokemons.add("kingler");
            pokemons.add("voltorb");
            pokemons.add("electrode");
            pokemons.add("exeggcute");
            pokemons.add("exeggutor");
            pokemons.add("cubone");
            pokemons.add("marowak");
            pokemons.add("hitmonlee");
            pokemons.add("hitmonchan");
            pokemons.add("lickitung");
            pokemons.add("koffing");
            pokemons.add("weezing");
            pokemons.add("rhyhorn");
            pokemons.add("rhydon");
            pokemons.add("chansey");
            pokemons.add("tangela");
            pokemons.add("kangaskhan");
            pokemons.add("horsea");
            pokemons.add("seadra");
            pokemons.add("goldeen");
            pokemons.add("seaking");
            pokemons.add("staryu");
            pokemons.add("starmie");
            pokemons.add("mr_mime");
            pokemons.add("scyther");
            pokemons.add("jynx");
            pokemons.add("electabuzz");
            pokemons.add("magmar");
            pokemons.add("pinsir");
            pokemons.add("tauros");
            pokemons.add("magikarp");
            pokemons.add("gyarados");
            pokemons.add("lapras");
            pokemons.add("ditto");
            pokemons.add("eevee");
            pokemons.add("vaporeon");
            pokemons.add("jolteon");
            pokemons.add("flareon");
            pokemons.add("porygon");
            pokemons.add("omanyte");
            pokemons.add("omastar");
            pokemons.add("kabuto");
            pokemons.add("kabutops");
            pokemons.add("aerodactyl");
            pokemons.add("snorlax");
            pokemons.add("articuno");
            pokemons.add("zapdos");
            pokemons.add("moltres");
            pokemons.add("dratini");
            pokemons.add("dragonair");
            pokemons.add("dragonite");
            pokemons.add("mewtwo");
            pokemons.add("mew");
            pokemons.add("bulbasaur");
            pokemons.add("ivysaur");
            pokemons.add("venusaur");
            pokemons.add("charmander");
            pokemons.add("charmeleon");
            pokemons.add("charizard");
            pokemons.add("squirtle");
            pokemons.add("wartortle");
            pokemons.add("blastoise");
            pokemons.add("caterpie");
            pokemons.add("metapod");
            pokemons.add("butterfree");
            pokemons.add("weedle");
            pokemons.add("kakuna");
            pokemons.add("beedrill");
            pokemons.add("pidgey");
            pokemons.add("pidgeotto");
            pokemons.add("pidgeot");
            pokemons.add("rattata");
            pokemons.add("raticate");
            pokemons.add("spearow");
            pokemons.add("fearow");
            pokemons.add("ekans");
            pokemons.add("arbok");
            pokemons.add("pikachu");
            pokemons.add("raichu");
            pokemons.add("sandshrew");
            pokemons.add("sandslash");
            pokemons.add("nidoran_f");
            pokemons.add("nidorina");
            pokemons.add("nidoqueen");
            pokemons.add("nidoran_m");
            pokemons.add("nidorino");
            pokemons.add("nidoking");
            pokemons.add("clefairy");
            pokemons.add("clefable");
            pokemons.add("vulpix");
            pokemons.add("ninetales");
            pokemons.add("jigglypuff");
            pokemons.add("wigglytuff");
            pokemons.add("zubat");
            pokemons.add("golbat");
            pokemons.add("oddish");
            pokemons.add("gloom");
            pokemons.add("vileplume");
            pokemons.add("paras");
            pokemons.add("parasect");
            pokemons.add("venonat");
            pokemons.add("venomoth");
            pokemons.add("diglett");
            pokemons.add("dugtrio");
            pokemons.add("meowth");
            pokemons.add("persian");
            pokemons.add("psyduck");
            pokemons.add("golduck");
            pokemons.add("mankey");
            pokemons.add("primeape");
            pokemons.add("growlithe");
            pokemons.add("arcanine");
            pokemons.add("poliwag");
            pokemons.add("poliwhirl");
            pokemons.add("poliwrath");
            pokemons.add("abra");
            pokemons.add("kadabra");
            pokemons.add("alakazam");
            pokemons.add("machop");
            pokemons.add("machoke");
            pokemons.add("machamp");
            pokemons.add("bellsprout");
            pokemons.add("weepinbell");
            pokemons.add("victreebel");
            pokemons.add("tentacool");
            pokemons.add("tentacruel");
            pokemons.add("geodude");
            pokemons.add("graveler");
            pokemons.add("golem");
            pokemons.add("ponyta");
            pokemons.add("rapidash");
            pokemons.add("slowpoke");
            pokemons.add("slowbro");
            pokemons.add("magnemite");
            pokemons.add("magneton");
            pokemons.add("farfetchd");
            pokemons.add("doduo");
            pokemons.add("dodrio");
            pokemons.add("seel");
            pokemons.add("dewgong");
            pokemons.add("grimer");
            pokemons.add("muk");
            pokemons.add("shellder");
            pokemons.add("cloyster");
            pokemons.add("gastly");
            pokemons.add("haunter");
            pokemons.add("gengar");
            pokemons.add("onix");
            pokemons.add("drowzee");
            pokemons.add("hypno");
            pokemons.add("krabby");
            pokemons.add("kingler");
            pokemons.add("voltorb");
            pokemons.add("electrode");
            pokemons.add("exeggcute");
            pokemons.add("exeggutor");
            pokemons.add("cubone");
            pokemons.add("marowak");
            pokemons.add("hitmonlee");
            pokemons.add("hitmonchan");
            pokemons.add("lickitung");
            pokemons.add("koffing");
            pokemons.add("weezing");
            pokemons.add("rhyhorn");
            pokemons.add("rhydon");
            pokemons.add("chansey");
            pokemons.add("tangela");
            pokemons.add("kangaskhan");
            pokemons.add("horsea");
            pokemons.add("seadra");
            pokemons.add("goldeen");
            pokemons.add("seaking");
            pokemons.add("staryu");
            pokemons.add("starmie");
            pokemons.add("mr_mime");
            pokemons.add("scyther");
            pokemons.add("jynx");
            pokemons.add("electabuzz");
            pokemons.add("magmar");
            pokemons.add("pinsir");
            pokemons.add("tauros");
            pokemons.add("magikarp");
            pokemons.add("gyarados");
            pokemons.add("lapras");
            pokemons.add("ditto");
            pokemons.add("eevee");
            pokemons.add("vaporeon");
            pokemons.add("jolteon");
            pokemons.add("flareon");
            pokemons.add("porygon");
            pokemons.add("omanyte");
            pokemons.add("omastar");
            pokemons.add("kabuto");
            pokemons.add("kabutops");
            pokemons.add("aerodactyl");
            pokemons.add("snorlax");
            pokemons.add("articuno");
            pokemons.add("zapdos");
            pokemons.add("moltres");
            pokemons.add("dratini");
            pokemons.add("dragonair");
            pokemons.add("dragonite");
            pokemons.add("mewtwo");
            pokemons.add("mew");
            pokemonSpawn.add("pokemons", pokemons);
            pokemonSpawn.addProperty("chance", 30);
            pool.add(pokemonSpawn);

            defaultConfig.add("luckPool", pool);

            JsonObject levelRange = new JsonObject();
            levelRange.addProperty("min", 5);
            levelRange.addProperty("max", 30);
            defaultConfig.add("levelRange", levelRange);

            defaultConfig.addProperty("shinyChancePercent", 5);

            File configDir = new File("./config");
            if (!configDir.exists()) configDir.mkdirs();

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(defaultConfig, writer);
            }

            System.out.println("[CobbleLuckyBlock] Default config created.");

        } catch (Exception e) {
            System.out.println("[CobbleLuckyBlock] Failed to create default config: " + e.getMessage());
        }
    }
}
