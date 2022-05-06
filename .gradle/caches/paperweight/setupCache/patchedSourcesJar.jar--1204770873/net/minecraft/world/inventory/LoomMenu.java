package net.minecraft.world.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;
// CraftBukkit start
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryLoom;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryView;
import org.bukkit.entity.Player;
// CraftBukkit end

public class LoomMenu extends AbstractContainerMenu {

    // CraftBukkit start
    private CraftInventoryView bukkitEntity = null;
    private Player player;

    @Override
    public CraftInventoryView getBukkitView() {
        if (this.bukkitEntity != null) {
            return this.bukkitEntity;
        }

        CraftInventoryLoom inventory = new CraftInventoryLoom(this.inputContainer, this.outputContainer);
        this.bukkitEntity = new CraftInventoryView(this.player, inventory, this);
        return this.bukkitEntity;
    }
    // CraftBukkit end
    private static final int INV_SLOT_START = 4;
    private static final int INV_SLOT_END = 31;
    private static final int USE_ROW_SLOT_START = 31;
    private static final int USE_ROW_SLOT_END = 40;
    private final ContainerLevelAccess access;
    final DataSlot selectedBannerPatternIndex;
    Runnable slotUpdateListener;
    final Slot bannerSlot;
    final Slot dyeSlot;
    private final Slot patternSlot;
    private final Slot resultSlot;
    long lastSoundTime;
    private final Container inputContainer;
    private final Container outputContainer;

    public LoomMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, ContainerLevelAccess.NULL);
    }

    public LoomMenu(int syncId, Inventory playerInventory, final ContainerLevelAccess context) {
        super(MenuType.LOOM, syncId);
        this.selectedBannerPatternIndex = DataSlot.standalone();
        this.slotUpdateListener = () -> {
        };
        this.inputContainer = new SimpleContainer(3) {
            @Override
            public void setChanged() {
                super.setChanged();
                LoomMenu.this.slotsChanged(this);
                LoomMenu.this.slotUpdateListener.run();
            }

            // CraftBukkit start
            @Override
            public Location getLocation() {
                return context.getLocation();
            }
            // CraftBukkit end
        };
        this.outputContainer = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                LoomMenu.this.slotUpdateListener.run();
            }

            // CraftBukkit start
            @Override
            public Location getLocation() {
                return context.getLocation();
            }
            // CraftBukkit end
        };
        this.access = context;
        this.bannerSlot = this.addSlot(new Slot(this.inputContainer, 0, 13, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof BannerItem;
            }
        });
        this.dyeSlot = this.addSlot(new Slot(this.inputContainer, 1, 33, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DyeItem;
            }
        });
        this.patternSlot = this.addSlot(new Slot(this.inputContainer, 2, 23, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof BannerPatternItem;
            }
        });
        this.resultSlot = this.addSlot(new Slot(this.outputContainer, 0, 143, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(net.minecraft.world.entity.player.Player player, ItemStack stack) {
                LoomMenu.this.bannerSlot.remove(1);
                LoomMenu.this.dyeSlot.remove(1);
                if (!LoomMenu.this.bannerSlot.hasItem() || !LoomMenu.this.dyeSlot.hasItem()) {
                    LoomMenu.this.selectedBannerPatternIndex.set(0);
                }

                context.execute((world, blockposition) -> {
                    long j = world.getGameTime();

                    if (LoomMenu.this.lastSoundTime != j) {
                        world.playSound((net.minecraft.world.entity.player.Player) null, blockposition, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                        LoomMenu.this.lastSoundTime = j;
                    }

                });
                super.onTake(player, stack);
            }
        });

        int j;

        for (j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInventory, k + j * 9 + 9, 8 + k * 18, 84 + j * 18));
            }
        }

        for (j = 0; j < 9; ++j) {
            this.addSlot(new Slot(playerInventory, j, 8 + j * 18, 142));
        }

        this.addDataSlot(this.selectedBannerPatternIndex);
        this.player = (Player) playerInventory.player.getBukkitEntity(); // CraftBukkit
    }

    public int getSelectedBannerPatternIndex() {
        return this.selectedBannerPatternIndex.get();
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        if (!this.checkReachable) return true; // CraftBukkit
        return stillValid(this.access, player, Blocks.LOOM);
    }

    @Override
    public boolean clickMenuButton(net.minecraft.world.entity.player.Player player, int id) {
        if (id > 0 && id <= BannerPattern.AVAILABLE_PATTERNS) {
            // Paper start
            int enumBannerPatternTypeOrdinal = id;
            io.papermc.paper.event.player.PlayerLoomPatternSelectEvent event = new io.papermc.paper.event.player.PlayerLoomPatternSelectEvent((Player) player.getBukkitEntity(), ((CraftInventoryLoom) getBukkitView().getTopInventory()), org.bukkit.block.banner.PatternType.getByIdentifier(BannerPattern.values()[id].getHashname()));
            if (!event.callEvent()) {
                ((Player) player.getBukkitEntity()).updateInventory();
                return false;
            }
            for (BannerPattern nms : BannerPattern.values()) {
                if (event.getPatternType().getIdentifier().equals(nms.getHashname())) {
                    enumBannerPatternTypeOrdinal = nms.ordinal();
                    break;
                }
            }
            ((Player) player.getBukkitEntity()).updateInventory();
            this.selectedBannerPatternIndex.set(enumBannerPatternTypeOrdinal);
            // Paper end
            this.setupResultSlot();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void slotsChanged(Container inventory) {
        ItemStack itemstack = this.bannerSlot.getItem();
        ItemStack itemstack1 = this.dyeSlot.getItem();
        ItemStack itemstack2 = this.patternSlot.getItem();
        ItemStack itemstack3 = this.resultSlot.getItem();

        if (!itemstack3.isEmpty() && (itemstack.isEmpty() || itemstack1.isEmpty() || this.selectedBannerPatternIndex.get() <= 0 || this.selectedBannerPatternIndex.get() >= BannerPattern.COUNT - BannerPattern.PATTERN_ITEM_COUNT && itemstack2.isEmpty())) {
            this.resultSlot.set(ItemStack.EMPTY);
            this.selectedBannerPatternIndex.set(0);
        } else if (!itemstack2.isEmpty() && itemstack2.getItem() instanceof BannerPatternItem) {
            CompoundTag nbttagcompound = BlockItem.getBlockEntityData(itemstack);
            boolean flag = nbttagcompound != null && nbttagcompound.contains("Patterns", 9) && !itemstack.isEmpty() && nbttagcompound.getList("Patterns", 10).size() >= 6;

            if (flag) {
                this.selectedBannerPatternIndex.set(0);
            } else {
                this.selectedBannerPatternIndex.set(((BannerPatternItem) itemstack2.getItem()).getBannerPattern().ordinal());
            }
        }

        this.setupResultSlot();
        //this.c(); // Paper - done below
        org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callPrepareResultEvent(this, 3); // Paper
    }

    public void registerUpdateListener(Runnable inventoryChangeListener) {
        this.slotUpdateListener = inventoryChangeListener;
    }

    @Override
    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot) this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();

            itemstack = itemstack1.copy();
            if (index == this.resultSlot.index) {
                if (!this.moveItemStackTo(itemstack1, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index != this.dyeSlot.index && index != this.bannerSlot.index && index != this.patternSlot.index) {
                if (itemstack1.getItem() instanceof BannerItem) {
                    if (!this.moveItemStackTo(itemstack1, this.bannerSlot.index, this.bannerSlot.index + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.getItem() instanceof DyeItem) {
                    if (!this.moveItemStackTo(itemstack1, this.dyeSlot.index, this.dyeSlot.index + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.getItem() instanceof BannerPatternItem) {
                    if (!this.moveItemStackTo(itemstack1, this.patternSlot.index, this.patternSlot.index + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 4 && index < 31) {
                    if (!this.moveItemStackTo(itemstack1, 31, 40, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 31 && index < 40 && !this.moveItemStackTo(itemstack1, 4, 31, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 4, 40, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public void removed(net.minecraft.world.entity.player.Player player) {
        super.removed(player);
        this.access.execute((world, blockposition) -> {
            this.clearContainer(player, this.inputContainer);
        });
    }

    private void setupResultSlot() {
        if (this.selectedBannerPatternIndex.get() > 0) {
            ItemStack itemstack = this.bannerSlot.getItem();
            ItemStack itemstack1 = this.dyeSlot.getItem();
            ItemStack itemstack2 = ItemStack.EMPTY;

            if (!itemstack.isEmpty() && !itemstack1.isEmpty()) {
                itemstack2 = itemstack.copy();
                itemstack2.setCount(1);
                BannerPattern enumbannerpatterntype = BannerPattern.values()[this.selectedBannerPatternIndex.get()];
                DyeColor enumcolor = ((DyeItem) itemstack1.getItem()).getDyeColor();
                CompoundTag nbttagcompound = BlockItem.getBlockEntityData(itemstack2);
                ListTag nbttaglist;

                if (nbttagcompound != null && nbttagcompound.contains("Patterns", 9)) {
                    nbttaglist = nbttagcompound.getList("Patterns", 10);
                    // CraftBukkit start
                    while (nbttaglist.size() > 20) {
                        nbttaglist.remove(20);
                    }
                    // CraftBukkit end
                } else {
                    nbttaglist = new ListTag();
                    if (nbttagcompound == null) {
                        nbttagcompound = new CompoundTag();
                    }

                    nbttagcompound.put("Patterns", nbttaglist);
                }

                CompoundTag nbttagcompound1 = new CompoundTag();

                nbttagcompound1.putString("Pattern", enumbannerpatterntype.getHashname());
                nbttagcompound1.putInt("Color", enumcolor.getId());
                nbttaglist.add(nbttagcompound1);
                BlockItem.setBlockEntityData(itemstack2, BlockEntityType.BANNER, nbttagcompound);
            }

            if (!ItemStack.matches(itemstack2, this.resultSlot.getItem())) {
                this.resultSlot.set(itemstack2);
            }
        }

    }

    public Slot getBannerSlot() {
        return this.bannerSlot;
    }

    public Slot getDyeSlot() {
        return this.dyeSlot;
    }

    public Slot getPatternSlot() {
        return this.patternSlot;
    }

    public Slot getResultSlot() {
        return this.resultSlot;
    }
}