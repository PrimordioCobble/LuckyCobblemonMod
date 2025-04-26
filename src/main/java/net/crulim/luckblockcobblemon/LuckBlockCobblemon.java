package net.crulim.luckblockcobblemon;

import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.crulim.luckblockcobblemon.item.ModItemGroups;
import net.crulim.luckblockcobblemon.item.ModItems;
import net.crulim.luckmod.block.CobbleLuckyBlockHandler;
import net.crulim.luckmod.block.LuckyBlockHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuckBlockCobblemon implements ModInitializer {
	public static final String MOD_ID = "luckblockcobblemon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModItemGroups.registerItemGroups();
		LuckyBlockHandler.loadConfig();


		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			CobbleLuckyBlockHandler.loadConfig();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					literal("luckyblock")
							.then(literal("reload")
									.executes(context -> {
										LuckyBlockHandler.reloadConfig();          // Reload base
										CobbleLuckyBlockHandler.reloadConfig();    // Reload cobblemon
										context.getSource().sendFeedback(() -> Text.literal("Both Lucky Blocks configs reloaded!"), false);
										return 1;
									})
							)
			);
		});
	}
}
