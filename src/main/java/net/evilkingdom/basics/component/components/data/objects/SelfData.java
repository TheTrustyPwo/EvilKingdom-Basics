package net.evilkingdom.basics.component.components.data.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.commons.datapoint.DataImplementor;
import net.evilkingdom.commons.datapoint.objects.Datapoint;
import net.evilkingdom.commons.datapoint.objects.DatapointModel;
import net.evilkingdom.commons.datapoint.objects.DatapointObject;
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

    private static HashSet<SelfData> cache = new HashSet<SelfData>();

    /**
     * Allows you to create a SelfData.
     */
    public SelfData() {
        this.plugin = Basics.getPlugin();

        this.spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
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
        return datapoint.get("self").thenApply(optionalDatapointModel -> {
            if (optionalDatapointModel.isEmpty()) {
                return false;
            }
            final DatapointModel datapointModel = optionalDatapointModel.get();
            if (datapointModel.getObjects().containsKey("spawn")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("spawn");
                final World world = Bukkit.getWorld(((String) datapointObject.getInnerObjects().get("world").getObject()));
                final double x = (double) datapointObject.getInnerObjects().get("x").getObject();
                final double y = (double) datapointObject.getInnerObjects().get("y").getObject();
                final double z = (double) datapointObject.getInnerObjects().get("z").getObject();
                final float yaw = ((Double) datapointObject.getInnerObjects().get("yaw").getObject()).floatValue();
                final float pitch = ((Double) datapointObject.getInnerObjects().get("pitch").getObject()).floatValue();
                this.spawn = new Location(world, x, y, z, yaw, pitch);
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
        final DatapointModel datapointModel = new DatapointModel("self");
        final DatapointObject spawnDatapointObject = new DatapointObject();
        spawnDatapointObject.getInnerObjects().put("world", new DatapointObject(this.spawn.getWorld().getName()));
        spawnDatapointObject.getInnerObjects().put("x", new DatapointObject(this.spawn.getX()));
        spawnDatapointObject.getInnerObjects().put("y", new DatapointObject(this.spawn.getY()));
        spawnDatapointObject.getInnerObjects().put("z", new DatapointObject(this.spawn.getZ()));
        spawnDatapointObject.getInnerObjects().put("yaw", new DatapointObject(this.spawn.getYaw()));
        spawnDatapointObject.getInnerObjects().put("pitch", new DatapointObject(this.spawn.getPitch()));
        datapointModel.getObjects().put("spawn", spawnDatapointObject);
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("basics_self")).findFirst().get();
        datapoint.save(datapointModel, asynchronous);
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
