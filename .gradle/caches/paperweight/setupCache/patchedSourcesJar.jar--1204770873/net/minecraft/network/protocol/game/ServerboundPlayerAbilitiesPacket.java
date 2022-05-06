package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.Abilities;

public class ServerboundPlayerAbilitiesPacket implements Packet<ServerGamePacketListener> {
    private static final int FLAG_FLYING = 2;
    private final boolean isFlying;

    public ServerboundPlayerAbilitiesPacket(Abilities abilities) {
        this.isFlying = abilities.flying;
    }

    public ServerboundPlayerAbilitiesPacket(FriendlyByteBuf buf) {
        byte b = buf.readByte();
        this.isFlying = (b & 2) != 0;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        byte b = 0;
        if (this.isFlying) {
            b = (byte)(b | 2);
        }

        buf.writeByte(b);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handlePlayerAbilities(this);
    }

    public boolean isFlying() {
        return this.isFlying;
    }
}