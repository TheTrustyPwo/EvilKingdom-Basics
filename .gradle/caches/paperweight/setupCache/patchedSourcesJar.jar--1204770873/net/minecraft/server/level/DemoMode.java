package net.minecraft.server.level;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class DemoMode extends ServerPlayerGameMode {
    public static final int DEMO_DAYS = 5;
    public static final int TOTAL_PLAY_TICKS = 120500;
    private boolean displayedIntro;
    private boolean demoHasEnded;
    private int demoEndedReminder;
    private int gameModeTicks;

    public DemoMode(ServerPlayer player) {
        super(player);
    }

    @Override
    public void tick() {
        super.tick();
        ++this.gameModeTicks;
        long l = this.level.getGameTime();
        long m = l / 24000L + 1L;
        if (!this.displayedIntro && this.gameModeTicks > 20) {
            this.displayedIntro = true;
            this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 0.0F));
        }

        this.demoHasEnded = l > 120500L;
        if (this.demoHasEnded) {
            ++this.demoEndedReminder;
        }

        if (l % 24000L == 500L) {
            if (m <= 6L) {
                if (m == 6L) {
                    this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 104.0F));
                } else {
                    this.player.sendMessage(new TranslatableComponent("demo.day." + m), Util.NIL_UUID);
                }
            }
        } else if (m == 1L) {
            if (l == 100L) {
                this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 101.0F));
            } else if (l == 175L) {
                this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 102.0F));
            } else if (l == 250L) {
                this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 103.0F));
            }
        } else if (m == 5L && l % 24000L == 22000L) {
            this.player.sendMessage(new TranslatableComponent("demo.day.warning"), Util.NIL_UUID);
        }

    }

    private void outputDemoReminder() {
        if (this.demoEndedReminder > 100) {
            this.player.sendMessage(new TranslatableComponent("demo.reminder"), Util.NIL_UUID);
            this.demoEndedReminder = 0;
        }

    }

    @Override
    public void handleBlockBreakAction(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int worldHeight) {
        if (this.demoHasEnded) {
            this.outputDemoReminder();
        } else {
            super.handleBlockBreakAction(pos, action, direction, worldHeight);
        }
    }

    @Override
    public InteractionResult useItem(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand) {
        if (this.demoHasEnded) {
            this.outputDemoReminder();
            return InteractionResult.PASS;
        } else {
            return super.useItem(player, world, stack, hand);
        }
    }

    @Override
    public InteractionResult useItemOn(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult) {
        if (this.demoHasEnded) {
            this.outputDemoReminder();
            return InteractionResult.PASS;
        } else {
            return super.useItemOn(player, world, stack, hand, hitResult);
        }
    }
}