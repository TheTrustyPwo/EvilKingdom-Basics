package net.minecraft.world.inventory.tooltip;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public class BundleTooltip implements TooltipComponent {
    private final NonNullList<ItemStack> items;
    private final int weight;

    public BundleTooltip(NonNullList<ItemStack> inventory, int bundleOccupancy) {
        this.items = inventory;
        this.weight = bundleOccupancy;
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public int getWeight() {
        return this.weight;
    }
}
