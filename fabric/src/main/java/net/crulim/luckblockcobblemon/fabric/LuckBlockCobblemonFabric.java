package net.crulim.luckblockcobblemon.fabric;

import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.config.LuckyBlockSettingsConfig;
import net.crulim.luckblockcobblemon.handler.AlphaLuckEvent;
import net.crulim.luckblockcobblemon.platform.Platform;
import net.crulim.luckblockcobblemon.registry.ModRegistration;
import net.crulim.luckblockcobblemon.registry.Registrar;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;

import java.util.function.Supplier;

/**
 * Fabric entrypoint: wires the common code to Fabric API.
 */
public class LuckBlockCobblemonFabric implements ModInitializer {

	/** Fabric allows registering straight into the vanilla registries during init. */
	private static final Registrar REGISTRAR = new Registrar() {
		@Override
		@SuppressWarnings("unchecked")
		public <T> T register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<T> factory) {
			Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY.get(registry.location());
			if (target == null) {
				throw new IllegalStateException("Unknown registry: " + registry.location());
			}
			return Registry.register(target, LuckBlockCobblemon.id(name), factory.get());
		}
	};

	@Override
	public void onInitialize() {
		Platform.set(new FabricPlatform());
		LuckBlockCobblemon.init();

		ResourceConditions.register(ConfigEnabledCondition.TYPE);
		ModRegistration.registerAll(REGISTRAR);

		if (LuckyBlockSettingsConfig.isPocketWorldgenEnabled()) {
			BiomeModifications.addFeature(
					BiomeSelectors.all(),
					GenerationStep.Decoration.SURFACE_STRUCTURES,
					ResourceKey.create(Registries.PLACED_FEATURE, LuckBlockCobblemon.id("pocket_lucky_block"))
			);
		}

		ServerLifecycleEvents.SERVER_STARTING.register(server -> LuckBlockCobblemon.onServerStarting());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> AlphaLuckEvent.onServerStopping());
		ServerTickEvents.END_SERVER_TICK.register(AlphaLuckEvent::onServerTick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				LuckBlockCobblemon.registerCommands(dispatcher));
	}
}
