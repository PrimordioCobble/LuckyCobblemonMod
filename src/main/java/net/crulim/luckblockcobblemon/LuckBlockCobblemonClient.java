package net.crulim.luckblockcobblemon;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.codec.PacketCodec;


public class LuckBlockCobblemonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
       /* LuckyConfig.loadConfig();

        // Registrar o payload sem dados (usando PacketCodec.unit)
        PayloadTypeRegistry.playS2C().register(
                OpenLuckyConfigPayload.ID,
                PacketCodec.unit(OpenLuckyConfigPayload.INSTANCE)
        );

        // Registrar o handler
        ClientPlayNetworking.registerGlobalReceiver(OpenLuckyConfigPayload.ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().setScreen(new LuckyConfigScreen());
            });
        }); */

    }
}
