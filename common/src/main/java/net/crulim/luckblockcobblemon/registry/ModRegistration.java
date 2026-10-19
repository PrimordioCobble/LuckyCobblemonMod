package net.crulim.luckblockcobblemon.registry;

import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.crulim.luckblockcobblemon.item.ModItemGroups;
import net.crulim.luckblockcobblemon.structure.AdaptiveBlockProcessor;
import net.crulim.luckblockcobblemon.world.ModWorldGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

/**
 * Everything the mod registers, split by registry so each loader can call it at the right moment.
 * Order matters: blocks, then items, then creative tabs.
 */
public final class ModRegistration {

    private ModRegistration() {
    }

    public static void blocks(Registrar registrar) {
        ModBlocks.register(registrar);
    }

    public static void items(Registrar registrar) {
        ModBlocks.registerBlockItems(registrar);
    }

    public static void creativeTabs(Registrar registrar) {
        ModItemGroups.register(registrar);
    }

    public static void features(Registrar registrar) {
        ModWorldGen.register(registrar);
    }

    public static void structureProcessors(Registrar registrar) {
        StructureProcessorType<AdaptiveBlockProcessor> type = () -> AdaptiveBlockProcessor.CODEC;
        registrar.register(Registries.STRUCTURE_PROCESSOR, "adaptive_block_processor", () -> type);
        LuckBlockCobblemon.ADAPTIVE_BLOCK_PROCESSOR = type;
    }

    /** Runs every phase in order. Used by loaders that register immediately (Fabric). */
    public static void registerAll(Registrar registrar) {
        structureProcessors(registrar);
        features(registrar);
        blocks(registrar);
        items(registrar);
        creativeTabs(registrar);
    }
}
