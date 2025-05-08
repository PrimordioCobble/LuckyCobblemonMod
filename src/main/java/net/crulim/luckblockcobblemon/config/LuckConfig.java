package net.crulim.luckblockcobblemon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LuckConfig {
    public static List<LuckPoolEntry> luckPool = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_PATH = "./config/luck_config.json";

    public static void load() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                generateDefaultConfig(file);
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)).getAsJsonObject();
            luckPool.clear();

            JsonArray poolArray = json.getAsJsonArray("luckPool");
            for (var element : poolArray) {
                JsonObject obj = element.getAsJsonObject();
                String id = obj.get("id").getAsString();
                int min = obj.get("min").getAsInt();
                int max = obj.get("max").getAsInt();
                luckPool.add(new LuckPoolEntry(id, min, max));
            }

            System.out.println("[LuckMod] Luck config loaded successfully.");
        } catch (Exception e) {
            System.out.println("[LuckMod] Failed to load config: " + e.getMessage());
        }
    }

    private static void generateDefaultConfig(File file) {
        try {
            JsonObject defaultConfig = new JsonObject();

            JsonArray poolArray = new JsonArray();
            poolArray.add(createDropItem("minecraft:diamond", 1, 3));
            poolArray.add(createDropItem("minecraft:emerald", 2, 4));
            poolArray.add(createDropItem("minecraft:golden_apple", 1, 1));
            poolArray.add(createDropItem("minecraft:ender_pearl", 4, 8));
            poolArray.add(createDropItem("minecraft:tnt", 5, 10));
            defaultConfig.add("luckPool", poolArray);

            File configDir = new File("./config");
            if (!configDir.exists()) configDir.mkdirs();

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(defaultConfig, writer);
            }

            System.out.println("[LuckMod] Default luck config created.");
        } catch (Exception e) {
            System.out.println("[LuckMod] Failed to create default config: " + e.getMessage());
        }
    }

    private static JsonObject createDropItem(String id, int min, int max) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("min", min);
        obj.addProperty("max", max);
        return obj;
    }

    public static class LuckPoolEntry {
        public final String id;
        public final int min;
        public final int max;

        public LuckPoolEntry(String id, int min, int max) {
            this.id = id;
            this.min = min;
            this.max = max;
        }
    }
}
