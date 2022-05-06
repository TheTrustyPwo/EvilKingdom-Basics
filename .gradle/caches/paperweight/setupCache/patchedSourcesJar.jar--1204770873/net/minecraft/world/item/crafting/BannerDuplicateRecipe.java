package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;

public class BannerDuplicateRecipe extends CustomRecipe {
    public BannerDuplicateRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        DyeColor dyeColor = null;
        ItemStack itemStack = null;
        ItemStack itemStack2 = null;

        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemStack3 = inventory.getItem(i);
            if (!itemStack3.isEmpty()) {
                Item item = itemStack3.getItem();
                if (!(item instanceof BannerItem)) {
                    return false;
                }

                BannerItem bannerItem = (BannerItem)item;
                if (dyeColor == null) {
                    dyeColor = bannerItem.getColor();
                } else if (dyeColor != bannerItem.getColor()) {
                    return false;
                }

                int j = BannerBlockEntity.getPatternCount(itemStack3);
                if (j > 6) {
                    return false;
                }

                if (j > 0) {
                    if (itemStack != null) {
                        return false;
                    }

                    itemStack = itemStack3;
                } else {
                    if (itemStack2 != null) {
                        return false;
                    }

                    itemStack2 = itemStack3;
                }
            }
        }

        return itemStack != null && itemStack2 != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory) {
        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                int j = BannerBlockEntity.getPatternCount(itemStack);
                if (j > 0 && j <= 6) {
                    ItemStack itemStack2 = itemStack.copy();
                    itemStack2.setCount(1);
                    return itemStack2;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inventory) {
        NonNullList<ItemStack> nonNullList = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);

        for(int i = 0; i < nonNullList.size(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                if (itemStack.getItem().hasCraftingRemainingItem()) {
                    nonNullList.set(i, new ItemStack(itemStack.getItem().getCraftingRemainingItem()));
                } else if (itemStack.hasTag() && BannerBlockEntity.getPatternCount(itemStack) > 0) {
                    ItemStack itemStack2 = itemStack.copy();
                    itemStack2.setCount(1);
                    nonNullList.set(i, itemStack2);
                }
            }
        }

        return nonNullList;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.BANNER_DUPLICATE;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }
}