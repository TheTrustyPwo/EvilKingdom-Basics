package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NbtPredicate {
    public static final NbtPredicate ANY = new NbtPredicate((CompoundTag)null);
    @Nullable
    private final CompoundTag tag;

    public NbtPredicate(@Nullable CompoundTag nbt) {
        this.tag = nbt;
    }

    public boolean matches(ItemStack stack) {
        return this == ANY ? true : this.matches(stack.getTag());
    }

    public boolean matches(Entity entity) {
        return this == ANY ? true : this.matches(getEntityTagToCompare(entity));
    }

    public boolean matches(@Nullable Tag element) {
        if (element == null) {
            return this == ANY;
        } else {
            return this.tag == null || NbtUtils.compareNbt(this.tag, element, true);
        }
    }

    public JsonElement serializeToJson() {
        return (JsonElement)(this != ANY && this.tag != null ? new JsonPrimitive(this.tag.toString()) : JsonNull.INSTANCE);
    }

    public static NbtPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            CompoundTag compoundTag;
            try {
                compoundTag = TagParser.parseTag(GsonHelper.convertToString(json, "nbt"));
            } catch (CommandSyntaxException var3) {
                throw new JsonSyntaxException("Invalid nbt tag: " + var3.getMessage());
            }

            return new NbtPredicate(compoundTag);
        } else {
            return ANY;
        }
    }

    public static CompoundTag getEntityTagToCompare(Entity entity) {
        CompoundTag compoundTag = entity.saveWithoutId(new CompoundTag());
        if (entity instanceof Player) {
            ItemStack itemStack = ((Player)entity).getInventory().getSelected();
            if (!itemStack.isEmpty()) {
                compoundTag.put("SelectedItem", itemStack.save(new CompoundTag()));
            }
        }

        return compoundTag;
    }
}