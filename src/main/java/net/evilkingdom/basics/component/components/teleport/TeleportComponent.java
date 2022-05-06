package net.evilkingdom.basics.component.components.teleport;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.teleport.commands.SetSpawnCommand;
import net.evilkingdom.basics.component.components.teleport.commands.SpawnCommand;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;

import java.util.concurrent.ExecutionException;

public class TeleportComponent {

    private final Basics plugin;

    /**
     * Allows you to create the component.
     */
    public TeleportComponent() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to initialize the component.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Teleport] &aInitializing..."));
        this.registerCommands();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Teleport] &aInitialized."));
    }

    /**
     * Allows you to terminate the component.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Teleport] &cTerminating..."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Teleport] &cTerminated."));
    }

    /**
     * Allows you to register the commands.
     */
    private void registerCommands() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Teleport] &aRegistering commands..."));
        new SpawnCommand().register();
        new SetSpawnCommand().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Teleport] &aRegistered commands."));
    }

}
