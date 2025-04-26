package net.crulim.luckmod.block;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import com.google.gson.GsonBuilder;

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
            File file = new File("./config/luck_cobble_config.json");
            if (!file.exists()) {
                generateDefaultConfig(file);
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)).getAsJsonObject();

            minecraftItems.clear();
            cobblemonItems.clear();
            pokemons.clear();

            // Carrega normalmente
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

            System.out.println("[LuckyBlock] Config loaded successfully.");

        } catch (Exception e) {
            System.out.println("[LuckyBlock] Failed to load config: " + e.getMessage());
        }
    }


    public static void reloadConfig() {
        loadConfig();
        System.out.println("[LuckyBlock] Config reloaded!");
    }

    private static void generateDefaultConfig(File file) {
        try {
            JsonObject defaultConfig = new JsonObject();

            // Explicações para o usuário
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
            pokemons.add("eevee");
            pokemons.add("charmander");
            pokemons.add("pikachu");
            pokemons.add("dratini");
            defaultConfig.add("pokemons", pokemons);

            defaultConfig.addProperty("_info_levelRange", "Defines minimum and maximum levels for Pokémon that spawn");
            JsonObject levelRange = new JsonObject();
            levelRange.addProperty("min", 5);
            levelRange.addProperty("max", 30);
            defaultConfig.add("levelRange", levelRange);

            defaultConfig.addProperty("_info_shinyChancePercent", "Percentage chance for a spawned Pokémon to be shiny");
            defaultConfig.addProperty("shinyChancePercent", 5);

            File configDir = new File("./config");
            if (!configDir.exists()) configDir.mkdirs();

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(defaultConfig, writer);
            }

            System.out.println("[LuckyBlock] Default config example with explanations created.");
        } catch (Exception e) {
            System.out.println("[LuckyBlock] Failed to create default config: " + e.getMessage());
        }
    }




    private static JsonObject createDropItem(String id, int min, int max) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("min", min);
        obj.addProperty("max", max);
        return obj;
    }

    public static void handleLuck(ServerWorld world, BlockPos pos) {
        if (minecraftItems.isEmpty() && cobblemonItems.isEmpty() && pokemons.isEmpty()) {
            System.out.println("[LuckyBlock] Warning: No drops configured!");
            return;
        }

        Random random = world.getRandom();
        int event = random.nextInt(3); // 0 = Minecraft item, 1 = Cobblemon item, 2 = Pokémon

        if (event == 0 && !minecraftItems.isEmpty()) {
            dropMinecraftItem(world, pos);
        } else if (event == 1 && !cobblemonItems.isEmpty()) {
            dropCobblemonItem(world, pos);
        } else if (event == 2 && !pokemons.isEmpty()) {
            spawnPokemon(world, pos);
        } else {
            System.out.println("[LuckyBlock] No valid drop for event " + event);
        }
    }


    private static void dropMinecraftItem(ServerWorld world, BlockPos pos) {
        Random random = world.getRandom();
        DropItem drop = minecraftItems.get(random.nextInt(minecraftItems.size()));
        Item item = Registries.ITEM.get(Identifier.of(drop.id));
        ItemStack stack = new ItemStack(item, random.nextBetween(drop.min, drop.max));
        Block.dropStack(world, pos, stack);
    }

    private static void dropCobblemonItem(ServerWorld world, BlockPos pos) {
        Random random = world.getRandom();
        DropItem drop = cobblemonItems.get(random.nextInt(cobblemonItems.size()));
        Item item = Registries.ITEM.get(Identifier.of(drop.id));
        ItemStack stack = new ItemStack(item, random.nextBetween(drop.min, drop.max));
        Block.dropStack(world, pos, stack);
    }

    private static void spawnPokemon(ServerWorld world, BlockPos pos) {
        Random random = world.getRandom();
        String speciesName = pokemons.get(random.nextInt(pokemons.size()));

        Species species = PokemonSpecies.INSTANCE.getByName(speciesName);
        if (species == null) {
            System.out.println("[LuckyBlock] Species not found: " + speciesName);
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
            System.out.println("[LuckyBlock] Failed to spawn Pokémon: " + speciesName);
        }
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
