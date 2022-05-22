package net.evilkingdom.basics.component.components.network.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import net.evilkingdom.commons.utilities.mojang.MojangUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SendCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public SendCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "send", new ArrayList<String>(List.of("connect")), this);
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
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            return;
        }
        final Player player = (Player) sender;
        if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.commands.send")) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.invalid-permissions").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.pitch"));
            return;
        }
        if (arguments.length != 2) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.pitch"));
            return;
        }
        MojangUtilities.getUUID(arguments[0]).whenComplete((optionalTargetUUID, uuidThrowable) -> {
            if (optionalTargetUUID.isEmpty()) {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[0]))));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.pitch"));
                return;
            }
            final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(optionalTargetUUID.get());
            final ArrayList<UUID> playerUUIDs = new ArrayList<UUID>(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getUniqueId()).collect(Collectors.toList()));
            this.plugin.getComponentManager().getNetworkComponent().getServers().forEach(networkServer -> playerUUIDs.addAll(networkServer.getOnlinePlayerUUIDs()));
            if (!playerUUIDs.contains(offlineTarget.getUniqueId())) {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.pitch"));
                return;
            }
            final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
            final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
            final String targetServerName = arguments[1].toLowerCase();
            if (!targetServerName.equals("here") && !this.plugin.getComponentManager().getNetworkComponent().getServers().stream().map(innerNetworkServer -> innerNetworkServer.getName()).toList().contains(targetServerName)) {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.invalid-server.not-located").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%server%", arguments[1]))));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.pitch"));
                return;
            }
            if (targetServerName.equals("here")) {
                if (Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getUniqueId()).collect(Collectors.toList()).contains(offlineTarget.getUniqueId())) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.invalid-send").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%target%", offlineTarget.getName()).replace("%server%", "here"))));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.pitch"));
                    return;
                }
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()).replace("%server%", "here"))));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.player.pitch"));
                final NetworkServer targetsCurrentNetworkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getOnlinePlayerUUIDs().contains(offlineTarget.getUniqueId())).findFirst().get();
                final TransmissionServer targetsCurrentTransmissionServer = transmissionSite.getServers().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals(targetsCurrentNetworkServer.getName())).findFirst().get();
                final JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("player", player.getUniqueId().toString());
                jsonObject.addProperty("server", targetServerName);
                final Transmission sendTransmission = new Transmission(transmissionSite, targetsCurrentTransmissionServer, "basics", TransmissionType.MESSAGE, UUID.randomUUID(), "player_send=" + new Gson().toJson(jsonObject));
                sendTransmission.send();
                CompletableFuture.runAsync(() -> {
                    while (!Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getUniqueId()).collect(Collectors.toList()).contains(offlineTarget.getUniqueId())) {
                        //It won't send the message until the player is registered as connected to the server.
                    }
                    final Player target = offlineTarget.getPlayer();
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.success.target").forEach(string -> target.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()).replace("%server%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.servers.internal.prettified-name")))));
                    target.playSound(target.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.target.pitch"));
                });
            } else {
                final NetworkServer networkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getName().equals(targetServerName)).findFirst().get();
                if (networkServer.getStatus() != NetworkServerStatus.ONLINE) {
                    String preStatus = null;
                    switch (networkServer.getStatus()) {
                        case STARTING -> preStatus = "&bstarting";
                        case STOPPING -> preStatus = "&3stopping";
                        case OFFLINE -> preStatus = "&coffline";
                    }
                    final String status = preStatus;
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.invalid-server.offline").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%status%", status).replace("%server%", networkServer.getPrettifiedName()))));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.pitch"));
                    return;
                }
                if (networkServer.getOnlinePlayerUUIDs().contains(offlineTarget.getUniqueId())) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.invalid-send").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%target%", offlineTarget.getName()).replace("%server%", networkServer.getPrettifiedName()))));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.pitch"));
                    return;
                }
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()).replace("%server%", networkServer.getPrettifiedName()))));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.player.pitch"));
                final TransmissionServer transmissionServer = transmissionSite.getServers().stream().filter(innerTransmissionServer -> innerTransmissionServer.getName().equals(networkServer.getName())).findFirst().get();
                if (Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getUniqueId()).collect(Collectors.toList()).contains(offlineTarget.getUniqueId())) {
                    transmissionSite.send(offlineTarget.getPlayer(), transmissionServer);
                } else {
                    final NetworkServer targetsCurrentNetworkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getOnlinePlayerUUIDs().contains(offlineTarget.getUniqueId())).findFirst().get();
                    final TransmissionServer targetsCurrentTransmissionServer = transmissionSite.getServers().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals(targetsCurrentNetworkServer.getName())).findFirst().get();
                    final JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("player", player.getUniqueId().toString());
                    jsonObject.addProperty("server", targetServerName);
                    final Transmission sendTransmission = new Transmission(transmissionSite, targetsCurrentTransmissionServer, "basics", TransmissionType.MESSAGE, UUID.randomUUID(), "player_send=" + offlineTarget.getUniqueId() + "~" + new Gson().toJson(jsonObject));
                    sendTransmission.send();
                }
                final JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("player", offlineTarget.getUniqueId().toString());
                jsonObject.addProperty("sender", player.getUniqueId().toString());
                jsonObject.addProperty("reason", "server_stop");
                final Transmission sentTransmission = new Transmission(transmissionSite, transmissionServer, "basics", TransmissionType.MESSAGE, UUID.randomUUID(), "player_sent=" + new Gson().toJson(jsonObject));
                sentTransmission.send();
            }
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
        if (!(sender instanceof Player)) {
            return new ArrayList<String>();
        }
        final Player player = (Player) sender;
        if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.commands.send")) {
            return new ArrayList<String>();
        }
        ArrayList<String> tabCompletion = new ArrayList<String>();
        switch (arguments.length) {
            case 1 -> {
                final ArrayList<UUID> playerUUIDs = new ArrayList<UUID>(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getUniqueId()).collect(Collectors.toList()));
                this.plugin.getComponentManager().getNetworkComponent().getServers().forEach(networkServer -> playerUUIDs.addAll(networkServer.getOnlinePlayerUUIDs()));
                playerUUIDs.remove(player.getUniqueId());
                tabCompletion.addAll(playerUUIDs.stream().map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).collect(Collectors.toList()));
            }
            case 2 -> {
                final ArrayList<String> serverNames = new ArrayList<String>(this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(networkServer -> networkServer.getStatus() == NetworkServerStatus.ONLINE).map(networkServer -> networkServer.getName()).toList());
                serverNames.add("here");
                tabCompletion.addAll(serverNames);
            }
        }
        return tabCompletion;
    }

}
