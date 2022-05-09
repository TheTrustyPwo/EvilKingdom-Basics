package net.evilkingdom.basics.component.components.network.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.SelfData;
import net.evilkingdom.commons.Commons;
import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.transmission.TransmissionImplementor;
import net.evilkingdom.commons.transmission.objects.TransmissionSite;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StopCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public StopCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "stop", new ArrayList<String>(), this);
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
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.commands.stop")) {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.stop.messages.invalid-permissions").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.stop.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.stop.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.stop.sounds.error.pitch"));
                return;
            }
        }
        if (arguments.length != 0) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.stop.messages.invalid-usage").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.stop.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.stop.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.stop.sounds.error.pitch"));
            }
            return;
        }
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.stop.messages.success").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.stop.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.stop.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.stop.sounds.success.pitch"));
        }
        final String lobbyName = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.shutdowns.lobby.name");
        final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
        final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
        if (transmissionSite.getServerName().equals(lobbyName)) {
            final ArrayList<String> kickMessageList = new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.shutdowns.lobby.kick-message").stream().map(string -> StringUtilities.colorize(string)).collect(Collectors.toList()));
            final StringBuilder kickMessage = new StringBuilder();
            for (int i = 0; i < kickMessageList.size(); i++) {
                if (i == (kickMessageList.size() - 1)) {
                    kickMessage.append(kickMessageList.get(i));
                } else {
                    kickMessage.append(kickMessageList.get(i)).append("\n");
                }
            }
            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> onlinePlayer.kick(Component.text(kickMessage.toString())));
        } else {
            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> transmissionImplementor.send(onlinePlayer, lobbyName));
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            Arrays.stream(Bukkit.getPluginManager().getPlugins()).filter(plugin -> plugin.getDescription().getDepend().contains("Commons")).forEach(dependingPlugin -> Bukkit.getPluginManager().disablePlugin(dependingPlugin));
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "minecraft:stop");
        }, 20L);
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
        ArrayList<String> tabCompletion = new ArrayList<String>();
        switch (arguments.length) {
            case 1 ->  tabCompletion.addAll(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
            case 2 -> tabCompletion.addAll(Arrays.asList("gems", "tokens"));
        }
        return tabCompletion;
    }

}
