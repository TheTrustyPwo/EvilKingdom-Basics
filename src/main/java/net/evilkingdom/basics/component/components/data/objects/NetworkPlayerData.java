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
import org.bukkit.permissions.PermissionAttachment;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NetworkPlayerData {

    private final Basics plugin;

    private final UUID uuid;
    private Optional<String> prefix, suffix;
    private Optional<PermissionAttachment> permissionAttachment;
    private final ArrayList<String> ranks, permissions, negatedPermissions;

    private static final HashSet<NetworkPlayerData> cache = new HashSet<NetworkPlayerData>();

    /**
     * Allows you to create a NetworkPlayerData.
     *
     * @param uuid ~ The UUID of the player.
     */
    public NetworkPlayerData(final UUID uuid) {
        this.plugin = Basics.getPlugin();

        this.uuid = uuid;
        this.ranks = new ArrayList<String>();
        this.permissions = new ArrayList<String>();
        this.negatedPermissions = new ArrayList<String>();
        this.prefix = Optional.empty();
        this.suffix = Optional.empty();
        this.permissionAttachment = Optional.empty();
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
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin && innerDatasite.getName().equals("network")).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_players")).findFirst().get();
        return datapoint.get(this.uuid.toString()).thenApply(optionalJsonObject -> {
            if (optionalJsonObject.isEmpty()) {
                return false;
            }
            final JsonObject jsonObject = optionalJsonObject.get();
            if (jsonObject.has("ranks")) {
                jsonObject.get("ranks").getAsJsonArray().forEach(jsonElement -> this.ranks.add(jsonElement.getAsString()));
            }
            if (jsonObject.has("permissions")) {
                jsonObject.get("permissions").getAsJsonArray().forEach(jsonElement -> this.permissions.add(jsonElement.getAsString()));
            }
            if (jsonObject.has("negatedPermissions")) {
                jsonObject.get("negatedPermissions").getAsJsonArray().forEach(jsonElement -> this.negatedPermissions.add(jsonElement.getAsString()));
            }
            if (jsonObject.has("prefix")) {
                this.prefix = Optional.of(jsonObject.get("prefix").getAsString());
            }
            if (jsonObject.has("suffix")) {
                this.suffix = Optional.of(jsonObject.get("suffix").getAsString());
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
        final JsonArray ranksJsonArray = new JsonArray();
        this.ranks.forEach(rank -> ranksJsonArray.add(rank));
        jsonObject.add("ranks", ranksJsonArray);
        final JsonArray permissionsJsonArray = new JsonArray();
        this.permissions.forEach(permission -> permissionsJsonArray.add(permission));
        jsonObject.add("permissions", permissionsJsonArray);
        final JsonArray negatedPermissionsJsonArray = new JsonArray();
        this.negatedPermissions.forEach(negatedPermission -> negatedPermissionsJsonArray.add(negatedPermission));
        jsonObject.add("negatedPermissions", negatedPermissionsJsonArray);
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin && innerDatasite.getName().equals("network")).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_players")).findFirst().get();
        datapoint.save(jsonObject, this.uuid.toString(), asynchronous);
    }

    /**
     * Allows you to retrieve the data's ranks.
     *
     * @return The data's ranks.
     */
    public ArrayList<String> getRanks() {
        return this.ranks;
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
     * Allows you to retrieve the data's refined permissions.
     * "Refined" means it goes through not only THEIR permissions (priority), but it also goes through all of their ranks permissions as well.
     *
     * @return The data's refined permissions.
     */
    public ArrayList<String> getRefinedPermissions() {
        final ArrayList<String> refinedPermissions = new ArrayList<String>();
        for (final NetworkRankData networkRankData : this.ranks.stream().map(rankName -> NetworkRankData.getViaCache(rankName).get()).sorted(Comparator.comparingLong(rank -> rank.getAuthority())).collect(Collectors.toList())) {
            refinedPermissions.addAll(networkRankData.getPermissions());
        }
        refinedPermissions.addAll(this.permissions);
        return refinedPermissions;
    }

    /**
     * Allows you to retrieve the data's refined negated permissions.
     * "Refined" means it goes through not only THEIR negated permissions (priority), but it also goes through all of their ranks negated permissions as well.
     *
     * @return The data's refined negated permissions.
     */
    public ArrayList<String> getRefinedNegatedPermissions() {
        final ArrayList<String> refinedNegatedPermissions = new ArrayList<String>();
        for (final NetworkRankData networkRankData : this.ranks.stream().map(rankName -> NetworkRankData.getViaCache(rankName).get()).sorted(Comparator.comparingLong(rank -> rank.getAuthority())).collect(Collectors.toList())) {
            refinedNegatedPermissions.addAll(networkRankData.getNegatedPermissions());
            refinedNegatedPermissions.removeAll(networkRankData.getPermissions());
        }
        refinedNegatedPermissions.addAll(this.negatedPermissions);
        return refinedNegatedPermissions;
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
     * Allows you to set the data's permission attachment.
     *
     * @param permissionAttachment ~ The data's permission attachment to set.
     */
    public void setPermissionAttachment(final Optional<PermissionAttachment> permissionAttachment) {
        this.permissionAttachment = permissionAttachment;
    }

    /**
     * Allows you to retrieve the data's permission attachment.
     *
     * @return The data's permission attachment.
     */
    public Optional<PermissionAttachment> getPermissionAttachment() {
        return this.permissionAttachment;
    }

    /**
     * Allows you to retrieve the data's refined prefix.
     * "Refined" means it goes through not only THEIR prefix (priority), but it also goes through all of their ranks prefixes as well.
     *
     * @return The data's refined prefix.
     */
    public Optional<String> getRefinedPrefix() {
        Optional<String> refinedPrefix = this.prefix;
        for (final NetworkRankData networkRankData : this.ranks.stream().map(rankName -> NetworkRankData.getViaCache(rankName).get()).sorted(Comparator.comparingLong(rank -> rank.getAuthority())).sorted(Collections.reverseOrder()).collect(Collectors.toList())) {
            if (networkRankData.getPrefix().isPresent()) {
                refinedPrefix = networkRankData.getPrefix();
            }
        }
        return refinedPrefix;
    }

    /**
     * Allows you to retrieve the data's refined suffix.
     * "Refined" means it goes through not only THEIR suffix (priority), but it also goes through all of their ranks suffixes as well.
     *
     * @return The data's refined suffix.
     */
    public Optional<String> getRefinedSuffix() {
        Optional<String> refinedSuffix = this.suffix;
        for (final NetworkRankData networkRankData : this.ranks.stream().map(rankName -> NetworkRankData.getViaCache(rankName).get()).sorted(Comparator.comparingLong(rank -> rank.getAuthority())).sorted(Collections.reverseOrder()).collect(Collectors.toList())) {
            if (networkRankData.getSuffix().isPresent()) {
                refinedSuffix = networkRankData.getSuffix();
            }
        }
        return refinedSuffix;
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
    public static HashSet<NetworkPlayerData> getCache() {
        return cache;
    }

    /**
     * Allows you to retrieve a NetworkPlayerData from a UUID.
     * It will automatically either create a new NetworkPlayerData if it doesn't exist, load from the NetworkPlayerData if cached, or the fetch the NetworkPlayerData from the database.
     *
     * @param uuid ~ The player's UUID.
     * @return The self class.
     */
    public static CompletableFuture<NetworkPlayerData> get(final UUID uuid) {
        final Optional<NetworkPlayerData> optionalNetworkPlayerData = cache.stream().filter(networkPlayerData -> networkPlayerData.getUUID() == uuid).findAny();
        if (optionalNetworkPlayerData.isPresent()) {
            return CompletableFuture.supplyAsync(() -> optionalNetworkPlayerData.get());
        } else {
            final NetworkPlayerData networkPlayerData = new NetworkPlayerData(uuid);
            return networkPlayerData.load().thenApply(loadSuccessful -> networkPlayerData);
        }
    }

    /**
     * Allows you to retrieve a NetworkPlayerData from the cache directly.
     * This should only be used if you know this will be cached.
     *
     * @param uuid ~ The player's UUID.
     * @return The self class.
     */
    public static Optional<NetworkPlayerData> getViaCache(final UUID uuid) {
        return cache.stream().filter(networkPlayerData -> networkPlayerData.getUUID() == uuid).findFirst();
    }

}
