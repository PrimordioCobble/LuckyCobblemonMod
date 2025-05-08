package net.crulim.luckblockcobblemon.config;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CobbleLuckConfig {
    public static Group groupOne;
    public static Group groupTwo;

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("luckmod/config/luck_cobble_config.json");
        try {
            if (!Files.exists(path)) {
                System.err.println("[CobbleLuckBlock] Config file not found: " + path);
                return;
            }
            String json = Files.readString(path);
            ConfigWrapper wrapper = new Gson().fromJson(json, ConfigWrapper.class);
            groupOne = wrapper.groupOne;
            groupTwo = wrapper.groupTwo;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ConfigWrapper {
        public Group groupOne;
        public Group groupTwo;
    }

    public static class Group {
        public List<String> speciesList;
        public int minLevel;
        public int maxLevel;
        public double shinyChance;
        public double chance;
    }
}
