package net.evilkingdom.basics;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.component.ComponentManager;
import net.evilkingdom.commons.Commons;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Basics extends JavaPlugin {

    private static Basics plugin;
    private ComponentManager componentManager;

    /**
     * Bukkit's detection for the plugin enabling.
     */
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics] &aInitializing..."));
        plugin = this;
        this.componentManager = new ComponentManager();
        this.componentManager.initialize();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics] &aInitialized."));
    }

    /**
     * Bukkit's detection for the plugin disabling.
     */
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics] &cTerminating..."));
        this.componentManager.terminate();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics] &cTerminated."));
    }

    /**
     * Allows you to retrieve the plugin.
     *
     * @return The plugin.
     */
    public static Basics getPlugin() {
        return plugin;
    }

    /**
     * Allows you to retrieve the Component Manager.
     *
     * @return The Component Manager.
     */
    public ComponentManager getComponentManager() {
        return this.componentManager;
    }

}
