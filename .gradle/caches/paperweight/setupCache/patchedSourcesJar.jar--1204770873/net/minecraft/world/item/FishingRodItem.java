package net.minecraft.world.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import org.bukkit.event.player.PlayerFishEvent; // CraftBukkit

public class FishingRodItem extends Item implements Vanishable {

    public FishingRodItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack itemstack = user.getItemInHand(hand);
        int i;

        if (user.fishing != null) {
            if (!world.isClientSide) {
                i = user.fishing.retrieve(itemstack);
                itemstack.hurtAndBreak(i, user, (entityhuman1) -> {
                    entityhuman1.broadcastBreakEvent(hand);
                });
            }

            world.playSound((Player) null, user.getX(), user.getY(), user.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
            world.gameEvent(user, GameEvent.FISHING_ROD_REEL_IN, (Entity) user);
        } else {
            // world.playSound((EntityHuman) null, entityhuman.getX(), entityhuman.getY(), entityhuman.getZ(), SoundEffects.FISHING_BOBBER_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
            if (!world.isClientSide) {
                i = EnchantmentHelper.getFishingSpeedBonus(itemstack);
                int j = EnchantmentHelper.getFishingLuckBonus(itemstack);

                // CraftBukkit start
                FishingHook entityfishinghook = new FishingHook(user, world, j, i);
                PlayerFishEvent playerFishEvent = new PlayerFishEvent((org.bukkit.entity.Player) user.getBukkitEntity(), null, (org.bukkit.entity.FishHook) entityfishinghook.getBukkitEntity(), PlayerFishEvent.State.FISHING);
                world.getCraftServer().getPluginManager().callEvent(playerFishEvent);

                if (playerFishEvent.isCancelled()) {
                    user.fishing = null;
                    return InteractionResultHolder.pass(itemstack);
                }
                world.playSound((Player) null, user.getX(), user.getY(), user.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
                world.addFreshEntity(entityfishinghook);
                // CraftBukkit end
            }

            user.awardStat(Stats.ITEM_USED.get(this));
            world.gameEvent(user, GameEvent.FISHING_ROD_CAST, (Entity) user);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, world.isClientSide());
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }
}
