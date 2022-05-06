package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.logging.LogUtils;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundAddMobPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQuery;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQuery;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

// CraftBukkit start
import io.papermc.paper.adventure.ChatProcessor; // Paper
import io.papermc.paper.adventure.PaperAdventure; // Paper
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_18_R2.util.LazyPlayerSet;
import org.bukkit.craftbukkit.v1_18_R2.util.Waitable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent; // Paper
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.SmithingInventory;
import co.aikar.timings.MinecraftTimings; // Paper
// CraftBukkit end

public class ServerGamePacketListenerImpl implements ServerPlayerConnection, ServerGamePacketListener {

    static final Logger LOGGER = LogUtils.getLogger();
    private static final int LATENCY_CHECK_INTERVAL = 15000;
    public final Connection connection;
    private final MinecraftServer server;
    public Runnable playerJoinReady; // Paper
    public ServerPlayer player;
    private int tickCount;
    private long keepAliveTime = Util.getMillis();
    private boolean keepAlivePending;
    private long keepAliveChallenge;
    // CraftBukkit start - multithreaded fields
    private final AtomicInteger chatSpamTickCount = new AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger tabSpamLimiter = new java.util.concurrent.atomic.AtomicInteger(); // Paper - configurable tab spam limits
    private final java.util.concurrent.atomic.AtomicInteger recipeSpamPackets =  new java.util.concurrent.atomic.AtomicInteger(); // Paper - auto recipe limit
    // CraftBukkit end
    private int dropSpamTickCount;
    private double firstGoodX;
    private double firstGoodY;
    private double firstGoodZ;
    private double lastGoodX;
    private double lastGoodY;
    private double lastGoodZ;
    @Nullable
    private Entity lastVehicle;
    private double vehicleFirstGoodX;
    private double vehicleFirstGoodY;
    private double vehicleFirstGoodZ;
    private double vehicleLastGoodX;
    private double vehicleLastGoodY;
    private double vehicleLastGoodZ;
    @Nullable
    private Vec3 awaitingPositionFromClient;
    private int awaitingTeleport;
    private int awaitingTeleportTime;
    private boolean clientIsFloating;
    private int aboveGroundTickCount;
    private boolean clientVehicleIsFloating;
    private int aboveGroundVehicleTickCount;
    private int receivedMovePacketCount;
    private int knownMovePacketCount;
    private static final int MAX_SIGN_LINE_LENGTH = Integer.getInteger("Paper.maxSignLength", 80);
    private static final long KEEPALIVE_LIMIT = Long.getLong("paper.playerconnection.keepalive", 30) * 1000; // Paper - provide property to set keepalive limit

    private String clientBrandName = null; // Paper - Brand name

    public ServerGamePacketListenerImpl(MinecraftServer server, Connection connection, ServerPlayer player) {
        this.server = server;
        this.connection = connection;
        connection.setListener(this);
        this.player = player;
        player.connection = this;
        this.keepAliveTime = Util.getMillis();
        player.getTextFilter().join();

        // CraftBukkit start - add fields and methods
        this.cserver = server.server;
    }

    private final org.bukkit.craftbukkit.v1_18_R2.CraftServer cserver;
    public boolean processedDisconnect;
    private int lastTick = MinecraftServer.currentTick;
    private int allowedPlayerTicks = 1;
    private int lastDropTick = MinecraftServer.currentTick;
    private int lastBookTick  = MinecraftServer.currentTick;
    private int dropCount = 0;

    // Get position of last block hit for BlockDamageLevel.STOPPED
    private double lastPosX = Double.MAX_VALUE;
    private double lastPosY = Double.MAX_VALUE;
    private double lastPosZ = Double.MAX_VALUE;
    private float lastPitch = Float.MAX_VALUE;
    private float lastYaw = Float.MAX_VALUE;
    private boolean justTeleported = false;
    private boolean hasMoved; // Spigot

    public CraftPlayer getCraftPlayer() {
        return (this.player == null) ? null : (CraftPlayer) this.player.getBukkitEntity();
    }
    // CraftBukkit end

    public void tick() {
        // Paper start - login async
        Runnable playerJoinReady = this.playerJoinReady;
        if (playerJoinReady != null) {
            this.playerJoinReady = null;
            playerJoinReady.run();
        }
        // Don't tick if not valid (dead), otherwise we load chunks below
        if (this.player.valid) {
        // Paper end
        this.resetPosition();
        this.player.xo = this.player.getX();
        this.player.yo = this.player.getY();
        this.player.zo = this.player.getZ();
        this.player.doTick();
        this.player.absMoveTo(this.firstGoodX, this.firstGoodY, this.firstGoodZ, this.player.getYRot(), this.player.getXRot());
        ++this.tickCount;
        this.knownMovePacketCount = this.receivedMovePacketCount;
        if (this.clientIsFloating && !this.player.isSleeping() && !this.player.isPassenger()) {
            if (++this.aboveGroundTickCount > 80) {
                ServerGamePacketListenerImpl.LOGGER.warn("{} was kicked for floating too long!", this.player.getName().getString());
                this.disconnect(com.destroystokyo.paper.PaperConfig.flyingKickPlayerMessage, org.bukkit.event.player.PlayerKickEvent.Cause.FLYING_PLAYER); // Paper - use configurable kick message & kick event cause
                return;
            }
        } else {
            this.clientIsFloating = false;
            this.aboveGroundTickCount = 0;
        }

        this.lastVehicle = this.player.getRootVehicle();
        if (this.lastVehicle != this.player && this.lastVehicle.getControllingPassenger() == this.player) {
            this.vehicleFirstGoodX = this.lastVehicle.getX();
            this.vehicleFirstGoodY = this.lastVehicle.getY();
            this.vehicleFirstGoodZ = this.lastVehicle.getZ();
            this.vehicleLastGoodX = this.lastVehicle.getX();
            this.vehicleLastGoodY = this.lastVehicle.getY();
            this.vehicleLastGoodZ = this.lastVehicle.getZ();
            if (this.clientVehicleIsFloating && this.player.getRootVehicle().getControllingPassenger() == this.player) {
                if (++this.aboveGroundVehicleTickCount > 80) {
                    ServerGamePacketListenerImpl.LOGGER.warn("{} was kicked for floating a vehicle too long!", this.player.getName().getString());
                    this.disconnect(com.destroystokyo.paper.PaperConfig.flyingKickVehicleMessage, org.bukkit.event.player.PlayerKickEvent.Cause.FLYING_VEHICLE); // Paper - use configurable kick message & kick event cause
                    return;
                }
            } else {
                this.clientVehicleIsFloating = false;
                this.aboveGroundVehicleTickCount = 0;
            }
        } else {
            this.lastVehicle = null;
            this.clientVehicleIsFloating = false;
            this.aboveGroundVehicleTickCount = 0;
        }} // Paper - end if (valid)

        this.server.getProfiler().push("keepAlive");
        // Paper Start - give clients a longer time to respond to pings as per pre 1.12.2 timings
        // This should effectively place the keepalive handling back to "as it was" before 1.12.2
        long currentTime = Util.getMillis();
        long elapsedTime = currentTime - this.keepAliveTime;

        if (this.keepAlivePending) {
            if (!this.processedDisconnect && elapsedTime >= KEEPALIVE_LIMIT) { // check keepalive limit, don't fire if already disconnected
                ServerGamePacketListenerImpl.LOGGER.warn("{} was kicked due to keepalive timeout!", this.player.getScoreboardName()); // more info
                this.disconnect(new TranslatableComponent("disconnect.timeout", new Object[0]), org.bukkit.event.player.PlayerKickEvent.Cause.TIMEOUT); // Paper - kick event cause
            }
        } else {
            if (elapsedTime >= 15000L) { // 15 seconds
                this.keepAlivePending = true;
                this.keepAliveTime = currentTime;
                this.keepAliveChallenge = currentTime;
                this.send(new ClientboundKeepAlivePacket(this.keepAliveChallenge));
            }
        }
        // Paper end

        this.server.getProfiler().pop();
        // CraftBukkit start
        for (int spam; (spam = this.chatSpamTickCount.get()) > 0 && !this.chatSpamTickCount.compareAndSet(spam, spam - 1); ) ;
        if (tabSpamLimiter.get() > 0) tabSpamLimiter.getAndDecrement(); // Paper - split to seperate variable
        if (recipeSpamPackets.get() > 0) recipeSpamPackets.getAndDecrement(); // Paper
        /* Use thread-safe field access instead
        if (this.chatSpamTickCount > 0) {
            --this.chatSpamTickCount;
        }
        */
        // CraftBukkit end

        if (this.dropSpamTickCount > 0) {
            --this.dropSpamTickCount;
        }

        if (this.player.getLastActionTime() > 0L && this.server.getPlayerIdleTimeout() > 0 && Util.getMillis() - this.player.getLastActionTime() > (long) (this.server.getPlayerIdleTimeout() * 1000 * 60) && !this.player.wonGame) { // Paper - Prevent AFK kick while watching end credits.
            this.player.resetLastActionTime(); // CraftBukkit - SPIGOT-854
            this.disconnect(new TranslatableComponent("multiplayer.disconnect.idling"), org.bukkit.event.player.PlayerKickEvent.Cause.IDLING); // Paper - kick event cause
        }

    }

    public void resetPosition() {
        this.firstGoodX = this.player.getX();
        this.firstGoodY = this.player.getY();
        this.firstGoodZ = this.player.getZ();
        this.lastGoodX = this.player.getX();
        this.lastGoodY = this.player.getY();
        this.lastGoodZ = this.player.getZ();
    }

    @Override
    public Connection getConnection() {
        return this.connection;
    }

    private boolean isSingleplayerOwner() {
        return this.server.isSingleplayerOwner(this.player.getGameProfile());
    }

    public void disconnect(String s) {
        // Paper start
        this.disconnect(PaperAdventure.LEGACY_SECTION_UXRC.deserialize(s), org.bukkit.event.player.PlayerKickEvent.Cause.UNKNOWN);
    }

    public void disconnect(String s, PlayerKickEvent.Cause cause) {
        this.disconnect(PaperAdventure.LEGACY_SECTION_UXRC.deserialize(s), cause);
    }

    public void disconnect(final Component reason) {
        this.disconnect(PaperAdventure.asAdventure(reason), org.bukkit.event.player.PlayerKickEvent.Cause.UNKNOWN);
    }

    public void disconnect(final Component reason, PlayerKickEvent.Cause cause) {
        this.disconnect(PaperAdventure.asAdventure(reason), cause);
    }

    public void disconnect(net.kyori.adventure.text.Component reason, org.bukkit.event.player.PlayerKickEvent.Cause cause) {
        // Paper end
        // CraftBukkit start - fire PlayerKickEvent
        if (this.processedDisconnect) {
            return;
        }
        net.kyori.adventure.text.Component leaveMessage = net.kyori.adventure.text.Component.translatable("multiplayer.player.left", net.kyori.adventure.text.format.NamedTextColor.YELLOW, this.player.getBukkitEntity().displayName()); // Paper - Adventure

        PlayerKickEvent event = new PlayerKickEvent(this.player.getBukkitEntity(), reason, leaveMessage, cause); // Paper - Adventure & kick event reason

        if (this.cserver.getServer().isRunning()) {
            this.cserver.getPluginManager().callEvent(event);
        }

        if (event.isCancelled()) {
            // Do not kick the player
            return;
        }
        // Send the possibly modified leave message
        final Component ichatbasecomponent = PaperAdventure.asVanilla(event.reason()); // Paper - Adventure
        // CraftBukkit end

        this.player.quitReason = org.bukkit.event.player.PlayerQuitEvent.QuitReason.KICKED; // Paper
        this.connection.send(new ClientboundDisconnectPacket(ichatbasecomponent), (future) -> {
            this.connection.disconnect(ichatbasecomponent);
        });
        this.onDisconnect(ichatbasecomponent, event.leaveMessage()); // CraftBukkit - fire quit instantly // Paper - use kick event leave message
        this.connection.setReadOnly();
        MinecraftServer minecraftserver = this.server;
        Connection networkmanager = this.connection;

        Objects.requireNonNull(this.connection);
        // CraftBukkit - Don't wait
        minecraftserver.scheduleOnMain(networkmanager::handleDisconnection); // Paper
    }

    private <T, R> void filterTextPacket(T text, Consumer<R> consumer, BiFunction<TextFilter, T, CompletableFuture<R>> backingFilterer) {
        BlockableEventLoop<?> iasynctaskhandler = this.player.getLevel().getServer();
        Consumer<R> consumer1 = (object) -> {
            if (this.getConnection().isConnected()) {
                try {
                    consumer.accept(object);
                } catch (Exception exception) {
                    ServerGamePacketListenerImpl.LOGGER.error("Failed to handle chat packet {}, suppressing error", text, exception);
                }
            } else {
                ServerGamePacketListenerImpl.LOGGER.debug("Ignoring packet due to disconnection");
            }

        };

        ((CompletableFuture) backingFilterer.apply(this.player.getTextFilter(), text)).thenAcceptAsync(consumer1, iasynctaskhandler);
    }

    private void filterTextPacket(String text, Consumer<TextFilter.FilteredText> consumer) {
        this.filterTextPacket(text, consumer, TextFilter::processStreamMessage);
    }

    private void filterTextPacket(List<String> texts, Consumer<List<TextFilter.FilteredText>> consumer) {
        this.filterTextPacket(texts, consumer, TextFilter::processMessageBundle);
    }

    @Override
    public void handlePlayerInput(ServerboundPlayerInputPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.setPlayerInput(packet.getXxa(), packet.getZza(), packet.isJumping(), packet.isShiftKeyDown());
    }

    private static boolean containsInvalidValues(double x, double y, double z, float yaw, float pitch) {
        return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || !Floats.isFinite(pitch) || !Floats.isFinite(yaw);
    }

    private static double clampHorizontal(double d) {
        return Mth.clamp(d, -3.0E7D, 3.0E7D);
    }

    private static double clampVertical(double d) {
        return Mth.clamp(d, -2.0E7D, 2.0E7D);
    }

    @Override
    public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (ServerGamePacketListenerImpl.containsInvalidValues(packet.getX(), packet.getY(), packet.getZ(), packet.getYRot(), packet.getXRot())) {
            this.disconnect(new TranslatableComponent("multiplayer.disconnect.invalid_vehicle_movement"), org.bukkit.event.player.PlayerKickEvent.Cause.INVALID_VEHICLE_MOVEMENT); // Paper - kick event cause
        } else {
            Entity entity = this.player.getRootVehicle();

            // Paper start
            if (this.awaitingPositionFromClient != null || this.player.isImmobile() || entity.isRemoved()) {
                return;
            }
            // Paper end

            if (entity != this.player && entity.getControllingPassenger() == this.player && entity == this.lastVehicle) {
                ServerLevel worldserver = this.player.getLevel();
                double d0 = entity.getX();final double fromX = d0; // Paper - OBFHELPER
                double d1 = entity.getY();final double fromY = d1; // Paper - OBFHELPER
                double d2 = entity.getZ();final double fromZ = d2; // Paper - OBFHELPER
                double d3 = ServerGamePacketListenerImpl.clampHorizontal(packet.getX()); final double toX = d3; // Paper - OBFHELPER
                double d4 = ServerGamePacketListenerImpl.clampVertical(packet.getY()); final double toY = d4; // Paper - OBFHELPER
                double d5 = ServerGamePacketListenerImpl.clampHorizontal(packet.getZ()); final double toZ = d5; // Paper - OBFHELPER
                float f = Mth.wrapDegrees(packet.getYRot());
                float f1 = Mth.wrapDegrees(packet.getXRot());
                double d6 = d3 - this.vehicleFirstGoodX;
                double d7 = d4 - this.vehicleFirstGoodY;
                double d8 = d5 - this.vehicleFirstGoodZ;
                double d9 = entity.getDeltaMovement().lengthSqr();
                // Paper start - fix large move vectors killing the server
                double currDeltaX = toX - fromX;
                double currDeltaY = toY - fromY;
                double currDeltaZ = toZ - fromZ;
                double d10 = Math.max(d6 * d6 + d7 * d7 + d8 * d8, (currDeltaX * currDeltaX + currDeltaY * currDeltaY + currDeltaZ * currDeltaZ) - 1);
                // Paper end - fix large move vectors killing the server

                // Paper start - fix large move vectors killing the server
                double otherFieldX = d3 - this.vehicleLastGoodX;
                double otherFieldY = d4 - this.vehicleLastGoodY - 1.0E-6D;
                double otherFieldZ = d5 - this.vehicleLastGoodZ;
                d10 = Math.max(d10, (otherFieldX * otherFieldX + otherFieldY * otherFieldY + otherFieldZ * otherFieldZ) - 1);
                // Paper end - fix large move vectors killing the server

                // CraftBukkit start - handle custom speeds and skipped ticks
                this.allowedPlayerTicks += (System.currentTimeMillis() / 50) - this.lastTick;
                this.allowedPlayerTicks = Math.max(this.allowedPlayerTicks, 1);
                this.lastTick = (int) (System.currentTimeMillis() / 50);

                ++this.receivedMovePacketCount;
                int i = this.receivedMovePacketCount - this.knownMovePacketCount;
                if (i > Math.max(this.allowedPlayerTicks, 5)) {
                    ServerGamePacketListenerImpl.LOGGER.debug(this.player.getScoreboardName() + " is sending move packets too frequently (" + i + " packets since last tick)");
                    i = 1;
                }

                if (d10 > 0) {
                    this.allowedPlayerTicks -= 1;
                } else {
                    this.allowedPlayerTicks = 20;
                }
                double speed;
                if (this.player.getAbilities().flying) {
                    speed = this.player.getAbilities().flyingSpeed * 20f;
                } else {
                    speed = this.player.getAbilities().walkingSpeed * 10f;
                }
                speed *= 2f; // TODO: Get the speed of the vehicle instead of the player

                // Paper start - Prevent moving into unloaded chunks
                if (player.level.paperConfig.preventMovingIntoUnloadedChunks && (
                    !worldserver.areChunksLoadedForMove(this.player.getBoundingBox().expandTowards(new Vec3(toX, toY, toZ).subtract(this.player.position()))) ||
                        !worldserver.areChunksLoadedForMove(entity.getBoundingBox().expandTowards(new Vec3(toX, toY, toZ).subtract(entity.position())))
                    )) {
                    this.connection.send(new ClientboundMoveVehiclePacket(entity));
                    return;
                }
                // Paper end

                if (d10 - d9 > Math.max(100.0D, Math.pow((double) (org.spigotmc.SpigotConfig.movedTooQuicklyMultiplier * (float) i * speed), 2)) && !this.isSingleplayerOwner()) {
                // CraftBukkit end
                    ServerGamePacketListenerImpl.LOGGER.warn("{} (vehicle of {}) moved too quickly! {},{},{}", new Object[]{entity.getName().getString(), this.player.getName().getString(), d6, d7, d8});
                    this.connection.send(new ClientboundMoveVehiclePacket(entity));
                    return;
                }

                AABB oldBox = entity.getBoundingBox(); // Paper - copy from player movement packet

                d6 = d3 - this.vehicleLastGoodX; // Paper - diff on change, used for checking large move vectors above
                d7 = d4 - this.vehicleLastGoodY - 1.0E-6D; // Paper - diff on change, used for checking large move vectors above
                d8 = d5 - this.vehicleLastGoodZ; // Paper - diff on change, used for checking large move vectors above
                boolean flag1 = entity.verticalCollisionBelow;

                entity.move(MoverType.PLAYER, new Vec3(d6, d7, d8));
                boolean didCollide = toX != entity.getX() || toY != entity.getY() || toZ != entity.getZ(); // Paper - needed here as the difference in Y can be reset - also note: this is only a guess at whether collisions took place, floating point errors can make this true when it shouldn't be...
                double d11 = d7;

                d6 = d3 - entity.getX();
                d7 = d4 - entity.getY();
                if (d7 > -0.5D || d7 < 0.5D) {
                    d7 = 0.0D;
                }

                d8 = d5 - entity.getZ();
                d10 = d6 * d6 + d7 * d7 + d8 * d8;
                boolean flag2 = false;

                if (d10 > org.spigotmc.SpigotConfig.movedWronglyThreshold) { // Spigot
                    flag2 = true; // Paper - diff on change, this should be moved wrongly
                    ServerGamePacketListenerImpl.LOGGER.warn("{} (vehicle of {}) moved wrongly! {}", new Object[]{entity.getName().getString(), this.player.getName().getString(), Math.sqrt(d10)});
                }
                Location curPos = this.getCraftPlayer().getLocation(); // Spigot

                entity.absMoveTo(d3, d4, d5, f, f1);
                this.player.absMoveTo(d3, d4, d5, this.player.getYRot(), this.player.getXRot()); // CraftBukkit

                // Paper start - optimise out extra getCubes
                boolean teleportBack = flag2; // violating this is always a fail
                if (!teleportBack) {
                    // note: only call after setLocation, or else getBoundingBox is wrong
                    AABB newBox = entity.getBoundingBox();
                    if (didCollide || !oldBox.equals(newBox)) {
                        teleportBack = this.hasNewCollision(worldserver, entity, oldBox, newBox);
                    } // else: no collision at all detected, why do we care?
                }
                if (teleportBack) { // Paper end - optimise out extra getCubes
                    entity.absMoveTo(d0, d1, d2, f, f1);
                    this.player.absMoveTo(d0, d1, d2, this.player.getYRot(), this.player.getXRot()); // CraftBukkit
                    this.connection.send(new ClientboundMoveVehiclePacket(entity));
                    return;
                }

                // CraftBukkit start - fire PlayerMoveEvent
                Player player = this.getCraftPlayer();
                // Spigot Start
                if ( !this.hasMoved )
                {
                    this.lastPosX = curPos.getX();
                    this.lastPosY = curPos.getY();
                    this.lastPosZ = curPos.getZ();
                    this.lastYaw = curPos.getYaw();
                    this.lastPitch = curPos.getPitch();
                    this.hasMoved = true;
                }
                // Spigot End
                Location from = new Location(player.getWorld(), this.lastPosX, this.lastPosY, this.lastPosZ, this.lastYaw, this.lastPitch); // Get the Players previous Event location.
                Location to = player.getLocation().clone(); // Start off the To location as the Players current location.

                // If the packet contains movement information then we update the To location with the correct XYZ.
                to.setX(packet.getX());
                to.setY(packet.getY());
                to.setZ(packet.getZ());


                // If the packet contains look information then we update the To location with the correct Yaw & Pitch.
                to.setYaw(packet.getYRot());
                to.setPitch(packet.getXRot());

                // Prevent 40 event-calls for less than a single pixel of movement >.>
                double delta = Math.pow(this.lastPosX - to.getX(), 2) + Math.pow(this.lastPosY - to.getY(), 2) + Math.pow(this.lastPosZ - to.getZ(), 2);
                float deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());

                if ((delta > 1f / 256 || deltaAngle > 10f) && !this.player.isImmobile()) {
                    this.lastPosX = to.getX();
                    this.lastPosY = to.getY();
                    this.lastPosZ = to.getZ();
                    this.lastYaw = to.getYaw();
                    this.lastPitch = to.getPitch();

                    // Skip the first time we do this
                    if (true) { // Spigot - don't skip any move events
                        Location oldTo = to.clone();
                        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
                        this.cserver.getPluginManager().callEvent(event);

                        // If the event is cancelled we move the player back to their old location.
                        if (event.isCancelled()) {
                            this.teleport(from);
                            return;
                        }

                        // If a Plugin has changed the To destination then we teleport the Player
                        // there to avoid any 'Moved wrongly' or 'Moved too quickly' errors.
                        // We only do this if the Event was not cancelled.
                        if (!oldTo.equals(event.getTo()) && !event.isCancelled()) {
                            this.player.getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                            return;
                        }

                        // Check to see if the Players Location has some how changed during the call of the event.
                        // This can happen due to a plugin teleporting the player instead of using .setTo()
                        if (!from.equals(this.getCraftPlayer().getLocation()) && this.justTeleported) {
                            this.justTeleported = false;
                            return;
                        }
                    }
                }
                // CraftBukkit end

                this.player.getLevel().getChunkSource().move(this.player);
                this.player.checkMovementStatistics(this.player.getX() - d0, this.player.getY() - d1, this.player.getZ() - d2);
                this.clientVehicleIsFloating = d11 >= -0.03125D && !flag1 && !this.server.isFlightAllowed() && !entity.isNoGravity() && this.noBlocksAround(entity);
                this.vehicleLastGoodX = entity.getX();
                this.vehicleLastGoodY = entity.getY();
                this.vehicleLastGoodZ = entity.getZ();
            }

        }
    }

    private boolean noBlocksAround(Entity entity) {
        // Paper start - stop using streams, this is already a known fixed problem in Entity#move
        AABB box = entity.getBoundingBox().inflate(0.0625D).expandTowards(0.0D, -0.55D, 0.0D);
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);

        Level world = entity.level;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= maxY; ++y) {
            for (int z = minZ; z <= maxZ; ++z) {
                for (int x = minX; x <= maxX; ++x) {
                    pos.set(x, y, z);
                    BlockState type = world.getBlockStateIfLoaded(pos);
                    if (type != null && !type.isAir()) {
                        return false;
                    }
                }
            }
        }

        return true;
        // Paper end - stop using streams, this is already a known fixed problem in Entity#move
    }

    @Override
    public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (packet.getId() == this.awaitingTeleport && this.awaitingPositionFromClient != null) { // CraftBukkit
            this.player.moveTo(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot()); // Paper - use proper setPositionRotation for teleportation
            this.lastGoodX = this.awaitingPositionFromClient.x;
            this.lastGoodY = this.awaitingPositionFromClient.y;
            this.lastGoodZ = this.awaitingPositionFromClient.z;
            if (this.player.isChangingDimension()) {
                this.player.hasChangedDimension();
            }

            this.awaitingPositionFromClient = null;
            this.player.getLevel().getChunkSource().move(this.player); // CraftBukkit
        }

    }

    @Override
    public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        Optional<? extends Recipe<?>> optional = this.server.getRecipeManager().byKey(packet.getRecipe()); // CraftBukkit - decompile error
        ServerRecipeBook recipebookserver = this.player.getRecipeBook();

        Objects.requireNonNull(recipebookserver);
        optional.ifPresent(recipebookserver::removeHighlight);
    }

    @Override
    public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.getRecipeBook().setBookSetting(packet.getBookType(), packet.isOpen(), packet.isFiltering());
    }

    @Override
    public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (packet.getAction() == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
            ResourceLocation minecraftkey = packet.getTab();
            Advancement advancement = this.server.getAdvancements().getAdvancement(minecraftkey);

            if (advancement != null) {
                this.player.getAdvancements().setSelectedTab(advancement);
            }
        }

    }

    @Override
    public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet) {
        // PlayerConnectionUtils.ensureMainThread(packetplayintabcomplete, this, this.player.getWorldServer()); // Paper - run this async
        // CraftBukkit start
        if (this.chatSpamTickCount.addAndGet(com.destroystokyo.paper.PaperConfig.tabSpamIncrement) > com.destroystokyo.paper.PaperConfig.tabSpamLimit && !this.server.getPlayerList().isOp(this.player.getGameProfile())) { // Paper start - split and make configurable
            server.scheduleOnMain(() -> this.disconnect(new TranslatableComponent("disconnect.spam", new Object[0]), org.bukkit.event.player.PlayerKickEvent.Cause.SPAM)); // Paper - kick event cause
            return;
        }
        // Paper start
        String str = packet.getCommand(); int index = -1;
        if (str.length() > 64 && ((index = str.indexOf(' ')) == -1 || index >= 64)) {
            server.scheduleOnMain(() -> this.disconnect(new TranslatableComponent("disconnect.spam", new Object[0]), org.bukkit.event.player.PlayerKickEvent.Cause.SPAM)); // Paper - kick event cause
            return;
        }
        // Paper end
        // CraftBukkit end
        // Paper start - Don't suggest if tab-complete is disabled
        if (org.spigotmc.SpigotConfig.tabComplete < 0) {
            return;
        }
        // Paper end
        StringReader stringreader = new StringReader(packet.getCommand());

        if (stringreader.canRead() && stringreader.peek() == '/') {
            stringreader.skip();
        }

        // Paper start - async tab completion
        com.destroystokyo.paper.event.server.AsyncTabCompleteEvent event;
        String buffer = packet.getCommand();
        event = new com.destroystokyo.paper.event.server.AsyncTabCompleteEvent(this.getCraftPlayer(),
                buffer, true, null);
        event.callEvent();
        java.util.List<com.destroystokyo.paper.event.server.AsyncTabCompleteEvent.Completion> completions = event.isCancelled() ? com.google.common.collect.ImmutableList.of() : event.completions();
        // If the event isn't handled, we can assume that we have no completions, and so we'll ask the server
        if (!event.isHandled()) {
            if (!event.isCancelled()) {

                this.server.scheduleOnMain(() -> { // Paper - This needs to be on main
                    ParseResults<CommandSourceStack> parseresults = this.server.getCommands().getDispatcher().parse(stringreader, this.player.createCommandSourceStack());

                    this.server.getCommands().getDispatcher().getCompletionSuggestions(parseresults).thenAccept((suggestions) -> {
                        // Paper start
                        com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent suggestEvent = new com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent(this.getCraftPlayer(), suggestions, buffer);
                        suggestEvent.setCancelled(suggestions.isEmpty());
                        if (!suggestEvent.callEvent()) return;
                        this.connection.send(new ClientboundCommandSuggestionsPacket(packet.getId(), (com.mojang.brigadier.suggestion.Suggestions) suggestEvent.getSuggestions())); // CraftBukkit - decompile error // Paper
                        // Paper end
                    });
                });
            }
        } else if (!completions.isEmpty()) {
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder0 = new com.mojang.brigadier.suggestion.SuggestionsBuilder(packet.getCommand(), stringreader.getTotalLength());

            final com.mojang.brigadier.suggestion.SuggestionsBuilder builder = builder0.createOffset(builder0.getInput().lastIndexOf(' ') + 1);
            completions.forEach(completion -> {
                if (completion.tooltip() == null) {
                    builder.suggest(completion.suggestion());
                } else {
                    builder.suggest(completion.suggestion(), PaperAdventure.asVanilla(completion.tooltip()));
                }
            });
            com.mojang.brigadier.suggestion.Suggestions suggestions = builder.buildFuture().join();
            com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent suggestEvent = new com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent(this.getCraftPlayer(), suggestions, buffer);
            suggestEvent.setCancelled(suggestions.isEmpty());
            if (!suggestEvent.callEvent()) return;
            this.connection.send(new ClientboundCommandSuggestionsPacket(packet.getId(), suggestEvent.getSuggestions()));
        }
        // Paper end - async tab completion
    }

    @Override
    public void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (!this.server.isCommandBlockEnabled()) {
            this.player.sendMessage(new TranslatableComponent("advMode.notEnabled"), Util.NIL_UUID);
        } else if (!this.player.canUseGameMasterBlocks() && (!this.player.isCreative() || !this.player.getBukkitEntity().hasPermission("minecraft.commandblock"))) { // Paper - command block permission
            this.player.sendMessage(new TranslatableComponent("advMode.notAllowed"), Util.NIL_UUID);
        } else {
            BaseCommandBlock commandblocklistenerabstract = null;
            CommandBlockEntity tileentitycommand = null;
            BlockPos blockposition = packet.getPos();
            BlockEntity tileentity = this.player.level.getBlockEntity(blockposition);

            if (tileentity instanceof CommandBlockEntity) {
                tileentitycommand = (CommandBlockEntity) tileentity;
                commandblocklistenerabstract = tileentitycommand.getCommandBlock();
            }

            String s = packet.getCommand();
            boolean flag = packet.isTrackOutput();

            if (commandblocklistenerabstract != null) {
                CommandBlockEntity.Mode tileentitycommand_type = tileentitycommand.getMode();
                BlockState iblockdata = this.player.level.getBlockState(blockposition);
                Direction enumdirection = (Direction) iblockdata.getValue(CommandBlock.FACING);
                BlockState iblockdata1;

                switch (packet.getMode()) {
                    case SEQUENCE:
                        iblockdata1 = Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState();
                        break;
                    case AUTO:
                        iblockdata1 = Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState();
                        break;
                    case REDSTONE:
                    default:
                        iblockdata1 = Blocks.COMMAND_BLOCK.defaultBlockState();
                }

                BlockState iblockdata2 = (BlockState) ((BlockState) iblockdata1.setValue(CommandBlock.FACING, enumdirection)).setValue(CommandBlock.CONDITIONAL, packet.isConditional());

                if (iblockdata2 != iblockdata) {
                    this.player.level.setBlock(blockposition, iblockdata2, 2);
                    tileentity.setBlockState(iblockdata2);
                    this.player.level.getChunkAt(blockposition).setBlockEntity(tileentity);
                }

                commandblocklistenerabstract.setCommand(s);
                commandblocklistenerabstract.setTrackOutput(flag);
                if (!flag) {
                    commandblocklistenerabstract.setLastOutput((Component) null);
                }

                tileentitycommand.setAutomatic(packet.isAutomatic());
                if (tileentitycommand_type != packet.getMode()) {
                    tileentitycommand.onModeSwitch();
                }

                commandblocklistenerabstract.onUpdated();
                if (!StringUtil.isNullOrEmpty(s)) {
                    this.player.sendMessage(new TranslatableComponent("advMode.setCommand.success", new Object[]{s}), Util.NIL_UUID);
                }
            }

        }
    }

    @Override
    public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (!this.server.isCommandBlockEnabled()) {
            this.player.sendMessage(new TranslatableComponent("advMode.notEnabled"), Util.NIL_UUID);
        } else if (!this.player.canUseGameMasterBlocks() && (!this.player.isCreative() || !this.player.getBukkitEntity().hasPermission("minecraft.commandblock"))) { // Paper - command block permission
            this.player.sendMessage(new TranslatableComponent("advMode.notAllowed"), Util.NIL_UUID);
        } else {
            BaseCommandBlock commandblocklistenerabstract = packet.getCommandBlock(this.player.level);

            if (commandblocklistenerabstract != null) {
                commandblocklistenerabstract.setCommand(packet.getCommand());
                commandblocklistenerabstract.setTrackOutput(packet.isTrackOutput());
                if (!packet.isTrackOutput()) {
                    commandblocklistenerabstract.setLastOutput((Component) null);
                }

                commandblocklistenerabstract.onUpdated();
                this.player.sendMessage(new TranslatableComponent("advMode.setCommand.success", new Object[]{packet.getCommand()}), Util.NIL_UUID);
            }

        }
    }

    @Override
    public void handlePickItem(ServerboundPickItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        // Paper start - validate pick item position
        if (!(packet.getSlot() >= 0 && packet.getSlot() < this.player.getInventory().items.size())) {
            ServerGamePacketListenerImpl.LOGGER.warn("{} tried to set an invalid carried item", this.player.getName().getString());
            this.disconnect("Invalid hotbar selection (Hacking?)", org.bukkit.event.player.PlayerKickEvent.Cause.ILLEGAL_ACTION); // Paper - kick event cause
            return;
        }
        this.player.getInventory().pickSlot(packet.getSlot()); // Paper - Diff above if changed
        // Paper end
        this.player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, this.player.getInventory().selected, this.player.getInventory().getItem(this.player.getInventory().selected)));
        this.player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, packet.getSlot(), this.player.getInventory().getItem(packet.getSlot())));
        this.player.connection.send(new ClientboundSetCarriedItemPacket(this.player.getInventory().selected));
    }

    @Override
    public void handleRenameItem(ServerboundRenameItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.containerMenu instanceof AnvilMenu) {
            AnvilMenu containeranvil = (AnvilMenu) this.player.containerMenu;
            String s = SharedConstants.filterText(packet.getName());

            if (s.length() <= 50) {
                containeranvil.setItemName(s);
            }
        }

    }

    @Override
    public void handleSetBeaconPacket(ServerboundSetBeaconPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.containerMenu instanceof BeaconMenu) {
            ((BeaconMenu) this.player.containerMenu).updateEffects(packet.getPrimary(), packet.getSecondary());
        }

    }

    @Override
    public void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockposition = packet.getPos();
            BlockState iblockdata = this.player.level.getBlockState(blockposition);
            BlockEntity tileentity = this.player.level.getBlockEntity(blockposition);

            if (tileentity instanceof StructureBlockEntity) {
                StructureBlockEntity tileentitystructure = (StructureBlockEntity) tileentity;

                tileentitystructure.setMode(packet.getMode());
                tileentitystructure.setStructureName(packet.getName());
                tileentitystructure.setStructurePos(packet.getOffset());
                tileentitystructure.setStructureSize(packet.getSize());
                tileentitystructure.setMirror(packet.getMirror());
                tileentitystructure.setRotation(packet.getRotation());
                tileentitystructure.setMetaData(packet.getData());
                tileentitystructure.setIgnoreEntities(packet.isIgnoreEntities());
                tileentitystructure.setShowAir(packet.isShowAir());
                tileentitystructure.setShowBoundingBox(packet.isShowBoundingBox());
                tileentitystructure.setIntegrity(packet.getIntegrity());
                tileentitystructure.setSeed(packet.getSeed());
                if (tileentitystructure.hasStructureName()) {
                    String s = tileentitystructure.getStructureName();

                    if (packet.getUpdateType() == StructureBlockEntity.UpdateType.SAVE_AREA) {
                        if (tileentitystructure.saveStructure()) {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.save_success", new Object[]{s}), false);
                        } else {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.save_failure", new Object[]{s}), false);
                        }
                    } else if (packet.getUpdateType() == StructureBlockEntity.UpdateType.LOAD_AREA) {
                        if (!tileentitystructure.isStructureLoadable()) {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.load_not_found", new Object[]{s}), false);
                        } else if (tileentitystructure.loadStructure(this.player.getLevel())) {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.load_success", new Object[]{s}), false);
                        } else {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.load_prepare", new Object[]{s}), false);
                        }
                    } else if (packet.getUpdateType() == StructureBlockEntity.UpdateType.SCAN_AREA) {
                        if (tileentitystructure.detectSize()) {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.size_success", new Object[]{s}), false);
                        } else {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.size_failure"), false);
                        }
                    }
                } else {
                    this.player.displayClientMessage(new TranslatableComponent("structure_block.invalid_structure_name", new Object[]{packet.getName()}), false);
                }

                tileentitystructure.setChanged();
                this.player.level.sendBlockUpdated(blockposition, iblockdata, iblockdata, 3);
            }

        }
    }

    @Override
    public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockposition = packet.getPos();
            BlockState iblockdata = this.player.level.getBlockState(blockposition);
            BlockEntity tileentity = this.player.level.getBlockEntity(blockposition);

            if (tileentity instanceof JigsawBlockEntity) {
                JigsawBlockEntity tileentityjigsaw = (JigsawBlockEntity) tileentity;

                tileentityjigsaw.setName(packet.getName());
                tileentityjigsaw.setTarget(packet.getTarget());
                tileentityjigsaw.setPool(packet.getPool());
                tileentityjigsaw.setFinalState(packet.getFinalState());
                tileentityjigsaw.setJoint(packet.getJoint());
                tileentityjigsaw.setChanged();
                this.player.level.sendBlockUpdated(blockposition, iblockdata, iblockdata, 3);
            }

        }
    }

    @Override
    public void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockposition = packet.getPos();
            BlockEntity tileentity = this.player.level.getBlockEntity(blockposition);

            if (tileentity instanceof JigsawBlockEntity) {
                JigsawBlockEntity tileentityjigsaw = (JigsawBlockEntity) tileentity;

                tileentityjigsaw.generate(this.player.getLevel(), packet.levels(), packet.keepJigsaws());
            }

        }
    }

    @Override
    public void handleSelectTrade(ServerboundSelectTradePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        int i = packet.getItem();
        AbstractContainerMenu container = this.player.containerMenu;

        if (container instanceof MerchantMenu) {
            MerchantMenu containermerchant = (MerchantMenu) container;
            // CraftBukkit start
            final org.bukkit.event.inventory.TradeSelectEvent tradeSelectEvent = CraftEventFactory.callTradeSelectEvent(this.player, i, containermerchant);
            if (tradeSelectEvent.isCancelled()) {
                this.player.getBukkitEntity().updateInventory();
                return;
            }
            // CraftBukkit end

            containermerchant.setSelectionHint(i);
            containermerchant.tryMoveItems(i);
        }

    }

    @Override
    public void handleEditBook(ServerboundEditBookPacket packet) {
        // Paper start
        if (!this.cserver.isPrimaryThread()) {
            List<String> pageList = packet.getPages();
            long byteTotal = 0;
            int maxBookPageSize = com.destroystokyo.paper.PaperConfig.maxBookPageSize;
            double multiplier = Math.max(0.3D, Math.min(1D, com.destroystokyo.paper.PaperConfig.maxBookTotalSizeMultiplier));
            long byteAllowed = maxBookPageSize;
            for (String testString : pageList) {
                int byteLength = testString.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                if (byteLength > 256 * 4) {
                    ServerGamePacketListenerImpl.LOGGER.warn(this.player.getScoreboardName() + " tried to send a book with with a page too large!");
                    server.scheduleOnMain(() -> this.disconnect("Book too large!", org.bukkit.event.player.PlayerKickEvent.Cause.ILLEGAL_ACTION)); // Paper - kick event cause
                    return;
                }
                byteTotal += byteLength;
                int length = testString.length();
                int multibytes = 0;
                if (byteLength != length) {
                    for (char c : testString.toCharArray()) {
                        if (c > 127) {
                            multibytes++;
                        }
                    }
                }
                byteAllowed += (maxBookPageSize * Math.min(1, Math.max(0.1D, (double) length / 255D))) * multiplier;

                if (multibytes > 1) {
                    // penalize MB
                    byteAllowed -= multibytes;
                }
            }

            if (byteTotal > byteAllowed) {
                ServerGamePacketListenerImpl.LOGGER.warn(this.player.getScoreboardName() + " tried to send too large of a book. Book Size: " + byteTotal + " - Allowed:  "+ byteAllowed + " - Pages: " + pageList.size());
                server.scheduleOnMain(() -> this.disconnect("Book too large!", org.bukkit.event.player.PlayerKickEvent.Cause.ILLEGAL_ACTION)); // Paper - kick event cause
                return;
            }
        }
        // Paper end
        // CraftBukkit start
        if (this.lastBookTick + 20 > MinecraftServer.currentTick) {
            server.scheduleOnMain(() -> this.disconnect("Book edited too quickly!", org.bukkit.event.player.PlayerKickEvent.Cause.ILLEGAL_ACTION)); // Paper - kick event cause // Paper - Also ensure this is called on main
            return;
        }
        this.lastBookTick = MinecraftServer.currentTick;
        // CraftBukkit end
        int i = packet.getSlot();

        if (Inventory.isHotbarSlot(i) || i == 40) {
            List<String> list = Lists.newArrayList();
            Optional<String> optional = packet.getTitle();

            Objects.requireNonNull(list);
            optional.ifPresent(list::add);
            Stream<String> stream = packet.getPages().stream().limit(100L); // CraftBukkit - decompile error

            Objects.requireNonNull(list);
            stream.forEach(list::add);
            this.filterTextPacket((List) list, optional.isPresent() ? (list1) -> {
                this.signBook((TextFilter.FilteredText) list1.get(0), list1.subList(1, list1.size()), i);
            } : (list1) -> {
                this.updateBookContents(list1, i);
            });
        }
    }

    private void updateBookContents(List<TextFilter.FilteredText> pages, int slotId) {
        ItemStack itemstack = this.player.getInventory().getItem(slotId);

        if (itemstack.is(Items.WRITABLE_BOOK)) {
            this.updateBookPages(pages, UnaryOperator.identity(), itemstack.copy(), slotId, itemstack); // CraftBukkit
        }
    }

    private void signBook(TextFilter.FilteredText title, List<TextFilter.FilteredText> pages, int slotId) {
        ItemStack itemstack = this.player.getInventory().getItem(slotId);

        if (itemstack.is(Items.WRITABLE_BOOK)) {
            ItemStack itemstack1 = new ItemStack(Items.WRITTEN_BOOK);
            CompoundTag nbttagcompound = itemstack.getTag();

            if (nbttagcompound != null) {
                itemstack1.setTag(nbttagcompound.copy());
            }

            itemstack1.addTagElement("author", StringTag.valueOf(this.player.getName().getString()));
            if (this.player.isTextFilteringEnabled()) {
                itemstack1.addTagElement("title", StringTag.valueOf(title.getFiltered()));
            } else {
                itemstack1.addTagElement("filtered_title", StringTag.valueOf(title.getFiltered()));
                itemstack1.addTagElement("title", StringTag.valueOf(title.getRaw()));
            }

            this.updateBookPages(pages, (s) -> {
                return Component.Serializer.toJson(new TextComponent(s));
            }, itemstack1, slotId, itemstack); // CraftBukkit
            this.player.getInventory().setItem(slotId, itemstack); // CraftBukkit - event factory updates the hand book
        }
    }

    private void updateBookPages(List<TextFilter.FilteredText> list, UnaryOperator<String> unaryoperator, ItemStack itemstack, int slot, ItemStack handItem) { // CraftBukkit
        ListTag nbttaglist = new ListTag();

        if (this.player.isTextFilteringEnabled()) {
            Stream<StringTag> stream = list.stream().map((itextfilter_a) -> { // CraftBukkit - decompile error
                return StringTag.valueOf((String) unaryoperator.apply(itextfilter_a.getFiltered()));
            });

            Objects.requireNonNull(nbttaglist);
            stream.forEach(nbttaglist::add);
        } else {
            CompoundTag nbttagcompound = new CompoundTag();
            int i = 0;

            for (int j = list.size(); i < j; ++i) {
                TextFilter.FilteredText itextfilter_a = (TextFilter.FilteredText) list.get(i);
                String s = itextfilter_a.getRaw();

                nbttaglist.add(StringTag.valueOf((String) unaryoperator.apply(s)));
                String s1 = itextfilter_a.getFiltered();

                if (!s.equals(s1)) {
                    nbttagcompound.putString(String.valueOf(i), (String) unaryoperator.apply(s1));
                }
            }

            if (!nbttagcompound.isEmpty()) {
                itemstack.addTagElement("filtered_pages", nbttagcompound);
            }
        }

        itemstack.addTagElement("pages", nbttaglist);
        this.player.getInventory().setItem(slot, CraftEventFactory.handleEditBookEvent(player, slot, handItem, itemstack)); // CraftBukkit // Paper - Don't ignore result (see other callsite for handleEditBookEvent)
    }

    @Override
    public void handleEntityTagQuery(ServerboundEntityTagQuery packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.hasPermissions(2)) {
            Entity entity = this.player.getLevel().getEntity(packet.getEntityId());

            if (entity != null) {
                CompoundTag nbttagcompound = entity.saveWithoutId(new CompoundTag());

                this.player.connection.send(new ClientboundTagQueryPacket(packet.getTransactionId(), nbttagcompound));
            }

        }
    }

    @Override
    public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQuery packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.hasPermissions(2)) {
            BlockEntity tileentity = this.player.getLevel().getBlockEntity(packet.getPos());
            CompoundTag nbttagcompound = tileentity != null ? tileentity.saveWithoutMetadata() : null;

            this.player.connection.send(new ClientboundTagQueryPacket(packet.getTransactionId(), nbttagcompound));
        }
    }

    @Override
    public void handleMovePlayer(ServerboundMovePlayerPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (ServerGamePacketListenerImpl.containsInvalidValues(packet.getX(0.0D), packet.getY(0.0D), packet.getZ(0.0D), packet.getYRot(0.0F), packet.getXRot(0.0F))) {
            this.disconnect(new TranslatableComponent("multiplayer.disconnect.invalid_player_movement"), org.bukkit.event.player.PlayerKickEvent.Cause.INVALID_PLAYER_MOVEMENT); // Paper - kick event cause
        } else {
            ServerLevel worldserver = this.player.getLevel();

            if (!this.player.wonGame && !this.player.isImmobile()) { // CraftBukkit
                if (this.tickCount == 0) {
                    this.resetPosition();
                }

                if (this.awaitingPositionFromClient != null) {
                    if (false && this.tickCount - this.awaitingTeleportTime > 20) { // Paper - this will greatly screw with clients with > 1000ms RTT
                        this.awaitingTeleportTime = this.tickCount;
                        this.teleport(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
                    }
                    this.allowedPlayerTicks = 20; // CraftBukkit
                } else {
                    this.awaitingTeleportTime = this.tickCount;
                    double d0 = ServerGamePacketListenerImpl.clampHorizontal(packet.getX(this.player.getX())); final double toX = d0; // Paper - OBFHELPER
                    double d1 = ServerGamePacketListenerImpl.clampVertical(packet.getY(this.player.getY())); final double toY = d1;
                    double d2 = ServerGamePacketListenerImpl.clampHorizontal(packet.getZ(this.player.getZ())); final double toZ = d2; // Paper - OBFHELPER
                    float f = Mth.wrapDegrees(packet.getYRot(this.player.getYRot()));
                    float f1 = Mth.wrapDegrees(packet.getXRot(this.player.getXRot()));

                    if (this.player.isPassenger()) {
                        this.player.absMoveTo(this.player.getX(), this.player.getY(), this.player.getZ(), f, f1);
                        this.player.getLevel().getChunkSource().move(this.player);
                        this.allowedPlayerTicks = 20; // CraftBukkit
                    } else {
                        // CraftBukkit - Make sure the move is valid but then reset it for plugins to modify
                        double prevX = this.player.getX();
                        double prevY = this.player.getY();
                        double prevZ = this.player.getZ();
                        float prevYaw = this.player.getYRot();
                        float prevPitch = this.player.getXRot();
                        // CraftBukkit end
                        double d3 = this.player.getX();
                        double d4 = this.player.getY();
                        double d5 = this.player.getZ();
                        double d6 = this.player.getY();
                        double d7 = d0 - this.firstGoodX;
                        double d8 = d1 - this.firstGoodY;
                        double d9 = d2 - this.firstGoodZ;
                        double d10 = this.player.getDeltaMovement().lengthSqr();
                        // Paper start - fix large move vectors killing the server
                        double currDeltaX = toX - prevX;
                        double currDeltaY = toY - prevY;
                        double currDeltaZ = toZ - prevZ;
                        double d11 = Math.max(d7 * d7 + d8 * d8 + d9 * d9, (currDeltaX * currDeltaX + currDeltaY * currDeltaY + currDeltaZ * currDeltaZ) - 1);
                        // Paper end - fix large move vectors killing the server
                        // Paper start - fix large move vectors killing the server
                        double otherFieldX = d0 - this.lastGoodX;
                        double otherFieldY = d1 - this.lastGoodY;
                        double otherFieldZ = d2 - this.lastGoodZ;
                        d11 = Math.max(d11, (otherFieldX * otherFieldX + otherFieldY * otherFieldY + otherFieldZ * otherFieldZ) - 1);
                        // Paper end - fix large move vectors killing the server

                        if (this.player.isSleeping()) {
                            if (d11 > 1.0D) {
                                this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), f, f1);
                            }

                        } else {
                            ++this.receivedMovePacketCount;
                            int i = this.receivedMovePacketCount - this.knownMovePacketCount;

                            // CraftBukkit start - handle custom speeds and skipped ticks
                            this.allowedPlayerTicks += (System.currentTimeMillis() / 50) - this.lastTick;
                            this.allowedPlayerTicks = Math.max(this.allowedPlayerTicks, 1);
                            this.lastTick = (int) (System.currentTimeMillis() / 50);

                            if (i > Math.max(this.allowedPlayerTicks, 5)) {
                                ServerGamePacketListenerImpl.LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", this.player.getName().getString(), i);
                                i = 1;
                            }

                            if (packet.hasRot || d11 > 0) {
                                this.allowedPlayerTicks -= 1;
                            } else {
                                this.allowedPlayerTicks = 20;
                            }
                            double speed;
                            if (this.player.getAbilities().flying) {
                                speed = this.player.getAbilities().flyingSpeed * 20f;
                            } else {
                                speed = this.player.getAbilities().walkingSpeed * 10f;
                            }
                            // Paper start - Prevent moving into unloaded chunks
                            if (player.level.paperConfig.preventMovingIntoUnloadedChunks && (this.player.getX() != toX || this.player.getZ() != toZ) && !worldserver.areChunksLoadedForMove(this.player.getBoundingBox().expandTowards(new Vec3(toX, toY, toZ).subtract(this.player.position())))) {
                                this.internalTeleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot(), Collections.emptySet(), true);
                                return;
                            }
                            // Paper end

                            if (!this.player.isChangingDimension() && (!this.player.getLevel().getGameRules().getBoolean(GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK) || !this.player.isFallFlying())) {
                                float f2 = this.player.isFallFlying() ? 300.0F : 100.0F;

                                if (d11 - d10 > Math.max(f2, Math.pow((double) (org.spigotmc.SpigotConfig.movedTooQuicklyMultiplier * (float) i * speed), 2)) && !this.isSingleplayerOwner()) {
                                // CraftBukkit end
                                    ServerGamePacketListenerImpl.LOGGER.warn("{} moved too quickly! {},{},{}", new Object[]{this.player.getName().getString(), d7, d8, d9});
                                    this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot());
                                    return;
                                }
                            }

                            AABB axisalignedbb = this.player.getBoundingBox(); // Paper - diff on change, should be old AABB

                            d7 = d0 - this.lastGoodX; // Paper - diff on change, used for checking large move vectors above
                            d8 = d1 - this.lastGoodY; // Paper - diff on change, used for checking large move vectors above
                            d9 = d2 - this.lastGoodZ; // Paper - diff on change, used for checking large move vectors above
                            boolean flag = d8 > 0.0D;

                            if (this.player.isOnGround() && !packet.isOnGround() && flag) {
                                // Paper start - Add player jump event
                                Player player = this.getCraftPlayer();
                                Location from = new Location(player.getWorld(), lastPosX, lastPosY, lastPosZ, lastYaw, lastPitch); // Get the Players previous Event location.
                                Location to = player.getLocation().clone(); // Start off the To location as the Players current location.

                                // If the packet contains movement information then we update the To location with the correct XYZ.
                                if (packet.hasPos) {
                                    to.setX(packet.x);
                                    to.setY(packet.y);
                                    to.setZ(packet.z);
                                }

                                // If the packet contains look information then we update the To location with the correct Yaw & Pitch.
                                if (packet.hasRot) {
                                    to.setYaw(packet.yRot);
                                    to.setPitch(packet.xRot);
                                }

                                com.destroystokyo.paper.event.player.PlayerJumpEvent event = new com.destroystokyo.paper.event.player.PlayerJumpEvent(player, from, to);

                                if (event.callEvent()) {
                                    this.player.jumpFromGround();
                                } else {
                                    from = event.getFrom();
                                    this.internalTeleport(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch(), Collections.emptySet(), false);
                                    return;
                                }
                                // Paper end
                            }

                            boolean flag1 = this.player.verticalCollisionBelow;

                            this.player.move(MoverType.PLAYER, new Vec3(d7, d8, d9));
                            boolean didCollide = toX != this.player.getX() || toY != this.player.getY() || toZ != this.player.getZ(); // Paper - needed here as the difference in Y can be reset - also note: this is only a guess at whether collisions took place, floating point errors can make this true when it shouldn't be...
                            this.player.onGround = packet.isOnGround(); // CraftBukkit - SPIGOT-5810, SPIGOT-5835, SPIGOT-6828: reset by this.player.move
                            // Paper start - prevent position desync
                            if (this.awaitingPositionFromClient != null) {
                                return; // ... thanks Mojang for letting move calls teleport across dimensions.
                            }
                            // Paper end - prevent position desync
                            double d12 = d8;

                            d7 = d0 - this.player.getX();
                            d8 = d1 - this.player.getY();
                            if (d8 > -0.5D || d8 < 0.5D) {
                                d8 = 0.0D;
                            }

                            d9 = d2 - this.player.getZ();
                            d11 = d7 * d7 + d8 * d8 + d9 * d9;
                            boolean flag2 = false;

                            if (!this.player.isChangingDimension() && d11 > org.spigotmc.SpigotConfig.movedWronglyThreshold && !this.player.isSleeping() && !this.player.gameMode.isCreative() && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) { // Spigot
                                flag2 = true; // Paper - diff on change, this should be moved wrongly
                                ServerGamePacketListenerImpl.LOGGER.warn("{} moved wrongly!", this.player.getName().getString());
                            }

                            this.player.absMoveTo(d0, d1, d2, f, f1);
                            // Paper start - optimise out extra getCubes
                            // Original for reference:
                            // boolean teleportBack = flag2 && worldserver.getCubes(this.player, axisalignedbb) || (didCollide && this.a((IWorldReader) worldserver, axisalignedbb));
                            boolean teleportBack = flag2; // violating this is always a fail
                            if (!this.player.noPhysics && !this.player.isSleeping() && !teleportBack) {
                                AABB newBox = this.player.getBoundingBox();
                                if (didCollide || !axisalignedbb.equals(newBox)) {
                                    // note: only call after setLocation, or else getBoundingBox is wrong
                                    teleportBack = this.hasNewCollision(worldserver, this.player, axisalignedbb, newBox);
                                } // else: no collision at all detected, why do we care?
                            }
                            if (!this.player.noPhysics && !this.player.isSleeping() && teleportBack) { // Paper end - optimise out extra getCubes
                                this.teleport(d3, d4, d5, f, f1);
                            } else {
                                // CraftBukkit start - fire PlayerMoveEvent
                                // Rest to old location first
                                this.player.absMoveTo(prevX, prevY, prevZ, prevYaw, prevPitch);

                                Player player = this.getCraftPlayer();
                                Location from = new Location(player.getWorld(), this.lastPosX, this.lastPosY, this.lastPosZ, this.lastYaw, this.lastPitch); // Get the Players previous Event location.
                                Location to = player.getLocation().clone(); // Start off the To location as the Players current location.

                                // If the packet contains movement information then we update the To location with the correct XYZ.
                                if (packet.hasPos) {
                                    to.setX(packet.x);
                                    to.setY(packet.y);
                                    to.setZ(packet.z);
                                }

                                // If the packet contains look information then we update the To location with the correct Yaw & Pitch.
                                if (packet.hasRot) {
                                    to.setYaw(packet.yRot);
                                    to.setPitch(packet.xRot);
                                }

                                // Prevent 40 event-calls for less than a single pixel of movement >.>
                                double delta = Math.pow(this.lastPosX - to.getX(), 2) + Math.pow(this.lastPosY - to.getY(), 2) + Math.pow(this.lastPosZ - to.getZ(), 2);
                                float deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());

                                if ((delta > 1f / 256 || deltaAngle > 10f) && !this.player.isImmobile()) {
                                    this.lastPosX = to.getX();
                                    this.lastPosY = to.getY();
                                    this.lastPosZ = to.getZ();
                                    this.lastYaw = to.getYaw();
                                    this.lastPitch = to.getPitch();

                                    // Skip the first time we do this
                                    if (from.getX() != Double.MAX_VALUE) {
                                        Location oldTo = to.clone();
                                        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
                                        this.cserver.getPluginManager().callEvent(event);

                                        // If the event is cancelled we move the player back to their old location.
                                        if (event.isCancelled()) {
                                            this.teleport(from);
                                            return;
                                        }

                                        // If a Plugin has changed the To destination then we teleport the Player
                                        // there to avoid any 'Moved wrongly' or 'Moved too quickly' errors.
                                        // We only do this if the Event was not cancelled.
                                        if (!oldTo.equals(event.getTo()) && !event.isCancelled()) {
                                            this.player.getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                                            return;
                                        }

                                        // Check to see if the Players Location has some how changed during the call of the event.
                                        // This can happen due to a plugin teleporting the player instead of using .setTo()
                                        if (!from.equals(this.getCraftPlayer().getLocation()) && this.justTeleported) {
                                            this.justTeleported = false;
                                            return;
                                        }
                                    }
                                }
                                this.player.absMoveTo(d0, d1, d2, f, f1); // Copied from above
                                // CraftBukkit end

                                this.clientIsFloating = d12 >= -0.03125D && !flag1 && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR && !this.server.isFlightAllowed() && !this.player.getAbilities().mayfly && !this.player.hasEffect(MobEffects.LEVITATION) && !this.player.isFallFlying() && !this.player.isAutoSpinAttack() && this.noBlocksAround(this.player);
                                this.player.getLevel().getChunkSource().move(this.player);
                                this.player.doCheckFallDamage(this.player.getY() - d6, packet.isOnGround());
                                this.player.setOnGround(packet.isOnGround());
                                if (flag) {
                                    this.player.resetFallDistance();
                                }

                                this.player.checkMovementStatistics(this.player.getX() - d3, this.player.getY() - d4, this.player.getZ() - d5);
                                this.lastGoodX = this.player.getX();
                                this.lastGoodY = this.player.getY();
                                this.lastGoodZ = this.player.getZ();
                            }
                        }
                    }
                }
            }
        }
    }

    // Paper start - optimise out extra getCubes
    private boolean hasNewCollision(final ServerLevel world, final Entity entity, final AABB oldBox, final AABB newBox) {
        final List<AABB> collisions = io.papermc.paper.util.CachedLists.getTempCollisionList();
        try {
            io.papermc.paper.util.CollisionUtil.getCollisions(world, entity, newBox, collisions, false, true,
                true, false, null, null);

            for (int i = 0, len = collisions.size(); i < len; ++i) {
                final AABB box = collisions.get(i);
                if (!io.papermc.paper.util.CollisionUtil.voxelShapeIntersect(box, oldBox)) {
                    return true;
                }
            }

            return false;
        } finally {
            io.papermc.paper.util.CachedLists.returnTempCollisionList(collisions);
        }
    }
    // Paper end - optimise out extra getCubes

    private boolean isPlayerCollidingWithAnythingNew(LevelReader world, AABB box) {
        Iterable<VoxelShape> iterable = world.getCollisions(this.player, this.player.getBoundingBox().deflate(9.999999747378752E-6D));
        VoxelShape voxelshape = Shapes.create(box.deflate(9.999999747378752E-6D));
        Iterator iterator = iterable.iterator();

        VoxelShape voxelshape1;

        do {
            if (!iterator.hasNext()) {
                return false;
            }

            voxelshape1 = (VoxelShape) iterator.next();
        } while (Shapes.joinIsNotEmpty(voxelshape1, voxelshape, BooleanOp.AND));

        return true;
    }

    // CraftBukkit start - Delegate to teleport(Location)
    public void dismount(double x, double y, double z, float yaw, float pitch) {
        this.dismount(x, y, z, yaw, pitch, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public void dismount(double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        this.teleport(d0, d1, d2, f, f1, Collections.emptySet(), true, cause);
    }

    public void teleport(double x, double y, double z, float yaw, float pitch) {
        this.teleport(x, y, z, yaw, pitch, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public void teleport(double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        this.teleport(d0, d1, d2, f, f1, Collections.emptySet(), false, cause);
    }

    public void teleport(double x, double y, double z, float yaw, float pitch, Set<ClientboundPlayerPositionPacket.RelativeArgument> flags) {
        this.teleport(x, y, z, yaw, pitch, flags, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public void teleport(double d0, double d1, double d2, float f, float f1, Set<ClientboundPlayerPositionPacket.RelativeArgument> set, PlayerTeleportEvent.TeleportCause cause) {
        this.teleport(d0, d1, d2, f, f1, set, false, cause);
    }

    public boolean teleport(double d0, double d1, double d2, float f, float f1, Set<ClientboundPlayerPositionPacket.RelativeArgument> set, boolean flag, PlayerTeleportEvent.TeleportCause cause) { // CraftBukkit - Return event status
        Player player = this.getCraftPlayer();
        Location from = player.getLocation();

        double x = d0;
        double y = d1;
        double z = d2;
        float yaw = f;
        float pitch = f1;

        Location to = new Location(this.getCraftPlayer().getWorld(), x, y, z, yaw, pitch);
        // SPIGOT-5171: Triggered on join
        if (from.equals(to)) {
            this.internalTeleport(d0, d1, d2, f, f1, set, flag);
            return false; // CraftBukkit - Return event status
        }

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from.clone(), to.clone(), cause);
        this.cserver.getPluginManager().callEvent(event);

        if (event.isCancelled() || !to.equals(event.getTo())) {
            set.clear(); // Can't relative teleport
            to = event.isCancelled() ? event.getFrom() : event.getTo();
            d0 = to.getX();
            d1 = to.getY();
            d2 = to.getZ();
            f = to.getYaw();
            f1 = to.getPitch();
        }

        this.internalTeleport(d0, d1, d2, f, f1, set, flag);
        return event.isCancelled(); // CraftBukkit - Return event status
    }

    public void teleport(Location dest) {
        this.internalTeleport(dest.getX(), dest.getY(), dest.getZ(), dest.getYaw(), dest.getPitch(), Collections.<ClientboundPlayerPositionPacket.RelativeArgument>emptySet(), true);
    }

    private void internalTeleport(double d0, double d1, double d2, float f, float f1, Set<ClientboundPlayerPositionPacket.RelativeArgument> set, boolean flag) {
        // Paper start
        if (player.isRemoved()) {
            LOGGER.info("Attempt to teleport removed player {} restricted", player.getScoreboardName());
            if (server.isDebugging()) io.papermc.paper.util.TraceUtil.dumpTraceForThread("Attempt to teleport removed player");
            return;
        }
        // Paper end
        // CraftBukkit start
        if (Float.isNaN(f)) {
            f = 0;
        }
        if (Float.isNaN(f1)) {
            f1 = 0;
        }

        this.justTeleported = true;
        // CraftBukkit end
        double d3 = set.contains(ClientboundPlayerPositionPacket.RelativeArgument.X) ? this.player.getX() : 0.0D;
        double d4 = set.contains(ClientboundPlayerPositionPacket.RelativeArgument.Y) ? this.player.getY() : 0.0D;
        double d5 = set.contains(ClientboundPlayerPositionPacket.RelativeArgument.Z) ? this.player.getZ() : 0.0D;
        float f2 = set.contains(ClientboundPlayerPositionPacket.RelativeArgument.Y_ROT) ? this.player.getYRot() : 0.0F;
        float f3 = set.contains(ClientboundPlayerPositionPacket.RelativeArgument.X_ROT) ? this.player.getXRot() : 0.0F;

        this.awaitingPositionFromClient = new Vec3(d0, d1, d2);
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }

        // CraftBukkit start - update last location
        this.lastPosX = this.awaitingPositionFromClient.x;
        this.lastPosY = this.awaitingPositionFromClient.y;
        this.lastPosZ = this.awaitingPositionFromClient.z;
        this.lastYaw = f;
        this.lastPitch = f1;
        // CraftBukkit end

        this.awaitingTeleportTime = this.tickCount;
        this.player.moveTo(d0, d1, d2, f, f1); // Paper - use proper setPositionRotation for teleportation
        this.player.connection.send(new ClientboundPlayerPositionPacket(d0 - d3, d1 - d4, d2 - d5, f - f2, f1 - f3, set, this.awaitingTeleport, flag));
    }

    @Override
    public void handlePlayerAction(ServerboundPlayerActionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.isImmobile()) return; // CraftBukkit
        BlockPos blockposition = packet.getPos();

        this.player.resetLastActionTime();
        ServerboundPlayerActionPacket.Action packetplayinblockdig_enumplayerdigtype = packet.getAction();

        switch (packetplayinblockdig_enumplayerdigtype) {
            case SWAP_ITEM_WITH_OFFHAND:
                if (!this.player.isSpectator()) {
                    ItemStack itemstack = this.player.getItemInHand(InteractionHand.OFF_HAND);

                    // CraftBukkit start - inspiration taken from DispenserRegistry (See SpigotCraft#394)
                    CraftItemStack mainHand = CraftItemStack.asCraftMirror(itemstack);
                    CraftItemStack offHand = CraftItemStack.asCraftMirror(this.player.getItemInHand(InteractionHand.MAIN_HAND));
                    PlayerSwapHandItemsEvent swapItemsEvent = new PlayerSwapHandItemsEvent(this.getCraftPlayer(), mainHand.clone(), offHand.clone());
                    this.cserver.getPluginManager().callEvent(swapItemsEvent);
                    if (swapItemsEvent.isCancelled()) {
                        return;
                    }
                    if (swapItemsEvent.getOffHandItem().equals(offHand)) {
                        this.player.setItemInHand(InteractionHand.OFF_HAND, this.player.getItemInHand(InteractionHand.MAIN_HAND));
                    } else {
                        this.player.setItemInHand(InteractionHand.OFF_HAND, CraftItemStack.asNMSCopy(swapItemsEvent.getOffHandItem()));
                    }
                    if (swapItemsEvent.getMainHandItem().equals(mainHand)) {
                        this.player.setItemInHand(InteractionHand.MAIN_HAND, itemstack);
                    } else {
                        this.player.setItemInHand(InteractionHand.MAIN_HAND, CraftItemStack.asNMSCopy(swapItemsEvent.getMainHandItem()));
                    }
                    // CraftBukkit end
                    this.player.stopUsingItem();
                }

                return;
            case DROP_ITEM:
                if (!this.player.isSpectator()) {
                    // limit how quickly items can be dropped
                    // If the ticks aren't the same then the count starts from 0 and we update the lastDropTick.
                    if (this.lastDropTick != MinecraftServer.currentTick) {
                        this.dropCount = 0;
                        this.lastDropTick = MinecraftServer.currentTick;
                    } else {
                        // Else we increment the drop count and check the amount.
                        this.dropCount++;
                        if (this.dropCount >= 20) {
                            ServerGamePacketListenerImpl.LOGGER.warn(this.player.getScoreboardName() + " dropped their items too quickly!");
                            this.disconnect("You dropped your items too quickly (Hacking?)", org.bukkit.event.player.PlayerKickEvent.Cause.ILLEGAL_ACTION); // Paper - kick event cause
                            return;
                        }
                    }
                    // CraftBukkit end
                    this.player.drop(false);
                }

                return;
            case DROP_ALL_ITEMS:
                if (!this.player.isSpectator()) {
                    this.player.drop(true);
                }

                return;
            case RELEASE_USE_ITEM:
                this.player.releaseUsingItem();
                return;
            case START_DESTROY_BLOCK:
            case ABORT_DESTROY_BLOCK:
            case STOP_DESTROY_BLOCK:
                // Paper start - Don't allow digging in unloaded chunks
                if (this.player.level.getChunkIfLoadedImmediately(blockposition.getX() >> 4, blockposition.getZ() >> 4) == null) {
                    return;
                }
                this.player.gameMode.handleBlockBreakAction(blockposition, packetplayinblockdig_enumplayerdigtype, packet.getDirection(), this.player.level.getMaxBuildHeight());
                // Paper end - Don't allow digging in unloaded chunks
                return;
            default:
                throw new IllegalArgumentException("Invalid player action");
        }
    }

    private static boolean wasBlockPlacementAttempt(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            Item item = stack.getItem();

            return (item instanceof BlockItem || item instanceof BucketItem) && !player.getCooldowns().isOnCooldown(item);
        }
    }

    // Spigot start - limit place/interactions
    private int limitedPackets;
    private long lastLimitedPacket = -1;
    private static final int THRESHOLD = com.destroystokyo.paper.PaperConfig.packetInSpamThreshold; // Paper - Configurable threshold

    private boolean checkLimit(long timestamp) {
        if (this.lastLimitedPacket != -1 && timestamp - this.lastLimitedPacket < THRESHOLD && this.limitedPackets++ >= 8) { // Paper - Use threshold, raise packet limit to 8
            return false;
        }

        if (this.lastLimitedPacket == -1 || timestamp - this.lastLimitedPacket >= THRESHOLD) { // Paper
            this.lastLimitedPacket = timestamp;
            this.limitedPackets = 0;
            return true;
        }

        return true;
    }
    // Spigot end

    // Paper start
    private static final int SURVIVAL_PLACE_DISTANCE_SQUARED = 6 * 6;
    private static final int CREATIVE_PLACE_DISTANCE_SQUARED = 7 * 7;
    private boolean isOutsideOfReach(double x, double y, double z) {
        Location eyeLoc = this.getCraftPlayer().getEyeLocation();
        double reachDistance = org.bukkit.util.NumberConversions.square(eyeLoc.getX() - x) + org.bukkit.util.NumberConversions.square(eyeLoc.getY() - y) + org.bukkit.util.NumberConversions.square(eyeLoc.getZ() - z);
        return reachDistance > (this.getCraftPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE ? CREATIVE_PLACE_DISTANCE_SQUARED : SURVIVAL_PLACE_DISTANCE_SQUARED);
    }
    // Paper end

    @Override
    public void handleUseItemOn(ServerboundUseItemOnPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.isImmobile()) return; // CraftBukkit
        if (!this.checkLimit(packet.timestamp)) return; // Spigot - check limit
        ServerLevel worldserver = this.player.getLevel();
        InteractionHand enumhand = packet.getHand();
        ItemStack itemstack = this.player.getItemInHand(enumhand);
        BlockHitResult movingobjectpositionblock = packet.getHitResult();
        Vec3 vec3d = movingobjectpositionblock.getLocation();
        BlockPos blockposition = movingobjectpositionblock.getBlockPos();
        Vec3 vec3d1 = vec3d.subtract(Vec3.atCenterOf(blockposition));

        // Paper start - improve distance check
        final Vec3 clickedLocation = movingobjectpositionblock.getLocation();
        if (isOutsideOfReach(blockposition.getX() + 0.5D, blockposition.getY() + 0.5D, blockposition.getZ() + 0.5D)
            || !Double.isFinite(clickedLocation.x) || !Double.isFinite(clickedLocation.y) || !Double.isFinite(clickedLocation.z)
            || isOutsideOfReach(clickedLocation.x, clickedLocation.y, clickedLocation.z)) {
            return;
        }
        // Paper end

        if (this.player.level.getServer() != null && this.player.chunkPosition().getChessboardDistance(new ChunkPos(blockposition)) < this.player.level.spigotConfig.viewDistance) { // Spigot
            double d0 = 1.0000001D;

            if (Math.abs(vec3d1.x()) < 1.0000001D && Math.abs(vec3d1.y()) < 1.0000001D && Math.abs(vec3d1.z()) < 1.0000001D) {
                Direction enumdirection = movingobjectpositionblock.getDirection();

                this.player.resetLastActionTime();
                int i = this.player.level.getMaxBuildHeight();

                if (blockposition.getY() < i) {
                    if (this.awaitingPositionFromClient == null && this.player.distanceToSqr((double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 0.5D, (double) blockposition.getZ() + 0.5D) < 64.0D && (worldserver.mayInteract(this.player, blockposition)  || (worldserver.paperConfig.allowUsingSignsInsideSpawnProtection && worldserver.getBlockState(blockposition).getBlock() instanceof net.minecraft.world.level.block.SignBlock))) { // Paper - sign check
                        this.player.stopUsingItem(); // CraftBukkit - SPIGOT-4706
                        InteractionResult enuminteractionresult = this.player.gameMode.useItemOn(this.player, worldserver, itemstack, enumhand, movingobjectpositionblock);

                        if (enumdirection == Direction.UP && !enuminteractionresult.consumesAction() && blockposition.getY() >= i - 1 && ServerGamePacketListenerImpl.wasBlockPlacementAttempt(this.player, itemstack)) {
                            MutableComponent ichatmutablecomponent = (new TranslatableComponent("build.tooHigh", new Object[]{i - 1})).withStyle(ChatFormatting.RED);

                            this.player.sendMessage(ichatmutablecomponent, ChatType.GAME_INFO, Util.NIL_UUID);
                } else if (enuminteractionresult.shouldSwing() && !this.player.gameMode.interactResult) {
                            this.player.swing(enumhand, true);
                        }
                    }
                } else {
                    MutableComponent ichatmutablecomponent1 = (new TranslatableComponent("build.tooHigh", new Object[]{i - 1})).withStyle(ChatFormatting.RED);

                    this.player.sendMessage(ichatmutablecomponent1, ChatType.GAME_INFO, Util.NIL_UUID);
                }

                this.player.connection.send(new ClientboundBlockUpdatePacket(worldserver, blockposition));
                this.player.connection.send(new ClientboundBlockUpdatePacket(worldserver, blockposition.relative(enumdirection)));
            } else {
                ServerGamePacketListenerImpl.LOGGER.warn("Ignoring UseItemOnPacket from {}: Location {} too far away from hit block {}.", new Object[]{this.player.getGameProfile().getName(), vec3d, blockposition});
            }
        } else {
            ServerGamePacketListenerImpl.LOGGER.warn("Ignoring UseItemOnPacket from {}: hit position {} too far away from player {}.", new Object[]{this.player.getGameProfile().getName(), blockposition, this.player.blockPosition()});
        }
    }

    @Override
    public void handleUseItem(ServerboundUseItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.isImmobile()) return; // CraftBukkit
        if (!this.checkLimit(packet.timestamp)) return; // Spigot - check limit
        ServerLevel worldserver = this.player.getLevel();
        InteractionHand enumhand = packet.getHand();
        ItemStack itemstack = this.player.getItemInHand(enumhand);

        this.player.resetLastActionTime();
        if (!itemstack.isEmpty()) {
            // CraftBukkit start
            // Raytrace to look for 'rogue armswings'
            float f1 = this.player.getXRot();
            float f2 = this.player.getYRot();
            double d0 = this.player.getX();
            double d1 = this.player.getY() + (double) this.player.getEyeHeight();
            double d2 = this.player.getZ();
            Vec3 vec3d = new Vec3(d0, d1, d2);

            float f3 = Mth.cos(-f2 * 0.017453292F - 3.1415927F);
            float f4 = Mth.sin(-f2 * 0.017453292F - 3.1415927F);
            float f5 = -Mth.cos(-f1 * 0.017453292F);
            float f6 = Mth.sin(-f1 * 0.017453292F);
            float f7 = f4 * f5;
            float f8 = f3 * f5;
            double d3 = player.gameMode.getGameModeForPlayer()== GameType.CREATIVE ? 5.0D : 4.5D;
            Vec3 vec3d1 = vec3d.add((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
            HitResult movingobjectposition = this.player.level.clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.player));

            boolean cancelled;
            if (movingobjectposition == null || movingobjectposition.getType() != HitResult.Type.BLOCK) {
                org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.RIGHT_CLICK_AIR, itemstack, enumhand);
                cancelled = event.useItemInHand() == Event.Result.DENY;
            } else {
                BlockHitResult movingobjectpositionblock = (BlockHitResult) movingobjectposition;
                if (player.gameMode.firedInteract && player.gameMode.interactPosition.equals(movingobjectpositionblock.getBlockPos()) && player.gameMode.interactHand == enumhand && ItemStack.tagMatches(player.gameMode.interactItemStack, itemstack)) {
                    cancelled = player.gameMode.interactResult;
                } else {
                    org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, movingobjectpositionblock.getBlockPos(), movingobjectpositionblock.getDirection(), itemstack, true, enumhand);
                    cancelled = event.useItemInHand() == Event.Result.DENY;
                }
                player.gameMode.firedInteract = false;
            }

            if (cancelled) {
                this.player.getBukkitEntity().updateInventory(); // SPIGOT-2524
                return;
            }
            // Paper start
            itemstack = this.player.getItemInHand(enumhand);
            if (itemstack.isEmpty()) return;
            // Paper end
            InteractionResult enuminteractionresult = this.player.gameMode.useItem(this.player, worldserver, itemstack, enumhand);

            if (enuminteractionresult.shouldSwing()) {
                this.player.swing(enumhand, true);
            }

        }
    }

    @Override
    public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.isSpectator()) {
            Iterator iterator = this.server.getAllLevels().iterator();

            while (iterator.hasNext()) {
                ServerLevel worldserver = (ServerLevel) iterator.next();
                Entity entity = packet.getEntity(worldserver);

                if (entity != null) {
                    this.player.teleportTo(worldserver, entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot(), org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.SPECTATE); // CraftBukkit
                    return;
                }
            }
        }

    }

    @Override
    public void handleResourcePackResponse(ServerboundResourcePackPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (packet.getAction() == ServerboundResourcePackPacket.Action.DECLINED && this.server.isResourcePackRequired()) {
            ServerGamePacketListenerImpl.LOGGER.info("Disconnecting {} due to resource pack rejection", this.player.getName());
            this.disconnect(new TranslatableComponent("multiplayer.requiredTexturePrompt.disconnect"), org.bukkit.event.player.PlayerKickEvent.Cause.RESOURCE_PACK_REJECTION); // Paper - add cause
        }
        // Paper start
        PlayerResourcePackStatusEvent.Status packStatus = PlayerResourcePackStatusEvent.Status.values()[packet.action.ordinal()];
        player.getBukkitEntity().setResourcePackStatus(packStatus);
        this.cserver.getPluginManager().callEvent(new PlayerResourcePackStatusEvent(this.getCraftPlayer(), packStatus)); // CraftBukkit
        // Paper end
    }

    @Override
    public void handlePaddleBoat(ServerboundPaddleBoatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        Entity entity = this.player.getVehicle();

        if (entity instanceof Boat) {
            ((Boat) entity).setPaddleState(packet.getLeft(), packet.getRight());
        }

    }

    @Override
    public void handlePong(ServerboundPongPacket packet) {}

    @Override
    public void onDisconnect(Component reason) {
        // Paper start
        this.onDisconnect(reason, null);
    }
    public void onDisconnect(Component reason, @Nullable net.kyori.adventure.text.Component quitMessage) {
        // Paper end
        // CraftBukkit start - Rarely it would send a disconnect line twice
        if (this.processedDisconnect) {
            return;
        } else {
            this.processedDisconnect = true;
        }
        // CraftBukkit end
        ServerGamePacketListenerImpl.LOGGER.info("{} lost connection: {}", this.player.getName().getString(), reason.getString());
        // CraftBukkit start - Replace vanilla quit message handling with our own.
        /*
        this.server.invalidateStatus();
        this.server.getPlayerList().broadcastMessage((new ChatMessage("multiplayer.player.left", new Object[]{this.player.getDisplayName()})).withStyle(EnumChatFormat.YELLOW), ChatMessageType.SYSTEM, SystemUtils.NIL_UUID);
        */

        this.player.disconnect();
        // Paper start - Adventure
        quitMessage = quitMessage == null ? this.server.getPlayerList().remove(this.player) : this.server.getPlayerList().remove(this.player, quitMessage); // Paper - pass in quitMessage to fix kick message not being used
        if ((quitMessage != null) && !quitMessage.equals(net.kyori.adventure.text.Component.empty())) {
            this.server.getPlayerList().broadcastMessage(PaperAdventure.asVanilla(quitMessage), ChatType.SYSTEM, Util.NIL_UUID);
            // Paper end
        }
        // CraftBukkit end
        this.player.getTextFilter().leave();
        if (this.isSingleplayerOwner()) {
            ServerGamePacketListenerImpl.LOGGER.info("Stopping singleplayer server as player logged out");
            this.server.halt(false);
        }

    }

    @Override
    public void send(Packet<?> packet) {
        this.send(packet, (GenericFutureListener) null);
    }

    public void send(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> listener) {
        // CraftBukkit start
        if (packet == null || this.processedDisconnect) { // Spigot
            return;
        } else if (packet instanceof ClientboundSetDefaultSpawnPositionPacket) {
            ClientboundSetDefaultSpawnPositionPacket packet6 = (ClientboundSetDefaultSpawnPositionPacket) packet;
            this.player.compassTarget = new Location(this.getCraftPlayer().getWorld(), packet6.pos.getX(), packet6.pos.getY(), packet6.pos.getZ());
        }
        // CraftBukkit end

        try {
            this.connection.send(packet, listener);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Sending packet");
            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Packet being sent");

            crashreportsystemdetails.setDetail("Packet class", () -> {
                return packet.getClass().getCanonicalName();
            });
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.isImmobile()) return; // CraftBukkit
        if (packet.getSlot() >= 0 && packet.getSlot() < Inventory.getSelectionSize()) {
            if (packet.getSlot() == this.player.getInventory().selected) { return; } // Paper - don't fire itemheldevent when there wasn't a slot change
            PlayerItemHeldEvent event = new PlayerItemHeldEvent(this.getCraftPlayer(), this.player.getInventory().selected, packet.getSlot());
            this.cserver.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                this.send(new ClientboundSetCarriedItemPacket(this.player.getInventory().selected));
                this.player.resetLastActionTime();
                return;
            }
            // CraftBukkit end
            if (this.player.getInventory().selected != packet.getSlot() && this.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                this.player.stopUsingItem();
            }

            this.player.getInventory().selected = packet.getSlot();
            this.player.resetLastActionTime();
        } else {
            ServerGamePacketListenerImpl.LOGGER.warn("{} tried to set an invalid carried item", this.player.getName().getString());
            this.disconnect("Invalid hotbar selection (Hacking?)", org.bukkit.event.player.PlayerKickEvent.Cause.ILLEGAL_ACTION); // CraftBukkit // Paper - kick event cause
        }
    }

    @Override
    public void handleChat(ServerboundChatPacket packet) {
        // CraftBukkit start - async chat
        // SPIGOT-3638
        if (this.server.isStopped()) {
            return;
        }
        // CraftBukkit end
        String s = StringUtils.normalizeSpace(packet.getMessage());

        for (int i = 0; i < s.length(); ++i) {
            if (!SharedConstants.isAllowedChatCharacter(s.charAt(i))) {
                this.server.scheduleOnMain(() -> { // Paper - push to main for event firing
                this.disconnect(new TranslatableComponent("multiplayer.disconnect.illegal_characters"), org.bukkit.event.player.PlayerKickEvent.Cause.ILLEGAL_CHARACTERS); // Paper - add cause
                }); // Paper - push to main for event firing
                return;
            }
        }

        if (s.startsWith("/")) {
            PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
            this.handleChat(TextFilter.FilteredText.passThrough(s));
        } else {
            this.handleChat(TextFilter.FilteredText.passThrough(s)); // CraftBukkit - filter NYI
        }

    }

    private void handleChat(TextFilter.FilteredText message) {
        if (this.player.isRemoved() || this.player.getChatVisibility() == ChatVisiblity.HIDDEN) { // CraftBukkit - dead men tell no tales
            this.send(new ClientboundChatPacket((new TranslatableComponent("chat.disabled.options")).withStyle(ChatFormatting.RED), ChatType.SYSTEM, Util.NIL_UUID));
        } else {
            this.player.resetLastActionTime();
            String s = message.getRaw();

            // CraftBukkit start
            boolean isSync = s.startsWith("/");
            if (isSync) {
                try {
                    this.server.server.playerCommandState = true;
                    this.handleCommand(s);
                } finally {
                    this.server.server.playerCommandState = false;
                }
            } else if (s.isEmpty()) {
                ServerGamePacketListenerImpl.LOGGER.warn(this.player.getScoreboardName() + " tried to send an empty message");
            } else if (this.getCraftPlayer().isConversing()) {
                final String conversationInput = s;
                this.server.processQueue.add(new Runnable() {
                    @Override
                    public void run() {
                        ServerGamePacketListenerImpl.this.getCraftPlayer().acceptConversationInput(conversationInput);
                    }
                });
            } else if (this.player.getChatVisibility() == ChatVisiblity.SYSTEM) { // Re-add "Command Only" flag check
                this.send(new ClientboundChatPacket((new TranslatableComponent("chat.cannotSend")).withStyle(ChatFormatting.RED), ChatType.SYSTEM, Util.NIL_UUID));
            } else if (true) {
                this.chat(s, true);
                // CraftBukkit end - the below is for reference. :)
            } else {
                String s1 = message.getFiltered();
                TranslatableComponent chatmessage = s1.isEmpty() ? null : new TranslatableComponent("chat.type.text", new Object[]{this.player.getDisplayName(), s1});
                TranslatableComponent chatmessage1 = new TranslatableComponent("chat.type.text", new Object[]{this.player.getDisplayName(), s});

                this.server.getPlayerList().broadcastMessage(chatmessage1, (entityplayer) -> {
                    return this.player.shouldFilterMessageTo(entityplayer) ? chatmessage : chatmessage1;
                }, ChatType.CHAT, this.player.getUUID());
            }

            // Spigot start - spam exclusions
            boolean counted = true;
            for ( String exclude : org.spigotmc.SpigotConfig.spamExclusions )
            {
                if ( exclude != null && s.startsWith( exclude ) )
                {
                    counted = false;
                    break;
                }
            }
            // Spigot end
            // CraftBukkit start - replaced with thread safe throttle
            // this.chatSpamTickCount += 20;
            if (counted && this.chatSpamTickCount.addAndGet(20) > 200 && !this.server.getPlayerList().isOp(this.player.getGameProfile())) { // Spigot
                if (!isSync) {
                    Waitable waitable = new Waitable() {
                        @Override
                        protected Object evaluate() {
                            ServerGamePacketListenerImpl.this.disconnect(new TranslatableComponent("disconnect.spam"), org.bukkit.event.player.PlayerKickEvent.Cause.SPAM); // Paper - kick event cause
                            return null;
                        }
                    };

                    this.server.processQueue.add(waitable);

                    try {
                        waitable.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    this.disconnect(new TranslatableComponent("disconnect.spam"), org.bukkit.event.player.PlayerKickEvent.Cause.SPAM); // Paper - kick event cause
                }
                // CraftBukkit end
            }

        }
    }

    // CraftBukkit start - add method
    public void chat(String s, boolean async) {
        if (s.isEmpty() || this.player.getChatVisibility() == ChatVisiblity.HIDDEN) {
            return;
        }

        if (!async && s.startsWith("/")) {
            // Paper Start
            if (!org.spigotmc.AsyncCatcher.shuttingDown && !org.bukkit.Bukkit.isPrimaryThread()) {
                final String fCommandLine = s;
                LOGGER.error("Command Dispatched Async: " + fCommandLine);
                LOGGER.error("Please notify author of plugin causing this execution to fix this bug! see: http://bit.ly/1oSiM6C", new Throwable());
                Waitable wait = new Waitable() {
                    @Override
                    protected Object evaluate() {
                        chat(fCommandLine, false);
                        return null;
                    }
                };
                server.processQueue.add(wait);
                try {
                    wait.get();
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // This is proper habit for java. If we aren't handling it, pass it on!
                } catch (Exception e) {
                    throw new RuntimeException("Exception processing chat command", e.getCause());
                }
            }
            // Paper End
            this.handleCommand(s);
        } else if (this.player.getChatVisibility() == ChatVisiblity.SYSTEM) {
            // Do nothing, this is coming from a plugin
        // Paper start
        } else if (true) {
            final ChatProcessor cp = new ChatProcessor(this.server, this.player, s, async);
            cp.process();
            // Paper end
        } else if (false) { // Paper
            Player player = this.getCraftPlayer();
            AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(async, player, s, new LazyPlayerSet(this.server));
            this.cserver.getPluginManager().callEvent(event);

            if (PlayerChatEvent.getHandlerList().getRegisteredListeners().length != 0) {
                // Evil plugins still listening to deprecated event
                final PlayerChatEvent queueEvent = new PlayerChatEvent(player, event.getMessage(), event.getFormat(), event.getRecipients());
                queueEvent.setCancelled(event.isCancelled());
                Waitable waitable = new Waitable() {
                    @Override
                    protected Object evaluate() {
                        org.bukkit.Bukkit.getPluginManager().callEvent(queueEvent);

                        if (queueEvent.isCancelled()) {
                            return null;
                        }

                        String message = String.format(queueEvent.getFormat(), queueEvent.getPlayer().getDisplayName(), queueEvent.getMessage());
                        ServerGamePacketListenerImpl.this.server.console.sendMessage(message);
                        if (((LazyPlayerSet) queueEvent.getRecipients()).isLazy()) {
                            for (ServerPlayer recipient : ServerGamePacketListenerImpl.this.server.getPlayerList().players) {
                                recipient.getBukkitEntity().sendMessage(ServerGamePacketListenerImpl.this.player.getUUID(), message);
                            }
                        } else {
                            for (Player player : queueEvent.getRecipients()) {
                                player.sendMessage(ServerGamePacketListenerImpl.this.player.getUUID(), message);
                            }
                        }
                        return null;
                    }};
                if (async) {
                    server.processQueue.add(waitable);
                } else {
                    waitable.run();
                }
                try {
                    waitable.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // This is proper habit for java. If we aren't handling it, pass it on!
                } catch (ExecutionException e) {
                    throw new RuntimeException("Exception processing chat event", e.getCause());
                }
            } else {
                if (event.isCancelled()) {
                    return;
                }

                s = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
                server.console.sendMessage(s);
                if (((LazyPlayerSet) event.getRecipients()).isLazy()) {
                    for (ServerPlayer recipient : this.server.getPlayerList().players) {
                        recipient.getBukkitEntity().sendMessage(ServerGamePacketListenerImpl.this.player.getUUID(), s);
                    }
                } else {
                    for (Player recipient : event.getRecipients()) {
                        recipient.sendMessage(ServerGamePacketListenerImpl.this.player.getUUID(), s);
                    }
                }
            }
        }
    }
    // CraftBukkit end

    private void handleCommand(String input) {
        MinecraftTimings.playerCommandTimer.startTiming(); // Paper
        // CraftBukkit start - whole method
        if ( org.spigotmc.SpigotConfig.logCommands ) // Spigot
        this.LOGGER.info(this.player.getScoreboardName() + " issued server command: " + input);

        CraftPlayer player = this.getCraftPlayer();

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, input, new LazyPlayerSet(this.server));
        this.cserver.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            MinecraftTimings.playerCommandTimer.stopTiming(); // Paper
            return;
        }

        try {
            if (this.cserver.dispatchCommand(event.getPlayer(), event.getMessage().substring(1))) {
                return;
            }
        } catch (org.bukkit.command.CommandException ex) {
            player.sendMessage(org.bukkit.ChatColor.RED + "An internal error occurred while attempting to perform this command");
            java.util.logging.Logger.getLogger(ServerGamePacketListenerImpl.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            return;
        } finally {
            MinecraftTimings.playerCommandTimer.stopTiming(); // Paper
        }
        // this.server.getCommands().performCommand(this.player.createCommandSourceStack(), s);
        // CraftBukkit end
    }

    @Override
    public void handleAnimate(ServerboundSwingPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.isImmobile()) return; // CraftBukkit
        this.player.resetLastActionTime();
        // CraftBukkit start - Raytrace to look for 'rogue armswings'
        float f1 = this.player.getXRot();
        float f2 = this.player.getYRot();
        double d0 = this.player.getX();
        double d1 = this.player.getY() + (double) this.player.getEyeHeight();
        double d2 = this.player.getZ();
        Vec3 vec3d = new Vec3(d0, d1, d2);

        float f3 = Mth.cos(-f2 * 0.017453292F - 3.1415927F);
        float f4 = Mth.sin(-f2 * 0.017453292F - 3.1415927F);
        float f5 = -Mth.cos(-f1 * 0.017453292F);
        float f6 = Mth.sin(-f1 * 0.017453292F);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = player.gameMode.getGameModeForPlayer()== GameType.CREATIVE ? 5.0D : 4.5D;
        Vec3 vec3d1 = vec3d.add((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
        HitResult movingobjectposition = this.player.level.clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.player));

        if (movingobjectposition == null || movingobjectposition.getType() != HitResult.Type.BLOCK || this.player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) { // Paper - call PlayerInteractEvent when left-clicking on a block in adventure mode
            CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_AIR, this.player.getInventory().getSelected(), InteractionHand.MAIN_HAND);
        }

        // Arm swing animation
        io.papermc.paper.event.player.PlayerArmSwingEvent event = new io.papermc.paper.event.player.PlayerArmSwingEvent(this.getCraftPlayer(), packet.getHand() == InteractionHand.MAIN_HAND ? org.bukkit.inventory.EquipmentSlot.HAND : org.bukkit.inventory.EquipmentSlot.OFF_HAND); // Paper
        this.cserver.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;
        // CraftBukkit end
        this.player.swing(packet.getHand());
    }

    @Override
    public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        // CraftBukkit start
        if (this.player.isRemoved()) return;
        switch (packet.getAction()) {
            case PRESS_SHIFT_KEY:
            case RELEASE_SHIFT_KEY:
                PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this.getCraftPlayer(), packet.getAction() == ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY);
                this.cserver.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }
                break;
            case START_SPRINTING:
            case STOP_SPRINTING:
                PlayerToggleSprintEvent e2 = new PlayerToggleSprintEvent(this.getCraftPlayer(), packet.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING);
                this.cserver.getPluginManager().callEvent(e2);

                if (e2.isCancelled()) {
                    return;
                }
                break;
        }
        // CraftBukkit end
        this.player.resetLastActionTime();
        PlayerRideableJumping ijumpable;

        switch (packet.getAction()) {
            case PRESS_SHIFT_KEY:
                this.player.setShiftKeyDown(true);

                // Paper start - Hang on!
                if (this.player.level.paperConfig.parrotsHangOnBetter) {
                    this.player.removeEntitiesOnShoulder();
                }
                // Paper end

                break;
            case RELEASE_SHIFT_KEY:
                this.player.setShiftKeyDown(false);
                break;
            case START_SPRINTING:
                this.player.setSprinting(true);
                break;
            case STOP_SPRINTING:
                this.player.setSprinting(false);
                break;
            case STOP_SLEEPING:
                if (this.player.isSleeping()) {
                    this.player.stopSleepInBed(false, true);
                    this.awaitingPositionFromClient = this.player.position();
                }
                break;
            case START_RIDING_JUMP:
                if (this.player.getVehicle() instanceof PlayerRideableJumping) {
                    ijumpable = (PlayerRideableJumping) this.player.getVehicle();
                    int i = packet.getData();

                    if (ijumpable.canJump() && i > 0) {
                        ijumpable.handleStartJump(i);
                    }
                }
                break;
            case STOP_RIDING_JUMP:
                if (this.player.getVehicle() instanceof PlayerRideableJumping) {
                    ijumpable = (PlayerRideableJumping) this.player.getVehicle();
                    ijumpable.handleStopJump();
                }
                break;
            case OPEN_INVENTORY:
                if (this.player.getVehicle() instanceof AbstractHorse) {
                    ((AbstractHorse) this.player.getVehicle()).openInventory(this.player);
                }
                break;
            case START_FALL_FLYING:
                if (!this.player.tryToStartFallFlying()) {
                    this.player.stopFallFlying();
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid client command!");
        }

    }

    @Override
    public void handleInteract(ServerboundInteractPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.isImmobile()) return; // CraftBukkit
        ServerLevel worldserver = this.player.getLevel();
        final Entity entity = packet.getTarget(worldserver);
        // Spigot Start
        if ( entity == this.player && !this.player.isSpectator() )
        {
            this.disconnect( "Cannot interact with self!", org.bukkit.event.player.PlayerKickEvent.Cause.SELF_INTERACTION ); // Paper - add cause
            return;
        }
        // Spigot End

        this.player.resetLastActionTime();
        this.player.setShiftKeyDown(packet.isUsingSecondaryAction());
        if (entity != null) {
            if (!worldserver.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                return;
            }

            double d0 = 36.0D;

            if (this.player.distanceToSqr(entity) < 36.0D) {
                packet.dispatch(new ServerboundInteractPacket.Handler() {
                    private void performInteraction(InteractionHand enumhand, ServerGamePacketListenerImpl.EntityInteraction playerconnection_a, PlayerInteractEntityEvent event) { // CraftBukkit
                        ItemStack itemstack = ServerGamePacketListenerImpl.this.player.getItemInHand(enumhand).copy();
                        // CraftBukkit start
                        ItemStack itemInHand = ServerGamePacketListenerImpl.this.player.getItemInHand(enumhand);
                        boolean triggerLeashUpdate = itemInHand != null && itemInHand.getItem() == Items.LEAD && entity instanceof Mob;
                        Item origItem = ServerGamePacketListenerImpl.this.player.getInventory().getSelected() == null ? null : ServerGamePacketListenerImpl.this.player.getInventory().getSelected().getItem();

                        ServerGamePacketListenerImpl.this.cserver.getPluginManager().callEvent(event);

                        // Entity in bucket - SPIGOT-4048 and SPIGOT-6859
                        if ((entity instanceof Bucketable && entity instanceof LivingEntity && origItem != null && origItem.asItem() == Items.WATER_BUCKET) && (event.isCancelled() || ServerGamePacketListenerImpl.this.player.getInventory().getSelected() == null || ServerGamePacketListenerImpl.this.player.getInventory().getSelected().getItem() != origItem)) {
                            ServerGamePacketListenerImpl.this.send(new ClientboundAddMobPacket((LivingEntity) entity));
                            player.containerMenu.sendAllDataToRemote();
                        }

                        if (triggerLeashUpdate && (event.isCancelled() || ServerGamePacketListenerImpl.this.player.getInventory().getSelected() == null || ServerGamePacketListenerImpl.this.player.getInventory().getSelected().getItem() != origItem)) {
                            // Refresh the current leash state
                            ServerGamePacketListenerImpl.this.send(new ClientboundSetEntityLinkPacket(entity, ((Mob) entity).getLeashHolder()));
                        }

                        if (event.isCancelled() || ServerGamePacketListenerImpl.this.player.getInventory().getSelected() == null || ServerGamePacketListenerImpl.this.player.getInventory().getSelected().getItem() != origItem) {
                            // Refresh the current entity metadata
                            // Paper start - update entity for all players
                            ClientboundSetEntityDataPacket packet1 = new ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData(), true);
                            if (entity.tracker != null) {
                                entity.tracker.broadcast(packet1);
                            } else {
                                ServerGamePacketListenerImpl.this.send(packet1);
                            }
                            // Paper end
                        }

                        if (event.isCancelled()) {
                            ServerGamePacketListenerImpl.this.player.containerMenu.sendAllDataToRemote(); // Paper - Refresh player inventory
                            return;
                        }
                        // CraftBukkit end

                        InteractionResult enuminteractionresult = playerconnection_a.run(ServerGamePacketListenerImpl.this.player, entity, enumhand);

                        // CraftBukkit start
                        if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
                             player.containerMenu.sendAllDataToRemote();
                        }
                        // CraftBukkit end

                        if (enuminteractionresult.consumesAction()) {
                            CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(ServerGamePacketListenerImpl.this.player, itemstack, entity);
                            if (enuminteractionresult.shouldSwing()) {
                                ServerGamePacketListenerImpl.this.player.swing(enumhand, true);
                            }
                        }

                    }

                    @Override
                    public void onInteraction(InteractionHand hand) {
                        this.performInteraction(hand, net.minecraft.world.entity.player.Player::interactOn, new PlayerInteractEntityEvent(ServerGamePacketListenerImpl.this.getCraftPlayer(), entity.getBukkitEntity(), (hand == InteractionHand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND));
                    }

                    @Override
                    public void onInteraction(InteractionHand hand, Vec3 pos) {
                        this.performInteraction(hand, (entityplayer, entity1, enumhand1) -> {
                            return entity1.interactAt(entityplayer, pos, enumhand1);
                        }, new PlayerInteractAtEntityEvent(ServerGamePacketListenerImpl.this.getCraftPlayer(), entity.getBukkitEntity(), new org.bukkit.util.Vector(pos.x, pos.y, pos.z), (hand == InteractionHand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND)); // CraftBukkit
                    }

                    @Override
                    public void onAttack() {
                        // CraftBukkit start
                        if (!(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb) && !(entity instanceof AbstractArrow) && (entity != ServerGamePacketListenerImpl.this.player || ServerGamePacketListenerImpl.this.player.isSpectator())) {
                            ItemStack itemInHand = ServerGamePacketListenerImpl.this.player.getMainHandItem();
                            ServerGamePacketListenerImpl.this.player.attack(entity);

                            if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
                                player.containerMenu.sendAllDataToRemote();
                            }
                            // CraftBukkit end
                        } else {
                            ServerGamePacketListenerImpl.this.disconnect(new TranslatableComponent("multiplayer.disconnect.invalid_entity_attacked"),  org.bukkit.event.player.PlayerKickEvent.Cause.INVALID_ENTITY_ATTACKED); // Paper - add cause
                            ServerGamePacketListenerImpl.LOGGER.warn("Player {} tried to attack an invalid entity", ServerGamePacketListenerImpl.this.player.getName().getString());
                        }
                    }
                });
            }
        }
        // Paper start - fire event
        else {
            packet.dispatch(new net.minecraft.network.protocol.game.ServerboundInteractPacket.Handler() {
                @Override
                public void onInteraction(net.minecraft.world.InteractionHand hand) {
                    ServerGamePacketListenerImpl.this.callPlayerUseUnknownEntityEvent(packet, hand);
                }

                @Override
                public void onInteraction(net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.Vec3 pos) {
                    ServerGamePacketListenerImpl.this.callPlayerUseUnknownEntityEvent(packet, hand);
                }

                @Override
                public void onAttack() {
                    ServerGamePacketListenerImpl.this.callPlayerUseUnknownEntityEvent(packet, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            });
        }

    }

    private void callPlayerUseUnknownEntityEvent(ServerboundInteractPacket packet, InteractionHand hand) {
        this.cserver.getPluginManager().callEvent(new com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent(
            this.getCraftPlayer(),
            packet.getEntityId(),
            packet.getActionType() == ServerboundInteractPacket.ActionType.ATTACK,
            hand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND
        ));
    }
    // Paper end

    @Override
    public void handleClientCommand(ServerboundClientCommandPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.resetLastActionTime();
        ServerboundClientCommandPacket.Action packetplayinclientcommand_enumclientcommand = packet.getAction();

        switch (packetplayinclientcommand_enumclientcommand) {
            case PERFORM_RESPAWN:
                if (this.player.wonGame) {
                    this.player.wonGame = false;
                    this.player = this.server.getPlayerList().respawn(this.player, this.server.getLevel(this.player.getRespawnDimension()), true, null, true, org.bukkit.event.player.PlayerRespawnEvent.RespawnFlag.END_PORTAL); // Paper - add isEndCreditsRespawn argument
                    CriteriaTriggers.CHANGED_DIMENSION.trigger(this.player, Level.END, Level.OVERWORLD);
                } else {
                    if (this.player.getHealth() > 0.0F) {
                        return;
                    }

                    this.player = this.server.getPlayerList().respawn(this.player, false);
                    if (this.server.isHardcore()) {
                        this.player.setGameMode(GameType.SPECTATOR, org.bukkit.event.player.PlayerGameModeChangeEvent.Cause.HARDCORE_DEATH, null); // Paper
                        ((GameRules.BooleanValue) this.player.getLevel().getGameRules().getRule(GameRules.RULE_SPECTATORSGENERATECHUNKS)).set(false, this.player.getLevel()); // Paper
                    }
                }
                break;
            case REQUEST_STATS:
                this.player.getStats().sendStats(this.player);
        }

    }

    @Override
    public void handleContainerClose(ServerboundContainerClosePacket packet) {
        // Paper start
        handleContainerClose(packet, InventoryCloseEvent.Reason.PLAYER);
    }
    public void handleContainerClose(ServerboundContainerClosePacket packetplayinclosewindow, InventoryCloseEvent.Reason reason) {
        // Paper end
        PacketUtils.ensureRunningOnSameThread(packetplayinclosewindow, this, this.player.getLevel());

        if (this.player.isImmobile()) return; // CraftBukkit
        CraftEventFactory.handleInventoryCloseEvent(this.player, reason); // CraftBukkit // Paper

        this.player.doCloseContainer();
    }

    @Override
    public void handleContainerClick(ServerboundContainerClickPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.isImmobile()) return; // CraftBukkit
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId == packet.getContainerId() && this.player.containerMenu.stillValid(this.player)) { // CraftBukkit
            boolean cancelled = this.player.isSpectator(); // CraftBukkit - see below if
            if (false/*this.player.isSpectator()*/) { // CraftBukkit
                this.player.containerMenu.sendAllDataToRemote();
            } else {
                int i = packet.getSlotNum();

                if (!this.player.containerMenu.isValidSlotIndex(i)) {
                    ServerGamePacketListenerImpl.LOGGER.debug("Player {} clicked invalid slot index: {}, available slots: {}", new Object[]{this.player.getName(), i, this.player.containerMenu.slots.size()});
                } else {
                    boolean flag = packet.getStateId() != this.player.containerMenu.getStateId();

                    this.player.containerMenu.suppressRemoteUpdates();
                    // CraftBukkit start - Call InventoryClickEvent
                    if (packet.getSlotNum() < -1 && packet.getSlotNum() != -999) {
                        return;
                    }

                    InventoryView inventory = this.player.containerMenu.getBukkitView();
                    SlotType type = inventory.getSlotType(packet.getSlotNum());

                    InventoryClickEvent event;
                    ClickType click = ClickType.UNKNOWN;
                    InventoryAction action = InventoryAction.UNKNOWN;

                    ItemStack itemstack = ItemStack.EMPTY;

                    switch (packet.getClickType()) {
                        case PICKUP:
                            if (packet.getButtonNum() == 0) {
                                click = ClickType.LEFT;
                            } else if (packet.getButtonNum() == 1) {
                                click = ClickType.RIGHT;
                            }
                            if (packet.getButtonNum() == 0 || packet.getButtonNum() == 1) {
                                action = InventoryAction.NOTHING; // Don't want to repeat ourselves
                                if (packet.getSlotNum() == -999) {
                                    if (!player.containerMenu.getCarried().isEmpty()) {
                                        action = packet.getButtonNum() == 0 ? InventoryAction.DROP_ALL_CURSOR : InventoryAction.DROP_ONE_CURSOR;
                                    }
                                } else if (packet.getSlotNum() < 0)  {
                                    action = InventoryAction.NOTHING;
                                } else {
                                    Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                                    if (slot != null) {
                                        ItemStack clickedItem = slot.getItem();
                                        ItemStack cursor = player.containerMenu.getCarried();
                                        if (clickedItem.isEmpty()) {
                                            if (!cursor.isEmpty()) {
                                                action = packet.getButtonNum() == 0 ? InventoryAction.PLACE_ALL : InventoryAction.PLACE_ONE;
                                            }
                                        } else if (slot.mayPickup(player)) {
                                            if (cursor.isEmpty()) {
                                                action = packet.getButtonNum() == 0 ? InventoryAction.PICKUP_ALL : InventoryAction.PICKUP_HALF;
                                            } else if (slot.mayPlace(cursor)) {
                                                if (clickedItem.sameItem(cursor) && ItemStack.tagMatches(clickedItem, cursor)) {
                                                    int toPlace = packet.getButtonNum() == 0 ? cursor.getCount() : 1;
                                                    toPlace = Math.min(toPlace, clickedItem.getMaxStackSize() - clickedItem.getCount());
                                                    toPlace = Math.min(toPlace, slot.container.getMaxStackSize() - clickedItem.getCount());
                                                    if (toPlace == 1) {
                                                        action = InventoryAction.PLACE_ONE;
                                                    } else if (toPlace == cursor.getCount()) {
                                                        action = InventoryAction.PLACE_ALL;
                                                    } else if (toPlace < 0) {
                                                        action = toPlace != -1 ? InventoryAction.PICKUP_SOME : InventoryAction.PICKUP_ONE; // this happens with oversized stacks
                                                    } else if (toPlace != 0) {
                                                        action = InventoryAction.PLACE_SOME;
                                                    }
                                                } else if (cursor.getCount() <= slot.getMaxStackSize()) {
                                                    action = InventoryAction.SWAP_WITH_CURSOR;
                                                }
                                            } else if (cursor.getItem() == clickedItem.getItem() && ItemStack.tagMatches(cursor, clickedItem)) {
                                                if (clickedItem.getCount() >= 0) {
                                                    if (clickedItem.getCount() + cursor.getCount() <= cursor.getMaxStackSize()) {
                                                        // As of 1.5, this is result slots only
                                                        action = InventoryAction.PICKUP_ALL;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        // TODO check on updates
                        case QUICK_MOVE:
                            if (packet.getButtonNum() == 0) {
                                click = ClickType.SHIFT_LEFT;
                            } else if (packet.getButtonNum() == 1) {
                                click = ClickType.SHIFT_RIGHT;
                            }
                            if (packet.getButtonNum() == 0 || packet.getButtonNum() == 1) {
                                if (packet.getSlotNum() < 0) {
                                    action = InventoryAction.NOTHING;
                                } else {
                                    Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                                    if (slot != null && slot.mayPickup(this.player) && slot.hasItem()) {
                                        action = InventoryAction.MOVE_TO_OTHER_INVENTORY;
                                    } else {
                                        action = InventoryAction.NOTHING;
                                    }
                                }
                            }
                            break;
                        case SWAP:
                            if ((packet.getButtonNum() >= 0 && packet.getButtonNum() < 9) || packet.getButtonNum() == 40) {
                                click = (packet.getButtonNum() == 40) ? ClickType.SWAP_OFFHAND : ClickType.NUMBER_KEY;
                                Slot clickedSlot = this.player.containerMenu.getSlot(packet.getSlotNum());
                                if (clickedSlot.mayPickup(player)) {
                                    ItemStack hotbar = this.player.getInventory().getItem(packet.getButtonNum());
                                    boolean canCleanSwap = hotbar.isEmpty() || (clickedSlot.container == this.player.getInventory() && clickedSlot.mayPlace(hotbar)); // the slot will accept the hotbar item
                                    if (clickedSlot.hasItem()) {
                                        if (canCleanSwap) {
                                            action = InventoryAction.HOTBAR_SWAP;
                                        } else {
                                            action = InventoryAction.HOTBAR_MOVE_AND_READD;
                                        }
                                    } else if (!clickedSlot.hasItem() && !hotbar.isEmpty() && clickedSlot.mayPlace(hotbar)) {
                                        action = InventoryAction.HOTBAR_SWAP;
                                    } else {
                                        action = InventoryAction.NOTHING;
                                    }
                                } else {
                                    action = InventoryAction.NOTHING;
                                }
                            }
                            break;
                        case CLONE:
                            if (packet.getButtonNum() == 2) {
                                click = ClickType.MIDDLE;
                                if (packet.getSlotNum() < 0) {
                                    action = InventoryAction.NOTHING;
                                } else {
                                    Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                                    if (slot != null && slot.hasItem() && this.player.getAbilities().instabuild && player.containerMenu.getCarried().isEmpty()) {
                                        action = InventoryAction.CLONE_STACK;
                                    } else {
                                        action = InventoryAction.NOTHING;
                                    }
                                }
                            } else {
                                click = ClickType.UNKNOWN;
                                action = InventoryAction.UNKNOWN;
                            }
                            break;
                        case THROW:
                            if (packet.getSlotNum() >= 0) {
                                if (packet.getButtonNum() == 0) {
                                    click = ClickType.DROP;
                                    Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                                    if (slot != null && slot.hasItem() && slot.mayPickup(player) && !slot.getItem().isEmpty() && slot.getItem().getItem() != Item.byBlock(Blocks.AIR)) {
                                        action = InventoryAction.DROP_ONE_SLOT;
                                    } else {
                                        action = InventoryAction.NOTHING;
                                    }
                                } else if (packet.getButtonNum() == 1) {
                                    click = ClickType.CONTROL_DROP;
                                    Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                                    if (slot != null && slot.hasItem() && slot.mayPickup(player) && !slot.getItem().isEmpty() && slot.getItem().getItem() != Item.byBlock(Blocks.AIR)) {
                                        action = InventoryAction.DROP_ALL_SLOT;
                                    } else {
                                        action = InventoryAction.NOTHING;
                                    }
                                }
                            } else {
                                // Sane default (because this happens when they are holding nothing. Don't ask why.)
                                click = ClickType.LEFT;
                                if (packet.getButtonNum() == 1) {
                                    click = ClickType.RIGHT;
                                }
                                action = InventoryAction.NOTHING;
                            }
                            break;
                        case QUICK_CRAFT:
                            this.player.containerMenu.clicked(packet.getSlotNum(), packet.getButtonNum(), packet.getClickType(), this.player);
                            break;
                        case PICKUP_ALL:
                            click = ClickType.DOUBLE_CLICK;
                            action = InventoryAction.NOTHING;
                            if (packet.getSlotNum() >= 0 && !this.player.containerMenu.getCarried().isEmpty()) {
                                ItemStack cursor = this.player.containerMenu.getCarried();
                                action = InventoryAction.NOTHING;
                                // Quick check for if we have any of the item
                                if (inventory.getTopInventory().contains(CraftMagicNumbers.getMaterial(cursor.getItem())) || inventory.getBottomInventory().contains(CraftMagicNumbers.getMaterial(cursor.getItem()))) {
                                    action = InventoryAction.COLLECT_TO_CURSOR;
                                }
                            }
                            break;
                        default:
                            break;
                    }

                    if (packet.getClickType() != net.minecraft.world.inventory.ClickType.QUICK_CRAFT) {
                        if (click == ClickType.NUMBER_KEY) {
                            event = new InventoryClickEvent(inventory, type, packet.getSlotNum(), click, action, packet.getButtonNum());
                        } else {
                            event = new InventoryClickEvent(inventory, type, packet.getSlotNum(), click, action);
                        }

                        org.bukkit.inventory.Inventory top = inventory.getTopInventory();
                        if (packet.getSlotNum() == 0 && top instanceof CraftingInventory) {
                            org.bukkit.inventory.Recipe recipe = ((CraftingInventory) top).getRecipe();
                            if (recipe != null) {
                                if (click == ClickType.NUMBER_KEY) {
                                    event = new CraftItemEvent(recipe, inventory, type, packet.getSlotNum(), click, action, packet.getButtonNum());
                                } else {
                                    event = new CraftItemEvent(recipe, inventory, type, packet.getSlotNum(), click, action);
                                }
                            }
                        }

                        if (packet.getSlotNum() == 2 && top instanceof SmithingInventory) {
                            org.bukkit.inventory.ItemStack result = ((SmithingInventory) top).getResult();
                            if (result != null) {
                                if (click == ClickType.NUMBER_KEY) {
                                    event = new SmithItemEvent(inventory, type, packet.getSlotNum(), click, action, packet.getButtonNum());
                                } else {
                                    event = new SmithItemEvent(inventory, type, packet.getSlotNum(), click, action);
                                }
                            }
                        }

                        event.setCancelled(cancelled);
                        AbstractContainerMenu oldContainer = this.player.containerMenu; // SPIGOT-1224
                        this.cserver.getPluginManager().callEvent(event);
                        if (this.player.containerMenu != oldContainer) {
                            return;
                        }

                        switch (event.getResult()) {
                            case ALLOW:
                            case DEFAULT:
                                this.player.containerMenu.clicked(i, packet.getButtonNum(), packet.getClickType(), this.player);
                                break;
                            case DENY:
                                /* Needs enum constructor in InventoryAction
                                if (action.modifiesOtherSlots()) {

                                } else {
                                    if (action.modifiesCursor()) {
                                        this.player.playerConnection.sendPacket(new Packet103SetSlot(-1, -1, this.player.inventory.getCarried()));
                                    }
                                    if (action.modifiesClicked()) {
                                        this.player.playerConnection.sendPacket(new Packet103SetSlot(this.player.activeContainer.windowId, packet102windowclick.slot, this.player.activeContainer.getSlot(packet102windowclick.slot).getItem()));
                                    }
                                }*/
                                switch (action) {
                                    // Modified other slots
                                    case PICKUP_ALL:
                                    case MOVE_TO_OTHER_INVENTORY:
                                    case HOTBAR_MOVE_AND_READD:
                                    case HOTBAR_SWAP:
                                    case COLLECT_TO_CURSOR:
                                    case UNKNOWN:
                                        this.player.containerMenu.sendAllDataToRemote();
                                        break;
                                    // Modified cursor and clicked
                                    case PICKUP_SOME:
                                    case PICKUP_HALF:
                                    case PICKUP_ONE:
                                    case PLACE_ALL:
                                    case PLACE_SOME:
                                    case PLACE_ONE:
                                    case SWAP_WITH_CURSOR:
                                        this.player.connection.send(new ClientboundContainerSetSlotPacket(-1, -1, this.player.inventoryMenu.incrementStateId(), this.player.containerMenu.getCarried()));
                                        this.player.connection.send(new ClientboundContainerSetSlotPacket(this.player.containerMenu.containerId, this.player.inventoryMenu.incrementStateId(), packet.getSlotNum(), this.player.containerMenu.getSlot(packet.getSlotNum()).getItem()));
                                        break;
                                    // Modified clicked only
                                    case DROP_ALL_SLOT:
                                    case DROP_ONE_SLOT:
                                        this.player.connection.send(new ClientboundContainerSetSlotPacket(this.player.containerMenu.containerId, this.player.inventoryMenu.incrementStateId(), packet.getSlotNum(), this.player.containerMenu.getSlot(packet.getSlotNum()).getItem()));
                                        break;
                                    // Modified cursor only
                                    case DROP_ALL_CURSOR:
                                    case DROP_ONE_CURSOR:
                                    case CLONE_STACK:
                                        this.player.connection.send(new ClientboundContainerSetSlotPacket(-1, -1, this.player.inventoryMenu.incrementStateId(), this.player.containerMenu.getCarried()));
                                        break;
                                    // Nothing
                                    case NOTHING:
                                        break;
                                }
                        }

                        if (event instanceof CraftItemEvent || event instanceof SmithItemEvent) {
                            // Need to update the inventory on crafting to
                            // correctly support custom recipes
                            player.containerMenu.sendAllDataToRemote();
                        }
                    }
                    // CraftBukkit end
                    ObjectIterator objectiterator = Int2ObjectMaps.fastIterable(packet.getChangedSlots()).iterator();

                    while (objectiterator.hasNext()) {
                        Entry<ItemStack> entry = (Entry) objectiterator.next();

                        this.player.containerMenu.setRemoteSlotNoCopy(entry.getIntKey(), (ItemStack) entry.getValue());
                    }

                    this.player.containerMenu.setRemoteCarried(packet.getCarriedItem());
                    this.player.containerMenu.resumeRemoteUpdates();
                    if (flag) {
                        this.player.containerMenu.broadcastFullState();
                    } else {
                        this.player.containerMenu.broadcastChanges();
                    }

                }
            }
        }
    }

    @Override
    public void handlePlaceRecipe(ServerboundPlaceRecipePacket packet) {
        // Paper start
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            if (recipeSpamPackets.addAndGet(com.destroystokyo.paper.PaperConfig.autoRecipeIncrement) > com.destroystokyo.paper.PaperConfig.autoRecipeLimit) {
                server.scheduleOnMain(() -> this.disconnect(new TranslatableComponent("disconnect.spam", new Object[0]), org.bukkit.event.player.PlayerKickEvent.Cause.SPAM)); // Paper - kick event cause
                return;
            }
        }
        // Paper end
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.resetLastActionTime();
        if (!this.player.isSpectator() && this.player.containerMenu.containerId == packet.getContainerId() && this.player.containerMenu instanceof RecipeBookMenu) {
            // Paper start - fire event for clicking recipes in the recipe book
            com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent event = new com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent(
                player.getBukkitEntity(), org.bukkit.craftbukkit.v1_18_R2.util.CraftNamespacedKey.fromMinecraft(packet.getRecipe()), packet.isShiftDown());
            if (event.callEvent() && this.player.containerMenu instanceof RecipeBookMenu<?> recipeBookMenu) { // check if inventory changed during event handling
                this.server.getRecipeManager().byKey(org.bukkit.craftbukkit.v1_18_R2.util.CraftNamespacedKey.toMinecraft(event.getRecipe())).ifPresent((irecipe) -> {
                    recipeBookMenu.handlePlacement(event.isMakeAll(), irecipe, this.player);
                });
            } // Paper end
        }
    }

    @Override
    public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.isImmobile()) return; // CraftBukkit
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId == packet.getContainerId() && !this.player.isSpectator()) {
            boolean flag = this.player.containerMenu.clickMenuButton(this.player, packet.getButtonId());

            if (flag) {
                this.player.containerMenu.broadcastChanges();
            }
        }

    }

    @Override
    public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.gameMode.isCreative()) {
            boolean flag = packet.getSlotNum() < 0;
            ItemStack itemstack = packet.getItem();
            CompoundTag nbttagcompound = BlockItem.getBlockEntityData(itemstack);

            if (!itemstack.isEmpty() && nbttagcompound != null && nbttagcompound.contains("x") && nbttagcompound.contains("y") && nbttagcompound.contains("z") && this.player.getBukkitEntity().hasPermission("minecraft.nbt.copy")) { // Spigot
                BlockPos blockposition = BlockEntity.getPosFromTag(nbttagcompound);
                // Paper start
                BlockEntity tileentity = null;
                if (this.player.distanceToSqr(blockposition.getX(), blockposition.getY(), blockposition.getZ()) < 32 * 32 && this.player.getLevel().isLoadedAndInBounds(blockposition)) {
                    tileentity = this.player.level.getBlockEntity(blockposition);
                }
                // Paper end

                if (tileentity != null) {
                    tileentity.saveToItem(itemstack);
                }
            }

            boolean flag1 = packet.getSlotNum() >= 1 && packet.getSlotNum() <= 45;
            boolean flag2 = itemstack.isEmpty() || itemstack.getDamageValue() >= 0 && itemstack.getCount() <= 64 && !itemstack.isEmpty();
            if (flag || (flag1 && !ItemStack.matches(this.player.inventoryMenu.getSlot(packet.getSlotNum()).getItem(), packet.getItem()))) { // Insist on valid slot
                // CraftBukkit start - Call click event
                InventoryView inventory = this.player.inventoryMenu.getBukkitView();
                org.bukkit.inventory.ItemStack item = CraftItemStack.asBukkitCopy(packet.getItem());

                SlotType type = SlotType.QUICKBAR;
                if (flag) {
                    type = SlotType.OUTSIDE;
                } else if (packet.getSlotNum() < 36) {
                    if (packet.getSlotNum() >= 5 && packet.getSlotNum() < 9) {
                        type = SlotType.ARMOR;
                    } else {
                        type = SlotType.CONTAINER;
                    }
                }
                InventoryCreativeEvent event = new InventoryCreativeEvent(inventory, type, flag ? -999 : packet.getSlotNum(), item);
                this.cserver.getPluginManager().callEvent(event);

                itemstack = CraftItemStack.asNMSCopy(event.getCursor());

                switch (event.getResult()) {
                case ALLOW:
                    // Plugin cleared the id / stacksize checks
                    flag2 = true;
                    break;
                case DEFAULT:
                    break;
                case DENY:
                    // Reset the slot
                    if (packet.getSlotNum() >= 0) {
                        this.player.connection.send(new ClientboundContainerSetSlotPacket(this.player.inventoryMenu.containerId, this.player.inventoryMenu.incrementStateId(), packet.getSlotNum(), this.player.inventoryMenu.getSlot(packet.getSlotNum()).getItem()));
                        this.player.connection.send(new ClientboundContainerSetSlotPacket(-1, this.player.inventoryMenu.incrementStateId(), -1, ItemStack.EMPTY));
                    }
                    return;
                }
            }
            // CraftBukkit end

            if (flag1 && flag2) {
                this.player.inventoryMenu.getSlot(packet.getSlotNum()).set(itemstack);
                this.player.inventoryMenu.broadcastChanges();
            } else if (flag && flag2 && this.dropSpamTickCount < 200) {
                this.dropSpamTickCount += 20;
                this.player.drop(itemstack, true);
            }
        }

    }

    @Override
    public void handleSignUpdate(ServerboundSignUpdatePacket packet) {
        List<String> list = (List) Stream.of(packet.getLines()).map(ChatFormatting::stripFormatting).collect(Collectors.toList());

        this.filterTextPacket(list, (list1) -> {
            this.updateSignText(packet, list1);
        });
    }

    private void updateSignText(ServerboundSignUpdatePacket packet, List<TextFilter.FilteredText> signText) {
        if (this.player.isImmobile()) return; // CraftBukkit
        this.player.resetLastActionTime();
        ServerLevel worldserver = this.player.getLevel();
        BlockPos blockposition = packet.getPos();

        if (worldserver.hasChunkAt(blockposition)) {
            BlockState iblockdata = worldserver.getBlockState(blockposition);
            BlockEntity tileentity = worldserver.getBlockEntity(blockposition);

            if (!(tileentity instanceof SignBlockEntity)) {
                return;
            }

            SignBlockEntity tileentitysign = (SignBlockEntity) tileentity;

            if (!tileentitysign.isEditable() || !this.player.getUUID().equals(tileentitysign.getPlayerWhoMayEdit())) {
                ServerGamePacketListenerImpl.LOGGER.warn("Player {} just tried to change non-editable sign", this.player.getName().getString());
                if (this.player.distanceToSqr(blockposition.getX(), blockposition.getY(), blockposition.getZ()) < 32 * 32) // Paper
                this.send(tileentity.getUpdatePacket()); // CraftBukkit
                return;
            }

            // CraftBukkit start // Paper start - Adventure
            Player player = this.player.getBukkitEntity();
            int x = packet.getPos().getX();
            int y = packet.getPos().getY();
            int z = packet.getPos().getZ();
            List<net.kyori.adventure.text.Component> lines = new java.util.ArrayList<>();

            for (int i = 0; i < signText.size(); ++i) {
                TextFilter.FilteredText currentLine = signText.get(i);
                // Paper start - cap line length - modified clients can send longer data than normal
                if (MAX_SIGN_LINE_LENGTH > 0 && currentLine.getRaw().length() > MAX_SIGN_LINE_LENGTH) {
                    // This handles multibyte characters as 1
                    int offset = currentLine.getRaw().codePoints().limit(MAX_SIGN_LINE_LENGTH).map(Character::charCount).sum();
                    if (offset < currentLine.getRaw().length()) {
                        signText.set(i, currentLine = net.minecraft.server.network.TextFilter.FilteredText.passThrough(currentLine.getRaw().substring(0, offset))); // this will break any filtering, but filtering is NYI as of 1.17
                    }
                }
                // Paper end

                if (this.player.isTextFilteringEnabled()) {
                    lines.add(net.kyori.adventure.text.Component.text(SharedConstants.filterText(currentLine.getFiltered())));
                } else {
                    lines.add(net.kyori.adventure.text.Component.text(SharedConstants.filterText(currentLine.getRaw())));
                }
            }
            SignChangeEvent event = new SignChangeEvent((org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock) player.getWorld().getBlockAt(x, y, z), this.player.getBukkitEntity(), lines);
            this.cserver.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                for (int i = 0; i < 4; i++) {
                    tileentitysign.setMessage(i, PaperAdventure.asVanilla(event.line(i)));
                }
                // Paper end
                tileentitysign.isEditable = false;
            }
            // CraftBukkit end

            tileentitysign.setChanged();
            worldserver.sendBlockUpdated(blockposition, iblockdata, iblockdata, 3);
        }

    }

    @Override
    public void handleKeepAlive(ServerboundKeepAlivePacket packet) {
        //PlayerConnectionUtils.ensureMainThread(packetplayinkeepalive, this, this.player.getWorldServer()); // CraftBukkit // Paper - This shouldn't be on the main thread
        if (this.keepAlivePending && packet.getId() == this.keepAliveChallenge) {
            int i = (int) (Util.getMillis() - this.keepAliveTime);

            this.player.latency = (this.player.latency * 3 + i) / 4;
            this.keepAlivePending = false;
        } else if (!this.isSingleplayerOwner()) {
            // Paper start - This needs to be handled on the main thread for plugins
            server.submit(() -> {
            this.disconnect(new TranslatableComponent("disconnect.timeout"), org.bukkit.event.player.PlayerKickEvent.Cause.TIMEOUT); // Paper - kick event cause
            });
            // Paper end
        }

    }

    @Override
    public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        // CraftBukkit start
        if (this.player.getAbilities().mayfly && this.player.getAbilities().flying != packet.isFlying()) {
            PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(this.player.getBukkitEntity(), packet.isFlying());
            this.cserver.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                this.player.getAbilities().flying = packet.isFlying(); // Actually set the player's flying status
            } else {
                this.player.onUpdateAbilities(); // Tell the player their ability was reverted
            }
        }
        // CraftBukkit end
    }

    @Override
    public void handleClientInformation(ServerboundClientInformationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.updateOptions(packet);
    }

    // CraftBukkit start
    private static final ResourceLocation CUSTOM_REGISTER = new ResourceLocation("register");
    private static final ResourceLocation CUSTOM_UNREGISTER = new ResourceLocation("unregister");

    private static final ResourceLocation MINECRAFT_BRAND = new ResourceLocation("brand"); // Paper - Brand support

    @Override
    public void handleCustomPayload(ServerboundCustomPayloadPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (packet.identifier.equals(CUSTOM_REGISTER)) {
            try {
                String channels = packet.data.toString(com.google.common.base.Charsets.UTF_8);
                for (String channel : channels.split("\0")) {
                    this.getCraftPlayer().addChannel(channel);
                }
            } catch (Exception ex) {
                ServerGamePacketListenerImpl.LOGGER.error("Couldn\'t register custom payload", ex);
                this.disconnect("Invalid payload REGISTER!", org.bukkit.event.player.PlayerKickEvent.Cause.INVALID_PAYLOAD); // Paper - kick event cause
            }
        } else if (packet.identifier.equals(CUSTOM_UNREGISTER)) {
            try {
                String channels = packet.data.toString(com.google.common.base.Charsets.UTF_8);
                for (String channel : channels.split("\0")) {
                    this.getCraftPlayer().removeChannel(channel);
                }
            } catch (Exception ex) {
                ServerGamePacketListenerImpl.LOGGER.error("Couldn\'t unregister custom payload", ex);
                this.disconnect("Invalid payload UNREGISTER!", org.bukkit.event.player.PlayerKickEvent.Cause.INVALID_PAYLOAD); // Paper - kick event cause
            }
        } else {
            try {
                byte[] data = new byte[packet.data.readableBytes()];
                packet.data.readBytes(data);
                // Paper start - Brand support
                if (packet.identifier.equals(MINECRAFT_BRAND)) {
                    try {
                        this.clientBrandName = new FriendlyByteBuf(io.netty.buffer.Unpooled.copiedBuffer(data)).readUtf(256);
                    } catch (StringIndexOutOfBoundsException ex) {
                        this.clientBrandName = "illegal";
                    }
                }
                // Paper end
                this.cserver.getMessenger().dispatchIncomingMessage(this.player.getBukkitEntity(), packet.identifier.toString(), data);
            } catch (Exception ex) {
                ServerGamePacketListenerImpl.LOGGER.error("Couldn\'t dispatch custom payload", ex);
                this.disconnect("Invalid custom payload!", org.bukkit.event.player.PlayerKickEvent.Cause.INVALID_PAYLOAD); // Paper - kick event cause
            }
        }

    }

    // Paper start - brand support
    public String getClientBrandName() {
        return clientBrandName;
    }
    // Paper end

    public final boolean isDisconnected() {
        return (!this.player.joining && !this.connection.isConnected()) || this.processedDisconnect; // Paper
    }
    // CraftBukkit end

    @Override
    public void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.hasPermissions(2) || this.isSingleplayerOwner()) {
            //this.minecraftServer.a(packetplayindifficultychange.b(), false); // Paper - don't allow clients to change this
        }
    }

    @Override
    public void handleLockDifficulty(ServerboundLockDifficultyPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.hasPermissions(2) || this.isSingleplayerOwner()) {
            this.server.setDifficultyLocked(packet.isLocked());
        }
    }

    @Override
    public ServerPlayer getPlayer() {
        return this.player;
    }

    @FunctionalInterface
    private interface EntityInteraction {

        InteractionResult run(ServerPlayer player, Entity entity, InteractionHand hand);
    }
}