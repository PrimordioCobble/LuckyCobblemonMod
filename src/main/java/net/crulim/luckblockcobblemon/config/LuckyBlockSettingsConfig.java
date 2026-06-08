package net.crulim.luckblockcobblemon.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LuckyBlockSettingsConfig {
    private static final Logger LOGGER = Logger.getLogger(LuckyBlockSettingsConfig.class.getName());

    private static boolean enablePocketRecipe = true;
    private static boolean enableVanillaRecipe = true;
    private static boolean enablePocketWorldgen = true;
    private static boolean enableStructureWorldgen = true;
    private static boolean enableStructureEvents = true;
    private static boolean strictConfigEvents = true;

    private static boolean breakCreativeDefaultLuckyBlock = false;
    private static boolean breakCreativeThemedLuckyBlocks = false;
    private static boolean breakCreativeLegendaryLuckyBlock = false;

    private LuckyBlockSettingsConfig() {
    }

    public static void load() {
        try {
            JsonObject json = LuckyBlockConfigManager.loadObjectWithLegacyMigration(
                    LuckyBlockConfigManager.settingsFile(),
                    LuckyBlockConfigManager.legacySettingsFile(),
                    LuckyBlockSettingsConfig::createDefaultSettings
            );

            normalizeSettingsJson(json);

            enablePocketRecipe = getBoolean(json, "enablePocketRecipe", true);
            enableVanillaRecipe = getBoolean(json, "enableVanillaRecipe", true);
            enablePocketWorldgen = getBoolean(json, "enablePocketWorldgen", true);
            enableStructureWorldgen = getBoolean(json, "enableStructureWorldgen", true);
            enableStructureEvents = getBoolean(json, "enableStructureEvents", true);
            strictConfigEvents = getBoolean(json, "strictConfigEvents", true);

            breakCreativeDefaultLuckyBlock = getBoolean(json, "breakCreativeDefaultLuckyBlock", getLegacyBreakCreativeFromDefaultPool(false));
            breakCreativeThemedLuckyBlocks = getBoolean(json, "breakCreativeThemedLuckyBlocks", getLegacyBreakCreativeFromThemedConfig(false));
            breakCreativeLegendaryLuckyBlock = getBoolean(json, "breakCreativeLegendaryLuckyBlock", getLegacyBreakCreativeFromLegendaryPool(false));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[LuckyBlockSettingsConfig] Failed to load settings. Using safe defaults.", e);
            enablePocketRecipe = true;
            enableVanillaRecipe = true;
            enablePocketWorldgen = true;
            enableStructureWorldgen = false;
            enableStructureEvents = false;
            strictConfigEvents = true;
            breakCreativeDefaultLuckyBlock = false;
            breakCreativeThemedLuckyBlocks = false;
            breakCreativeLegendaryLuckyBlock = false;
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

    public static boolean areStructureEventsEnabled() {
        return enableStructureEvents;
    }

    public static boolean isStrictConfigEventsEnabled() {
        return strictConfigEvents;
    }

    public static boolean isDefaultLuckyBlockBreakCreativeAllowed() {
        return breakCreativeDefaultLuckyBlock;
    }

    public static boolean isThemedLuckyBlockBreakCreativeAllowed() {
        return breakCreativeThemedLuckyBlocks;
    }

    public static boolean isLegendaryLuckyBlockBreakCreativeAllowed() {
        return breakCreativeLegendaryLuckyBlock;
    }

    public static boolean isEnabled(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        return switch (key) {
            case "enablePocketRecipe" -> enablePocketRecipe;
            case "enableVanillaRecipe" -> enableVanillaRecipe;
            case "enablePocketWorldgen" -> enablePocketWorldgen;
            case "enableStructureWorldgen" -> enableStructureWorldgen;
            case "enableStructureEvents" -> enableStructureEvents;
            case "strictConfigEvents" -> strictConfigEvents;
            default -> false;
        };
    }


    private static void normalizeSettingsJson(JsonObject json) {
        if (json == null) {
            return;
        }

        boolean changed = false;
        if (!json.has("configVersion")) {
            json.addProperty("configVersion", LuckyBlockConfigManager.CONFIG_VERSION);
            changed = true;
        }
        if (!json.has("_comment")) {
            json.addProperty("_comment", "Main Lucky Block Cobblemon settings. New configs live in config/luckyblockcobblemon/.");
            changed = true;
        }
        if (!json.has("enablePocketRecipe")) {
            json.addProperty("enablePocketRecipe", true);
            changed = true;
        }
        if (!json.has("enableVanillaRecipe")) {
            json.addProperty("enableVanillaRecipe", true);
            changed = true;
        }
        if (!json.has("enablePocketWorldgen")) {
            json.addProperty("enablePocketWorldgen", true);
            changed = true;
        }
        if (!json.has("enableStructureWorldgen")) {
            json.addProperty("enableStructureWorldgen", true);
            changed = true;
        }
        if (!json.has("enableStructureEvents")) {
            json.addProperty("enableStructureEvents", true);
            changed = true;
        }
        if (!json.has("strictConfigEvents")) {
            json.addProperty("strictConfigEvents", true);
            changed = true;
        }
        if (!json.has("breakCreativeDefaultLuckyBlock")) {
            json.addProperty("breakCreativeDefaultLuckyBlock", getLegacyBreakCreativeFromDefaultPool(false));
            changed = true;
        }
        if (!json.has("breakCreativeThemedLuckyBlocks")) {
            json.addProperty("breakCreativeThemedLuckyBlocks", getLegacyBreakCreativeFromThemedConfig(false));
            changed = true;
        }
        if (!json.has("breakCreativeLegendaryLuckyBlock")) {
            json.addProperty("breakCreativeLegendaryLuckyBlock", getLegacyBreakCreativeFromLegendaryPool(false));
            changed = true;
        }

        if (changed) {
            LuckyBlockConfigManager.writeJson(LuckyBlockConfigManager.settingsFile(), json);
        }
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        try {
            return json != null && json.has(key) ? json.get(key).getAsBoolean() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static JsonObject createDefaultSettings() {
        JsonObject bundled = LuckyBlockConfigManager.loadBundledObject("luckyblockcobblemon/default_configs/settings.json");
        if (bundled != null) {
            return bundled;
        }

        JsonObject json = new JsonObject();
        json.addProperty("configVersion", LuckyBlockConfigManager.CONFIG_VERSION);
        json.addProperty("_comment", "Main Lucky Block Cobblemon settings. New configs live in config/luckyblockcobblemon/.");

        JsonArray note = new JsonArray();
        note.add("enablePocketRecipe: enables/disables the recipe for luck_block_pocket.");
        note.add("enableVanillaRecipe: enables/disables the recipe for luck_block_vanilla.");
        note.add("enablePocketWorldgen: enables/disables natural generation of themed Lucky Blocks in new chunks.");
        note.add("enableStructureWorldgen: enables/disables natural structure generation from this mod in new chunks.");
        note.add("enableStructureEvents: allows structure events inside Lucky Block pools.");
        note.add("strictConfigEvents: if true, Lucky Blocks only execute valid events from the loaded config and never use hidden fallback events.");
        note.add("Recipe changes require /reload or a server restart. Worldgen changes require a server restart and only affect new chunks.");
        json.add("_note", note);

        json.addProperty("enablePocketRecipe", true);
        json.addProperty("enableVanillaRecipe", true);
        json.addProperty("enablePocketWorldgen", true);
        json.addProperty("enableStructureWorldgen", true);
        json.addProperty("enableStructureEvents", true);
        json.addProperty("strictConfigEvents", true);
        json.addProperty("breakCreativeDefaultLuckyBlock", false);
        json.addProperty("breakCreativeThemedLuckyBlocks", false);
        json.addProperty("breakCreativeLegendaryLuckyBlock", false);
        return json;
    }

    private static boolean getLegacyBreakCreativeFromDefaultPool(boolean fallback) {
        return getLegacyBreakCreative(LuckyBlockConfigManager.legacyDefaultPoolFile(), fallback);
    }

    private static boolean getLegacyBreakCreativeFromLegendaryPool(boolean fallback) {
        return getLegacyBreakCreative(LuckyBlockConfigManager.legacyLegendaryPoolFile(), fallback);
    }

    private static boolean getLegacyBreakCreativeFromThemedConfig(boolean fallback) {
        return getLegacyBreakCreative(LuckyBlockConfigManager.legacyThemedTypeLevelConfigFile(), fallback);
    }

    private static boolean getLegacyBreakCreative(Path file, boolean fallback) {
        if (file == null || !Files.exists(file)) {
            return fallback;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return getBoolean(json, "breakCreative", fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
