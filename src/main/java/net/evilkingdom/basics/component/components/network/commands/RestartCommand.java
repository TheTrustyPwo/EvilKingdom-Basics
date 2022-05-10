package net.evilkingdom.basics.component.components.network.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.network.enums.NetworkServerStatus;
import net.evilkingdom.basics.component.components.network.objects.NetworkServer;
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
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RestartCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public RestartCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "restart", new ArrayList<String>(List.of("saferestart")), this);
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
            if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.commands.restart")) {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.restart.messages.invalid-permissions").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.restart.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.error.pitch"));
                return;
            }
        }
        if (arguments.length != 0) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.restart.messages.invalid-usage").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.restart.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.error.pitch"));
                return;
            }
            return;
        }
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.restart.messages.success").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.restart.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.success.pitch"));
        }
        final String lobbyName = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.shutdowns.lobby.name");
        final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
        final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
        if (transmissionSite.getServerName().equals(lobbyName)) {
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
        } else {
            final NetworkServer lobbyNetworkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getName().equals(lobbyName)).findFirst().get();
            if (lobbyNetworkServer.getStatus() != NetworkServerStatus.ONLINE) {
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
            } else {
                Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                    transmissionImplementor.send(onlinePlayer, lobbyName);
                    CompletableFuture.runAsync(() -> {
                        final NetworkServer networkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getName().equals(lobbyName)).findFirst().get();
                        while (!networkServer.getOnlinePlayerUUIDs().contains(onlinePlayer.getUniqueId())) {
                            //It won't send the message until the player is registered as connected to the lobby.
                        }
                        final JsonArray jsonArray = new JsonArray();
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.shutdowns.kick.server.message").forEach(string -> jsonArray.add(string));
                        final TransmissionServer transmissionServer = transmissionSite.getServers().stream().filter(innerTransmissionServer -> innerTransmissionServer.getName().equals(lobbyName)).findFirst().get();
                        final Transmission messageTransmission = new Transmission(transmissionSite, transmissionServer, "basics", TransmissionType.MESSAGE, UUID.randomUUID(), "player_message=" + onlinePlayer.getUniqueId() + "~" + new Gson().toJson(jsonArray));
                        final Transmission soundTransmission = new Transmission(transmissionSite, transmissionServer, "basics", TransmissionType.MESSAGE, UUID.randomUUID(), "player_sound=" + onlinePlayer.getUniqueId() + "~" + this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.shutdowns.kick.server.sound.sound") + ":" + this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.shutdowns.kick.server.sound.volume") + ":" + this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.shutdowns.kick.server.sound.pitch"));
                        messageTransmission.send();
                        soundTransmission.send();
                    });
                });
            }
        }
        CompletableFuture.runAsync(() -> {
            while (!Bukkit.getOnlinePlayers().isEmpty()) {
                //It won't stop the server until all of the players are offline.
            }
            Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "spigot:restart"));
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
