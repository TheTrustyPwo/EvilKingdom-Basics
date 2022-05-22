package net.evilkingdom.basics.component.components.network.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.SelfData;
import net.evilkingdom.basics.component.components.network.enums.NetworkServerStatus;
import net.evilkingdom.basics.component.components.network.objects.NetworkServer;
import net.evilkingdom.commons.Commons;
import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.transmission.TransmissionImplementor;
import net.evilkingdom.commons.transmission.enums.TransmissionType;
import net.evilkingdom.commons.transmission.objects.Transmission;
import net.evilkingdom.commons.transmission.objects.TransmissionServer;
import net.evilkingdom.commons.transmission.objects.TransmissionSite;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
        final Command command = new Command(this.plugin, "stop", this);
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
        if (this.plugin.getComponentManager().getNetworkComponent().isStopping()) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.stop.messages.invalid-stop").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
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
        this.plugin.getComponentManager().getNetworkComponent().setStopping(true);
        final String lobbyName = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.shutdowns.lobby");
        final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
        final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
        final boolean kickPlayer;
        if (transmissionSite.getServerName().equals(lobbyName)) {
            kickPlayer = true;
        } else {
            final NetworkServer lobbyNetworkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getName().equals(lobbyName)).findFirst().get();
            kickPlayer = lobbyNetworkServer.getStatus() != NetworkServerStatus.ONLINE;
        }
        if (kickPlayer) {
            final ArrayList<String> kickMessageList = new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.shutdowns.kick.lobby").stream().map(string -> StringUtilities.colorize(string)).collect(Collectors.toList()));
            final StringBuilder kickMessage = new StringBuilder();
            for (int i = 0; i < kickMessageList.size(); i++) {
                if (i == (kickMessageList.size() - 1)) {
                    kickMessage.append(kickMessageList.get(i));
                } else {
                    kickMessage.append(kickMessageList.get(i)).append("\n");
                }
            }
            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> onlinePlayer.kick(Component.text(kickMessage.toString())));
            CompletableFuture.runAsync(() -> {
                while (!Bukkit.getOnlinePlayers().isEmpty()) {
                    //It won't stop the server until all of the players are offline.
                }
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "minecraft:stop"), 20L);
            });
        } else {
            final TransmissionServer transmissionServer = transmissionSite.getServers().stream().filter(innerTransmissionServer -> innerTransmissionServer.getName().equals(lobbyName)).findFirst().get();
            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                transmissionSite.send(onlinePlayer, transmissionServer);
                final JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("player", onlinePlayer.getUniqueId().toString());
                jsonObject.addProperty("reason", "server_stop");
                final Transmission sentTransmission = new Transmission(transmissionSite, transmissionServer, "basics", TransmissionType.MESSAGE, UUID.randomUUID(), "player_sent=" + new Gson().toJson(jsonObject));
                sentTransmission.send();
            });
        }
        CompletableFuture.runAsync(() -> {
            while (!Bukkit.getOnlinePlayers().isEmpty()) {
                //It won't stop the server until all of the players are offline.
            }
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "minecraft:stop"), 20L);
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
