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
import net.evilkingdom.commons.utilities.time.TimeUtilities;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlowChatCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public SlowChatCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "slowchat", this);
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
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.slowchat.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            return;
        }
        final Player player = (Player) sender;
        if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.chat.commands.slowchat")) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.slowchat.messages.invalid-permissions").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.slowchat.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.error.pitch"));
            return;
        }
        if (arguments.length != 1) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.slowchat.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.slowchat.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.error.pitch"));
            return;
        }
        final Optional<Long> optionalDuration = TimeUtilities.get(arguments[0]);
        if (optionalDuration.isEmpty()) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.slowchat.messages.invalid-duration").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.slowchat.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.error.pitch"));
            return;
        }
        final long duration = optionalDuration.get();
        final SelfData selfData = SelfData.getViaCache().get();
        if (duration == 0L) {
            selfData.setChatSlow(Optional.empty());
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.slowchat.messages.success.removed.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.slowchat.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.player.pitch"));
            Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> (onlinePlayer.getUniqueId() != player.getUniqueId()) && !LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.chat.global.slowchat.bypass")).forEach(onlinePlayer -> {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.slowchat.messages.success.removed.online-players").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()))));
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.slowchat.sounds.success.online-players.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.online-players.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.online-players.pitch"));
            });
            Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> (onlinePlayer.getUniqueId() != player.getUniqueId()) && LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.chat.global.slowchat.bypass")).forEach(onlinePlayer -> {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.slowchat.messages.success.removed.online-bypassers").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()))));
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.slowchat.sounds.success.online-bypassers.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.online-bypassers.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.online-bypassers.pitch"));
            });
        } else {
            selfData.setChatSlow(Optional.of(duration));
            final String formattedDuration = TimeUtilities.format(duration);
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.slowchat.messages.success.set.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%duration%", formattedDuration))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.slowchat.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.player.pitch"));
            Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> (onlinePlayer.getUniqueId() != player.getUniqueId()) && !LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.chat.global.slowchat.bypass")).forEach(onlinePlayer -> {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.slowchat.messages.success.set.online-players").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%duration%", formattedDuration).replace("%player%", player.getName()))));
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.slowchat.sounds.success.online-players.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.online-players.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.online-players.pitch"));
            });
            Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> (onlinePlayer.getUniqueId() != player.getUniqueId()) && LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.chat.global.slowchat.bypass")).forEach(onlinePlayer -> {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.slowchat.messages.success.set.online-bypassers").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%duration%", formattedDuration).replace("%player%", player.getName()))));
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.slowchat.sounds.success.online-bypassers.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.online-bypassers.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.slowchat.sounds.success.online-bypassers.pitch"));
            });
        }
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
