package net.evilkingdom.basics.component.components.data.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.commons.cooldown.CooldownImplementor;
import net.evilkingdom.commons.cooldown.objects.Cooldown;
import net.evilkingdom.commons.datapoint.DataImplementor;
import net.evilkingdom.commons.datapoint.objects.Datapoint;
import net.evilkingdom.commons.datapoint.objects.Datasite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlayerData {

    private final Basics plugin;

    private final UUID uuid;
    private boolean canMessage, canChat, canStaffChat;
    private ArrayList<UUID> ignored;
    private Optional<UUID> reply;
    private final ArrayList<Cooldown> cooldowns;

    private static final HashSet<PlayerData> cache = new HashSet<PlayerData>();

    /**
     * Allows you to create a PlayerData.
     *
     * @param uuid ~ The UUID of the player.
     */
    public PlayerData(final UUID uuid) {
        this.plugin = Basics.getPlugin();

        this.uuid = uuid;
        this.canChat = true;
        this.canMessage = true;
        this.ignored = new ArrayList<UUID>();
        this.cooldowns = new ArrayList<Cooldown>();
        this.reply = Optional.empty();
    }

    /**
     * Allows you to retrieve if the data exists in the Mongo database.
     *
     * @return If the data exists or not.
     */
    public CompletableFuture<Boolean> exists() {
        if (cache.contains(this)) {
            return CompletableFuture.supplyAsync(() -> true);
        } else {
            final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
            final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin && !innerDatasite.getName().equals("network")).findFirst().get();
            final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_players")).findFirst().get();
            return datapoint.exists(this.uuid.toString());
        }
    }

    /**
     * Allows you to load the data from the Mongo database.
     * Runs asynchronously in order to keep the server from lagging.
     *
     * @return If the data could be loaded or not.
     */
    private CompletableFuture<Boolean> load() {
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin && !innerDatasite.getName().equals("network")).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_players")).findFirst().get();
        return datapoint.get(this.uuid.toString()).thenApply(optionalJsonObject -> {
            if (optionalJsonObject.isEmpty()) {
                return false;
            }
            final JsonObject jsonObject = optionalJsonObject.get();
            if (jsonObject.has("canChat")) {
                this.canChat = jsonObject.get("canChat").getAsBoolean();
            }
            if (jsonObject.has("canMessage")) {
                this.canMessage = jsonObject.get("canMessage").getAsBoolean();
            }
            if (jsonObject.has("canStaffChat")) {
                this.canStaffChat = jsonObject.get("canStaffChat").getAsBoolean();
            }
            if (jsonObject.has("ignored")) {
                jsonObject.get("ignored").getAsJsonArray().forEach(jsonElement -> this.ignored.add(UUID.fromString(jsonElement.getAsString())));
            }
            if (jsonObject.has("cooldowns")) {
                jsonObject.get("cooldowns").getAsJsonArray().forEach(jsonElement -> {
                    final JsonObject cooldownJsonObject = jsonElement.getAsJsonObject();
                    this.cooldowns.add(new Cooldown(this.plugin, "player-" + this.uuid + "-" + cooldownJsonObject.get("type").getAsString(), cooldownJsonObject.get("timeLeft").getAsLong()));
                });
            }
            return true;
        });
    }

    /**
     * Allows you to save the data to the Mongo database.
     *
     * @param asynchronous ~ If the save is asynchronous (should always be unless it's an emergency saves).
     */
    public void save(final boolean asynchronous) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("canMessage", this.canMessage);
        jsonObject.addProperty("canChat", this.canChat);
        jsonObject.addProperty("canStaffChat", this.canStaffChat);
        final JsonArray ignoredJsonArray = new JsonArray();
        this.ignored.forEach(uuid -> ignoredJsonArray.add(uuid.toString()));
        jsonObject.add("ignored", ignoredJsonArray);
        final JsonArray cooldownsJsonArray = new JsonArray();
        this.getCooldowns().forEach(cooldown -> {
            final JsonObject cooldownJsonObject = new JsonObject();
            cooldownJsonObject.addProperty("type", cooldown.getIdentifier().replaceFirst("player-" + this.uuid + "-", ""));
            cooldownJsonObject.addProperty("timeLeft", cooldown.getTimeLeft());
            cooldownsJsonArray.add(cooldownJsonObject);
        });
        jsonObject.add("cooldowns", cooldownsJsonArray);
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin && !innerDatasite.getName().equals("network")).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_players")).findFirst().get();
        datapoint.save(jsonObject, this.uuid.toString(), asynchronous);
    }

    /**
     * Allows you to retrieve the data's cooldowns.
     *
     * @return The data's cooldowns.
     */
    public ArrayList<Cooldown> getCooldowns() {
        final CooldownImplementor cooldownImplementor = CooldownImplementor.get(this.plugin);
        return new ArrayList<Cooldown>(cooldownImplementor.getCooldowns().stream().filter(cooldown -> cooldown.getIdentifier().startsWith("player-" + this.uuid)).collect(Collectors.toList()));
    }

    /**
     * Allows you to set the data's staff chat state.
     *
     * @param canStaffChat ~ The data's staff chat state to set.
     */
    public void setCanStaffChat(final boolean canStaffChat) {
        this.canStaffChat = canStaffChat;
    }

    /**
     * Allows you to retrieve if the data can staff chat.
     *
     * @return If the data can staff chat.
     */
    public Boolean canStaffChat() {
        return this.canStaffChat;
    }

    /**
     * Allows you to set the data's chat state.
     *
     * @param canChat ~ The data's chat state to set.
     */
    public void setCanChat(final boolean canChat) {
        this.canChat = canChat;
    }

    /**
     * Allows you to retrieve if the data can chat.
     *
     * @return If the data can chat.
     */
    public Boolean canChat() {
        return this.canChat;
    }

    /**
     * Allows you to set the data's messages state.
     *
     * @param canMessage ~ The data's messages state to set.
     */
    public void setCanMessage(final boolean canMessage) {
        this.canMessage = canMessage;
    }

    /**
     * Allows you to retrieve if the data can message.
     *
     * @return If the data can message.
     */
    public Boolean canMessage() {
        return this.canMessage;
    }

    /**
     * Allows you to set the data's reply.
     *
     * @param reply ~ The data's reply to set.
     */
    public void setReply(final Optional<UUID> reply) {
        this.reply = reply;
    }

    /**
     * Allows you to retrieve the data's reply.
     *
     * @return The data's reply.
     */
    public Optional<UUID> getReply() {
        return this.reply;
    }

    /**
     * Allows you to retrieve the data's ignored.
     *
     * @return The  data's ignored.
     */
    public ArrayList<UUID> getIgnored() {
        return this.ignored;
    }

    /**
     * Allows you to retrieve the data's UUID.
     *
     * @return The data's UUID.
     */
    public UUID getUUID() {
        return this.uuid;
    }

    /**
     * Allows you to cache the data.
     */
    public void cache() {
        cache.add(this);
        this.cooldowns.forEach(cooldown -> cooldown.start());
    }

    /**
     * Allows you to uncache the data.
     */
    public void uncache() {
        this.getCooldowns().forEach(cooldown -> cooldown.stop());
        cache.remove(this);
    }

    /**
     * Allows you to retrieve if the data is cached.
     *
     * @return If the data is cached.
     */
    public boolean isCached() {
        return cache.contains(this);
    }

    /**
     * Allows you to retrieve the cache.
     *
     * @return The cache.
     */
    public static HashSet<PlayerData> getCache() {
        return cache;
    }

    /**
     * Allows you to retrieve a PlayerData from a UUID.
     * It will automatically either create a new PlayerData if it doesn't exist, load from the PlayerData if cached, or the fetch the PlayerData from the database.
     *
     * @param uuid ~ The player's UUID.
     * @return The self class.
     */
    public static CompletableFuture<PlayerData> get(final UUID uuid) {
        final Optional<PlayerData> optionalPlayerData = cache.stream().filter(playerData -> playerData.getUUID() == uuid).findAny();
        if (optionalPlayerData.isPresent()) {
            return CompletableFuture.supplyAsync(() -> optionalPlayerData.get());
        } else {
            final PlayerData playerData = new PlayerData(uuid);
            return playerData.load().thenApply(loadSuccessful -> playerData);
        }
    }

    /**
     * Allows you to retrieve a PlayerData from the cache directly.
     * This should only be used if you know this will be cached.
     *
     * @return The self class.
     */
    public static Optional<PlayerData> getViaCache(final UUID uuid) {
        return cache.stream().filter(playerData -> playerData.getUUID() == uuid).findFirst();
    }

}
