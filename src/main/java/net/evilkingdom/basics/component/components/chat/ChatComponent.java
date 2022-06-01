package net.evilkingdom.basics.component.components.chat;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.chat.commands.*;
import net.evilkingdom.basics.component.components.chat.listeners.ChatListener;
import net.evilkingdom.basics.component.components.chat.listeners.ConnectionListener;
import net.evilkingdom.basics.component.components.teleport.commands.SetSpawnCommand;
import net.evilkingdom.basics.component.components.teleport.commands.SpawnCommand;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;

public class ChatComponent {

    private final Basics plugin;

    /**
     * Allows you to create the component.
     */
    public ChatComponent() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to initialize the component.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Chat] &aInitializing..."));
        this.registerListeners();
        this.registerCommands();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Chat] &aInitialized."));
    }

    /**
     * Allows you to terminate the component.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Chat] &cTerminating..."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Basics » Component » Components » Chat] &cTerminated."));
    }

    /**
     * Allows you to register the commands.
     */
    private void registerCommands() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Chat] &aRegistering commands..."));
        new MuteChatCommand().register();
        new ToggleChatCommand().register();
        new ClearChatCommand().register();
        new SlowChatCommand().register();
        new ToggleMessagesCommand().register();
        new MessageCommand().register();
        new ReplyCommand().register();
        new IgnoreCommand().register();
        new UnignoreCommand().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Chat] &aRegistered commands."));
    }

    /**
     * Allows you to register the listeners.
     */
    private void registerListeners() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Chat] &aRegistering listeners..."));
        new ChatListener().register();
        new ConnectionListener().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Basics » Component » Components » Chat] &aRegistered listeners."));
    }

}
