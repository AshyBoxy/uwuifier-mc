package xyz.ashyboxy.uwuifiermc;

import lol.bai.badpackets.api.S2CPacketReceiver;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import xyz.ashyboxy.Uwuifier;

public class UwuifierClientMod implements ClientModInitializer {
    public static boolean isOnServer = false;

    @Override
    public void onInitializeClient() {
        ClientLoginConnectionEvents.INIT.register((handler, client) -> {
            isOnServer = false;
        });

        S2CPacketReceiver.register(UwuifierMod.SERVER_INFORM_ID,
                (client, handler, buf, responseSender) -> {
                    isOnServer = buf.readBoolean();
                    UwuifierMod.LOGGER.info("Uwuifier exists on the server");
                });

        ClientSendMessageEvents.MODIFY_CHAT.register((message) -> {
            if (!isOnServer) {
                return Uwuifier.uwuify(message);
            }
            return message;
        });
    }
}
