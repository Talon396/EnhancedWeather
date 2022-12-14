package sh.talonfox.enhancedweather.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import sh.talonfox.enhancedweather.EnhancedWeather;

public class WindSync {
    public static Identifier PACKET_ID = new Identifier("enhancedweather","wind_s2c_sync");
    /*
    PACKET BUFFER STRUCTURE
    int: Dimension ID
    float: Global Wind Angle
    float: Global Wind Speed
    int: Low Wind Timer
    int: High Wind Timer
     */
    public static void send(MinecraftServer server, int dimid) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(dimid);
        if(dimid == 0) {
            buf.writeFloat(EnhancedWeather.WIND.AngleGlobal);
            buf.writeFloat(EnhancedWeather.WIND.SpeedGlobal);
            buf.writeInt(EnhancedWeather.WIND.LowWindTimer);
            buf.writeInt(EnhancedWeather.WIND.HighWindTimer);
        } else if(dimid == -1) {
            buf.writeFloat(EnhancedWeather.NETHER_WIND.AngleGlobal);
            buf.writeFloat(EnhancedWeather.NETHER_WIND.SpeedGlobal);
            buf.writeInt(EnhancedWeather.WIND.LowWindTimer);
            buf.writeInt(EnhancedWeather.WIND.HighWindTimer);
        }
        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, PACKET_ID, buf);
        }
    }
    public static void onReceive(MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        int dimid = packetByteBuf.readInt();
        if(dimid == 0) {
            EnhancedWeather.CLIENT_WIND.AngleGlobal = packetByteBuf.readFloat();
            EnhancedWeather.CLIENT_WIND.SpeedGlobal = packetByteBuf.readFloat();
            EnhancedWeather.CLIENT_WIND.LowWindTimer = packetByteBuf.readInt();
            EnhancedWeather.CLIENT_WIND.HighWindTimer = packetByteBuf.readInt();
        } else if(dimid == -1) {
            EnhancedWeather.NETHER_CLIENT_WIND.AngleGlobal = packetByteBuf.readFloat();
            EnhancedWeather.NETHER_CLIENT_WIND.SpeedGlobal = packetByteBuf.readFloat();
            EnhancedWeather.NETHER_CLIENT_WIND.LowWindTimer = packetByteBuf.readInt();
            EnhancedWeather.NETHER_CLIENT_WIND.HighWindTimer = packetByteBuf.readInt();
        }
    }
}
