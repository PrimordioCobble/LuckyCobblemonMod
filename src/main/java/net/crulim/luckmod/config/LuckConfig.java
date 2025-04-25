
package net.crulim.luckmod.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LuckConfig {
    public static List<LuckEntry> luckPool;

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("luckmod/config.json");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                System.err.println("[LuckMod] Config file not found at " + path + ". Please create it manually.");
                return;
            }
            String json = Files.readString(path);
            Type type = new TypeToken<ConfigWrapper>(){}.getType();
            ConfigWrapper wrapper = new Gson().fromJson(json, type);
            luckPool = wrapper.luckPool;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static class ConfigWrapper {
        public List<LuckEntry> luckPool;
    }

    public static class LuckEntry {
        public String type;
        public String item;
        public int amount;
        public String command;
        public double chance;


        public double power;
        public List<String> effects;
        public int duration;
        public int amplifier;
    }

}
