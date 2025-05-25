package net.crulim.luckblockcobblemon.handler;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LuckyBlockHandlerVanilla {
    private static final Random random = Random.create();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_PATH = "config/luckyblock_legendary.json";

    private static final List<String> legendaryList = new ArrayList<>();
    private static float shinyChance = 2.0F;
    private static int minLevel = 50;
    private static int maxLevel = 70;
    private static final List<TimeLevelRange> timeLeveling = new ArrayList<>();
    private static boolean breakCreative = false;

    private static class TimeLevelRange {
        int minDays, maxDays, minLevel, maxLevel;

        TimeLevelRange(int minDays, int maxDays, int minLevel, int maxLevel) {
            this.minDays = minDays;
            this.maxDays = maxDays;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }
    }

    static {
        loadConfig();
    }

    public static void reloadConfig() {
        loadConfig();
        System.out.println("[LuckyBlockLegendary] Config recarregada via comando.");
    }

    public static void handleLuckEvent(ServerWorld world, BlockPos pos, int luck, Random _random) {
        if (legendaryList.isEmpty()) {
            System.out.println("[LuckyBlockLegendary] Lista de lendários vazia.");
            return;
        }

        String speciesName = legendaryList.get(random.nextInt(legendaryList.size()));
        Species species = PokemonSpecies.INSTANCE.getByName(speciesName);
        if (species == null) {
            System.out.println("[LuckyBlockLegendary] Espécie inválida: " + speciesName);
            return;
        }

        int level = getLevel(world);
        boolean isShiny = random.nextFloat() * 100F < shinyChance;

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(level);
        pokemon.setShiny(isShiny);
        pokemon.getMoveSet().clear();

        int index = 0;
        for (MoveTemplate move : pokemon.getRelearnableMoves()) {
            if (index >= 4) break;
            if (move != null) {
                pokemon.getMoveSet().setMove(index++, new Move(move, move.getPp(), 0));
            }
        }

        Vec3d spawnPos = Vec3d.ofCenter(pos).add(0, 1, 0);
        PokemonEntity entity = pokemon.sendOut(world, spawnPos, null, e -> null);

        if (entity == null) {
            System.out.println("[LuckyBlockLegendary] Falha ao spawnar: " + speciesName);
        } else {
            System.out.println("[LuckyBlockLegendary] Spawnado: " + speciesName + " lvl " + level + " shiny: " + isShiny);
        }
    }

    private static int getLevel(ServerWorld world) {
        long days = world.getTimeOfDay() / 24000L;

        for (TimeLevelRange range : timeLeveling) {
            if (days >= range.minDays && days <= range.maxDays) {
                return random.nextBetween(range.minLevel, range.maxLevel + 1);
            }
        }

        return random.nextBetween(minLevel, maxLevel + 1);
    }

    public static void loadConfig() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                generateDefault(file);
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)).getAsJsonObject();

            legendaryList.clear();
            for (JsonElement el : json.getAsJsonArray("legendaryPool")) {
                legendaryList.add(el.getAsString());
            }

            minLevel = json.has("minLevel") ? json.get("minLevel").getAsInt() : 50;
            maxLevel = json.has("maxLevel") ? json.get("maxLevel").getAsInt() : 70;
            shinyChance = json.has("shinyChance") ? json.get("shinyChance").getAsFloat() : 2.0F;

            breakCreative = json.has("breakCreative") && json.get("breakCreative").getAsBoolean();

            timeLeveling.clear();
            if (json.has("timeLeveling")) {
                for (JsonElement element : json.getAsJsonArray("timeLeveling")) {
                    JsonObject obj = element.getAsJsonObject();
                    timeLeveling.add(new TimeLevelRange(
                            obj.get("minDays").getAsInt(),
                            obj.get("maxDays").getAsInt(),
                            obj.get("minLevel").getAsInt(),
                            obj.get("maxLevel").getAsInt()
                    ));
                }
            }

            System.out.println("[LuckyBlockLegendary] Config carregado com sucesso.");

        } catch (Exception e) {
            System.out.println("[LuckyBlockLegendary] Erro ao carregar config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isBreakCreativeAllowed() {
        return breakCreative;
    }

    private static void generateDefault(File file) {
        try {
            JsonObject root = new JsonObject();
            JsonArray pool = new JsonArray();
            root.addProperty("breakCreative", false);
            JsonArray note = new JsonArray();
            note.add("INFO: The breakCreative property controls whether Lucky Blocks can be broken in creative mode.");
            note.add("If set to true, players in creative mode will be able to break and activate Lucky Blocks.");
            note.add("If set to false, Lucky Blocks cannot be activated in creative mode.");
            root.add("_note", note);

            // Lista completa de lendários (nomes juntos, padrão Cobblemon)
            String[] legendaryNames = {
                    "articuno", "zapdos", "moltres", "mewtwo", "mew",
                    "raikou", "entei", "suicune", "lugia", "hooh", "celebi",
                    "regirock", "regice", "registeel", "latias", "latios", "kyogre", "groudon", "rayquaza", "jirachi", "deoxys",
                    "uxie", "mesprit", "azelf", "dialga", "palkia", "heatran", "regigigas", "giratina", "cresselia", "phione", "manaphy", "darkrai", "shaymin", "arceus",
                    "victini", "cobalion", "terrakion", "virizion", "tornadus", "thundurus", "reshiram", "zekrom", "landorus", "kyurem", "keldeo", "meloetta", "genesect",
                    "xerneas", "yveltal", "zygarde", "diancie", "hoopa", "volcanion",
                    "tapukoko", "tapulele", "tapubulu", "tapufini", "cosmog", "cosmoem", "solgaleo", "lunala", "nihilego", "buzzwole", "pheromosa", "xurkitree", "celesteela", "kartana", "guzzlord", "necrozma", "magearna", "marshadow", "poipole", "naganadel", "stakataka", "blacephalon", "zeraora",
                    "zacian", "zamazenta", "eternatus", "kubfu", "urshifu", "zarude", "regieleki", "regidrago", "glastrier", "spectrier", "calyrex",
                    "enamorus", "koraidon", "miraidon", "wochian", "chienpao", "tinglu", "chiyu", "roaringmoon", "ironvaliant", "walkingwake", "ironleaves", "ogerpon", "okidogi", "munkidori", "fezandipiti", "terapagos"
            };
            for (String name : legendaryNames) {
                pool.add(name);
            }

            root.add("legendaryPool", pool);
            root.addProperty("minLevel", 60);
            root.addProperty("maxLevel", 100);
            root.addProperty("shinyChance", 0.02F);

            // Sistema de timeLeveling extenso
            JsonArray timeLvl = new JsonArray();

            JsonObject range1 = new JsonObject();
            range1.addProperty("minDays", 0);
            range1.addProperty("maxDays", 20);
            range1.addProperty("minLevel", 45);
            range1.addProperty("maxLevel", 60);
            timeLvl.add(range1);

            JsonObject range2 = new JsonObject();
            range2.addProperty("minDays", 21);
            range2.addProperty("maxDays", 50);
            range2.addProperty("minLevel", 61);
            range2.addProperty("maxLevel", 80);
            timeLvl.add(range2);

            JsonObject range3 = new JsonObject();
            range3.addProperty("minDays", 51);
            range3.addProperty("maxDays", 99999);
            range3.addProperty("minLevel", 81);
            range3.addProperty("maxLevel", 100);
            timeLvl.add(range3);



            root.add("timeLeveling", timeLvl);



            File dir = new File("./config");
            if (!dir.exists()) dir.mkdirs();

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            System.out.println("[LuckyBlockLegendary] Config padrão criado.");
        } catch (Exception e) {
            System.out.println("[LuckyBlockLegendary] Falha ao criar config padrão.");
            e.printStackTrace();
        }
    }
}
