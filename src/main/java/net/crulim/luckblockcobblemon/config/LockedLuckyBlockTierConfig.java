package net.crulim.luckblockcobblemon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LockedLuckyBlockTierConfig {
    private static final Logger LOGGER = Logger.getLogger(LockedLuckyBlockTierConfig.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<Integer, LevelRange> TIERS = new LinkedHashMap<>();

    private static final Map<String, String> TYPE_ALIASES;
    private static final Map<String, String> TYPE_TO_BASE_BLOCK_PATH;
    private static final List<String> COMMAND_SUGGESTIONS;

    static {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("default", "default");
        aliases.put("normal", "default");
        aliases.put("base", "default");
        aliases.put("pocket", "default");
        aliases.put("fire", "fire");
        aliases.put("water", "water");
        aliases.put("grass", "grass");
        aliases.put("ground", "ground");
        aliases.put("fly", "fly");
        aliases.put("steel", "steel");
        aliases.put("eletric", "eletric");
        aliases.put("fairy", "fairy");
        TYPE_ALIASES = Collections.unmodifiableMap(aliases);

        Map<String, String> basePaths = new LinkedHashMap<>();
        basePaths.put("default", "luck_block_pocket");
        basePaths.put("fire", "luck_block_pocket_fire");
        basePaths.put("water", "luck_block_pocket_water");
        basePaths.put("grass", "luck_block_pocket_grass");
        basePaths.put("ground", "luck_block_pocket_ground");
        basePaths.put("fly", "luck_block_pocket_fly");
        basePaths.put("steel", "luck_block_pocket_steel");
        basePaths.put("eletric", "luck_block_pocket_eletric");
        basePaths.put("fairy", "luck_block_pocket_fairy");
        TYPE_TO_BASE_BLOCK_PATH = Collections.unmodifiableMap(basePaths);

        COMMAND_SUGGESTIONS = Collections.unmodifiableList(new ArrayList<>(TYPE_ALIASES.keySet()));
        loadDefaultsIntoMemory();
    }

    private LockedLuckyBlockTierConfig() {
    }

    public static void load() {
        File file = getConfigFile();

        if (!file.exists()) {
            generateDefaultConfig(file);
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            Map<Integer, LevelRange> loaded = new LinkedHashMap<>();

            if (root.has("tiers") && root.get("tiers").isJsonArray()) {
                JsonArray tiers = root.getAsJsonArray("tiers");
                for (JsonElement element : tiers) {
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }

                    JsonObject tierObject = element.getAsJsonObject();
                    if (!tierObject.has("tier") || !tierObject.has("minLevel") || !tierObject.has("maxLevel")) {
                        continue;
                    }

                    int tier = tierObject.get("tier").getAsInt();
                    int minLevel = tierObject.get("minLevel").getAsInt();
                    int maxLevel = tierObject.get("maxLevel").getAsInt();

                    if (tier < 1 || tier > 10) {
                        LOGGER.warning("[LockedLuckyBlockTierConfig] Ignoring invalid tier: " + tier);
                        continue;
                    }

                    if (minLevel < 1) {
                        minLevel = 1;
                    }

                    if (maxLevel < minLevel) {
                        int temp = minLevel;
                        minLevel = maxLevel;
                        maxLevel = temp;
                    }

                    loaded.put(tier, new LevelRange(minLevel, maxLevel));
                }
            }

            TIERS.clear();
            TIERS.putAll(createDefaultTiers());
            TIERS.putAll(loaded);
            LOGGER.info("[LockedLuckyBlockTierConfig] Loaded " + TIERS.size() + " locked lucky block tiers.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[LockedLuckyBlockTierConfig] Failed to load config. Using defaults.", e);
            loadDefaultsIntoMemory();
        }
    }

    public static void reload() {
        load();
    }

    public static LevelRange getRange(int tier) {
        LevelRange range = TIERS.get(tier);
        if (range != null) {
            return range;
        }
        return createDefaultTiers().getOrDefault(tier, new LevelRange(1, 10));
    }

    public static String canonicalizeType(String type) {
        if (type == null) {
            return null;
        }
        return TYPE_ALIASES.get(type.toLowerCase(Locale.ROOT));
    }

    public static List<String> getCommandSuggestions() {
        return COMMAND_SUGGESTIONS;
    }

    public static Map<String, String> getOfficialTypesToBaseBlockPaths() {
        return TYPE_TO_BASE_BLOCK_PATH;
    }

    public static boolean isValidTier(int tier) {
        return tier >= 1 && tier <= 10;
    }

    public static String getLockedBlockPath(String type, int tier) {
        String canonicalType = canonicalizeType(type);
        if (canonicalType == null || !isValidTier(tier)) {
            return null;
        }

        if ("default".equals(canonicalType)) {
            return "luck_block_pocket_locked_t" + tier;
        }

        String basePath = TYPE_TO_BASE_BLOCK_PATH.get(canonicalType);
        if (basePath == null) {
            return null;
        }
        return basePath + "_locked_t" + tier;
    }

    public static Optional<LockedBlockInfo> resolveLockedBlock(Identifier blockId) {
        if (blockId == null) {
            return Optional.empty();
        }
        return resolveLockedBlock(blockId.getPath());
    }

    public static Optional<LockedBlockInfo> resolveLockedBlock(String blockPath) {
        if (blockPath == null) {
            return Optional.empty();
        }

        String lowerPath = blockPath.toLowerCase(Locale.ROOT);
        for (int tier = 1; tier <= 10; tier++) {
            String defaultPath = "luck_block_pocket_locked_t" + tier;
            if (defaultPath.equals(lowerPath)) {
                return Optional.of(new LockedBlockInfo("default", "luck_block_pocket", tier, getRange(tier), true));
            }

            for (Map.Entry<String, String> entry : TYPE_TO_BASE_BLOCK_PATH.entrySet()) {
                String type = entry.getKey();
                if ("default".equals(type)) {
                    continue;
                }

                String lockedPath = entry.getValue() + "_locked_t" + tier;
                if (lockedPath.equals(lowerPath)) {
                    return Optional.of(new LockedBlockInfo(type, entry.getValue(), tier, getRange(tier), false));
                }
            }
        }

        return Optional.empty();
    }

    private static File getConfigFile() {
        LuckyBlockConfigManager.ensureDirectories();
        Path newPath = LuckyBlockConfigManager.tiersFile();
        Path legacyPath = LuckyBlockConfigManager.legacyTiersFile();

        if (Files.exists(legacyPath) && LuckyBlockConfigManager.shouldMigrateLegacy(newPath, LockedLuckyBlockTierConfig::createDefaultConfigObject)) {
            try {
                JsonObject legacyJson;
                try (Reader reader = new InputStreamReader(new FileInputStream(legacyPath.toFile()), StandardCharsets.UTF_8)) {
                    legacyJson = JsonParser.parseReader(reader).getAsJsonObject();
                }
                if (!legacyJson.has("configVersion")) {
                    legacyJson.addProperty("configVersion", LuckyBlockConfigManager.CONFIG_VERSION);
                }
                legacyJson.addProperty("_legacySource", legacyPath.toString());
                LuckyBlockConfigManager.writeJson(newPath, legacyJson);
                LOGGER.info("[LockedLuckyBlockTierConfig] Migrated legacy tiers config to " + newPath);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "[LockedLuckyBlockTierConfig] Failed to migrate legacy tiers config.", e);
            }
        }

        return newPath.toFile();
    }

    private static void generateDefaultConfig(File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            JsonObject root = createDefaultConfigObject();

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[LockedLuckyBlockTierConfig] Failed to generate default config.", e);
        }
    }

    private static JsonObject createDefaultConfigObject() {
        JsonObject root = LuckyBlockConfigManager.loadBundledObject("luckyblockcobblemon/default_configs/tiers.json");
        if (root != null) {
            return root;
        }

        root = new JsonObject();
        root.addProperty("configVersion", LuckyBlockConfigManager.CONFIG_VERSION);
        root.addProperty("_comment", "Locked-tier Lucky Block level ranges. Used by /luckyblockgive <target> <type> <tier> [amount].");

        JsonArray tiers = new JsonArray();
        for (Map.Entry<Integer, LevelRange> entry : createDefaultTiers().entrySet()) {
            JsonObject tier = new JsonObject();
            tier.addProperty("tier", entry.getKey());
            tier.addProperty("minLevel", entry.getValue().minLevel());
            tier.addProperty("maxLevel", entry.getValue().maxLevel());
            tiers.add(tier);
        }
        root.add("tiers", tiers);
        return root;
    }

    private static void loadDefaultsIntoMemory() {
        TIERS.clear();
        TIERS.putAll(createDefaultTiers());
    }

    private static Map<Integer, LevelRange> createDefaultTiers() {
        Map<Integer, LevelRange> defaults = new LinkedHashMap<>();
        defaults.put(1, new LevelRange(1, 10));
        defaults.put(2, new LevelRange(11, 20));
        defaults.put(3, new LevelRange(21, 30));
        defaults.put(4, new LevelRange(31, 40));
        defaults.put(5, new LevelRange(41, 50));
        defaults.put(6, new LevelRange(51, 60));
        defaults.put(7, new LevelRange(61, 70));
        defaults.put(8, new LevelRange(71, 80));
        defaults.put(9, new LevelRange(81, 90));
        defaults.put(10, new LevelRange(91, 100));
        return defaults;
    }

    public static final class LevelRange {
        private final int minLevel;
        private final int maxLevel;

        public LevelRange(int minLevel, int maxLevel) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        public int minLevel() {
            return minLevel;
        }

        public int maxLevel() {
            return maxLevel;
        }
    }

    public static final class LockedBlockInfo {
        private final String type;
        private final String basePoolKey;
        private final int tier;
        private final LevelRange range;
        private final boolean defaultType;

        private LockedBlockInfo(String type, String basePoolKey, int tier, LevelRange range, boolean defaultType) {
            this.type = type;
            this.basePoolKey = basePoolKey;
            this.tier = tier;
            this.range = range;
            this.defaultType = defaultType;
        }

        public String type() {
            return type;
        }

        public String basePoolKey() {
            return basePoolKey;
        }

        public int tier() {
            return tier;
        }

        public LevelRange range() {
            return range;
        }

        public boolean defaultType() {
            return defaultType;
        }
    }
}
