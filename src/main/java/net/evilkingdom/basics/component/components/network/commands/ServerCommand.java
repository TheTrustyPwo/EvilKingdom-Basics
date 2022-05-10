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

public class ServerCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public ServerCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "server", new ArrayList<String>(List.of("join")), this);
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
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.server.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            return;
        }
        final Player player = (Player) sender;
        if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.commands.server")) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.server.messages.invalid-permissions").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.server.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.error.pitch"));
            return;
        }
        if (arguments.length != 1) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.server.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.server.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.error.pitch"));
            return;
        }
        final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
        final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
        final String targetServerName = arguments[0].toLowerCase();
        if (!targetServerName.equals(transmissionSite.getServerName()) && !this.plugin.getComponentManager().getNetworkComponent().getServers().stream().map(innerNetworkServer -> innerNetworkServer.getName()).toList().contains(targetServerName)) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.server.messages.invalid-server.not-located").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%server%", arguments[1]))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.server.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.error.pitch"));
            return;
        }
        if (targetServerName.equals(transmissionSite.getServerName())) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.server.messages.invalid-server.already-on").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%server%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.servers.internal.prettified-name")))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.server.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.error.pitch"));
            return;
        }
        final NetworkServer networkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getName().equals(targetServerName)).findFirst().get();
        if (networkServer.getStatus() != NetworkServerStatus.ONLINE) {
            String preStatus = null;
            switch (networkServer.getStatus()) {
                case STARTING -> preStatus = "&bstarting";
                case OFFLINE -> preStatus = "&coffline";
            }
            final String status = preStatus;
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.server.messages.invalid-server.offline").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%status%", status).replace("%server%", networkServer.getPrettifiedName()))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.server.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.error.pitch"));
            return;
        }
        transmissionImplementor.send(player, targetServerName);
        CompletableFuture.runAsync(() -> {
            while (!networkServer.getOnlinePlayerUUIDs().contains(player.getUniqueId())) {
                //It won't send the message until the player is registered as connected to the server.
            }
            final JsonArray jsonArray = new JsonArray();
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.server.messages.success").forEach(string -> jsonArray.add(string.replace("%player%", player.getName()).replace("%server%", networkServer.getPrettifiedName())));
            final TransmissionServer transmissionServer = transmissionSite.getServers().stream().filter(innerTransmissionServer -> innerTransmissionServer.getName().equals(networkServer.getName())).findFirst().get();
            final Transmission messageTransmission = new Transmission(transmissionSite, transmissionServer, "basics", TransmissionType.MESSAGE, UUID.randomUUID(), "player_message=" + player.getUniqueId() + "~" + new Gson().toJson(jsonArray));
            final Transmission soundTransmission = new Transmission(transmissionSite, transmissionServer, "basics", TransmissionType.MESSAGE, UUID.randomUUID(), "player_sound=" + player.getUniqueId() + "~" + this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.server.sounds.success.sound") + ":" + this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.success.volume") + ":" + this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.success.pitch"));
            messageTransmission.send();
            soundTransmission.send();
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
        ArrayList<String> tabCompletion = new ArrayList<String>();
        if (LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.commands.server")) {
            switch (arguments.length) {
                case 1 -> {
                    final ArrayList<String> serverNames = new ArrayList<String>(this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(networkServer -> networkServer.getStatus() == NetworkServerStatus.ONLINE).map(networkServer -> networkServer.getName()).toList());
                    tabCompletion.addAll(serverNames);
                }
            }
        }
        return tabCompletion;
    }

}
