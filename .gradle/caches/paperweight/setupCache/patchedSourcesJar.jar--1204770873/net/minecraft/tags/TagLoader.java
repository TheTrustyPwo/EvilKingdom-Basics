package net.minecraft.tags;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class TagLoader<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final String PATH_SUFFIX = ".json";
    private static final int PATH_SUFFIX_LENGTH = ".json".length();
    private final Function<ResourceLocation, Optional<T>> idToValue;
    private final String directory;

    public TagLoader(Function<ResourceLocation, Optional<T>> registryGetter, String dataType) {
        this.idToValue = registryGetter;
        this.directory = dataType;
    }

    public Map<ResourceLocation, Tag.Builder> load(ResourceManager manager) {
        Map<ResourceLocation, Tag.Builder> map = Maps.newHashMap();

        for(ResourceLocation resourceLocation : manager.listResources(this.directory, (stringx) -> {
            return stringx.endsWith(".json");
        })) {
            String string = resourceLocation.getPath();
            ResourceLocation resourceLocation2 = new ResourceLocation(resourceLocation.getNamespace(), string.substring(this.directory.length() + 1, string.length() - PATH_SUFFIX_LENGTH));

            try {
                for(Resource resource : manager.getResources(resourceLocation)) {
                    try {
                        InputStream inputStream = resource.getInputStream();

                        try {
                            Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                            try {
                                JsonObject jsonObject = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                                if (jsonObject == null) {
                                    LOGGER.error("Couldn't load tag list {} from {} in data pack {} as it is empty or null", resourceLocation2, resourceLocation, resource.getSourceName());
                                } else {
                                    map.computeIfAbsent(resourceLocation2, (resourceLocationx) -> {
                                        return Tag.Builder.tag();
                                    }).addFromJson(jsonObject, resource.getSourceName());
                                }
                            } catch (Throwable var23) {
                                try {
                                    reader.close();
                                } catch (Throwable var22) {
                                    var23.addSuppressed(var22);
                                }

                                throw var23;
                            }

                            reader.close();
                        } catch (Throwable var24) {
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (Throwable var21) {
                                    var24.addSuppressed(var21);
                                }
                            }

                            throw var24;
                        }

                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (RuntimeException | IOException var25) {
                        LOGGER.error("Couldn't read tag list {} from {} in data pack {}", resourceLocation2, resourceLocation, resource.getSourceName(), var25);
                    } finally {
                        IOUtils.closeQuietly((Closeable)resource);
                    }
                }
            } catch (IOException var27) {
                LOGGER.error("Couldn't read tag list {} from {}", resourceLocation2, resourceLocation, var27);
            }
        }

        return map;
    }

    private static void visitDependenciesAndElement(Map<ResourceLocation, Tag.Builder> map, Multimap<ResourceLocation, ResourceLocation> multimap, Set<ResourceLocation> set, ResourceLocation resourceLocation, BiConsumer<ResourceLocation, Tag.Builder> biConsumer) {
        if (set.add(resourceLocation)) {
            multimap.get(resourceLocation).forEach((resourceLocationx) -> {
                visitDependenciesAndElement(map, multimap, set, resourceLocationx, biConsumer);
            });
            Tag.Builder builder = map.get(resourceLocation);
            if (builder != null) {
                biConsumer.accept(resourceLocation, builder);
            }

        }
    }

    private static boolean isCyclic(Multimap<ResourceLocation, ResourceLocation> multimap, ResourceLocation resourceLocation, ResourceLocation resourceLocation2) {
        Collection<ResourceLocation> collection = multimap.get(resourceLocation2);
        return collection.contains(resourceLocation) ? true : collection.stream().anyMatch((resourceLocation2x) -> {
            return isCyclic(multimap, resourceLocation, resourceLocation2x);
        });
    }

    private static void addDependencyIfNotCyclic(Multimap<ResourceLocation, ResourceLocation> multimap, ResourceLocation resourceLocation, ResourceLocation resourceLocation2) {
        if (!isCyclic(multimap, resourceLocation, resourceLocation2)) {
            multimap.put(resourceLocation, resourceLocation2);
        }

    }

    public Map<ResourceLocation, Tag<T>> build(Map<ResourceLocation, Tag.Builder> tags) {
        Map<ResourceLocation, Tag<T>> map = Maps.newHashMap();
        Function<ResourceLocation, Tag<T>> function = map::get;
        Function<ResourceLocation, T> function2 = (id) -> {
            return this.idToValue.apply(id).orElse((T)null);
        };
        Multimap<ResourceLocation, ResourceLocation> multimap = HashMultimap.create();
        tags.forEach((resourceLocation, builder) -> {
            builder.visitRequiredDependencies((resourceLocation2) -> {
                addDependencyIfNotCyclic(multimap, resourceLocation, resourceLocation2);
            });
        });
        tags.forEach((resourceLocation, builder) -> {
            builder.visitOptionalDependencies((resourceLocation2) -> {
                addDependencyIfNotCyclic(multimap, resourceLocation, resourceLocation2);
            });
        });
        Set<ResourceLocation> set = Sets.newHashSet();
        tags.keySet().forEach((resourceLocation) -> {
            visitDependenciesAndElement(tags, multimap, set, resourceLocation, (resourceLocationx, builder) -> {
                builder.build(function, function2).ifLeft((collection) -> {
                    LOGGER.error("Couldn't load tag {} as it is missing following references: {}", resourceLocationx, collection.stream().map(Objects::toString).collect(Collectors.joining(", ")));
                }).ifRight((tag) -> {
                    map.put(resourceLocationx, tag);
                });
            });
        });
        return map;
    }

    public Map<ResourceLocation, Tag<T>> loadAndBuild(ResourceManager manager) {
        return this.build(this.load(manager));
    }
}