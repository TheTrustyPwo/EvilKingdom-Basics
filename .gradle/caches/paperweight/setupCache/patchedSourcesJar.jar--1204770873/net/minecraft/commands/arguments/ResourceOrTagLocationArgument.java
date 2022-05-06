package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;

public class ResourceOrTagLocationArgument<T> implements ArgumentType<ResourceOrTagLocationArgument.Result<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
    private static final DynamicCommandExceptionType ERROR_INVALID_BIOME = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("commands.locatebiome.invalid", id);
    });
    private static final DynamicCommandExceptionType ERROR_INVALID_STRUCTURE = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("commands.locate.invalid", id);
    });
    final ResourceKey<? extends Registry<T>> registryKey;

    public ResourceOrTagLocationArgument(ResourceKey<? extends Registry<T>> registryRef) {
        this.registryKey = registryRef;
    }

    public static <T> ResourceOrTagLocationArgument<T> resourceOrTag(ResourceKey<? extends Registry<T>> registryRef) {
        return new ResourceOrTagLocationArgument<>(registryRef);
    }

    private static <T> ResourceOrTagLocationArgument.Result<T> getRegistryType(CommandContext<CommandSourceStack> context, String name, ResourceKey<Registry<T>> registryRef, DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        ResourceOrTagLocationArgument.Result<?> result = context.getArgument(name, ResourceOrTagLocationArgument.Result.class);
        Optional<ResourceOrTagLocationArgument.Result<T>> optional = result.cast(registryRef);
        return optional.orElseThrow(() -> {
            return invalidException.create(result);
        });
    }

    public static ResourceOrTagLocationArgument.Result<Biome> getBiome(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return getRegistryType(context, name, Registry.BIOME_REGISTRY, ERROR_INVALID_BIOME);
    }

    public static ResourceOrTagLocationArgument.Result<ConfiguredStructureFeature<?, ?>> getStructureFeature(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return getRegistryType(context, name, Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, ERROR_INVALID_STRUCTURE);
    }

    public ResourceOrTagLocationArgument.Result<T> parse(StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '#') {
            int i = stringReader.getCursor();

            try {
                stringReader.skip();
                ResourceLocation resourceLocation = ResourceLocation.read(stringReader);
                return new ResourceOrTagLocationArgument.TagResult<>(TagKey.create(this.registryKey, resourceLocation));
            } catch (CommandSyntaxException var4) {
                stringReader.setCursor(i);
                throw var4;
            }
        } else {
            ResourceLocation resourceLocation2 = ResourceLocation.read(stringReader);
            return new ResourceOrTagLocationArgument.ResourceResult<>(ResourceKey.create(this.registryKey, resourceLocation2));
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        Object var4 = commandContext.getSource();
        if (var4 instanceof SharedSuggestionProvider) {
            SharedSuggestionProvider sharedSuggestionProvider = (SharedSuggestionProvider)var4;
            return sharedSuggestionProvider.suggestRegistryElements(this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ALL, suggestionsBuilder, commandContext);
        } else {
            return suggestionsBuilder.buildFuture();
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    static record ResourceResult<T>(ResourceKey<T> key) implements ResourceOrTagLocationArgument.Result<T> {
        @Override
        public Either<ResourceKey<T>, TagKey<T>> unwrap() {
            return Either.left(this.key);
        }

        @Override
        public <E> Optional<ResourceOrTagLocationArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> registryRef) {
            return this.key.cast(registryRef).map(ResourceOrTagLocationArgument.ResourceResult::new);
        }

        @Override
        public boolean test(Holder<T> holder) {
            return holder.is(this.key);
        }

        @Override
        public String asPrintable() {
            return this.key.location().toString();
        }
    }

    public interface Result<T> extends Predicate<Holder<T>> {
        Either<ResourceKey<T>, TagKey<T>> unwrap();

        <E> Optional<ResourceOrTagLocationArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> registryRef);

        String asPrintable();
    }

    public static class Serializer implements ArgumentSerializer<ResourceOrTagLocationArgument<?>> {
        @Override
        public void serializeToNetwork(ResourceOrTagLocationArgument<?> type, FriendlyByteBuf buf) {
            buf.writeResourceLocation(type.registryKey.location());
        }

        @Override
        public ResourceOrTagLocationArgument<?> deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
            ResourceLocation resourceLocation = friendlyByteBuf.readResourceLocation();
            return new ResourceOrTagLocationArgument(ResourceKey.createRegistryKey(resourceLocation));
        }

        @Override
        public void serializeToJson(ResourceOrTagLocationArgument<?> type, JsonObject json) {
            json.addProperty("registry", type.registryKey.location().toString());
        }
    }

    static record TagResult<T>(TagKey<T> key) implements ResourceOrTagLocationArgument.Result<T> {
        @Override
        public Either<ResourceKey<T>, TagKey<T>> unwrap() {
            return Either.right(this.key);
        }

        @Override
        public <E> Optional<ResourceOrTagLocationArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> registryRef) {
            return this.key.cast(registryRef).map(ResourceOrTagLocationArgument.TagResult::new);
        }

        @Override
        public boolean test(Holder<T> holder) {
            return holder.is(this.key);
        }

        @Override
        public String asPrintable() {
            return "#" + this.key.location();
        }
    }
}