package net.crulim.luckblockcobblemon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LuckyBlockConfigManager {
    public static final int CONFIG_VERSION = 2;

    private static final Logger LOGGER = Logger.getLogger(LuckyBlockConfigManager.class.getName());
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private LuckyBlockConfigManager() {
    }

    public static Path rootDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("luckyblockcobblemon");
    }

    public static Path poolsDir() {
        return rootDir().resolve("pools");
    }

    public static Path themedPoolsDir() {
        return poolsDir().resolve("themed");
    }

    public static Path settingsFile() {
        return rootDir().resolve("settings.json");
    }

    public static Path tiersFile() {
        return rootDir().resolve("tiers.json");
    }

    public static Path defaultPoolFile() {
        return poolsDir().resolve("default.json");
    }

    public static Path legendaryPoolFile() {
        return poolsDir().resolve("legendary.json");
    }

    public static Path themedPoolFile(String typeOrBlockKey) {
        return themedPoolsDir().resolve(toThemedType(typeOrBlockKey) + ".json");
    }

    public static Path legacySettingsFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("luckyblock_settings.json");
    }

    public static Path legacyTiersFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("luckyblock_locked_tiers.json");
    }

    public static Path legacyDefaultPoolFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("luckyblock_config.json");
    }

    public static Path legacyLegendaryPoolFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("luckyblock_legendary.json");
    }

    public static Path legacyThemedPoolFile(String typeOrBlockKey) {
        return FabricLoader.getInstance().getConfigDir().resolve("luckblockpocket").resolve(toLegacyThemedPoolFileName(typeOrBlockKey));
    }

    public static Path legacyThemedLevelConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("luckblockpocket").resolve("level_config.json");
    }

    public static Path legacyThemedTypeLevelConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("luckblockpocket").resolve("lvlconfig_types.json");
    }

    public static void ensureDirectories() {
        try {
            Files.createDirectories(rootDir());
            Files.createDirectories(poolsDir());
            Files.createDirectories(themedPoolsDir());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[LuckyBlockConfig] Failed to create config directories.", e);
        }
    }

    public static JsonObject loadObject(Path file, Supplier<JsonObject> defaultSupplier) {
        ensureDirectories();

        if (!Files.exists(file)) {
            JsonObject defaults = defaultSupplier.get();
            writeJson(file, defaults);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[LuckyBlockConfig] Failed to load " + file + ". Using an empty config for safety.", e);
        }

        return new JsonObject();
    }

    public static JsonObject loadObjectWithLegacyMigration(Path newFile, Path legacyFile, Supplier<JsonObject> defaultSupplier) {
        ensureDirectories();

        if (legacyFile != null && Files.exists(legacyFile) && shouldMigrateLegacy(newFile, defaultSupplier)) {
            backupGeneratedDefaultBeforeMigration(newFile);
            JsonObject migrated = migrateLegacyToObject(legacyFile, defaultSupplier);
            writeJson(newFile, migrated);
            LOGGER.info("[LuckyBlockConfig] Migrated legacy config " + legacyFile + " -> " + newFile);
            return migrated;
        }

        warnIfLegacyWasNotMigrated(newFile, legacyFile);
        return loadObject(newFile, defaultSupplier);
    }

    public static JsonObject loadPoolObjectWithLegacyMigration(Path newFile, Path legacyFile, Supplier<JsonObject> defaultSupplier) {
        ensureDirectories();

        if (legacyFile != null && Files.exists(legacyFile) && shouldMigrateLegacy(newFile, defaultSupplier)) {
            backupGeneratedDefaultBeforeMigration(newFile);
            JsonObject migrated = migrateLegacyPoolToObject(legacyFile, defaultSupplier);
            writeJson(newFile, migrated);
            LOGGER.info("[LuckyBlockConfig] Migrated legacy pool " + legacyFile + " -> " + newFile);
            return migrated;
        }

        warnIfLegacyWasNotMigrated(newFile, legacyFile);
        return loadObject(newFile, defaultSupplier);
    }

    public static boolean shouldMigrateLegacy(Path newFile, Supplier<JsonObject> defaultSupplier) {
        if (newFile == null) {
            return false;
        }

        if (!Files.exists(newFile)) {
            return true;
        }

        try (Reader reader = Files.newBufferedReader(newFile, StandardCharsets.UTF_8)) {
            JsonElement existingElement = JsonParser.parseReader(reader);
            if (existingElement == null || !existingElement.isJsonObject()) {
                return true;
            }

            JsonObject existing = existingElement.getAsJsonObject();
            if (existing.has("_legacySource")) {
                return false;
            }

            JsonObject defaults = defaultSupplier.get();
            if (defaults == null) {
                return false;
            }

            return existing.equals(defaults);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[LuckyBlockConfig] Could not compare generated config with defaults: " + newFile, e);
            return false;
        }
    }

    private static void backupGeneratedDefaultBeforeMigration(Path newFile) {
        if (newFile == null || !Files.exists(newFile)) {
            return;
        }

        try {
            Path backup = newFile.resolveSibling(newFile.getFileName().toString() + ".generated_default_backup");
            if (!Files.exists(backup)) {
                Files.copy(newFile, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[LuckyBlockConfig] Failed to back up generated default config before legacy migration: " + newFile, e);
        }
    }

    private static void warnIfLegacyWasNotMigrated(Path newFile, Path legacyFile) {
        if (legacyFile == null || !Files.exists(legacyFile) || newFile == null || !Files.exists(newFile)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(newFile, StandardCharsets.UTF_8)) {
            JsonElement existingElement = JsonParser.parseReader(reader);
            if (existingElement != null && existingElement.isJsonObject()) {
                JsonObject existing = existingElement.getAsJsonObject();
                if (existing.has("_legacySource")) {
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        LOGGER.warning("[LuckyBlockConfig] Legacy config exists but was not auto-migrated because the new config already exists and appears customized: " + legacyFile + " -> " + newFile);
    }

    public static JsonArray getEvents(JsonObject root) {
        if (root == null) {
            return new JsonArray();
        }
        if (root.has("events") && root.get("events").isJsonArray()) {
            return root.getAsJsonArray("events");
        }
        if (root.has("luckPool") && root.get("luckPool").isJsonArray()) {
            return root.getAsJsonArray("luckPool");
        }
        if (root.has("pool") && root.get("pool").isJsonArray()) {
            return root.getAsJsonArray("pool");
        }
        return new JsonArray();
    }

    public static JsonObject loadBundledObject(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }

        try (InputStream stream = LuckyBlockConfigManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LOGGER.warning("[LuckyBlockConfig] Bundled default config not found: " + resourcePath);
                return null;
            }

            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (element != null && element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[LuckyBlockConfig] Failed to load bundled default config: " + resourcePath, e);
        }

        return null;
    }

    public static void writeJson(Path file, JsonElement json) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[LuckyBlockConfig] Failed to write " + file, e);
        }
    }

    public static String toThemedType(String key) {
        if (key == null || key.isBlank()) {
            return "default";
        }

        String normalized = key.toLowerCase(Locale.ROOT).trim();
        if (normalized.startsWith("luck_block_pocket_")) {
            normalized = normalized.substring("luck_block_pocket_".length());
        }
        if (normalized.endsWith(".json")) {
            normalized = normalized.substring(0, normalized.length() - ".json".length());
        }

        return switch (normalized) {
            case "electric" -> "eletric";
            case "grass_bug" -> "grass";
            case "water_ice" -> "water";
            case "ground_fighting" -> "ground";
            default -> normalized;
        };
    }

    public static String toLegacyThemedPoolKey(String type) {
        String normalized = toThemedType(type);
        return "luck_block_pocket_" + normalized;
    }

    private static String toLegacyThemedPoolFileName(String type) {
        return toLegacyThemedPoolKey(type) + ".json";
    }

    public static JsonArray toObjectArray(List<JsonObject> events) {
        JsonArray array = new JsonArray();
        if (events != null) {
            for (JsonObject event : events) {
                if (event != null) {
                    array.add(event);
                }
            }
        }
        return array;
    }

    private static JsonObject migrateLegacyToObject(Path legacyFile, Supplier<JsonObject> defaultSupplier) {
        try (Reader reader = Files.newBufferedReader(legacyFile, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                object.addProperty("configVersion", CONFIG_VERSION);
                object.addProperty("_legacySource", legacyFile.toString());
                return object;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[LuckyBlockConfig] Failed to migrate legacy config " + legacyFile, e);
        }
        return defaultSupplier.get();
    }

    private static JsonObject migrateLegacyPoolToObject(Path legacyFile, Supplier<JsonObject> defaultSupplier) {
        try (Reader reader = Files.newBufferedReader(legacyFile, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject root = new JsonObject();
            root.addProperty("configVersion", CONFIG_VERSION);
            root.addProperty("_legacySource", legacyFile.toString());

            if (element != null && element.isJsonArray()) {
                root.addProperty("_comment", "Migrated legacy Lucky Block pool. Edit the events array.");
                root.add("events", element.getAsJsonArray());
                return root;
            }

            if (element != null && element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                object.addProperty("configVersion", CONFIG_VERSION);
                object.addProperty("_legacySource", legacyFile.toString());
                if (!object.has("events") && object.has("luckPool") && object.get("luckPool").isJsonArray()) {
                    object.add("events", object.getAsJsonArray("luckPool"));
                }
                return object;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[LuckyBlockConfig] Failed to migrate legacy pool " + legacyFile, e);
        }
        return defaultSupplier.get();
    }
}
