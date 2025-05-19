package net.crulim.luckblockcobblemon;

import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.crulim.luckblockcobblemon.config.LuckConfig;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerPocket;
import net.crulim.luckblockcobblemon.handler.PocketLuckHandler;
import net.crulim.luckblockcobblemon.item.ModItemGroups;
import net.crulim.luckblockcobblemon.item.ModItems;
import net.crulim.luckblockcobblemon.block.CobbleLuckyBlockHandler;
import net.crulim.luckblockcobblemon.block.LuckyBlockHandler;

import net.crulim.luckblockcobblemon.util.ReadmeGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.crulim.luckblockcobblemon.command.StructureCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuckBlockCobblemon implements ModInitializer {
	public static final String MOD_ID = "luckblockcobblemon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.registerModBlocks();
		ModItems.registerModItems();
		ModItemGroups.registerItemGroups();
		ModItemGroups.registerItemGroups();



		PocketLuckHandler.loadConfig();
		ReadmeGenerator.generateReadmeIfMissing();


		/* ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			CobbleLuckyBlockHandler.loadConfig();
		}); */
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			LuckyBlockHandlerPocket.loadConfig();
		});
		/* ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			LuckyBlockHandler.loadConfig();
		}); */

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			StructureCommand.register(dispatcher);
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					literal("luckyblock")
							.then(literal("reload")
									.executes(context -> {
										/* LuckyBlockHandler.reloadConfig();
										CobbleLuckyBlockHandler.reloadConfig(); */
										LuckyBlockHandlerPocket.reloadConfig();
										PocketLuckHandler.reloadConfig();
										context.getSource().sendFeedback(() -> Text.literal("Both Lucky Blocks configs reloaded!"), false);
										return 1;
									})
							)
			);
		});
	}


}
