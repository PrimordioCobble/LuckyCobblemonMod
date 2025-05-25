package net.crulim.luckblockcobblemon;

import net.crulim.luckblockcobblemon.block.ModBlocks;

import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerPocket;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerVanilla;
import net.crulim.luckblockcobblemon.handler.PocketLuckHandler;
import net.crulim.luckblockcobblemon.item.ModItemGroups;
import net.crulim.luckblockcobblemon.item.ModItems;
import net.crulim.luckblockcobblemon.util.ReadmeGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.crulim.luckblockcobblemon.command.StructureCommand;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;
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
					new Identifier(MOD_ID, "adaptive_block_processor"),
					() -> AdaptiveBlockProcessor.CODEC
			);

	@Override
	public void onInitialize() {
		ModBlocks.registerModBlocks();
		ModItems.registerModItems();
		ModItemGroups.registerItemGroups();
		ModItemGroups.registerItemGroups();
		LuckyBlockHandlerPocket.loadConfig();
		LuckyBlockHandlerVanilla.loadConfig();
		PocketLuckHandler.loadConfig();


		PocketLuckHandler.loadConfig();
		ReadmeGenerator.generateReadmeIfMissing();



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
