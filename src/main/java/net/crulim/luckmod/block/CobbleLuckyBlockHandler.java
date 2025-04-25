package net.crulim.luckmod.block;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.crulim.luckmod.config.CobbleLuckConfig;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.Vec3d;

public class CobbleLuckyBlockHandler {
    private static final Random RANDOM = Random.create();

    public static void handleLuck(ServerWorld world, BlockPos pos) {
        if (CobbleLuckConfig.groupOne == null || CobbleLuckConfig.groupTwo == null) {
            return;
        }

        CobbleLuckConfig.Group chosenGroup;
        double roll = RANDOM.nextDouble();

        if (roll <= CobbleLuckConfig.groupOne.chance) {
            chosenGroup = CobbleLuckConfig.groupOne;
        } else {
            chosenGroup = CobbleLuckConfig.groupTwo;
        }

        if (chosenGroup.speciesList == null || chosenGroup.speciesList.isEmpty()) {
            return;
        }

        String speciesName = chosenGroup.speciesList.get(RANDOM.nextInt(chosenGroup.speciesList.size()));
        Species species = PokemonSpecies.INSTANCE.getByName(speciesName);

        if (species == null) {
            System.err.println("[CobbleLuckyBlock] ⚠️ Species not found: " + speciesName);
            return;
        }

        int level = RANDOM.nextBetween(chosenGroup.minLevel, chosenGroup.maxLevel + 1);
        boolean isShiny = RANDOM.nextFloat() < chosenGroup.shinyChance;

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(level);
        pokemon.setShiny(isShiny);

        Vec3d spawnPos = pos.toCenterPos();
        pokemon.sendOut(world, spawnPos, null, entity -> null);
    }
}
