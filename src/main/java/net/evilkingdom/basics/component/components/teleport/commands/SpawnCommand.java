package net.evilkingdom.basics.component.components.teleport.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.SelfData;
import net.evilkingdom.basics.component.components.network.enums.NetworkServerStatus;
import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpawnCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public SpawnCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "spawn",this);
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
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.teleport.commands.spawn.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            return;
        }
        final Player player = (Player) sender;
        if (arguments.length > 1) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.teleport.commands.spawn.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.teleport.commands.spawn.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.error.pitch"));
            return;
        }
        switch (arguments.length) {
            case 0 -> {
                final SelfData selfData = SelfData.getViaCache().get();
                Bukkit.getScheduler().runTask(this.plugin, () -> player.teleport(selfData.getSpawn()));
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.teleport.commands.spawn.messages.success.no-target").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.teleport.commands.spawn.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.success.player.pitch"));
            }
            case 1 -> {
                if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.teleport.commands.spawn.targeted")) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.teleport.commands.spawn.messages.invalid-permissions").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.teleport.commands.spawn.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.error.pitch"));
                    return;
                }
                final Optional<? extends Player> optionalTarget = Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> onlinePlayer.getName().equalsIgnoreCase(arguments[0])).findFirst();
                if (optionalTarget.isEmpty()) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.teleport.commands.spawn.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[0]))));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.teleport.commands.spawn.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.error.pitch"));
                    return;
                }
                final Player target = optionalTarget.get();
                final SelfData selfData = SelfData.getViaCache().get();
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.teleport.commands.spawn.messages.with-target.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", target.getName()))));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.teleport.commands.spawn.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.success.player.pitch"));
                Bukkit.getScheduler().runTask(this.plugin, () -> target.teleport(selfData.getSpawn()));
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.teleport.commands.spawn.messages.with-target.target").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()))));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.teleport.commands.spawn.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.spawn.sounds.success.target.pitch"));
            }
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
        if (!(sender instanceof Player)) {
            return new ArrayList<String>();
        }
        final Player player = (Player) sender;
        if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.teleport.commands.spawn.targeted")) {
            return new ArrayList<String>();
        }
        ArrayList<String> tabCompletion = new ArrayList<String>();
        switch (arguments.length) {
            case 1 -> tabCompletion.addAll(Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> onlinePlayer.getUniqueId() != player.getUniqueId()).map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
        }
        return tabCompletion;
    }

}
