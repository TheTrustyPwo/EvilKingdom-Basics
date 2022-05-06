package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class AmbientMoodSettings {
    public static final Codec<AmbientMoodSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(SoundEvent.CODEC.fieldOf("sound").forGetter((ambientMoodSettings) -> {
            return ambientMoodSettings.soundEvent;
        }), Codec.INT.fieldOf("tick_delay").forGetter((ambientMoodSettings) -> {
            return ambientMoodSettings.tickDelay;
        }), Codec.INT.fieldOf("block_search_extent").forGetter((ambientMoodSettings) -> {
            return ambientMoodSettings.blockSearchExtent;
        }), Codec.DOUBLE.fieldOf("offset").forGetter((ambientMoodSettings) -> {
            return ambientMoodSettings.soundPositionOffset;
        })).apply(instance, AmbientMoodSettings::new);
    });
    public static final AmbientMoodSettings LEGACY_CAVE_SETTINGS = new AmbientMoodSettings(SoundEvents.AMBIENT_CAVE, 6000, 8, 2.0D);
    private final SoundEvent soundEvent;
    private final int tickDelay;
    private final int blockSearchExtent;
    private final double soundPositionOffset;

    public AmbientMoodSettings(SoundEvent sound, int cultivationTicks, int spawnRange, double extraDistance) {
        this.soundEvent = sound;
        this.tickDelay = cultivationTicks;
        this.blockSearchExtent = spawnRange;
        this.soundPositionOffset = extraDistance;
    }

    public SoundEvent getSoundEvent() {
        return this.soundEvent;
    }

    public int getTickDelay() {
        return this.tickDelay;
    }

    public int getBlockSearchExtent() {
        return this.blockSearchExtent;
    }

    public double getSoundPositionOffset() {
        return this.soundPositionOffset;
    }
}