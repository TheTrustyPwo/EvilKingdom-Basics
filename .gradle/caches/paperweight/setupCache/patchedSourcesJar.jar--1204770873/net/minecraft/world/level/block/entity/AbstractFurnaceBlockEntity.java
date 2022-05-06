package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
// CraftBukkit start
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.CookingRecipe;
// CraftBukkit end

public abstract class AbstractFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeHolder, StackedContentsCompatible {

    protected static final int SLOT_INPUT = 0;
    protected static final int SLOT_FUEL = 1;
    protected static final int SLOT_RESULT = 2;
    public static final int DATA_LIT_TIME = 0;
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_COOKING_PROGRESS = 2;
    public static final int DATA_COOKING_TOTAL_TIME = 3;
    public static final int NUM_DATA_VALUES = 4;
    public static final int BURN_TIME_STANDARD = 200;
    public static final int BURN_COOL_SPEED = 2;
    protected NonNullList<ItemStack> items;
    public int litTime;
    int litDuration;
    public double cookSpeedMultiplier = 1.0; // Paper - cook speed multiplier API
    public int cookingProgress;
    public int cookingTotalTime;
    protected final ContainerData dataAccess;
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed;
    public final RecipeType<? extends AbstractCookingRecipe> recipeType;

    protected AbstractFurnaceBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state, RecipeType<? extends AbstractCookingRecipe> recipeType) {
        super(blockEntityType, pos, state);
        this.items = NonNullList.withSize(3, ItemStack.EMPTY);
        this.dataAccess = new ContainerData() {
            @Override
            public int get(int index) {
                switch (index) {
                    case 0:
                        return AbstractFurnaceBlockEntity.this.litTime;
                    case 1:
                        return AbstractFurnaceBlockEntity.this.litDuration;
                    case 2:
                        return AbstractFurnaceBlockEntity.this.cookingProgress;
                    case 3:
                        return AbstractFurnaceBlockEntity.this.cookingTotalTime;
                    default:
                        return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0:
                        AbstractFurnaceBlockEntity.this.litTime = value;
                        break;
                    case 1:
                        AbstractFurnaceBlockEntity.this.litDuration = value;
                        break;
                    case 2:
                        AbstractFurnaceBlockEntity.this.cookingProgress = value;
                        break;
                    case 3:
                        AbstractFurnaceBlockEntity.this.cookingTotalTime = value;
                }

            }

            @Override
            public int getCount() {
                return 4;
            }
        };
        this.recipesUsed = new Object2IntOpenHashMap();
        this.recipeType = recipeType;
    }

    private static Map<Item, Integer> cachedBurnDurations = null; // Paper - cache burn durations
    public static Map<Item, Integer> getFuel() {
        // Paper start - cache burn durations
        if(cachedBurnDurations != null) {
            return cachedBurnDurations;
        }
        // Paper end
        Map<Item, Integer> map = Maps.newLinkedHashMap();

        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.LAVA_BUCKET, 20000);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.COAL_BLOCK, 16000);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.BLAZE_ROD, 2400);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.COAL, 1600);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.CHARCOAL, 1600);
        AbstractFurnaceBlockEntity.add(map, ItemTags.LOGS, 300);
        AbstractFurnaceBlockEntity.add(map, ItemTags.PLANKS, 300);
        AbstractFurnaceBlockEntity.add(map, ItemTags.WOODEN_STAIRS, 300);
        AbstractFurnaceBlockEntity.add(map, ItemTags.WOODEN_SLABS, 150);
        AbstractFurnaceBlockEntity.add(map, ItemTags.WOODEN_TRAPDOORS, 300);
        AbstractFurnaceBlockEntity.add(map, ItemTags.WOODEN_PRESSURE_PLATES, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.OAK_FENCE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.BIRCH_FENCE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.SPRUCE_FENCE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.JUNGLE_FENCE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.DARK_OAK_FENCE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.ACACIA_FENCE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.OAK_FENCE_GATE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.BIRCH_FENCE_GATE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.SPRUCE_FENCE_GATE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.JUNGLE_FENCE_GATE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.DARK_OAK_FENCE_GATE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.ACACIA_FENCE_GATE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.NOTE_BLOCK, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.BOOKSHELF, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.LECTERN, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.JUKEBOX, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.CHEST, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.TRAPPED_CHEST, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.CRAFTING_TABLE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.DAYLIGHT_DETECTOR, 300);
        AbstractFurnaceBlockEntity.add(map, ItemTags.BANNERS, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.BOW, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.FISHING_ROD, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.LADDER, 300);
        AbstractFurnaceBlockEntity.add(map, ItemTags.SIGNS, 200);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.WOODEN_SHOVEL, 200);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.WOODEN_SWORD, 200);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.WOODEN_HOE, 200);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.WOODEN_AXE, 200);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.WOODEN_PICKAXE, 200);
        AbstractFurnaceBlockEntity.add(map, ItemTags.WOODEN_DOORS, 200);
        AbstractFurnaceBlockEntity.add(map, ItemTags.BOATS, 1200);
        AbstractFurnaceBlockEntity.add(map, ItemTags.WOOL, 100);
        AbstractFurnaceBlockEntity.add(map, ItemTags.WOODEN_BUTTONS, 100);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.STICK, 100);
        AbstractFurnaceBlockEntity.add(map, ItemTags.SAPLINGS, 100);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.BOWL, 100);
        AbstractFurnaceBlockEntity.add(map, ItemTags.CARPETS, 67);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.DRIED_KELP_BLOCK, 4001);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Items.CROSSBOW, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.BAMBOO, 50);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.DEAD_BUSH, 100);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.SCAFFOLDING, 400);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.LOOM, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.BARREL, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.CARTOGRAPHY_TABLE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.FLETCHING_TABLE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.SMITHING_TABLE, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.COMPOSTER, 300);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.AZALEA, 100);
        AbstractFurnaceBlockEntity.add(map, (ItemLike) Blocks.FLOWERING_AZALEA, 100);
        // Paper start - cache burn durations
        cachedBurnDurations = com.google.common.collect.ImmutableMap.copyOf(map);
        return cachedBurnDurations;
        // Paper end
    }

    // CraftBukkit start - add fields and methods
    private int maxStack = MAX_STACK;
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();

    public List<ItemStack> getContents() {
        return this.items;
    }

    public void onOpen(CraftHumanEntity who) {
        this.transaction.add(who);
    }

    public void onClose(CraftHumanEntity who) {
        this.transaction.remove(who);
    }

    public List<HumanEntity> getViewers() {
        return this.transaction;
    }

    @Override
    public int getMaxStackSize() {
        return this.maxStack;
    }

    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    public Object2IntOpenHashMap<ResourceLocation> getRecipesUsed() {
        return this.recipesUsed; // PAIL private -> public
    }
    // CraftBukkit end

    private static boolean isNeverAFurnaceFuel(Item item) {
        return item.builtInRegistryHolder().is(ItemTags.NON_FLAMMABLE_WOOD);
    }

    private static void add(Map<Item, Integer> fuelTimes, TagKey<Item> tag, int fuelTime) {
        Iterator iterator = Registry.ITEM.getTagOrEmpty(tag).iterator();

        while (iterator.hasNext()) {
            Holder<Item> holder = (Holder) iterator.next();

            if (!AbstractFurnaceBlockEntity.isNeverAFurnaceFuel((Item) holder.value())) {
                fuelTimes.put((Item) holder.value(), fuelTime);
            }
        }

    }

    private static void add(Map<Item, Integer> fuelTimes, ItemLike item, int fuelTime) {
        Item item1 = item.asItem();

        if (AbstractFurnaceBlockEntity.isNeverAFurnaceFuel(item1)) {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                throw (IllegalStateException) Util.pauseInIde(new IllegalStateException("A developer tried to explicitly make fire resistant item " + item1.getName((ItemStack) null).getString() + " a furnace fuel. That will not work!"));
            }
        } else {
            fuelTimes.put(item1, fuelTime);
        }
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, this.items);
        this.litTime = nbt.getShort("BurnTime");
        this.cookingProgress = nbt.getShort("CookTime");
        this.cookingTotalTime = nbt.getShort("CookTimeTotal");
        this.litDuration = this.getBurnDuration((ItemStack) this.items.get(1));
        CompoundTag nbttagcompound1 = nbt.getCompound("RecipesUsed");
        Iterator iterator = nbttagcompound1.getAllKeys().iterator();

        while (iterator.hasNext()) {
            String s = (String) iterator.next();

            this.recipesUsed.put(new ResourceLocation(s), nbttagcompound1.getInt(s));
        }

        // Paper start - cook speed API
        if (nbt.contains("Paper.CookSpeedMultiplier")) {
            this.cookSpeedMultiplier = nbt.getDouble("Paper.CookSpeedMultiplier");
        }
        // Paper end
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putShort("BurnTime", (short) this.litTime);
        nbt.putShort("CookTime", (short) this.cookingProgress);
        nbt.putShort("CookTimeTotal", (short) this.cookingTotalTime);
        nbt.putDouble("Paper.CookSpeedMultiplier", this.cookSpeedMultiplier); // Paper - cook speed multiplier API
        ContainerHelper.saveAllItems(nbt, this.items);
        CompoundTag nbttagcompound1 = new CompoundTag();

        this.recipesUsed.forEach((minecraftkey, integer) -> {
            nbttagcompound1.putInt(minecraftkey.toString(), integer);
        });
        nbt.put("RecipesUsed", nbttagcompound1);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity) {
        boolean flag = blockEntity.isLit();
        boolean flag1 = false;

        if (blockEntity.isLit()) {
            --blockEntity.litTime;
        }

        ItemStack itemstack = (ItemStack) blockEntity.items.get(1);

        if (!blockEntity.isLit() && (itemstack.isEmpty() || ((ItemStack) blockEntity.items.get(0)).isEmpty())) {
            if (!blockEntity.isLit() && blockEntity.cookingProgress > 0) {
                blockEntity.cookingProgress = Mth.clamp(blockEntity.cookingProgress - 2, (int) 0, blockEntity.cookingTotalTime);
            }
        } else {
            Recipe<?> irecipe = (Recipe) world.getRecipeManager().getRecipeFor((RecipeType<AbstractCookingRecipe>) blockEntity.recipeType, blockEntity, world).orElse(null); // CraftBukkit - decompile error // Eclipse fail
            int i = blockEntity.getMaxStackSize();

            if (!blockEntity.isLit() && AbstractFurnaceBlockEntity.canBurn(irecipe, blockEntity.items, i)) {
                // CraftBukkit start
                CraftItemStack fuel = CraftItemStack.asCraftMirror(itemstack);

                FurnaceBurnEvent furnaceBurnEvent = new FurnaceBurnEvent(CraftBlock.at(world, pos), fuel, blockEntity.getBurnDuration(itemstack));
                world.getCraftServer().getPluginManager().callEvent(furnaceBurnEvent);

                if (furnaceBurnEvent.isCancelled()) {
                    return;
                }

                blockEntity.litTime = furnaceBurnEvent.getBurnTime();
                blockEntity.litDuration = blockEntity.litTime;
                if (blockEntity.isLit() && furnaceBurnEvent.isBurning()) {
                    // CraftBukkit end
                    flag1 = true;
                    if (!itemstack.isEmpty() && furnaceBurnEvent.willConsumeFuel()) { // Paper
                        Item item = itemstack.getItem();

                        itemstack.shrink(1);
                        if (itemstack.isEmpty()) {
                            Item item1 = item.getCraftingRemainingItem();

                            blockEntity.items.set(1, item1 == null ? ItemStack.EMPTY : new ItemStack(item1));
                        }
                    }
                }
            }

            if (blockEntity.isLit() && AbstractFurnaceBlockEntity.canBurn(irecipe, blockEntity.items, i)) {
                // CraftBukkit start
                if (irecipe != null && blockEntity.cookingProgress == 0) {
                    CraftItemStack source = CraftItemStack.asCraftMirror(blockEntity.items.get(0));
                    CookingRecipe<?> recipe = (CookingRecipe<?>) irecipe.toBukkitRecipe();

                    FurnaceStartSmeltEvent event = new FurnaceStartSmeltEvent(CraftBlock.at(world, pos), source, recipe, AbstractFurnaceBlockEntity.getTotalCookTime(world, blockEntity.recipeType, blockEntity, blockEntity.cookSpeedMultiplier)); // Paper - cook speed multiplier API
                    world.getCraftServer().getPluginManager().callEvent(event);

                    blockEntity.cookingTotalTime = event.getTotalCookTime();
                }
                // CraftBukkit end

                ++blockEntity.cookingProgress;
                if (blockEntity.cookingProgress >= blockEntity.cookingTotalTime) { // Paper - cook speed multiplier API
                    blockEntity.cookingProgress = 0;
                    blockEntity.cookingTotalTime = AbstractFurnaceBlockEntity.getTotalCookTime(world, blockEntity.recipeType, blockEntity, blockEntity.cookSpeedMultiplier);
                    if (AbstractFurnaceBlockEntity.burn(blockEntity.level, blockEntity.worldPosition, irecipe, blockEntity.items, i)) { // CraftBukkit
                        blockEntity.setRecipeUsed(irecipe);
                    }

                    flag1 = true;
                }
            } else {
                blockEntity.cookingProgress = 0;
            }
        }

        if (flag != blockEntity.isLit()) {
            flag1 = true;
            state = (BlockState) state.setValue(AbstractFurnaceBlock.LIT, blockEntity.isLit());
            world.setBlock(pos, state, 3);
        }

        if (flag1) {
            setChanged(world, pos, state);
        }

    }

    private static boolean canBurn(@Nullable Recipe<?> recipe, NonNullList<ItemStack> slots, int count) {
        if (!((ItemStack) slots.get(0)).isEmpty() && recipe != null) {
            ItemStack itemstack = recipe.getResultItem();

            if (itemstack.isEmpty()) {
                return false;
            } else {
                ItemStack itemstack1 = (ItemStack) slots.get(2);

                return itemstack1.isEmpty() ? true : (!itemstack1.sameItem(itemstack) ? false : (itemstack1.getCount() < count && itemstack1.getCount() < itemstack1.getMaxStackSize() ? true : itemstack1.getCount() < itemstack.getMaxStackSize()));
            }
        } else {
            return false;
        }
    }

    private static boolean burn(Level world, BlockPos blockposition, @Nullable Recipe<?> irecipe, NonNullList<ItemStack> nonnulllist, int i) { // CraftBukkit
        if (irecipe != null && AbstractFurnaceBlockEntity.canBurn(irecipe, nonnulllist, i)) {
            ItemStack itemstack = (ItemStack) nonnulllist.get(0);
            ItemStack itemstack1 = irecipe.getResultItem();
            ItemStack itemstack2 = (ItemStack) nonnulllist.get(2);

            // CraftBukkit start - fire FurnaceSmeltEvent
            CraftItemStack source = CraftItemStack.asCraftMirror(itemstack);
            org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(itemstack1);

            FurnaceSmeltEvent furnaceSmeltEvent = new FurnaceSmeltEvent(CraftBlock.at(world, blockposition), source, result, (org.bukkit.inventory.CookingRecipe<?>) irecipe.toBukkitRecipe()); // Paper
            world.getCraftServer().getPluginManager().callEvent(furnaceSmeltEvent);

            if (furnaceSmeltEvent.isCancelled()) {
                return false;
            }

            result = furnaceSmeltEvent.getResult();
            itemstack1 = CraftItemStack.asNMSCopy(result);

            if (!itemstack1.isEmpty()) {
                if (itemstack2.isEmpty()) {
                    nonnulllist.set(2, itemstack1.copy());
                } else if (CraftItemStack.asCraftMirror(itemstack2).isSimilar(result)) {
                    itemstack2.grow(itemstack1.getCount());
                } else {
                    return false;
                }
            }

            /*
            if (itemstack2.isEmpty()) {
                nonnulllist.set(2, itemstack1.copy());
            } else if (itemstack2.is(itemstack1.getItem())) {
                itemstack2.grow(1);
            }
            */
            // CraftBukkit end

            if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !((ItemStack) nonnulllist.get(1)).isEmpty() && ((ItemStack) nonnulllist.get(1)).is(Items.BUCKET)) {
                nonnulllist.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
            return true;
        } else {
            return false;
        }
    }

    protected int getBurnDuration(ItemStack fuel) {
        if (fuel.isEmpty()) {
            return 0;
        } else {
            Item item = fuel.getItem();

            return (Integer) AbstractFurnaceBlockEntity.getFuel().getOrDefault(item, 0);
        }
    }

    // Paper begin - Expose this function so CraftFurnace can correctly scale the total cooking time to a new multiplier
    public static int getTotalCookTime(@Nullable Level world, RecipeType<? extends AbstractCookingRecipe> recipeType, Container inventory, final double cookSpeedMultiplier) {
        /* Scale the recipe's cooking time to the current cookSpeedMultiplier */
        int cookTime = (world != null ? world.getRecipeManager() : net.minecraft.server.MinecraftServer.getServer().getRecipeManager()).getRecipeFor(recipeType, inventory, world /* passing a null level here is safe. world is only used for map extending recipes which won't happen here */).map(AbstractCookingRecipe::getCookingTime).orElse(200); // CraftBukkit - SPIGOT-4302 // Eclipse fail
        return (int) Math.ceil (cookTime / cookSpeedMultiplier);
    }
    // Paper end

    public static boolean isFuel(ItemStack stack) {
        return AbstractFurnaceBlockEntity.getFuel().containsKey(stack.getItem());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? AbstractFurnaceBlockEntity.SLOTS_FOR_DOWN : (side == Direction.UP ? AbstractFurnaceBlockEntity.SLOTS_FOR_UP : AbstractFurnaceBlockEntity.SLOTS_FOR_SIDES);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return dir == Direction.DOWN && slot == 1 ? stack.is(Items.WATER_BUCKET) || stack.is(Items.BUCKET) : true;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        Iterator iterator = this.items.iterator();

        ItemStack itemstack;

        do {
            if (!iterator.hasNext()) {
                return true;
            }

            itemstack = (ItemStack) iterator.next();
        } while (itemstack.isEmpty());

        return false;
    }

    @Override
    public ItemStack getItem(int slot) {
        return (ItemStack) this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack itemstack1 = (ItemStack) this.items.get(slot);
        boolean flag = !stack.isEmpty() && stack.sameItem(itemstack1) && ItemStack.tagMatches(stack, itemstack1);

        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        if (slot == 0 && !flag) {
            this.cookingTotalTime = AbstractFurnaceBlockEntity.getTotalCookTime(this.level, this.recipeType, this, this.cookSpeedMultiplier);
            this.cookingProgress = 0;
            this.setChanged();
        }

    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return this.level.getBlockEntity(this.worldPosition) != this ? false : player.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 2) {
            return false;
        } else if (slot != 1) {
            return true;
        } else {
            ItemStack itemstack1 = (ItemStack) this.items.get(1);

            return AbstractFurnaceBlockEntity.isFuel(stack) || stack.is(Items.BUCKET) && !itemstack1.is(Items.BUCKET);
        }
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public void setRecipeUsed(@Nullable Recipe<?> recipe) {
        if (recipe != null) {
            ResourceLocation minecraftkey = recipe.getId();

            this.recipesUsed.addTo(minecraftkey, 1);
        }

    }

    @Nullable
    @Override
    public Recipe<?> getRecipeUsed() {
        return null;
    }

    @Override
    public void awardUsedRecipes(net.minecraft.world.entity.player.Player player) {}

    public void awardUsedRecipesAndPopExperience(ServerPlayer entityplayer, ItemStack itemstack, int amount) { // CraftBukkit
        List<Recipe<?>> list = this.getRecipesToAwardAndPopExperience(entityplayer.getLevel(), entityplayer.position(), this.worldPosition, entityplayer, itemstack, amount); // CraftBukkit

        entityplayer.awardRecipes(list);
        this.recipesUsed.clear();
    }

    public List<Recipe<?>> getRecipesToAwardAndPopExperience(ServerLevel world, Vec3 pos) {
        // CraftBukkit start
        return this.getRecipesToAwardAndPopExperience(world, pos, this.worldPosition, null, null, 0);
    }

    public List<Recipe<?>> getRecipesToAwardAndPopExperience(ServerLevel worldserver, Vec3 vec3d, BlockPos blockposition, ServerPlayer entityplayer, ItemStack itemstack, int amount) {
        // CraftBukkit end
        List<Recipe<?>> list = Lists.newArrayList();
        ObjectIterator objectiterator = this.recipesUsed.object2IntEntrySet().iterator();

        while (objectiterator.hasNext()) {
            Entry<ResourceLocation> entry = (Entry) objectiterator.next();

            worldserver.getRecipeManager().byKey((ResourceLocation) entry.getKey()).ifPresent((irecipe) -> {
                list.add(irecipe);
                AbstractFurnaceBlockEntity.createExperience(worldserver, vec3d, entry.getIntValue(), ((AbstractCookingRecipe) irecipe).getExperience(), blockposition, entityplayer, itemstack, amount); // CraftBukkit
            });
        }

        return list;
    }

    private static void createExperience(ServerLevel worldserver, Vec3 vec3d, int i, float f, BlockPos blockposition, net.minecraft.world.entity.player.Player entityhuman, ItemStack itemstack, int amount) { // CraftBukkit
        int j = Mth.floor((float) i * f);
        float f1 = Mth.frac((float) i * f);

        if (f1 != 0.0F && Math.random() < (double) f1) {
            ++j;
        }

        // CraftBukkit start - fire FurnaceExtractEvent / BlockExpEvent
        BlockExpEvent event;
        if (amount != 0) {
            event = new FurnaceExtractEvent((Player) entityhuman.getBukkitEntity(), CraftBlock.at(worldserver, blockposition), org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers.getMaterial(itemstack.getItem()), amount, j);
        } else {
            event = new BlockExpEvent(CraftBlock.at(worldserver, blockposition), j);
        }
        worldserver.getCraftServer().getPluginManager().callEvent(event);
        j = event.getExpToDrop();
        // CraftBukkit end

        ExperienceOrb.award(worldserver, vec3d, j, org.bukkit.entity.ExperienceOrb.SpawnReason.FURNACE, entityhuman); // Paper
    }

    @Override
    public void fillStackedContents(StackedContents finder) {
        Iterator iterator = this.items.iterator();

        while (iterator.hasNext()) {
            ItemStack itemstack = (ItemStack) iterator.next();

            finder.accountStack(itemstack);
        }

    }
}