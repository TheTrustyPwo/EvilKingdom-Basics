package net.evilkingdom.basics.component.components.chat.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.SelfData;
import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ClearChatCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public ClearChatCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "clearchat", new ArrayList<String>(List.of("cc")), this);
        command.register();
    }

    /**
     * The execution of the command.
     *
     * @param sender ~ The command's sender.
     * @param arguments ~ The command's arguments.
     */
    @Override
    public void onExecution(final CommandSender sender, final String[] arguments) {
        if (!(sender instanceof Player)) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.clearchat.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            return;
        }
        final Player player = (Player) sender;
        if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.chat.commands.clearchat")) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.clearchat.messages.invalid-permissions").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.clearchat.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.clearchat.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.clearchat.sounds.error.pitch"));
            return;
        }
        if (arguments.length != 0) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.clearchat.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.clearchat.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.clearchat.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.clearchat.sounds.error.pitch"));
            return;
        }
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> onlinePlayer.sendMessage(StringUtils.repeat(" \n&c \n", 375)));
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.clearchat.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.clearchat.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.clearchat.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.clearchat.sounds.success.player.pitch"));
        Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> (onlinePlayer.getUniqueId() != player.getUniqueId())).forEach(onlinePlayer -> {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.clearchat.messages.success.online-players").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()))));
            onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.clearchat.sounds.success.online-players.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.clearchat.sounds.success.online-players.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.clearchat.sounds.success.online-players.pitch"));
        });
    }

    /**
     * The tab completion of the command.
     *
     * @param sender ~ The command's sender.
     * @param arguments ~ The command's arguments.
     */
    @Override
    public ArrayList<String> onTabCompletion(final CommandSender sender, final String[] arguments) {
        return new ArrayList<String>();
    }

}
