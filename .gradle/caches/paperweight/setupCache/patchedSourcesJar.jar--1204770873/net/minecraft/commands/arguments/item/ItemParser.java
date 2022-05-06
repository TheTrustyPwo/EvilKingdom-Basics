package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ItemParser {
    public static final SimpleCommandExceptionType ERROR_NO_TAGS_ALLOWED = new SimpleCommandExceptionType(new TranslatableComponent("argument.item.tag.disallowed"));
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("argument.item.id.invalid", id);
    });
    private static final char SYNTAX_START_NBT = '{';
    private static final char SYNTAX_TAG = '#';
    private static final BiFunction<SuggestionsBuilder, Registry<Item>, CompletableFuture<Suggestions>> SUGGEST_NOTHING = (builder, registry) -> {
        return builder.buildFuture();
    };
    private final StringReader reader;
    private final boolean forTesting;
    private Item item;
    @Nullable
    private CompoundTag nbt;
    @Nullable
    private TagKey<Item> tag;
    private int tagCursor;
    private BiFunction<SuggestionsBuilder, Registry<Item>, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

    public ItemParser(StringReader reader, boolean allowTag) {
        this.reader = reader;
        this.forTesting = allowTag;
    }

    public Item getItem() {
        return this.item;
    }

    @Nullable
    public CompoundTag getNbt() {
        return this.nbt;
    }

    public TagKey<Item> getTag() {
        return this.tag;
    }

    public void readItem() throws CommandSyntaxException {
        int i = this.reader.getCursor();
        ResourceLocation resourceLocation = ResourceLocation.read(this.reader);
        this.item = Registry.ITEM.getOptional(resourceLocation).orElseThrow(() -> {
            this.reader.setCursor(i);
            return ERROR_UNKNOWN_ITEM.createWithContext(this.reader, resourceLocation.toString());
        });
    }

    public void readTag() throws CommandSyntaxException {
        if (!this.forTesting) {
            throw ERROR_NO_TAGS_ALLOWED.create();
        } else {
            this.suggestions = this::suggestTag;
            this.reader.expect('#');
            this.tagCursor = this.reader.getCursor();
            this.tag = TagKey.create(Registry.ITEM_REGISTRY, ResourceLocation.read(this.reader));
        }
    }

    public void readNbt() throws CommandSyntaxException {
        this.nbt = (new TagParser(this.reader)).readStruct();
    }

    public ItemParser parse() throws CommandSyntaxException {
        this.suggestions = this::suggestItemIdOrTag;
        if (this.reader.canRead() && this.reader.peek() == '#') {
            this.readTag();
        } else {
            this.readItem();
            this.suggestions = this::suggestOpenNbt;
        }

        if (this.reader.canRead() && this.reader.peek() == '{') {
            this.suggestions = SUGGEST_NOTHING;
            this.readNbt();
        }

        return this;
    }

    private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder builder, Registry<Item> registry) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf('{'));
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder builder, Registry<Item> registry) {
        return SharedSuggestionProvider.suggestResource(registry.getTagNames().map(TagKey::location), builder.createOffset(this.tagCursor));
    }

    private CompletableFuture<Suggestions> suggestItemIdOrTag(SuggestionsBuilder builder, Registry<Item> registry) {
        if (this.forTesting) {
            SharedSuggestionProvider.suggestResource(registry.getTagNames().map(TagKey::location), builder, String.valueOf('#'));
        }

        return SharedSuggestionProvider.suggestResource(Registry.ITEM.keySet(), builder);
    }

    public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder builder, Registry<Item> registry) {
        return this.suggestions.apply(builder.createOffset(this.reader.getCursor()), registry);
    }
}