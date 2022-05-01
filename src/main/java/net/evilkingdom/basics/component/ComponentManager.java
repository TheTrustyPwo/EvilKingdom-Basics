package net.evilkingdom.basics.component;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.DataComponent;
import net.evilkingdom.basics.component.components.file.FileComponent;
import net.evilkingdom.basics.component.components.teleport.TeleportComponent;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;

public class ComponentManager {

    private final Basics plugin;

    private FileComponent fileComponent;
    private DataComponent dataComponent;
    private TeleportComponent teleportComponent;

    /**
     * Allows you to create the Component Manager.
     */
    public ComponentManager() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to initialize the Component Manager.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » ComponentManager] &aInitializing..."));
        this.fileComponent = new FileComponent();
        this.fileComponent.initialize();
        this.dataComponent = new DataComponent();
        this.dataComponent.initialize();
        this.teleportComponent = new TeleportComponent();
        this.teleportComponent.initialize();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » ComponentManager] &aInitialized."));
    }

    /**
     * Allows you to terminate the Component Manager.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » ComponentManager] &cTerminating..."));
        if (this.teleportComponent != null) {
            this.teleportComponent.terminate();
        }
        if (this.dataComponent != null) {
            this.dataComponent.terminate();
        }
        if (this.fileComponent != null) {
            this.fileComponent.terminate();
        }
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » ComponentManager] &cTerminated."));
    }

    /**
     * Allows you to retrieve the File component.
     *
     * @return The File component.
     */
    public FileComponent getFileComponent() {
        return this.fileComponent;
    }

    /**
     * Allows you to retrieve the Data component.
     *
     * @return The Data component.
     */
    public DataComponent getDataComponent() {
        return this.dataComponent;
    }

    /**
     * Allows you to retrieve the Teleport component.
     *
     * @return The Teleport component.
     */
    public TeleportComponent getTeleportComponent() {
        return this.teleportComponent;
    }

}
