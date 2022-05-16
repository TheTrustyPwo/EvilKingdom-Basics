package net.evilkingdom.basics.component.components.chat.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.SelfData;
import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.mojang.MojangUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MuteChatCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public MuteChatCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "mutechat", new ArrayList<String>(List.of("silencechat")), this);
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
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.mutechat.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            return;
        }
        final Player player = (Player) sender;
        if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.chat.commands.mutechat")) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.mutechat.messages.invalid-permissions").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.mutechat.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.mutechat.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.mutechat.sounds.error.pitch"));
            return;
        }
        if (arguments.length != 0) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.mutechat.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.mutechat.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.mutechat.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.mutechat.sounds.error.pitch"));
            return;
        }
        final SelfData selfData = SelfData.getViaCache().get();
        final String status;
        if (selfData.canChat()) {
            status = "&cdisabled";
            selfData.setCanChat(false);
        } else {
            status = "&aenabled";
            selfData.setCanChat(true);
        }
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.mutechat.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%status%", status))));
        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.mutechat.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.mutechat.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.mutechat.sounds.success.player.pitch"));
        Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> (onlinePlayer.getUniqueId() != player.getUniqueId()) && !LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.chat.global.mutechat.bypass")).forEach(onlinePlayer -> {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.mutechat.messages.success.online-players").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()).replace("%status%", status))));
            onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.mutechat.sounds.success.online-players.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.mutechat.sounds.success.online-players.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.mutechat.sounds.success.online-players.pitch"));
        });
        Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> (onlinePlayer.getUniqueId() != player.getUniqueId()) && LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.chat.global.mutechat.bypass")).forEach(onlinePlayer -> {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.mutechat.messages.success.online-bypassers").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()).replace("%status%", status))));
            onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.mutechat.sounds.success.online-bypassers.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.mutechat.sounds.success.online-bypassers.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.mutechat.sounds.success.online-bypassers.pitch"));
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
