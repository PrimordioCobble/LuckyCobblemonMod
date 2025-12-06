package net.crulim.luckblockcobblemon;

import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerPocket;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerVanilla;
import net.crulim.luckblockcobblemon.handler.PocketLuckHandler;
import net.crulim.luckblockcobblemon.item.ModItemGroups;
import net.crulim.luckblockcobblemon.item.ModItems;
import net.crulim.luckblockcobblemon.util.ReadmeGenerator;
import net.crulim.luckblockcobblemon.world.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.crulim.luckblockcobblemon.command.StructureCommand;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;

import net.minecraft.world.gen.GenerationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.crulim.luckblockcobblemon.structure.AdaptiveBlockProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class LuckBlockCobblemon implements ModInitializer {
	public static final String MOD_ID = "luckblockcobblemon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final StructureProcessorType<AdaptiveBlockProcessor> ADAPTIVE_BLOCK_PROCESSOR =
			Registry.register(
					Registries.STRUCTURE_PROCESSOR,
					Identifier.of(MOD_ID, "adaptive_block_processor"),
					() -> AdaptiveBlockProcessor.CODEC
			);

	@Override
	public void onInitialize() {
		ModWorldGen.init();
		ModBlocks.registerModBlocks();
		ModItems.registerModItems();
		ModItemGroups.registerItemGroups();
		LuckyBlockHandlerPocket.loadConfig();
		LuckyBlockHandlerVanilla.loadConfig();
		PocketLuckHandler.loadConfig();
		//LuckyConfig.loadConfig();


		ReadmeGenerator.generateReadmeIfMissing();
/*
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					literal("luckyconfig")
							.requires(source -> source.hasPermissionLevel(2))
							.executes(ctx -> {
								ServerPlayerEntity player = ctx.getSource().getPlayer();
								OpenLuckyConfigPacket.send(player);
								return 1;
							})
			);
		}); */

		BiomeModifications.addFeature(
				BiomeSelectors.all(),
				GenerationStep.Feature.SURFACE_STRUCTURES,
				RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of("luckblockcobblemon", "pocket_lucky_block"))
		);

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			LuckyBlockHandlerPocket.loadConfig();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			StructureCommand.register(dispatcher);
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					literal("luckyblock")
							.then(literal("reload")
									.executes(context -> {
										LuckyBlockHandlerPocket.reloadConfig();
										PocketLuckHandler.reloadConfig();
										LuckyBlockHandlerVanilla.reloadConfig();
										context.getSource().sendFeedback(() -> Text.literal("Both Lucky Blocks configs reloaded!"), false);
										return 1;
									})
							)
			);
		});
	}
}
