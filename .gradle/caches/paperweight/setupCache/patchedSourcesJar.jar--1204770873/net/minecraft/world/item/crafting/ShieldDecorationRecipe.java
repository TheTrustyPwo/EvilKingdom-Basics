package net.minecraft.world.item.crafting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ShieldDecorationRecipe extends CustomRecipe {
    public ShieldDecorationRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        ItemStack itemStack = ItemStack.EMPTY;
        ItemStack itemStack2 = ItemStack.EMPTY;

        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemStack3 = inventory.getItem(i);
            if (!itemStack3.isEmpty()) {
                if (itemStack3.getItem() instanceof BannerItem) {
                    if (!itemStack2.isEmpty()) {
                        return false;
                    }

                    itemStack2 = itemStack3;
                } else {
                    if (!itemStack3.is(Items.SHIELD)) {
                        return false;
                    }

                    if (!itemStack.isEmpty()) {
                        return false;
                    }

                    if (BlockItem.getBlockEntityData(itemStack3) != null) {
                        return false;
                    }

                    itemStack = itemStack3;
                }
            }
        }

        return !itemStack.isEmpty() && !itemStack2.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory) {
        ItemStack itemStack = ItemStack.EMPTY;
        ItemStack itemStack2 = ItemStack.EMPTY;

        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemStack3 = inventory.getItem(i);
            if (!itemStack3.isEmpty()) {
                if (itemStack3.getItem() instanceof BannerItem) {
                    itemStack = itemStack3;
                } else if (itemStack3.is(Items.SHIELD)) {
                    itemStack2 = itemStack3.copy();
                }
            }
        }

        if (itemStack2.isEmpty()) {
            return itemStack2;
        } else {
            CompoundTag compoundTag = BlockItem.getBlockEntityData(itemStack);
            CompoundTag compoundTag2 = compoundTag == null ? new CompoundTag() : compoundTag.copy();
            compoundTag2.putInt("Base", ((BannerItem)itemStack.getItem()).getColor().getId());
            BlockItem.setBlockEntityData(itemStack2, BlockEntityType.BANNER, compoundTag2);
            return itemStack2;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.SHIELD_DECORATION;
    }
}