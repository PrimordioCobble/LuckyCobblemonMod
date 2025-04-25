package net.crulim.luckblockcobblemon;

import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.crulim.luckblockcobblemon.item.ModItemGroups;
import net.crulim.luckblockcobblemon.item.ModItems;
import net.crulim.luckmod.config.CobbleLuckConfig;
import net.crulim.luckmod.config.LuckConfig;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuckBlockCobblemon implements ModInitializer {
	public static final String MOD_ID = "luckblockcobblemon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LuckConfig.load();
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModItemGroups.registerItemGroups();
		CobbleLuckConfig.load();


	}
}