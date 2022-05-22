package net.evilkingdom.basics.component.components.network.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.Gson;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AnnounceCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public AnnounceCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "announce", new ArrayList<String>(List.of("alert", "announcement", "bc", "broadcast")), this);
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
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.announce.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            return;
        }
        final Player player = (Player) sender;
        if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.commands.announce")) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.announce.messages.invalid-permissions").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.announce.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.error.pitch"));
            return;
        }
        if (arguments.length < 2) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.announce.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.announce.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.error.pitch"));
            return;
        }
        final StringBuilder messageStringBuilder = new StringBuilder();
        for (int i = 2; i < arguments.length; i++) {
            if (i == (arguments.length - 1)) {
                messageStringBuilder.append(arguments[i]);
            } else {
                messageStringBuilder.append(arguments[i]).append(" ");
            }
        }
        final String message = messageStringBuilder.toString();
        final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
        final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
        final String targetServerName = arguments[0].toLowerCase();
        if (!targetServerName.equals("network") && !targetServerName.equals("here") && !this.plugin.getComponentManager().getNetworkComponent().getServers().stream().map(innerNetworkServer -> innerNetworkServer.getName()).toList().contains(targetServerName)) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.announce.messages.invalid-server.not-located").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%server%", arguments[1]))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.announce.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.error.pitch"));
            return;
        }
        if (targetServerName.equals("network")) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.announce.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%message%", message).replace("%server%", "here"))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.announce.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.player.pitch"));
            Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> onlinePlayer.getUniqueId() != player.getUniqueId()).forEach(onlinePlayer -> {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.announce.messages.success.online-players").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()).replace("%message%", message))));
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.announce.sounds.success.online-players.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.online-players.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.online-players.pitch"));
            });
            this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(networkServer -> networkServer.getStatus() == NetworkServerStatus.ONLINE).forEach(networkServer -> {
                final TransmissionServer transmissionServer = transmissionSite.getServers().stream().filter(innerTransmissionServer -> innerTransmissionServer.getName().equals(networkServer.getName())).findFirst().get();
                final JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("player", player.getUniqueId().toString());
                jsonObject.addProperty("sender", player.getUniqueId().toString());
                jsonObject.addProperty("reason", "server_stop");
                final Transmission announceTransmission = new Transmission(transmissionSite, transmissionServer, "basics", TransmissionType.MESSAGE, UUID.randomUUID(), "announce=" + new Gson().toJson(jsonObject));
                announceTransmission.send();
            });
        } else if (targetServerName.equals("here")) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.announce.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%message%", message).replace("%server%", "here"))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.announce.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.player.pitch"));
            Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> onlinePlayer.getUniqueId() != player.getUniqueId()).forEach(onlinePlayer -> {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.announce.messages.success.online-players").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()).replace("%message%", message))));
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.announce.sounds.success.online-players.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.online-players.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.online-players.pitch"));
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
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.announce.messages.invalid-server.offline").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%status%", status).replace("%server%", networkServer.getPrettifiedName()))));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.announce.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.error.pitch"));
                return;
            }
            final TransmissionServer transmissionServer = transmissionSite.getServers().stream().filter(innerTransmissionServer -> innerTransmissionServer.getName().equals(networkServer.getName())).findFirst().get();
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.announce.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%message%", message).replace("%server%", networkServer.getPrettifiedName()))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.announce.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.player.pitch"));
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("player", player.getUniqueId().toString());
            jsonObject.addProperty("sender", player.getUniqueId().toString());
            jsonObject.addProperty("reason", "server_stop");
            final Transmission announceTransmission = new Transmission(transmissionSite, transmissionServer, "basics", TransmissionType.MESSAGE, UUID.randomUUID(), "announce=" + new Gson().toJson(jsonObject));
            announceTransmission.send();
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
        if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.commands.announce")) {
            return new ArrayList<String>();
        }
        ArrayList<String> tabCompletion = new ArrayList<String>();
        switch (arguments.length) {
            case 1 -> {
                final ArrayList<String> serverNames = new ArrayList<String>(this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(networkServer -> networkServer.getStatus() == NetworkServerStatus.ONLINE).map(networkServer -> networkServer.getName()).toList());
                serverNames.add("network");
                serverNames.add("here");
                tabCompletion.addAll(serverNames);
            }
        }
        return tabCompletion;
    }

}
