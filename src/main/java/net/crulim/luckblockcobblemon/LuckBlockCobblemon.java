package net.crulim.luckblockcobblemon;


import net.crulim.luckblockcobblemon.command.LuckyBlockGiveCommand;
import net.crulim.luckblockcobblemon.command.LuckyBlockLocateCommand;
import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.crulim.luckblockcobblemon.command.StructureCommand;
import net.crulim.luckblockcobblemon.config.LockedLuckyBlockTierConfig;
import net.crulim.luckblockcobblemon.config.LuckyBlockSettingsConfig;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerPocket;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerVanilla;
import net.crulim.luckblockcobblemon.handler.PocketLuckHandler;
import net.crulim.luckblockcobblemon.item.ModItemGroups;
import net.crulim.luckblockcobblemon.item.ModItems;
import net.crulim.luckblockcobblemon.resource.ConfigEnabledCondition;
import net.crulim.luckblockcobblemon.structure.AdaptiveBlockProcessor;
import net.crulim.luckblockcobblemon.util.ReadmeGenerator;
import net.crulim.luckblockcobblemon.world.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.literal;

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
		LuckyBlockSettingsConfig.load();
		LockedLuckyBlockTierConfig.load();
		ResourceConditions.register(ConfigEnabledCondition.TYPE);

		ModWorldGen.init();
		ModBlocks.registerModBlocks();
		ModItems.registerModItems();
		ModItemGroups.registerItemGroups();

		LuckyBlockHandlerPocket.loadConfig();
		LuckyBlockHandlerVanilla.loadConfig();
		PocketLuckHandler.loadConfig();

		ReadmeGenerator.generateReadmeIfMissing();

		if (LuckyBlockSettingsConfig.isPocketWorldgenEnabled()) {
			BiomeModifications.addFeature(
					BiomeSelectors.all(),
					GenerationStep.Feature.SURFACE_STRUCTURES,
					RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(MOD_ID, "pocket_lucky_block"))
			);
		}

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			LuckyBlockSettingsConfig.load();
			LockedLuckyBlockTierConfig.load();
			LuckyBlockHandlerPocket.loadConfig();
			LuckyBlockHandlerVanilla.loadConfig();
			PocketLuckHandler.loadConfig();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				StructureCommand.register(dispatcher)
		);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				LuckyBlockLocateCommand.register(dispatcher)
		);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				LuckyBlockGiveCommand.register(dispatcher)
		);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(
						literal("luckyblock")
								.requires(source -> source.hasPermissionLevel(2))
								.then(literal("reload")
										.executes(context -> {
											LuckyBlockSettingsConfig.load();
											LockedLuckyBlockTierConfig.reload();
											LuckyBlockHandlerPocket.reloadConfig();
											PocketLuckHandler.reloadConfig();
											LuckyBlockHandlerVanilla.reloadConfig();
											context.getSource().sendFeedback(
													() -> Text.literal("Lucky Block Cobblemon configs reloaded from config/luckyblockcobblemon/. Restart the server for worldgen changes."),
													false
											);
											return 1;
										})
								)
				)
		);
	}
}