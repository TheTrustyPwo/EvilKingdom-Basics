package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemPredicateArgument implements ArgumentType<ItemPredicateArgument.Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo=bar}");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("arguments.item.tag.unknown", id);
    });

    public static ItemPredicateArgument itemPredicate() {
        return new ItemPredicateArgument();
    }

    public ItemPredicateArgument.Result parse(StringReader stringReader) throws CommandSyntaxException {
        ItemParser itemParser = (new ItemParser(stringReader, true)).parse();
        if (itemParser.getItem() != null) {
            ItemPredicateArgument.ItemPredicate itemPredicate = new ItemPredicateArgument.ItemPredicate(itemParser.getItem(), itemParser.getNbt());
            return (context) -> {
                return itemPredicate;
            };
        } else {
            TagKey<Item> tagKey = itemParser.getTag();
            return (commandContext) -> {
                if (!Registry.ITEM.isKnownTagName(tagKey)) {
                    throw ERROR_UNKNOWN_TAG.create(tagKey);
                } else {
                    return new ItemPredicateArgument.TagPredicate(tagKey, itemParser.getNbt());
                }
            };
        }
    }

    public static Predicate<ItemStack> getItemPredicate(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, ItemPredicateArgument.Result.class).create(context);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        StringReader stringReader = new StringReader(suggestionsBuilder.getInput());
        stringReader.setCursor(suggestionsBuilder.getStart());
        ItemParser itemParser = new ItemParser(stringReader, true);

        try {
            itemParser.parse();
        } catch (CommandSyntaxException var6) {
        }

        return itemParser.fillSuggestions(suggestionsBuilder, Registry.ITEM);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    static class ItemPredicate implements Predicate<ItemStack> {
        private final Item item;
        @Nullable
        private final CompoundTag nbt;

        public ItemPredicate(Item item, @Nullable CompoundTag nbt) {
            this.item = item;
            this.nbt = nbt;
        }

        @Override
        public boolean test(ItemStack itemStack) {
            return itemStack.is(this.item) && NbtUtils.compareNbt(this.nbt, itemStack.getTag(), true);
        }
    }

    public interface Result {
        Predicate<ItemStack> create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }

    static class TagPredicate implements Predicate<ItemStack> {
        private final TagKey<Item> tag;
        @Nullable
        private final CompoundTag nbt;

        public TagPredicate(TagKey<Item> tag, @Nullable CompoundTag nbt) {
            this.tag = tag;
            this.nbt = nbt;
        }

        @Override
        public boolean test(ItemStack itemStack) {
            return itemStack.is(this.tag) && NbtUtils.compareNbt(this.nbt, itemStack.getTag(), true);
        }
    }
}