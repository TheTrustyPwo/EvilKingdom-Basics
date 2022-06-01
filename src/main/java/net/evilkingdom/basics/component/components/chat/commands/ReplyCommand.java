package net.evilkingdom.basics.component.components.chat.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.PlayerData;
import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReplyCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public ReplyCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "reply", new ArrayList<String>(List.of("r")), this);
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
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.reply.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            return;
        }
        final Player player = (Player) sender;
        if (arguments.length < 1) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.reply.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.reply.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.pitch"));
            return;
        }
        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
        if (playerData.getReply().isEmpty()) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.reply.messages.invalid-reply.no-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.reply.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.pitch"));
            return;
        }
        final Player target = Bukkit.getPlayer(playerData.getReply().get());
        if (!playerData.canMessage()) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.reply.messages.invalid-reply.messages-disabled").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.reply.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.pitch"));
            return;
        }
        if (playerData.getIgnored().contains(target.getUniqueId())) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.reply.messages.invalid-reply.ignoring-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", target.getName()))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.reply.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.pitch"));
            return;
        }
        final PlayerData targetData = PlayerData.getViaCache(target.getUniqueId()).get();
        if (!targetData.canMessage()) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.reply.messages.invalid-reply.player-messages-toggled").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", target.getName()))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.reply.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.pitch"));
            return;
        }
        if (targetData.getIgnored().contains(player.getUniqueId())) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.reply.messages.invalid-reply.ignored-by-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", target.getName()))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.reply.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.error.pitch"));
            return;
        }
        playerData.setReply(Optional.of(target.getUniqueId()));
        targetData.setReply(Optional.of(player.getUniqueId()));
        final StringBuilder messageStringBuilder = new StringBuilder();
        for (int i = 0; i < arguments.length; i++) {
            if (i == (arguments.length - 1)) {
                messageStringBuilder.append(arguments[i]);
            } else {
                messageStringBuilder.append(arguments[i]).append(" ");
            }
        }
        final String message = messageStringBuilder.toString();
        final String playerPrefix = LuckPermsUtilities.getPrefixViaCache(player.getUniqueId()).orElse("");
        final String playerSuffix = LuckPermsUtilities.getSuffixViaCache(player.getUniqueId()).orElse("");
        final String targetPrefix = LuckPermsUtilities.getPrefixViaCache(target.getUniqueId()).orElse("");
        final String targetSuffix = LuckPermsUtilities.getSuffixViaCache(target.getUniqueId()).orElse("");
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.reply.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player_prefix%", targetPrefix).replace("%player%", target.getName()).replace("%player_suffix%", targetSuffix).replace("%message%", message))));
        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.reply.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.success.player.pitch"));
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.commands.reply.messages.success.target").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player_prefix%", playerPrefix).replace("%player%", player.getName()).replace("%player_suffix%", playerSuffix).replace("%message%", message))));
        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.commands.reply.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.commands.reply.sounds.success.target.pitch"));
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
