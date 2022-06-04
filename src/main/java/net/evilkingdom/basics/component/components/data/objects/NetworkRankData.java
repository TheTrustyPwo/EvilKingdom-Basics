package net.evilkingdom.basics.component.components.data.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.commons.datapoint.DataImplementor;
import net.evilkingdom.commons.datapoint.objects.Datapoint;
import net.evilkingdom.commons.datapoint.objects.Datasite;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NetworkRankData {

    private final Basics plugin;

    private final String name;
    private String prettifiedName;
    private long authority;
    private Optional<String> prefix, suffix;
    private final ArrayList<String> permissions, negatedPermissions;

    private static final HashSet<NetworkRankData> cache = new HashSet<NetworkRankData>();

    /**
     * Allows you to create a NetworkRankData.
     *
     * @param name ~ The name of the rank.
     */
    public NetworkRankData(final String name) {
        this.plugin = Basics.getPlugin();

        this.name = name;
        this.prettifiedName = name;
        this.permissions = new ArrayList<String>();
        this.negatedPermissions = new ArrayList<String>();
        this.authority = 0L;
        this.prefix = Optional.empty();
        this.suffix = Optional.empty();
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
            final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin && innerDatasite.getName().equals("network")).findFirst().get();
            final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_ranks")).findFirst().get();
            return datapoint.exists(this.name);
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
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin && innerDatasite.getName().equals("network")).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_ranks")).findFirst().get();
        return datapoint.get(this.name).thenApply(optionalJsonObject -> {
            if (optionalJsonObject.isEmpty()) {
                return false;
            }
            final JsonObject jsonObject = optionalJsonObject.get();
            if (jsonObject.has("permissions")) {
                jsonObject.get("permissions").getAsJsonArray().forEach(jsonElement -> this.permissions.add(jsonElement.getAsString()));
            }
            if (jsonObject.has("negatedPermissions")) {
                jsonObject.get("negatedPermissions").getAsJsonArray().forEach(jsonElement -> this.negatedPermissions.add(jsonElement.getAsString()));
            }
            if (jsonObject.has("authority")) {
                this.authority = jsonObject.get("authority").getAsLong();
            }
            if (jsonObject.has("prefix")) {
                this.prefix = Optional.of(jsonObject.get("prefix").getAsString());
            }
            if (jsonObject.has("suffix")) {
                this.suffix = Optional.of(jsonObject.get("suffix").getAsString());
            }
            if (jsonObject.has("prettifiedName")) {
                this.prettifiedName = jsonObject.get("prettifiedName").getAsString();
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
        this.prefix.ifPresent(prefix -> jsonObject.addProperty("prefix", prefix));
        this.suffix.ifPresent(suffix -> jsonObject.addProperty("suffix", suffix));
        jsonObject.addProperty("authority", this.authority);
        jsonObject.addProperty("prettifiedName", this.prettifiedName);
        final JsonArray permissionsJsonArray = new JsonArray();
        this.permissions.forEach(permission -> permissionsJsonArray.add(permission));
        jsonObject.add("permissions", permissionsJsonArray);
        final JsonArray negatedPermissionsJsonArray = new JsonArray();
        this.negatedPermissions.forEach(negatedPermission -> negatedPermissionsJsonArray.add(negatedPermission));
        jsonObject.add("negatedPermissions", negatedPermissionsJsonArray);
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin && innerDatasite.getName().equals("network")).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_ranks")).findFirst().get();
        datapoint.save(jsonObject, this.name, asynchronous);
    }

    /**
     * Allows you to retrieve the data's permissions.
     *
     * @return The data's permissions.
     */
    public ArrayList<String> getPermissions() {
        return this.permissions;
    }

    /**
     * Allows you to retrieve the data's negated permissions.
     *
     * @return The data's negated permissions.
     */
    public ArrayList<String> getNegatedPermissions() {
        return this.negatedPermissions;
    }

    /**
     * Allows you to set the data's prefix.
     *
     * @param prefix ~ The data's prefix to set.
     */
    public void setPrefix(final Optional<String> prefix) {
        this.prefix = prefix;
    }

    /**
     * Allows you to set the data's suffix.
     *
     * @param suffix ~ The data's suffix to set.
     */
    public void setSuffix(final Optional<String> suffix) {
        this.suffix = suffix;
    }

    /**
     * Allows you to retrieve the data's prefix.
     *
     * @return The data's prefix.
     */
    public Optional<String> getPrefix() {
        return this.prefix;
    }

    /**
     * Allows you to retrieve the data's suffix.
     *
     * @return The data's suffix.
     */
    public Optional<String> getSuffix() {
        return this.suffix;
    }

    /**
     * Allows you to retrieve the data's authority.
     *
     * @return The data's authority.
     */
    public Long getAuthority() {
        return this.authority;
    }

    /**
     * Allows you to set the data's authority.
     *
     * @param authority ~ The data's authority to set.
     */
    public void setAuthority(final long authority) {
        this.authority = authority;
    }

    /**
     * Allows you to retrieve the data's prettified name.
     *
     * @return The data's prettified name.
     */
    public String getPrettifiedName() {
        return this.prettifiedName;
    }

    /**
     * Allows you to set the data's prettified name.
     *
     * @param prettifiedName ~ The data's prettified name to set.
     */
    public void setPrettifiedName(final String prettifiedName) {
        this.prettifiedName = prettifiedName;
    }

    /**
     * Allows you to retrieve the data's name.
     *
     * @return The data's name.
     */
    public String getName() {
        return this.name;
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
    public static HashSet<NetworkRankData> getCache() {
        return cache;
    }

    /**
     * Allows you to retrieve a NetworkRankData from a name.
     * It will automatically either create a new NetworkRankData if it doesn't exist, load from the NetworkRankData if cached, or the fetch the NetworkRankData from the database.
     *
     * @param name ~ The rank's name.
     * @return The self class.
     */
    public static CompletableFuture<NetworkRankData> get(final String name) {
        final Optional<NetworkRankData> optionalNetworkRankData = cache.stream().filter(networkRankData -> networkRankData.getName().equals(name)).findAny();
        if (optionalNetworkRankData.isPresent()) {
            return CompletableFuture.supplyAsync(() -> optionalNetworkRankData.get());
        } else {
            final NetworkRankData networkRankData = new NetworkRankData(name);
            return networkRankData.load().thenApply(loadSuccessful -> networkRankData);
        }
    }

    /**
     * Allows you to retrieve a NetworkRankData from the cache directly.
     * This should only be used if you know this will be cached.
     *
     * @param name ~ The rank's name.
     * @return The self class.
     */
    public static Optional<NetworkRankData> getViaCache(final String name) {
        return cache.stream().filter(networkRankData -> networkRankData.getName().equals(name)).findFirst();
    }


}
