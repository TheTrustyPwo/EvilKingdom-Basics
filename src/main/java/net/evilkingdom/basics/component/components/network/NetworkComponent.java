package net.evilkingdom.basics.component.components.network;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.network.listeners.ConnectionListener;
import net.evilkingdom.basics.component.components.network.listeners.custom.TransmissionListener;
import net.evilkingdom.basics.component.components.network.objects.NetworkServer;
import net.evilkingdom.commons.transmission.TransmissionImplementor;
import net.evilkingdom.commons.transmission.enums.TransmissionType;
import net.evilkingdom.commons.transmission.objects.Transmission;
import net.evilkingdom.commons.transmission.objects.TransmissionSite;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class NetworkComponent {

    private final Basics plugin;
    private BukkitTask serverTask;
    private HashSet<NetworkServer> servers;

    /**
     * Allows you to create the component.
     */
    public NetworkComponent() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to initialize the component.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Network] &aInitializing..."));
        this.initializeTransmissions();
        this.initializeServers();
        this.registerCommands();
        this.registerListeners();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Network] &aInitialized."));
    }

    /**
     * Allows you to terminate the component.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Network] &cTerminating..."));
        this.terminateServers();
        this.terminateTransmissions();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Network] &cTerminated."));
    }

    /**
     * Allows you to register the commands.
     */
    private void registerCommands() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Network] &aRegistering commands..."));
//        new SpawnCommand().register();
//        new SetSpawnCommand().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Network] &aRegistered commands."));
    }

    /**
     * Allows you to register the listeners.
     */
    private void registerListeners() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Network] &aRegistering listeners..."));
        new ConnectionListener().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Network] &aRegistered listeners."));
    }

    /**
     * Allows you to initialize the transmissions.
     */
    private void initializeTransmissions() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Network] &aInitializing transmissions..."));
        final TransmissionSite transmissionSite = new TransmissionSite(this.plugin, this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.transmissions.servers.internal"), "basics");
        transmissionSite.setHandler(new TransmissionListener());
        transmissionSite.register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Network] &aInitialized transmissions."));
    }

    /**
     * Allows you to terminate the transmissions.
     */
    private void terminateTransmissions() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Network] &cTerminating transmissions..."));
        final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
        final Optional<TransmissionSite> optionalTransmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst();
        if (optionalTransmissionSite.isPresent()) {
            final TransmissionSite transmissionSite = optionalTransmissionSite.get();
            transmissionSite.unregister();
        }
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Network] &cTerminated transmissions."));
    }

    /**
     * Allows you to initialize the servers.
     */
    private void initializeServers() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Network] &aInitializing servers..."));
        this.servers = new HashSet<NetworkServer>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.network.transmissions.servers.external").getKeys(false).stream().map(serverName -> new NetworkServer(serverName, this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.transmissions.servers.external." + serverName + ".ip"), this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.network.transmissions.servers.external." + serverName + ".port"))).collect(Collectors.toSet()));
        this.serverTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, () -> this.servers.forEach(server -> server.updateData()), 0L, 100L);
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Network] &aInitialized servers."));
    }

    /**
     * Allows you to terminate the servers.
     */
    private void terminateServers() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Network] &cTerminating servers..."));
        if (this.serverTask == null) {
            return;
        }
        this.serverTask.cancel();
        this.servers.clear();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Network] &cTerminated servers."));
    }

    /**
     * Allows you to retrieve the servers.
     *
     * @return ~ The servers.
     */
    public HashSet<NetworkServer> getServers() {
        return this.servers;
    }
}
