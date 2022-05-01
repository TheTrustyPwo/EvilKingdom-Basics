package net.evilkingdom.basics.component.components.data;

/*
 * Made with love by https://kodirati.com/.
 */

import com.mongodb.client.model.Sorts;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.listeners.ConnectionListener;
import net.evilkingdom.basics.component.components.data.objects.PlayerData;
import net.evilkingdom.basics.component.components.data.objects.SelfData;
import net.evilkingdom.commons.datapoint.DataImplementor;
import net.evilkingdom.commons.datapoint.enums.DatasiteType;
import net.evilkingdom.commons.datapoint.objects.Datapoint;
import net.evilkingdom.commons.datapoint.objects.Datasite;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DataComponent {

    private final Basics plugin;

    /**
     * Allows you to create the component.
     */
    public DataComponent() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to initialize the component.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Data] &aInitializing..."));
        this.connectToDatabase();
        this.initializeData();
        this.registerListeners();
        this.registerCommands();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Data] &aInitialized."));
    }

    /**
     * Allows you to terminate the component.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Data] &cTerminating..."));
        this.terminateData();
        this.disconnectFromDatabase();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Data] &cTerminated."));
    }

    /**
     * Allows you to register the commands.
     */
    private void registerCommands() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Data] &aRegistering commands..."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Data] &aRegistered commands."));
    }

    /**
     * Allows you to register the listeners.
     */
    private void registerListeners() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Data] &aRegistering listeners..."));
        new ConnectionListener().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Data] &aRegistered listeners."));
    }

    /**
     * Allows you to connect to the Mongo database.
     */
    private void connectToDatabase() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Data] &aConnecting to database..."));
        final Datasite datasite = new Datasite(this.plugin, this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.database.name"), DatasiteType.MONGO_DATABASE, new String[]{this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.database.connection-string")});
        new Datapoint(datasite, "basics_players");
        new Datapoint(datasite, "basics_self");
        try {
            datasite.initialize();
        } catch (final Exception exception) {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&c[Basics » Component » Components » Data] Failed to connect to database, terminating to prevent a shitshow."));
            this.plugin.getPluginLoader().disablePlugin(this.plugin);
        }
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Data] &aConnected to database."));
    }

    /**
     * Allows you to disconnect to the Mongo database.
     */
    private void disconnectFromDatabase() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Data] &cDisconnecting from database..."));
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getDatasites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        datasite.terminate();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Data] &cDisconnected from database."));
    }

    /**
     * Allows you to initialize the data.
     */
    public void initializeData() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Data] &aInitializing data..."));
        SelfData.get().whenComplete((selfData, selfDataThrowable) -> selfData.cache());
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Data] &aInitialized data."));
    }

    /**
     * Allows you to terminate the data.
     */
    public void terminateData() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Data] &cTerminating data..."));
        try {
            final SelfData selfData = SelfData.get().get();
            selfData.save(false);
            selfData.uncache();
        } catch (final ExecutionException | InterruptedException executionException) {
            //Does nothing, just in case! :)
        }
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            try {
                final PlayerData onlinePlayerData = PlayerData.get(onlinePlayer.getUniqueId()).get();
                onlinePlayerData.save(false);
                onlinePlayerData.uncache();
            } catch (final ExecutionException | InterruptedException executionException) {
                //Does nothing, just in case! :)
            }
        });
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Data] &cTerminated data."));
    }

}
