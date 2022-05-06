package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;

public class EmptyArgumentSerializer<T extends ArgumentType<?>> implements ArgumentSerializer<T> {
    private final Supplier<T> constructor;

    public EmptyArgumentSerializer(Supplier<T> supplier) {
        this.constructor = supplier;
    }

    @Override
    public void serializeToNetwork(T type, FriendlyByteBuf buf) {
    }

    @Override
    public T deserializeFromNetwork(FriendlyByteBuf buf) {
        return this.constructor.get();
    }

    @Override
    public void serializeToJson(T type, JsonObject json) {
    }
}