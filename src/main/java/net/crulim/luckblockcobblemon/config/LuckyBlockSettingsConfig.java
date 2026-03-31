package net.crulim.luckblockcobblemon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public final class LuckyBlockSettingsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final File FILE = new File("config/luckyblock_settings.json");

    private static boolean enablePocketRecipe = true;
    private static boolean enableVanillaRecipe = true;
    private static boolean enablePocketWorldgen = true;
    private static boolean enableStructureWorldgen = true;

    private LuckyBlockSettingsConfig() {
    }

    public static void load() {
        try {
            if (!FILE.exists()) {
                generateDefault();
            }

            try (Reader reader = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                enablePocketRecipe = getBoolean(json, "enablePocketRecipe", true);
                enableVanillaRecipe = getBoolean(json, "enableVanillaRecipe", true);
                enablePocketWorldgen = getBoolean(json, "enablePocketWorldgen", true);
                enableStructureWorldgen = getBoolean(json, "enableStructureWorldgen", true);
            }
        } catch (Exception e) {
            enablePocketRecipe = true;
            enableVanillaRecipe = true;
            enablePocketWorldgen = true;
            enableStructureWorldgen = true;
            e.printStackTrace();
        }
    }

    public static boolean isPocketRecipeEnabled() {
        return enablePocketRecipe;
    }

    public static boolean isVanillaRecipeEnabled() {
        return enableVanillaRecipe;
    }

    public static boolean isPocketWorldgenEnabled() {
        return enablePocketWorldgen;
    }

    public static boolean isStructureWorldgenEnabled() {
        return enableStructureWorldgen;
    }

    public static boolean isEnabled(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        JsonObject json = readJson();
        if (json == null) {
            return switch (key) {
                case "enablePocketRecipe", "enableVanillaRecipe", "enablePocketWorldgen", "enableStructureWorldgen" -> true;
                default -> false;
            };
        }

        return getBoolean(json, key, switch (key) {
            case "enablePocketRecipe", "enableVanillaRecipe", "enablePocketWorldgen", "enableStructureWorldgen" -> true;
            default -> false;
        });
    }

    private static JsonObject readJson() {
        try {
            if (!FILE.exists()) {
                generateDefault();
            }

            try (Reader reader = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static void generateDefault() {
        try {
            File parent = FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            JsonObject json = new JsonObject();

            JsonArray note = new JsonArray();
            note.add("enablePocketRecipe: enables/disables the recipe for luck_block_pocket.");
            note.add("enableVanillaRecipe: enables/disables the recipe for luck_block_vanilla.");
            note.add("enablePocketWorldgen: enables/disables natural generation of the pocket lucky block in new chunks.");
            note.add("enableStructureWorldgen: enables/disables natural structure generation from the mod in new chunks.");
            note.add("Recipe changes require /reload or a server restart.");
            note.add("Worldgen changes require a server restart and only affect new chunks.");
            json.add("_note", note);

            json.addProperty("enablePocketRecipe", true);
            json.addProperty("enableVanillaRecipe", true);
            json.addProperty("enablePocketWorldgen", true);
            json.addProperty("enableStructureWorldgen", true);

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}