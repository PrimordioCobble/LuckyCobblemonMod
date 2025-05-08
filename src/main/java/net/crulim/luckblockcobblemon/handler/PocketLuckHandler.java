package net.crulim.luckblockcobblemon.handler;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.entity.ItemEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PocketLuckHandler {

    public static void reloadConfig() {
        POOLS.clear();
        System.out.println("[PocketLuckHandler] Pools recarregadas com sucesso.");
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, List<JsonObject>> POOLS = new HashMap<>();
    private static final Random random = Random.create();
    private static final float shinyChancePercent = 5.0f;
    private static final int minLevel = 5;
    private static final int maxLevel = 15;

    public static void loadConfig() {
        File configDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "config/luckblockpocket");

        String[] expectedFiles = {
                "luck_block_pocket_fire",
                "luck_block_pocket_water",
                "luck_block_pocket_grass",
                "luck_block_pocket_ground",
                "luck_block_pocket_fly",
                "luck_block_pocket_fairy",
                "luck_block_pocket_eletric",
                "luck_block_pocket_steel"
        };

        for (String file : expectedFiles) {
            POOLS.computeIfAbsent(file, PocketLuckHandler::loadOrCreate);
        }

        System.out.println("[PocketLuckHandler] Configuração inicial carregada.");
    }



    public static void trigger(ServerWorld world, BlockPos pos, Identifier blockId) {
        String key = blockId.getPath();
        List<JsonObject> pool = POOLS.computeIfAbsent(key, PocketLuckHandler::loadOrCreate);

        if (pool.isEmpty()) return;

        JsonObject chosen = pick(pool);
        if (chosen == null) return;

        String type = chosen.get("type").getAsString();
        switch (type) {
            case "item" -> dropItems(world, pos, chosen);
            case "pokemon" -> spawnPokemon(world, pos, chosen, false);
            default -> System.out.println("[PocketLuckHandler] Tipo desconhecido: " + type);
        }
    }

    private static List<JsonObject> loadOrCreate(String key) {
        File configDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "config/luckblockpocket");
        File file = new File(configDir, key + ".json");
        if (!file.exists()) createDefault(file, key);

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            List<JsonObject> pool = new ArrayList<>();
            for (JsonElement e : array) {
                if (e.isJsonObject()) pool.add(e.getAsJsonObject());
            }
            return pool;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static void createDefault(File file, String key) {
        try {
            file.getParentFile().mkdirs();
            JsonArray array = new JsonArray();

            Map<String, List<String>> pokemonMap = Map.of(
                    "water_ice", List.of("squirtle", "wartortle", "blastoise",
                            "psyduck", "golduck",
                            "poliwag", "poliwhirl", "poliwrath",
                            "tentacool", "tentacruel",
                            "slowpoke", "slowbro",
                            "seel", "dewgong",
                            "shellder", "cloyster",
                            "krabby", "kingler",
                            "horsea", "seadra", "kingdra",
                            "goldeen", "seaking",
                            "staryu", "starmie",
                            "magikarp", "gyarados",
                            "lapras",
                            "vaporeon",
                            "totodile", "croconaw", "feraligatr",
                            "chinchou", "lanturn",
                            "marill", "azumarill",
                            "wooper", "quagsire",
                            "corsola",
                            "remoraid", "octillery",
                            "mantine", "mantyke",
                            "mudkip", "marshtomp", "swampert",
                            "lotad", "lombre", "ludicolo",
                            "wingull", "pelipper",
                            "carvanha", "sharpedo",
                            "wailmer", "wailord",
                            "barboach", "whiscash",
                            "corphish", "crawdaunt",
                            "feebas", "milotic",
                            "spheal", "sealeo", "walrein",
                            "clamperl", "huntail", "gorebyss",
                            "relicanth",
                            "luvdisc",
                            "piplup", "prinplup", "empoleon",
                            "buizel", "floatzel",
                            "shellos", "gastrodon",
                            "finneon", "lumineon",
                            "palkia",
                            "phione", "manaphy",
                            "oshawott", "dewott", "samurott",
                            "panpour", "simipour",
                            "tympole", "palpitoad", "seismitoad",
                            "basculin",
                            "tirtouga", "carracosta",
                            "ducklett", "swanna",
                            "frillish", "jellicent",
                            "alomomola",
                            "keldeo",
                            "froakie", "frogadier", "greninja",
                            "clauncher", "clawitzer",
                            "popplio", "brionne", "primarina",
                            "wishiwashi",
                            "dewpider", "araquanid",
                            "bruxish",
                            "sobble", "drizzile", "inteleon",
                            "arrokuda", "barraskewda",
                            "cramorant",
                            "chewtle", "drednaw",
                            "cufant", "copperajah",
                            "arctovish", "dracovish",
                            "eiscue",
                            "kubfu", "urshifu",
                            "basculegion",
                            "quaxly", "quaxwell", "quaquaval", "dewgong", "cloyster", "jynx", "lapras", "articuno",
                            "sneasel", "swinub", "piloswine", "delibird", "smoochum",
                            "snorunt", "glalie", "spheal", "sealeo", "walrein",
                            "regice", "glaceon", "mamoswine", "froslass", "vanillite",
                            "vanillish", "vanilluxe", "cubchoo", "beartic", "cryogonal",
                            "kyurem", "amaura", "aurorus", "bergmite", "avalugg",
                            "crabominable", "eiscue", "arctozolt", "arctovish", "mr_rime",
                            "glastrier", "cetoddle", "cetitan", "chien_pao"),
                    "fire", List.of("charmander", "charmeleon", "charizard",
                            "vulpix", "ninetales",
                            "growlithe", "arcanine",
                            "ponyta", "rapidash",
                            "magmar", "flareon",
                            "cyndaquil", "quilava", "typhlosion",
                            "slugma", "magcargo",
                            "houndour", "houndoom",
                            "magby", "entei",
                            "torchic", "combusken", "blaziken",
                            "numel", "camerupt",
                            "torkoal", "solrock",
                            "chimchar", "monferno", "infernape",
                            "magmortar",
                            "rotom_heat",
                            "victini",
                            "tepig", "pignite", "emboar",
                            "pansear", "simisear",
                            "darumaka", "darmanitan",
                            "litwick", "lampent", "chandelure",
                            "heatmor",
                            "larvesta", "volcarona",
                            "fennekin", "braixen", "delphox",
                            "litleo", "pyroar",
                            "volcanion",
                            "litten", "torracat", "incineroar",
                            "oricorio_baile",
                            "salandit", "salazzle",
                            "turtonator",
                            "scorbunny", "raboot", "cinderace",
                            "carkol", "coalossal",
                            "centiskorch", "sizzlipede",
                            "charcadet", "armarouge", "ceruledge",
                            "fuecoco", "crocalor", "skeledirge",
                            "chi_yu"),
                    "grass_bug", List.of("bulbasaur", "ivysaur", "venusaur", "oddish", "gloom", "vileplume", "paras", "parasect",
                            "bellsprout", "weepinbell", "victreebel", "exeggcute", "exeggutor", "tangela", "chikorita",
                            "bayleef", "meganium", "bellossom", "hoppip", "skiploom", "jumpluff", "sunkern", "sunflora",
                            "celebi", "treecko", "grovyle", "sceptile", "lotad", "lombre", "ludicolo", "seedot", "nuzleaf",
                            "shiftry", "shroomish", "breloom", "roselia", "cacnea", "cacturne", "lileep", "cradily",
                            "tropius", "turtwig", "grotle", "torterra", "budew", "roserade", "snover", "abomasnow",
                            "leafeon", "shaymin", "snivy", "servine", "serperior", "pansage", "simisage", "sewaddle",
                            "swadloon", "leavanny", "petilil", "lilligant", "maractus", "foongus", "amoonguss", "chespin",
                            "quilladin", "chesnaught", "skiddo", "gogoat", "phantump", "trevenant", "pumpkaboo",
                            "gourgeist", "rowlet", "dartrix", "decidueye", "bounsweet", "steenee", "tsareena", "morelull",
                            "shiinotic", "dhelmise", "grookey", "thwackey", "rillaboom", "applin", "flapple", "appletun",
                            "zarude", "sprigatito", "floragato", "meowscarada", "toedscool", "toedscruel", "bramblin",
                            "brambleghast", "wo_chien", "caterpie", "metapod", "butterfree", "weedle", "kakuna", "beedrill",
                            "venonat", "venomoth", "scyther", "pinsir", "ledyba", "ledian", "spinarak", "ariados", "yanma",
                            "pineco", "forretress", "heracross", "wurmple", "silcoon", "beautifly", "cascoon", "dustox",
                            "surskit", "masquerain", "nincada", "ninjask", "shedinja", "volbeat", "illumise", "anorith",
                            "armaldo", "kricketot", "kricketune", "burmy", "wormadam", "mothim", "combee", "vespiquen",
                            "skorupi", "yanmega", "venipede", "whirlipede", "scolipede", "dwebble", "crustle", "karrablast",
                            "escavalier", "joltik", "galvantula", "shelmet", "accelgor", "durant", "larvesta", "volcarona",
                            "scatterbug", "spewpa", "vivillon", "grubbin", "charjabug", "vikavolt", "cutiefly", "ribombee",
                            "blipbug", "dottler", "orbeetle", "snom", "frosmoth", "lokix", "nymble", "tarountula", "spidops"),
                    "ground_fighting", List.of("sandshrew", "sandslash", "diglett", "dugtrio", "geodude", "graveler", "golem", "onix",
                            "cubone", "marowak", "rhyhorn", "rhydon", "wooper", "quagsire", "gligar", "swinub",
                            "piloswine", "phanpy", "donphan", "larvitar", "pupitar", "flygon", "barboach", "whiscash",
                            "baltoy", "claydol", "gible", "gabite", "garchomp", "hippopotas", "hippowdon", "mamoswine",
                            "gliscor", "rhyperior", "gastrodon", "golett", "golurk", "landorus", "sandile", "krokorok",
                            "krookodile", "stunfisk", "palpitoad", "seismitoad", "excadrill", "drilbur", "mudbray", "mudsdale",
                            "silicobra", "sandaconda", "runerigus", "ursaluna", "toedscool", "toedscruel", "great_tusk",
                            "iron_treads", "ting_lu", "clodsire",
                            "mankey", "primeape", "machop", "machoke", "machamp", "hitmonlee", "hitmonchan", "poliwrath",
                            "heracross", "tyrogue", "hitmontop", "combusken", "blaziken", "breloom", "makuhita", "hariyama",
                            "meditite", "medicham", "monferno", "infernape", "riolu", "lucario", "gallade", "pignite",
                            "emboar", "throh", "sawk", "scraggy", "scrafty", "mienshao", "cobalion", "terrakion", "virizion",
                            "keldeo", "chesnaught", "hawlucha", "crabrawler", "crabominable", "passimian", "bewear",
                            "sirfetchd", "zamazenta", "kubfu", "urshifu", "zamazenta_crowned", "iron_hands", "iron_valiant",
                            "annihilape", "quaquaval", "koraidon"),
                    "fly_dragon", List.of("pidgey", "pidgeotto", "pidgeot", "spearow", "fearow", "zubat", "golbat", "farfetchd", "doduo", "dodrio",
                            "scyther", "gyarados", "aerodactyl", "articuno", "zapdos", "moltres", "dragonite", "hoothoot", "noctowl",
                            "crobat", "natu", "xatu", "murkrow", "gligar", "delibird", "skarmory", "lugia", "ho_oh", "tropius", "salamence",
                            "altaria", "rayquaza", "staraptor", "drifblim", "honchkrow", "togekiss", "yanmega", "gliscor", "braviary",
                            "mandibuzz", "noivern", "yveltal", "corviknight", "talonflame", "rookidee", "corvisquire", "noibat", "flapple",
                            "dragapult", "kilowattrel", "bombirdier", "iron_jugulis", "roaring_moon"),
                    "fairy_ghost_poison", List.of("clefairy", "clefable", "jigglypuff", "wigglytuff", "mr_mime", "snubbull", "granbull", "ralts",
                            "kirlia", "gardevoir", "azurill", "mawile", "aromatisse", "slurpuff", "florges", "sylveon", "mimikyu",
                            "tapukoko", "tapulele", "tapu_bulu", "tapu_fini", "diancie", "primarina", "hatterene", "grimmsnarl", "flutter_mane",
                            "gastly", "haunter", "gengar", "misdreavus", "mismagius", "shedinja", "sableye", "dusclops", "dusknoir", "froslass",
                            "yamask", "cofagrigus", "golurk", "phantump", "trevenant", "sandygast", "palossand", "dhelmise", "decidueye",
                            "runerigus", "spectrier", "basculegion", "anihilape", "greavard", "houndstone",
                            "ekans", "arbok", "nidoran_f", "nidorina", "nidoqueen", "nidoran_m", "nidorino", "nidoking",
                            "zubat", "golbat", "venonat", "venomoth", "grimer", "muk", "koffing", "weezing", "gastly", "haunter", "gengar",
                            "crobat", "qwilfish", "dustox", "roselia", "skuntank", "toxicroak", "trubbish", "garbodor", "nihilego",
                            "toxapex", "salandit", "salazzle", "poison_valiant", "glimmora"),
                    "eletric_dark", List.of("pikachu", "raichu", "magnemite", "magneton", "voltorb", "electrode", "electabuzz", "jolteon", "zapdos",
                            "pichu", "mareep", "flaaffy", "ampharos", "elekid", "raikou", "plusle", "minun", "electrike", "manectric",
                            "shinx", "luxio", "luxray", "rotom", "zekrom", "stunfisk", "helioptile", "heliolisk", "xurkitree", "tapu_koko",
                            "yamper", "boltund", "toxtricity", "pincurchin", "morpeko", "bellibolt", "ironhands", "raging_bolt",
                            "umbreon", "murkrow", "houndour", "houndoom", "tyranitar", "sableye", "carvanha", "sharpedo", "cacturne",
                            "absol", "crawdaunt", "honchkrow", "spiritomb", "drapion", "weavile", "darkrai", "bisharp", "pawniard",
                            "zorua", "zoroark", "yveltal", "inkay", "malamar", "greninja", "hoopa", "incineroar", "grimmsnarl",
                            "obstagoon", "morpeko", "urshifu", "chien_pao", "kingambit"),
                    "steel_rock", List.of("magnemite", "magneton", "forretress", "steelix", "scizor", "skarmory", "mawile", "aron", "lairon", "aggron",
                            "beldum", "metang", "metagross", "registeel", "empoleon", "shieldon", "bastiodon", "bronzor", "bronzong",
                            "lucario", "magnezone", "probopass", "excadrill", "ferroseed", "ferrothorn", "durant", "cobalion", "honedge",
                            "doublade", "aegislash", "klefki", "togedemaru", "solgaleo", "celesteela", "melmetal", "cufant", "copperajah",
                            "perrserker", "zacian", "zamazenta", "irontreads", "kingambit", "orthworm", "garganacl",
                            "geodude", "graveler", "golem", "onix", "kabuto", "kabutops", "omanyte", "omastar",
                            "aerodactyl", "shuckle", "corsola", "larvitar", "pupitar", "tyranitar", "nosepass", "lunatone",
                            "solrock", "relicanth", "regirock", "cranidos", "rampardos", "shieldon", "bastiodon",
                            "roggenrola", "boldore", "gigalith", "dwebble", "crustle", "binacle", "barbaracle",
                            "carbink", "lycanroc", "stakataka", "diancie", "nacli", "naclstack", "garganacl")
            );

            Map<String, List<String>> itemMap = Map.of(
                    "water_ice", List.of("cobblemon:mystic_water",
                            "cobblemon:water_stone",
                            "cobblemon:icy_rock",
                            "cobblemon:never_melt_ice",
                            "cobblemon:water_gem","cobblemon:ice_gem"),
                    "fire", List.of("cobblemon:fire_stone",
                            "cobblemon:heat_rock",
                            "cobblemon:magmarizer",
                            "cobblemon:rawst_berry",
                            "cobblemon:fire_gem"),
                    "grass_bug", List.of("cobblemon:miracle_seed",
                            "cobblemon:leaf_stone",
                            "cobblemon:big_root",
                            "cobblemon:silver_powder",
                            "cobblemon:coba_berry",
                            "cobblemon:grass_gem","cobblemon:bug_gem"),
                    "ground_fighting", List.of("cobblemon:soft_sand",
                            "cobblemon:smooth_rock",
                            "cobblemon:focus_band",
                            "cobblemon:black_belt",
                            "cobblemon:muscle_band",
                            "cobblemon:ground_gem","cobblemon:fighting_gem"),
                    "fly_dragon", List.of("cobblemon:sharp_beak",
                            "cobblemon:air_balloon",
                            "cobblemon:dragon_fang",
                            "cobblemon:float_stone",
                            "minecraft:ender_pearl",
                            "cobblemon:flying_gem","cobblemon:dragon_gem"),
                    "fairy_ghost_poison", List.of("cobblemon:fairy_feather",
                            "cobblemon:spell_tag",
                            "cobblemon:poison_barb",
                            "cobblemon:black_sludge","cobblemon:fairy_gem","cobblemon:ghost_gem","cobblemon:poison_gem"),
                    "eletric_dark", List.of(  "cobblemon:magnet",
                            "cobblemon:cell_battery",
                            "cobblemon:black_glasses",
                            "cobblemon:light_ball",
                            "minecraft:redstone",
                            "cobblemon:razor_claw","cobblemon:electric_gem","cobblemon:dark_gem"),
                    "steel_rock", List.of("cobblemon:metal_coat",
                            "cobblemon:iron_ball",
                            "cobblemon:hard_stone",
                            "cobblemon:smooth_rock",
                            "cobblemon:rocky_helmet",
                            "cobblemon:heavy_duty_boots",
                            "cobblemon:rock_gem","cobblemon:steel_gem")
            );

            Map<String, String> keyTypeMap = Map.ofEntries(
                    Map.entry("luck_block_pocket_water", "water_ice"),
                    Map.entry("luck_block_pocket_ice", "water_ice"),
                    Map.entry("luck_block_pocket_fire", "fire"),
                    Map.entry("luck_block_pocket_grass", "grass_bug"),
                    Map.entry("luck_block_pocket_bug", "grass_bug"),
                    Map.entry("luck_block_pocket_ground", "ground_fighting"),
                    Map.entry("luck_block_pocket_fighting", "ground_fighting"),
                    Map.entry("luck_block_pocket_fly", "fly_dragon"),
                    Map.entry("luck_block_pocket_dragon", "fly_dragon"),
                    Map.entry("luck_block_pocket_fairy", "fairy_ghost_poison"),
                    Map.entry("luck_block_pocket_ghost", "fairy_ghost_poison"),
                    Map.entry("luck_block_pocket_poison", "fairy_ghost_poison"),
                    Map.entry("luck_block_pocket_eletric", "eletric_dark"),
                    Map.entry("luck_block_pocket_dark", "eletric_dark"),
                    Map.entry("luck_block_pocket_steel", "steel_rock"),
                    Map.entry("luck_block_pocket_rock", "steel_rock")
            );

            String resolvedType = keyTypeMap.entrySet().stream()
                    .filter(e -> key.contains(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);

            if (resolvedType != null) {
                JsonObject pokemonDrop = new JsonObject();
                pokemonDrop.addProperty("type", "pokemon");
                pokemonDrop.add("pokemons", GSON.toJsonTree(pokemonMap.get(resolvedType)));
                pokemonDrop.addProperty("minLevel", 85);
                pokemonDrop.addProperty("maxLevel", 100);
                pokemonDrop.addProperty("shinyChance", 10);
                pokemonDrop.addProperty("chance", 85);

                JsonObject itemDrop = new JsonObject();
                itemDrop.addProperty("type", "item");
                itemDrop.add("items", GSON.toJsonTree(itemMap.getOrDefault(resolvedType, List.of("minecraft:stone"))));
                itemDrop.addProperty("min", 1);
                itemDrop.addProperty("max", 1);
                itemDrop.addProperty("chance", 15);

                array.add(pokemonDrop);
                array.add(itemDrop);
            }

            if (array.isEmpty()) {
                JsonObject fallback = new JsonObject();
                fallback.addProperty("type", "item");
                fallback.add("items", GSON.toJsonTree(List.of("minecraft:diamond")));
                fallback.addProperty("min", 1);
                fallback.addProperty("max", 1);
                fallback.addProperty("chance", 100);
                array.add(fallback);
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(array, writer);
            }

            System.out.println("[PocketLuckHandler] Arquivo default criado: " + key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static JsonObject pick(List<JsonObject> pool) {
    double total = 0;
    for (JsonObject obj : pool) {
        total += obj.get("chance").getAsDouble();
    }

    double roll = random.nextDouble() * total;
    double cumulative = 0;

    for (JsonObject obj : pool) {
        cumulative += obj.get("chance").getAsDouble();
        if (roll <= cumulative) return obj;
    }

    return null;
}

private static void dropItems(ServerWorld world, BlockPos pos, JsonObject data) {
    if (!data.has("items")) return;

    JsonArray array = data.getAsJsonArray("items");
    int min = data.has("min") ? data.get("min").getAsInt() : 1;
    int max = data.has("max") ? data.get("max").getAsInt() : min;
    int amount = random.nextBetween(min, max);

    for (int i = 0; i < amount; i++) {
        String itemId = array.get(random.nextInt(array.size())).getAsString();
        Identifier id = Identifier.tryParse(itemId);
        if (!Registries.ITEM.containsId(id)) {
            System.out.println("[PocketLuckHandler] Item desconhecido: " + itemId);
            continue;
        }
        ItemStack stack = new ItemStack(Registries.ITEM.get(id));
        ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, stack);
        world.spawnEntity(entity);
    }
}

private static void spawnPokemon(ServerWorld world, BlockPos pos, JsonObject data, boolean forceShiny) {
    if (!data.has("pokemons")) {
        System.out.println("[PocketLuckHandler] Nenhuma lista de pokemons encontrada.");
        return;
    }

    JsonArray pokemons = data.getAsJsonArray("pokemons");
    if (pokemons.isEmpty()) return;

    String speciesName = pokemons.get(random.nextInt(pokemons.size())).getAsString();
    speciesName = normalizeSpeciesName(speciesName);

    Species species = PokemonSpecies.INSTANCE.getByName(speciesName);
    if (species == null) {
        System.out.println("[PocketLuckHandler] Pokémon inválido: " + speciesName);
        return;
    }

    Pokemon pokemon = new Pokemon();
    pokemon.setSpecies(species);

    int level = data.has("minLevel") && data.has("maxLevel")
            ? random.nextBetween(data.get("minLevel").getAsInt(), data.get("maxLevel").getAsInt())
            : random.nextBetween(minLevel, maxLevel);

    pokemon.setLevel(level);

    float shinyChance = data.has("shinyChance") ? data.get("shinyChance").getAsFloat() : shinyChancePercent;
    boolean isShiny = forceShiny || (random.nextFloat() * 100F < shinyChance);
    pokemon.setShiny(isShiny);

    Vec3d spawnPos = Vec3d.ofCenter(pos).add(0, 1, 0);
    PokemonEntity entity = pokemon.sendOut(world, spawnPos, null, e -> null);

    if (entity == null) {
        System.out.println("[PocketLuckHandler] Falha ao spawnar Pokémon: " + speciesName);
        return;
    }

    if (data.has("aggressive") && data.get("aggressive").getAsBoolean()) {
        entity.setPersistent();
        entity.setTarget(world.getClosestPlayer(spawnPos.x, spawnPos.y, spawnPos.z, 16, false));
    }
}

private static String normalizeSpeciesName(String name) {
    return name.toLowerCase().replace(" ", "_").replace("-", "_");
}
}
