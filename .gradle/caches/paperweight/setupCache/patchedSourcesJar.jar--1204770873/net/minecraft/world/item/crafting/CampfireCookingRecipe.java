package net.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

// CraftBukkit start
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftCampfireRecipe;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftNamespacedKey;
import org.bukkit.inventory.Recipe;
// CraftBukkit end

public class CampfireCookingRecipe extends AbstractCookingRecipe {

    public CampfireCookingRecipe(ResourceLocation id, String group, Ingredient input, ItemStack output, float experience, int cookTime) {
        super(RecipeType.CAMPFIRE_COOKING, id, group, input, output, experience, cookTime);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.CAMPFIRE);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.CAMPFIRE_COOKING_RECIPE;
    }

    // CraftBukkit start
    @Override
    public Recipe toBukkitRecipe() {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);

        CraftCampfireRecipe recipe = new CraftCampfireRecipe(CraftNamespacedKey.fromMinecraft(this.id), result, CraftRecipe.toBukkit(this.ingredient), this.experience, this.cookingTime);
        recipe.setGroup(this.group);

        return recipe;
    }
    // CraftBukkit end
}