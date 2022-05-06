package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public class RepairItemRecipe extends CustomRecipe {
    public RepairItemRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        List<ItemStack> list = Lists.newArrayList();

        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                list.add(itemStack);
                if (list.size() > 1) {
                    ItemStack itemStack2 = list.get(0);
                    if (!itemStack.is(itemStack2.getItem()) || itemStack2.getCount() != 1 || itemStack.getCount() != 1 || !itemStack2.getItem().canBeDepleted()) {
                        return false;
                    }
                }
            }
        }

        return list.size() == 2;
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory) {
        List<ItemStack> list = Lists.newArrayList();

        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                list.add(itemStack);
                if (list.size() > 1) {
                    ItemStack itemStack2 = list.get(0);
                    if (!itemStack.is(itemStack2.getItem()) || itemStack2.getCount() != 1 || itemStack.getCount() != 1 || !itemStack2.getItem().canBeDepleted()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (list.size() == 2) {
            ItemStack itemStack3 = list.get(0);
            ItemStack itemStack4 = list.get(1);
            if (itemStack3.is(itemStack4.getItem()) && itemStack3.getCount() == 1 && itemStack4.getCount() == 1 && itemStack3.getItem().canBeDepleted()) {
                Item item = itemStack3.getItem();
                int j = item.getMaxDamage() - itemStack3.getDamageValue();
                int k = item.getMaxDamage() - itemStack4.getDamageValue();
                int l = j + k + item.getMaxDamage() * 5 / 100;
                int m = item.getMaxDamage() - l;
                if (m < 0) {
                    m = 0;
                }

                ItemStack itemStack5 = new ItemStack(itemStack3.getItem());
                itemStack5.setDamageValue(m);
                Map<Enchantment, Integer> map = Maps.newHashMap();
                Map<Enchantment, Integer> map2 = EnchantmentHelper.getEnchantments(itemStack3);
                Map<Enchantment, Integer> map3 = EnchantmentHelper.getEnchantments(itemStack4);
                Registry.ENCHANTMENT.stream().filter(Enchantment::isCurse).forEach((enchantment) -> {
                    int i = Math.max(map2.getOrDefault(enchantment, 0), map3.getOrDefault(enchantment, 0));
                    if (i > 0) {
                        map.put(enchantment, i);
                    }

                });
                if (!map.isEmpty()) {
                    EnchantmentHelper.setEnchantments(map, itemStack5);
                }

                return itemStack5;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.REPAIR_ITEM;
    }
}