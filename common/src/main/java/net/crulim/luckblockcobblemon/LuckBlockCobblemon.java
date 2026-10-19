package net.crulim.luckblockcobblemon;

import com.mojang.brigadier.CommandDispatcher;
import net.crulim.luckblockcobblemon.command.LuckyBlockGiveCommand;
import net.crulim.luckblockcobblemon.command.LuckyBlockLocateCommand;
import net.crulim.luckblockcobblemon.config.LockedLuckyBlockTierConfig;
import net.crulim.luckblockcobblemon.config.LuckyBlockSettingsConfig;
import net.crulim.luckblockcobblemon.handler.AlphaLuckEvent;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerPocket;
import net.crulim.luckblockcobblemon.handler.LuckyBlockHandlerVanilla;
import net.crulim.luckblockcobblemon.handler.PocketLuckHandler;
import net.crulim.luckblockcobblemon.platform.Platform;
import net.crulim.luckblockcobblemon.structure.AdaptiveBlockProcessor;
import net.crulim.luckblockcobblemon.util.ReadmeGenerator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.commands.Commands.literal;

/**
 * Loader-independent core of the mod. The fabric and neoforge entrypoints call these methods.
 */
public final class LuckBlockCobblemon {
	public static final String MOD_ID = "luckblockcobblemon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Assigned during registration (see registry.ModRegistration). */
	public static StructureProcessorType<AdaptiveBlockProcessor> ADAPTIVE_BLOCK_PROCESSOR;

	private LuckBlockCobblemon() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Loads configs and hooks Cobblemon events. Call after Platform.set(...). */
	public static void init() {
		LOGGER.info("Initializing Lucky Block Cobblemon on " + Platform.get().loaderName());

		LuckyBlockSettingsConfig.load();
		LockedLuckyBlockTierConfig.load();

		LuckyBlockHandlerPocket.loadConfig();
		LuckyBlockHandlerVanilla.loadConfig();
		PocketLuckHandler.loadConfig();

		ReadmeGenerator.generateReadmeIfMissing();
		AlphaLuckEvent.init();
	}

	/** Reloads every config when a server/world starts. */
	public static void onServerStarting() {
		LuckyBlockSettingsConfig.load();
		LockedLuckyBlockTierConfig.load();
		LuckyBlockHandlerPocket.loadConfig();
		LuckyBlockHandlerVanilla.loadConfig();
		PocketLuckHandler.loadConfig();
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		LuckyBlockLocateCommand.register(dispatcher);
		LuckyBlockGiveCommand.register(dispatcher);
		dispatcher.register(
				literal("luckyblock")
						.requires(source -> source.hasPermission(2))
						.then(literal("reload")
								.executes(context -> {
									LuckyBlockSettingsConfig.load();
									LockedLuckyBlockTierConfig.reload();
									LuckyBlockHandlerPocket.reloadConfig();
									PocketLuckHandler.reloadConfig();
									LuckyBlockHandlerVanilla.reloadConfig();
									context.getSource().sendSuccess(
											() -> Component.literal("Lucky Block Cobblemon configs reloaded from config/luckyblockcobblemon/. Restart the server for worldgen changes."),
											false
									);
									return 1;
								})
						)
		);
	}
}
