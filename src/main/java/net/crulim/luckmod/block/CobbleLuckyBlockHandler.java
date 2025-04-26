package net.crulim.luckmod.block;

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

    private static List<DropItem> minecraftItems = new ArrayList<>();
    private static List<DropItem> cobblemonItems = new ArrayList<>();
    private static List<String> pokemons = new ArrayList<>();
    private static int minLevel = 5;
    private static int maxLevel = 25;
    private static int shinyChancePercent = 5;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_PATH = "config/luck_cobble_config.json";

    public static void loadConfig() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                generateDefaultConfig(file);
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)).getAsJsonObject();

            minecraftItems.clear();
            cobblemonItems.clear();
            pokemons.clear();

            JsonArray mcItems = json.getAsJsonArray("minecraftItems");
            for (var element : mcItems) {
                JsonObject obj = element.getAsJsonObject();
                minecraftItems.add(new DropItem(obj.get("id").getAsString(), obj.get("min").getAsInt(), obj.get("max").getAsInt()));
            }

            JsonArray cobbleItems = json.getAsJsonArray("cobblemonItems");
            for (var element : cobbleItems) {
                JsonObject obj = element.getAsJsonObject();
                cobblemonItems.add(new DropItem(obj.get("id").getAsString(), obj.get("min").getAsInt(), obj.get("max").getAsInt()));
            }

            JsonArray pokeArray = json.getAsJsonArray("pokemons");
            for (var element : pokeArray) {
                pokemons.add(element.getAsString());
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

    public static void handleLuck(ServerWorld world, BlockPos pos) {
        List<String> validTypes = new ArrayList<>();
        if (!minecraftItems.isEmpty()) validTypes.add("minecraft");
        if (!cobblemonItems.isEmpty()) validTypes.add("cobblemon");
        if (!pokemons.isEmpty()) validTypes.add("pokemon");

        if (validTypes.isEmpty()) {
            System.out.println("[CobbleLuckyBlock] Warning: No valid drops configured!");
            return;
        }

        Random random = world.getRandom();
        String selectedType = validTypes.get(random.nextInt(validTypes.size()));

        switch (selectedType) {
            case "minecraft" -> dropMinecraftItem(world, pos);
            case "cobblemon" -> dropCobblemonItem(world, pos);
            case "pokemon" -> spawnPokemon(world, pos);
            default -> System.out.println("[CobbleLuckyBlock] Unknown drop type selected!");
        }
    }

    private static void dropMinecraftItem(ServerWorld world, BlockPos pos) {
        Random random = world.getRandom();
        DropItem drop = minecraftItems.get(random.nextInt(minecraftItems.size()));
        Item item = Registries.ITEM.get(Identifier.of(drop.id));
        ItemStack stack = new ItemStack(item, random.nextBetween(drop.min, drop.max));
        Block.dropStack(world, pos.up(), stack);
    }

    private static void dropCobblemonItem(ServerWorld world, BlockPos pos) {
        Random random = world.getRandom();
        DropItem drop = cobblemonItems.get(random.nextInt(cobblemonItems.size()));
        Item item = Registries.ITEM.get(Identifier.of(drop.id));
        ItemStack stack = new ItemStack(item, random.nextBetween(drop.min, drop.max));
        Block.dropStack(world, pos.up(), stack);
    }

    private static void spawnPokemon(ServerWorld world, BlockPos pos) {
        Random random = world.getRandom();
        String speciesName = pokemons.get(random.nextInt(pokemons.size()));

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

            defaultConfig.addProperty("_info_minecraftItems", "Minecraft item drops: id = item ID, min = minimum quantity, max = maximum quantity");
            JsonArray minecraftItems = new JsonArray();
            minecraftItems.add(createDropItem("minecraft:diamond", 1, 3));
            minecraftItems.add(createDropItem("minecraft:golden_apple", 1, 2));
            minecraftItems.add(createDropItem("minecraft:netherite_scrap", 1, 1));
            minecraftItems.add(createDropItem("minecraft:experience_bottle", 5, 10));
            defaultConfig.add("minecraftItems", minecraftItems);

            defaultConfig.addProperty("_info_cobblemonItems", "Cobblemon item drops: id = item ID, min = minimum quantity, max = maximum quantity");
            JsonArray cobblemonItems = new JsonArray();
            cobblemonItems.add(createDropItem("cobblemon:pokeball", 3, 6));
            cobblemonItems.add(createDropItem("cobblemon:great_ball", 2, 4));
            cobblemonItems.add(createDropItem("cobblemon:rare_candy", 1, 2));
            defaultConfig.add("cobblemonItems", cobblemonItems);

            defaultConfig.addProperty("_info_pokemons", "List of Pokémon species names that can spawn (example: eevee, charmander, dratini)");
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

            defaultConfig.add("pokemons", pokemons);

            JsonObject levelRange = new JsonObject();
            levelRange.addProperty("min", 5);
            levelRange.addProperty("max", 30);
            defaultConfig.add("levelRange", levelRange);

            defaultConfig.addProperty("_info_shinyChancePercent", "Chance (%) for the Pokémon to be shiny.");
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

    private static JsonObject createDropItem(String id, int min, int max) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("min", min);
        obj.addProperty("max", max);
        return obj;
    }

    private static class DropItem {
        String id;
        int min;
        int max;

        DropItem(String id, int min, int max) {
            this.id = id;
            this.min = min;
            this.max = max;
        }
    }
}
