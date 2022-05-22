package net.evilkingdom.basics.component.components.data.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.JsonObject;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.commons.datapoint.DataImplementor;
import net.evilkingdom.commons.datapoint.objects.Datapoint;
import net.evilkingdom.commons.datapoint.objects.Datasite;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SelfData {

    private final Basics plugin;

    private Location spawn;
    private boolean canChat;
    private Optional<Long> chatSlow;

    private static HashSet<SelfData> cache = new HashSet<SelfData>();

    /**
     * Allows you to create a SelfData.
     */
    public SelfData() {
        this.plugin = Basics.getPlugin();

        this.spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        this.canChat = true;
        this.chatSlow = Optional.empty();
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
            final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
            final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_self")).findFirst().get();
            return datapoint.exists("self");
        }
    }

    /**
     * Allows you to load the data from the Mongo database.
     *
     * @return If the data could be loaded or not.
     */
    private CompletableFuture<Boolean> load() {
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_self")).findFirst().get();
        return datapoint.get("self").thenApply(optionalJsonObject -> {
            if (optionalJsonObject.isEmpty()) {
                return false;
            }
            final JsonObject jsonObject = optionalJsonObject.get();
            if (jsonObject.has("spawn")) {
                final JsonObject spawnJsonObject = jsonObject.get("spawn").getAsJsonObject();
                this.spawn = new Location(Bukkit.getWorld(spawnJsonObject.get("world").getAsString()), spawnJsonObject.get("x").getAsDouble(), spawnJsonObject.get("y").getAsDouble(), spawnJsonObject.get("z").getAsDouble(), (float) spawnJsonObject.get("yaw").getAsDouble(), (float) spawnJsonObject.get("pitch").getAsDouble());
            }
            if (jsonObject.has("canChat")) {
                this.canChat = jsonObject.get("canChat").getAsBoolean();
            }
            if (jsonObject.has("chatSlow")) {
                this.chatSlow = Optional.of(jsonObject.get("chatSlow").getAsLong());
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
        jsonObject.addProperty("_id", "self");
        final JsonObject spawnJsonObject = new JsonObject();
        spawnJsonObject.addProperty("world", this.spawn.getWorld().getName());
        spawnJsonObject.addProperty("x", this.spawn.getX());
        spawnJsonObject.addProperty("y", this.spawn.getY());
        spawnJsonObject.addProperty("z", this.spawn.getZ());
        spawnJsonObject.addProperty("yaw", ((double) this.spawn.getYaw()));
        spawnJsonObject.addProperty("pitch", ((double) this.spawn.getPitch()));
        jsonObject.add("spawn", spawnJsonObject);
        jsonObject.addProperty("canChat", this.canChat);
        this.chatSlow.ifPresent(chatSlow -> jsonObject.addProperty("chatSlow", chatSlow));
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_self")).findFirst().get();
        datapoint.save(jsonObject, asynchronous);
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
     * Allows you to set the data's chat slow.
     *
     * @param chatSlow ~ The data's chat slow to set.
     */
    public void setChatSlow(final Optional<Long> chatSlow) {
        this.chatSlow = chatSlow;
    }

    /**
     * Allows you to retrieve the data's chat slow.
     *
     * @return The data's chat slow.
     */
    public Optional<Long> getChatSlow() {
        return this.chatSlow;
    }

    /**
     * Allows you to set the data's spawn.
     *
     * @param spawn ~ The data's spawn to set.
     */
    public void setSpawn(final Location spawn) {
        this.spawn = spawn;
    }

    /**
     * Allows you to retrieve the data's spawn.
     *
     * @return The data's spawn.
     */
    public Location getSpawn() {
        return this.spawn;
    }

    /**
     * Allows you to cache the data.
     */
    public void cache() {
        cache.add(this);
    }

    /**
     * Allows you to uncache the data.
     */
    public void uncache() {
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
    public static HashSet<SelfData> getCache() {
        return cache;
    }

    /**
     * Allows you to retrieve a SelfData.
     * It will automatically either create a new SelfData if it doesn't exist, load from the SelfData if cached, or the fetch the SelfData from the database.
     *
     * @return The self class.
     */
    public static CompletableFuture<SelfData> get() {
        final Optional<SelfData> optionalSelfData = cache.stream().findFirst();
        if (optionalSelfData.isPresent()) {
            return CompletableFuture.supplyAsync(() -> optionalSelfData.get());
        } else {
            final SelfData selfData = new SelfData();
            return selfData.load().thenApply(loadSuccessful -> selfData);
        }
    }

    /**
     * Allows you to retrieve a SelfData from the cache directly.
     * This should only be used if you know this will be cached.
     *
     * @return The self class.
     */
    public static Optional<SelfData> getViaCache() {
        return cache.stream().findFirst();
    }

}
