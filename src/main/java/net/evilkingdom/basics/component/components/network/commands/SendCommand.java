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
        if (arguments.length != 2) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.pitch"));
            return;
        }
        final Optional<? extends Player> optionalTarget = Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> onlinePlayer.getName().equalsIgnoreCase(arguments[0])).findFirst();
        if (optionalTarget.isEmpty()) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[0]))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.error.pitch"));
            return;
        }
        final Player target = optionalTarget.get();
        final Optional<NetworkServer> optionalNetworkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(networkServer -> networkServer.getName().equals(arguments[1])).findFirst();
        if (optionalNetworkServer.isEmpty()) {
            return;
        }
        final NetworkServer networkServer = optionalNetworkServer.get();
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", target.getName()).replace("%server%", networkServer.getPrettifiedName()))));
        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.player.pitch"));
        final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
        transmissionImplementor.send(target, networkServer.getName());
        final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
        CompletableFuture.runAsync(() -> {
            while (!networkServer.getOnlinePlayerUUIDs().contains(target.getUniqueId())) {
                //It won't send the message until the player is registered as connected to the server.
            }
            final JsonArray jsonArray = new JsonArray();
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.success.target").forEach(string -> jsonArray.add(string.replace("%player%", player.getName()).replace("%server%", networkServer.getPrettifiedName())));
            final TransmissionServer transmissionServer = transmissionSite.getServers().stream().filter(innerTransmissionServer -> innerTransmissionServer.getName().equals(networkServer.getName())).findFirst().get();
            final Transmission messageTransmission = new Transmission(transmissionSite, transmissionServer,"basics", TransmissionType.MESSAGE, UUID.randomUUID(), "player_message=" + target.getUniqueId() + "~" + new Gson().toJson(jsonArray));
            final Transmission soundTransmission = new Transmission(transmissionSite, transmissionServer,"basics", TransmissionType.MESSAGE, UUID.randomUUID(), "player_sound=" + target.getUniqueId() + "~" + this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.success.target.sound") + ":" + this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.target.volume") + ":" + this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.target.pitch"));
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
        switch (arguments.length) {
            case 1 -> {
                final ArrayList<String> playerNames = new ArrayList<String>(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
                playerNames.remove(player.getName());
                tabCompletion.addAll(playerNames);
            }
            case 2 -> this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(networkServer -> networkServer.getStatus() == NetworkServerStatus.ONLINE).forEach(networkServer -> tabCompletion.add(networkServer.getName()));
        }
        return tabCompletion;
    }

}
