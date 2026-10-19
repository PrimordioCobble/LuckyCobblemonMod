package net.crulim.luckblockcobblemon.handler;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.evolution.PreEvolution;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.crulim.luckblockcobblemon.config.LuckyBlockSettingsConfig;
import net.crulim.luckblockcobblemon.util.CobblemonRewards;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Alpha Awakening" event (pool type: alpha_cobblemonp).
 * Spawns a Cobblemon 1.8 Alpha Pokemon with an optional herd, a dramatic entrance and a boss bar.
 * Defeating or capturing the Alpha rewards the player with Type Gems and, by chance, a TM.
 * Never runs from locked-tier Lucky Blocks (filtered by the pool handlers).
 */
public final class AlphaLuckEvent {
    public static final String EVENT_TYPE = "alpha_cobblemonp";

    private static final String REWARD_TAG = "luckblockcobblemon_alpha";
    private static final Set<String> EXCLUDED_LABELS = Set.of("legendary", "mythical", "ultra_beast", "paradox", "restricted", "baby");
    private static final RandomSource random = RandomSource.create();
    private static final Map<UUID, Encounter> ACTIVE = new ConcurrentHashMap<>();

    private static boolean initialized = false;
    private static int tickCounter = 0;

    private AlphaLuckEvent() {
    }

    private static final class Encounter {
        final ServerLevel world;
        final PokemonEntity entity;
        final Pokemon pokemon;
        final ServerBossEvent bossBar;
        final double radiusSq;
        final long expiresAt;
        int shownLevel;

        Encounter(ServerLevel world, PokemonEntity entity, Pokemon pokemon, ServerBossEvent bossBar, int radius, long expiresAt) {
            this.world = world;
            this.entity = entity;
            this.pokemon = pokemon;
            this.bossBar = bossBar;
            this.radiusSq = (double) radius * radius;
            this.expiresAt = expiresAt;
            this.shownLevel = pokemon.getLevel();
        }
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, AlphaLuckEvent::onCaptured);
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, AlphaLuckEvent::onBattleVictory);
    }

    public static boolean isAlphaEvent(JsonObject event) {
        return event != null && event.has("type") && EVENT_TYPE.equals(event.get("type").getAsString());
    }

    public static boolean isEnabled() {
        return LuckyBlockSettingsConfig.areAlphaEventsEnabled();
    }

    /**
     * @param baseLevel level resolved by the calling pool (event min/max, time leveling or weighting).
     * @return true if the Alpha was spawned.
     */
    public static boolean spawn(ServerLevel world, BlockPos pos, JsonObject data, String poolKey, int baseLevel) {
        if (!isEnabled()) {
            return false;
        }

        Species species = pickSpecies(data);
        if (species == null) {
            System.out.println("[LuckyBlockAlpha] No valid species for alpha event in pool: " + poolKey);
            return false;
        }

        Vec3 spawnPos = Vec3.atCenterOf(pos).add(0, 1, 0);
        int radius = getInt(data, "announceRadius", 48);
        Player nearest = world.getNearestPlayer(spawnPos.x, spawnPos.y, spawnPos.z, radius, false);

        int level = Math.max(1, baseLevel) + getInt(data, "levelBonus", 5);
        if (getBoolean(data, "scaleWithParty", true) && nearest instanceof ServerPlayer serverPlayer) {
            int partyLevel = highestPartyLevel(serverPlayer);
            if (partyLevel > 0) {
                level = Math.max(level, partyLevel + getInt(data, "partyLevelBonus", 3));
            }
        }
        level = Mth.clamp(level, 1, getInt(data, "maxLevelCap", 100));

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(level);
        pokemon.setAlpha(true);
        pokemon.initialize();
        pokemon.setAlpha(true);
        pokemon.setShiny(CobblemonRewards.roll(getFloat(data, "shinyChance", 1.0F)));
        pokemon.heal();

        JsonObject rewards = data.has("rewards") && data.get("rewards").isJsonObject()
                ? data.getAsJsonObject("rewards").deepCopy()
                : new JsonObject();
        CompoundTag tag = new CompoundTag();
        tag.putString("pool", poolKey == null ? "default" : poolKey);
        tag.putString("rewards", rewards.toString());
        pokemon.getPersistentData().put(REWARD_TAG, tag);

        PokemonEntity entity = pokemon.sendOut(world, spawnPos, null, e -> null);
        if (entity == null) {
            System.out.println("[LuckyBlockAlpha] Failed to spawn alpha: " + species.getName());
            return false;
        }
        entity.setPersistenceRequired();

        playEntrance(world, spawnPos);
        announce(world, spawnPos, radius, pokemon, level);
        spawnHerd(world, pos, data, species, level);

        if (getBoolean(data, "bossBar", true)) {
            ServerBossEvent bar = new ServerBossEvent(
                    Component.translatable("luckblockcobblemon.alpha.bossbar", pokemon.getDisplayName(false), level),
                    colorFor(pokemon),
                    BossEvent.BossBarOverlay.NOTCHED_10
            );
            bar.setProgress(1.0F);
            long expiresAt = world.getGameTime() + 20L * getInt(data, "bossBarSeconds", 600);
            ACTIVE.put(pokemon.getUuid(), new Encounter(world, entity, pokemon, bar, radius, expiresAt));
        }

        System.out.println("[LuckyBlockAlpha] Alpha spawned: " + species.getName() + " lvl " + level + " (pool: " + poolKey + ")");
        return true;
    }

    private static Species pickSpecies(JsonObject data) {
        List<String> names = new ArrayList<>();
        for (String key : new String[]{"pokemons", "pokemon", "cobblemons"}) {
            if (data.has(key) && data.get(key).isJsonArray()) {
                for (JsonElement element : data.getAsJsonArray(key)) {
                    if (element != null && element.isJsonPrimitive()) {
                        names.add(element.getAsString());
                    }
                }
                break;
            }
        }

        if (!names.isEmpty()) {
            for (int attempt = 0; attempt < 8; attempt++) {
                Species species = PokemonSpecies.getByName(normalizeSpeciesName(names.get(random.nextInt(names.size()))));
                if (species != null && species.getImplemented()) {
                    return species;
                }
            }
            return null;
        }

        if (!getBoolean(data, "randomSpecies", false)) {
            return null;
        }

        int minBaseStatTotal = getInt(data, "minBaseStatTotal", 470);
        List<String> typeFilter = CobblemonRewards.readStringList(data, "speciesTypes");
        List<Species> candidates = new ArrayList<>();
        for (Species species : PokemonSpecies.getImplemented()) {
            if (species == null || !species.getEvolutions().isEmpty()) {
                continue;
            }
            boolean excluded = false;
            for (String label : species.getLabels()) {
                if (EXCLUDED_LABELS.contains(label)) {
                    excluded = true;
                    break;
                }
            }
            if (excluded) {
                continue;
            }
            int total = 0;
            for (Integer value : species.getBaseStats().values()) {
                total += value == null ? 0 : value;
            }
            if (total < minBaseStatTotal) {
                continue;
            }
            if (!typeFilter.isEmpty()) {
                boolean match = false;
                for (var type : species.getTypes()) {
                    if (type != null && typeFilter.contains(type.getName().toLowerCase(Locale.ROOT))) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
            }
            candidates.add(species);
        }

        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private static int highestPartyLevel(ServerPlayer player) {
        try {
            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            int highest = 0;
            for (Pokemon member : party) {
                if (member != null) {
                    highest = Math.max(highest, member.getLevel());
                }
            }
            return highest;
        } catch (Exception e) {
            return 0;
        }
    }

    private static void spawnHerd(ServerLevel world, BlockPos pos, JsonObject data, Species leader, int leaderLevel) {
        if (!data.has("herd") || !data.get("herd").isJsonObject()) {
            return;
        }

        JsonObject herd = data.getAsJsonObject("herd");
        int amount = CobblemonRewards.randomBetween(getInt(herd, "min", 2), getInt(herd, "max", 4));
        if (amount <= 0) {
            return;
        }

        List<Species> followers = new ArrayList<>();
        PreEvolution preEvolution = leader.getPreEvolution();
        int guard = 0;
        while (preEvolution != null && guard++ < 5) {
            Species previous = preEvolution.getSpecies();
            if (previous == null) {
                break;
            }
            followers.add(previous);
            preEvolution = previous.getPreEvolution();
        }
        if (followers.isEmpty()) {
            followers.add(leader);
        }

        int offsetMin = getInt(herd, "levelOffsetMin", -15);
        int offsetMax = getInt(herd, "levelOffsetMax", -5);
        float shinyChance = getFloat(herd, "shinyChance", getFloat(data, "shinyChance", 1.0F));

        for (int i = 0; i < amount; i++) {
            Species species = followers.get(random.nextInt(followers.size()));
            int level = Mth.clamp(leaderLevel + CobblemonRewards.randomBetween(offsetMin, offsetMax), 1, 100);

            Pokemon follower = new Pokemon();
            follower.setSpecies(species);
            follower.setLevel(level);
            follower.initialize();
            follower.setShiny(CobblemonRewards.roll(shinyChance));

            Vec3 followerPos = Vec3.atCenterOf(pos).add(random.nextIntBetweenInclusive(-3, 3), 1, random.nextIntBetweenInclusive(-3, 3));
            follower.sendOut(world, followerPos, null, e -> null);
        }
    }

    private static void playEntrance(ServerLevel world, Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.moveTo(pos);
            world.addFreshEntity(bolt);
        }

        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.8F, 0.7F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.0F, 1.0F);

        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2 * i) / 24;
            double x = pos.x + Math.cos(angle) * 2.5;
            double z = pos.z + Math.sin(angle) * 2.5;
            world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, pos.y, z, 2, 0.05, 0.4, 0.05, 0.01);
        }
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.5, pos.z, 60, 1.2, 1.0, 1.2, 0.1);
        world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }

    private static void announce(ServerLevel world, Vec3 pos, int radius, Pokemon pokemon, int level) {
        double radiusSq = (double) radius * radius;
        Component title = Component.translatable("luckblockcobblemon.alpha.title").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        Component subtitle = Component.translatable("luckblockcobblemon.alpha.subtitle", pokemon.getDisplayName(false), level).withStyle(ChatFormatting.GOLD);
        Component chat = Component.translatable("luckblockcobblemon.alpha.announce", pokemon.getDisplayName(false)).withStyle(ChatFormatting.RED);

        for (ServerPlayer player : world.players()) {
            if (player.distanceToSqr(pos) > radiusSq) {
                continue;
            }
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            player.displayClientMessage(chat, false);
        }
    }

    private static BossEvent.BossBarColor colorFor(Pokemon pokemon) {
        List<String> types = CobblemonRewards.typesOf(pokemon);
        String type = types.isEmpty() ? "" : types.get(0);
        return switch (type) {
            case "fire", "fighting" -> BossEvent.BossBarColor.RED;
            case "water", "ice", "dragon" -> BossEvent.BossBarColor.BLUE;
            case "grass", "bug" -> BossEvent.BossBarColor.GREEN;
            case "electric", "ground" -> BossEvent.BossBarColor.YELLOW;
            case "psychic", "fairy" -> BossEvent.BossBarColor.PINK;
            case "ghost", "poison", "dark" -> BossEvent.BossBarColor.PURPLE;
            default -> BossEvent.BossBarColor.WHITE;
        };
    }

    /** Called by the loader at the end of every server tick. */
    public static void onServerTick(MinecraftServer server) {
        if (ACTIVE.isEmpty() || ++tickCounter % 10 != 0) {
            return;
        }

        Iterator<Map.Entry<UUID, Encounter>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Encounter encounter = iterator.next().getValue();
            boolean finished = encounter.entity.isRemoved()
                    || !encounter.entity.isAlive()
                    || !encounter.pokemon.isWild()
                    || encounter.pokemon.getCurrentHealth() <= 0
                    || encounter.world.getGameTime() > encounter.expiresAt;

            if (finished) {
                encounter.bossBar.removeAllPlayers();
                iterator.remove();
                continue;
            }

            // Cobblemon 1.8 alphas can re-match their level to the challenger's party: keep the bar label in sync.
            if (encounter.pokemon.getLevel() != encounter.shownLevel) {
                encounter.shownLevel = encounter.pokemon.getLevel();
                encounter.bossBar.setName(Component.translatable("luckblockcobblemon.alpha.bossbar", encounter.pokemon.getDisplayName(false), encounter.shownLevel));
            }

            int maxHealth = Math.max(1, encounter.pokemon.getMaxHealth());
            encounter.bossBar.setProgress(Mth.clamp(encounter.pokemon.getCurrentHealth() / (float) maxHealth, 0F, 1F));

            for (ServerPlayer player : encounter.world.players()) {
                if (player.distanceToSqr(encounter.entity) <= encounter.radiusSq) {
                    encounter.bossBar.addPlayer(player);
                } else {
                    encounter.bossBar.removePlayer(player);
                }
            }
        }
    }

    /** Called by the loader when the server is stopping. */
    public static void onServerStopping() {
        for (Encounter encounter : ACTIVE.values()) {
            encounter.bossBar.removeAllPlayers();
        }
        ACTIVE.clear();
    }

    private static void onCaptured(PokemonCapturedEvent event) {
        // Capturing keeps the Alpha itself as the prize: no extra reward, just clear the tag.
        Pokemon pokemon = event.getPokemon();
        if (pokemon != null) {
            pokemon.getPersistentData().remove(REWARD_TAG);
        }
    }

    private static void onBattleVictory(BattleVictoryEvent event) {
        if (event.getWasWildCapture()) {
            return;
        }

        for (BattleActor loser : event.getLosers()) {
            for (BattlePokemon battlePokemon : loser.getPokemonList()) {
                Pokemon pokemon = battlePokemon.getOriginalPokemon();
                if (pokemon == null || !pokemon.getPersistentData().contains(REWARD_TAG)) {
                    continue;
                }

                for (BattleActor winner : event.getWinners()) {
                    if (winner instanceof PlayerBattleActor playerActor && playerActor.getEntity() != null) {
                        grantReward(playerActor.getEntity(), pokemon);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Defeat reward: exactly ONE reward, picked by weight between gems, TM, gem block and bonus item.
     * The *Chance fields of "rewards" are used as weights.
     */
    private static void grantReward(ServerPlayer player, Pokemon pokemon) {
        CompoundTag tag = pokemon.getPersistentData().getCompound(REWARD_TAG);
        pokemon.getPersistentData().remove(REWARD_TAG);
        if (player == null) {
            return;
        }

        JsonObject rewards = new JsonObject();
        try {
            String raw = tag.getString("rewards");
            if (raw != null && !raw.isBlank()) {
                rewards = JsonParser.parseString(raw).getAsJsonObject();
            }
        } catch (Exception ignored) {
        }

        List<String> pokemonTypes = CobblemonRewards.typesOf(pokemon);
        List<String> gemTypes = CobblemonRewards.readStringList(rewards, "gemTypes");
        if (gemTypes.isEmpty()) {
            gemTypes = pokemonTypes;
        }
        List<String> tmTypes = CobblemonRewards.readStringList(rewards, "tmTypes");
        if (tmTypes.isEmpty()) {
            tmTypes = pokemonTypes;
        }
        JsonArray bonusItems = rewards.has("bonusItems") && rewards.get("bonusItems").isJsonArray()
                ? rewards.getAsJsonArray("bonusItems")
                : null;

        float gemWeight = gemTypes.isEmpty() ? 0F : Math.max(0F, getFloat(rewards, "gemChance", 50F));
        float tmWeight = Math.max(0F, getFloat(rewards, "tmChance", 40F));
        float blockWeight = gemTypes.isEmpty() ? 0F : Math.max(0F, getFloat(rewards, "gemBlockChance", 10F));
        float bonusWeight = bonusItems == null ? 0F : Math.max(0F, getFloat(rewards, "bonusChance", 30F));
        float total = gemWeight + tmWeight + blockWeight + bonusWeight;

        ItemStack reward = ItemStack.EMPTY;
        if (total > 0F) {
            float roll = random.nextFloat() * total;
            if ((roll -= gemWeight) < 0F) {
                int amount = CobblemonRewards.randomBetween(getInt(rewards, "gemMin", 3), getInt(rewards, "gemMax", 6));
                reward = CobblemonRewards.gemStack(gemTypes.get(random.nextInt(gemTypes.size())), amount);
            } else if ((roll -= tmWeight) < 0F) {
                reward = CobblemonRewards.randomTm(tmTypes);
            } else if ((roll -= blockWeight) < 0F) {
                reward = CobblemonRewards.gemBlockStack(gemTypes.get(0));
            } else {
                reward = CobblemonRewards.randomItemFrom(bonusItems, 1);
            }
        }
        if (reward.isEmpty() && !gemTypes.isEmpty()) {
            reward = CobblemonRewards.gemStack(gemTypes.get(0), Math.max(1, getInt(rewards, "gemMin", 3)));
        }

        MutableComponent message = Component.translatable("luckblockcobblemon.alpha.reward.defeated", pokemon.getDisplayName(false))
                .withStyle(ChatFormatting.GOLD);
        if (!reward.isEmpty()) {
            message.append(Component.literal(" " + reward.getCount() + "x ").withStyle(ChatFormatting.GRAY));
            message.append(reward.getHoverName().copy().withStyle(ChatFormatting.AQUA));
            player.getInventory().placeItemBackInInventory(reward);
        }

        player.displayClientMessage(message, false);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static String normalizeSpeciesName(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return input
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("♀", "f")
                .replace("♂", "m")
                .replaceAll("[^a-z0-9\\-]", "");
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        try {
            return json != null && json.has(key) ? json.get(key).getAsInt() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static float getFloat(JsonObject json, String key, float fallback) {
        try {
            return json != null && json.has(key) ? json.get(key).getAsFloat() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        try {
            return json != null && json.has(key) ? json.get(key).getAsBoolean() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
