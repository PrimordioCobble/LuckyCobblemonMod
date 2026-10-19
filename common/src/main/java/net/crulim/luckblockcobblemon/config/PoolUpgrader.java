package net.crulim.luckblockcobblemon.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adds the content of a new mod version to pool files that already exist on disk, without touching
 * anything the server owner customized: only bundled events carrying an "upgradeId" that is missing
 * from the user's pool are appended, plus a few explicit additions.
 * A backup of the original file is written before the first upgrade.
 * Can be disabled with "autoUpgradePools": false in settings.json.
 */
public final class PoolUpgrader {
    public static final String CURRENT_UPGRADE = "0.2.0";
    private static final String APPLIED_KEY = "appliedUpgrades";
    private static final Logger LOGGER = Logger.getLogger(PoolUpgrader.class.getName());

    /**
     * Cobblemon 1.8 items appended to existing "cobblemon_allitems" events of the default pool.
     */
    public static final List<String> DEFAULT_ALLITEMS_ADDITIONS = List.of(
            "cobblemon:blank_tm",
            "cobblemon:tm_machine",
            "cobblemon:deepslate_crystal_core",
            "cobblemon:normal_gem_cluster", "cobblemon:fire_gem_cluster", "cobblemon:water_gem_cluster",
            "cobblemon:electric_gem_cluster", "cobblemon:grass_gem_cluster", "cobblemon:ice_gem_cluster",
            "cobblemon:fighting_gem_cluster", "cobblemon:poison_gem_cluster", "cobblemon:ground_gem_cluster",
            "cobblemon:flying_gem_cluster", "cobblemon:psychic_gem_cluster", "cobblemon:bug_gem_cluster",
            "cobblemon:rock_gem_cluster", "cobblemon:ghost_gem_cluster", "cobblemon:dragon_gem_cluster",
            "cobblemon:dark_gem_cluster", "cobblemon:steel_gem_cluster", "cobblemon:fairy_gem_cluster",
            "cobblemon:voltaconia", "cobblemon:verdroot", "cobblemon:mistleaf", "cobblemon:lucibell",
            "cobblemon:pomohana", "cobblemon:pauvillea", "cobblemon:lobaila", "cobblemon:wistensu",
            "cobblemon:disc_shelf",
            "cobblemon:white_plaque", "cobblemon:light_gray_plaque", "cobblemon:gray_plaque", "cobblemon:black_plaque",
            "cobblemon:brown_plaque", "cobblemon:red_plaque", "cobblemon:orange_plaque", "cobblemon:yellow_plaque",
            "cobblemon:lime_plaque", "cobblemon:green_plaque", "cobblemon:cyan_plaque", "cobblemon:light_blue_plaque",
            "cobblemon:blue_plaque", "cobblemon:purple_plaque", "cobblemon:magenta_plaque", "cobblemon:pink_plaque"
    );

    private PoolUpgrader() {
    }

    /**
     * @param file             pool file on disk (the user's config)
     * @param userRoot         loaded content of that file; modified in place
     * @param bundledResource  bundled default config path inside the jar
     * @param allItemsAdditions item ids merged into existing "cobblemon_allitems" events
     * @param newTopLevelKeys  top-level keys copied from the bundled config when missing
     */
    public static void upgrade(Path file, JsonObject userRoot, String bundledResource, List<String> allItemsAdditions, List<String> newTopLevelKeys) {
        if (userRoot == null || file == null || !LuckyBlockSettingsConfig.isAutoUpgradePoolsEnabled()) {
            return;
        }
        if (hasUpgrade(userRoot, CURRENT_UPGRADE)) {
            return;
        }

        JsonObject bundled = LuckyBlockConfigManager.loadBundledObject(bundledResource);
        if (bundled == null) {
            return;
        }

        int addedEvents = 0;
        int addedItems = 0;
        int addedKeys = 0;

        JsonArray userEvents = findEventsArray(userRoot);
        if (userEvents != null) {
            Set<String> existingIds = new HashSet<>();
            for (JsonElement element : userEvents) {
                if (element != null && element.isJsonObject() && element.getAsJsonObject().has("upgradeId")) {
                    existingIds.add(element.getAsJsonObject().get("upgradeId").getAsString());
                }
            }

            for (JsonElement element : LuckyBlockConfigManager.getEvents(bundled)) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject event = element.getAsJsonObject();
                if (!event.has("upgradeId")) {
                    continue;
                }
                String id = event.get("upgradeId").getAsString();
                if (existingIds.add(id)) {
                    userEvents.add(event.deepCopy());
                    addedEvents++;
                }
            }

            if (allItemsAdditions != null && !allItemsAdditions.isEmpty()) {
                for (JsonElement element : userEvents) {
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }
                    JsonObject event = element.getAsJsonObject();
                    if (!event.has("type") || !"cobblemon_allitems".equals(event.get("type").getAsString())
                            || !event.has("items") || !event.get("items").isJsonArray()) {
                        continue;
                    }
                    JsonArray items = event.getAsJsonArray("items");
                    Set<String> present = new HashSet<>();
                    for (JsonElement item : items) {
                        if (item != null && item.isJsonPrimitive()) {
                            present.add(item.getAsString());
                        }
                    }
                    for (String id : allItemsAdditions) {
                        if (present.add(id)) {
                            items.add(id);
                            addedItems++;
                        }
                    }
                }
            }
        }

        if (newTopLevelKeys != null) {
            for (String key : newTopLevelKeys) {
                if (!userRoot.has(key) && bundled.has(key)) {
                    userRoot.add(key, bundled.get(key).deepCopy());
                    addedKeys++;
                }
            }
        }

        backupOnce(file);
        markUpgrade(userRoot, CURRENT_UPGRADE);
        LuckyBlockConfigManager.writeJson(file, userRoot);
        LOGGER.info("[LuckyBlockConfig] Upgraded " + file.getFileName() + " to " + CURRENT_UPGRADE
                + " (events added: " + addedEvents + ", items added: " + addedItems + ", keys added: " + addedKeys + ").");
    }

    private static JsonArray findEventsArray(JsonObject root) {
        if (root.has("events") && root.get("events").isJsonArray()) {
            return root.getAsJsonArray("events");
        }
        if (root.has("luckPool") && root.get("luckPool").isJsonArray()) {
            return root.getAsJsonArray("luckPool");
        }
        if (root.has("pool") && root.get("pool").isJsonArray()) {
            return root.getAsJsonArray("pool");
        }
        return null;
    }

    private static boolean hasUpgrade(JsonObject root, String upgrade) {
        if (!root.has(APPLIED_KEY) || !root.get(APPLIED_KEY).isJsonArray()) {
            return false;
        }
        for (JsonElement element : root.getAsJsonArray(APPLIED_KEY)) {
            if (element != null && element.isJsonPrimitive() && upgrade.equals(element.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static void markUpgrade(JsonObject root, String upgrade) {
        JsonArray applied = root.has(APPLIED_KEY) && root.get(APPLIED_KEY).isJsonArray()
                ? root.getAsJsonArray(APPLIED_KEY)
                : new JsonArray();
        applied.add(upgrade);
        root.add(APPLIED_KEY, applied);
    }

    private static void backupOnce(Path file) {
        try {
            if (!Files.exists(file)) {
                return;
            }
            Path backup = file.resolveSibling(file.getFileName().toString() + ".pre-" + CURRENT_UPGRADE + ".bak");
            if (!Files.exists(backup)) {
                Files.copy(file, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[LuckyBlockConfig] Could not back up " + file + " before upgrading.", e);
        }
    }
}
