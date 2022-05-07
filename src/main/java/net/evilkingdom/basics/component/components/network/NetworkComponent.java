package net.evilkingdom.basics.component.components.network;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.network.listeners.ConnectionListener;
import net.evilkingdom.basics.component.components.network.listeners.custom.TransmissionListener;
import net.evilkingdom.commons.transmission.TransmissionImplementor;
import net.evilkingdom.commons.transmission.enums.TransmissionType;
import net.evilkingdom.commons.transmission.objects.Transmission;
import net.evilkingdom.commons.transmission.objects.TransmissionSite;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;

import java.util.Optional;
import java.util.UUID;

public class NetworkComponent {

    private final Basics plugin;

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
        this.registerCommands();
        this.registerListeners();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Network] &aInitialized."));
    }

    /**
     * Allows you to terminate the component.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Network] &cTerminating..."));
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
        final TransmissionSite transmissionSite = new TransmissionSite(this.plugin, this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.transmissions.sites.internal"), "basics");
        transmissionSite.setHandler(new TransmissionListener());
        transmissionSite.register();
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.transmissions.sites.external").forEach(serverName -> {
            final Transmission transmission = new Transmission(transmissionSite, TransmissionType.MESSAGE, serverName, "basics", UUID.randomUUID(),"server_status=online");
            transmission.send();
        });
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
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.transmissions.sites.external").forEach(serverName -> {
                final Transmission transmission = new Transmission(transmissionSite, TransmissionType.MESSAGE, serverName, "basics", UUID.randomUUID(),"server_status=offline");
                transmission.send();
            });
            transmissionSite.unregister();
        }
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Network] &cTerminated transmissions..."));
    }

}
