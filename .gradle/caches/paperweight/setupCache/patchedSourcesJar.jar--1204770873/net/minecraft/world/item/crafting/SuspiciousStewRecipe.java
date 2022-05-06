package net.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;

public class SuspiciousStewRecipe extends CustomRecipe {
    public SuspiciousStewRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        boolean bl = false;
        boolean bl2 = false;
        boolean bl3 = false;
        boolean bl4 = false;

        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                if (itemStack.is(Blocks.BROWN_MUSHROOM.asItem()) && !bl3) {
                    bl3 = true;
                } else if (itemStack.is(Blocks.RED_MUSHROOM.asItem()) && !bl2) {
                    bl2 = true;
                } else if (itemStack.is(ItemTags.SMALL_FLOWERS) && !bl) {
                    bl = true;
                } else {
                    if (!itemStack.is(Items.BOWL) || bl4) {
                        return false;
                    }

                    bl4 = true;
                }
            }
        }

        return bl && bl3 && bl2 && bl4;
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory) {
        ItemStack itemStack = ItemStack.EMPTY;

        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemStack2 = inventory.getItem(i);
            if (!itemStack2.isEmpty() && itemStack2.is(ItemTags.SMALL_FLOWERS)) {
                itemStack = itemStack2;
                break;
            }
        }

        ItemStack itemStack3 = new ItemStack(Items.SUSPICIOUS_STEW, 1);
        if (itemStack.getItem() instanceof BlockItem && ((BlockItem)itemStack.getItem()).getBlock() instanceof FlowerBlock) {
            FlowerBlock flowerBlock = (FlowerBlock)((BlockItem)itemStack.getItem()).getBlock();
            MobEffect mobEffect = flowerBlock.getSuspiciousStewEffect();
            SuspiciousStewItem.saveMobEffect(itemStack3, mobEffect, flowerBlock.getEffectDuration());
        }

        return itemStack3;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.SUSPICIOUS_STEW;
    }
}