package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChunkToProtochunkFix extends DataFix {
    private static final int NUM_SECTIONS = 16;

    public ChunkToProtochunkFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return TypeRewriteRule.seq(this.writeFixAndRead("ChunkToProtoChunkFix", this.getInputSchema().getType(References.CHUNK), this.getOutputSchema().getType(References.CHUNK), (dynamic) -> {
            return dynamic.update("Level", ChunkToProtochunkFix::fixChunkData);
        }), this.writeAndRead("Structure biome inject", this.getInputSchema().getType(References.STRUCTURE_FEATURE), this.getOutputSchema().getType(References.STRUCTURE_FEATURE)));
    }

    private static <T> Dynamic<T> fixChunkData(Dynamic<T> dynamic) {
        boolean bl = dynamic.get("TerrainPopulated").asBoolean(false);
        boolean bl2 = dynamic.get("LightPopulated").asNumber().result().isEmpty() || dynamic.get("LightPopulated").asBoolean(false);
        String string;
        if (bl) {
            if (bl2) {
                string = "mobs_spawned";
            } else {
                string = "decorated";
            }
        } else {
            string = "carved";
        }

        return repackTicks(repackBiomes(dynamic)).set("Status", dynamic.createString(string)).set("hasLegacyStructureData", dynamic.createBoolean(true));
    }

    private static <T> Dynamic<T> repackBiomes(Dynamic<T> dynamic) {
        return dynamic.update("Biomes", (dynamic2) -> {
            return DataFixUtils.orElse(dynamic2.asByteBufferOpt().result().map((byteBuffer) -> {
                int[] is = new int[256];

                for(int i = 0; i < is.length; ++i) {
                    if (i < byteBuffer.capacity()) {
                        is[i] = byteBuffer.get(i) & 255;
                    }
                }

                return dynamic.createIntList(Arrays.stream(is));
            }), dynamic2);
        });
    }

    private static <T> Dynamic<T> repackTicks(Dynamic<T> dynamic) {
        return DataFixUtils.orElse(dynamic.get("TileTicks").asStreamOpt().result().map((stream) -> {
            List<ShortList> list = IntStream.range(0, 16).mapToObj((i) -> {
                return new ShortArrayList();
            }).collect(Collectors.toList());
            stream.forEach((dynamicx) -> {
                int i = dynamicx.get("x").asInt(0);
                int j = dynamicx.get("y").asInt(0);
                int k = dynamicx.get("z").asInt(0);
                short s = packOffsetCoordinates(i, j, k);
                list.get(j >> 4).add(s);
            });
            return dynamic.remove("TileTicks").set("ToBeTicked", dynamic.createList(list.stream().map((shortList) -> {
                return dynamic.createList(shortList.intStream().mapToObj((i) -> {
                    return dynamic.createShort((short)i);
                }));
            })));
        }), dynamic);
    }

    private static short packOffsetCoordinates(int x, int y, int z) {
        return (short)(x & 15 | (y & 15) << 4 | (z & 15) << 8);
    }
}