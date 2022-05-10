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

public class FindCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public FindCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "find", new ArrayList<String>(List.of("whereis", "locate")), this);
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
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.find.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            return;
        }
        final Player player = (Player) sender;
        if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.commands.find")) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.find.messages.invalid-permissions").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.find.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.find.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.find.sounds.error.pitch"));
            return;
        }
        if (arguments.length != 1) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.find.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.find.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.find.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.find.sounds.error.pitch"));
            return;
        }
        MojangUtilities.getUUID(arguments[0]).whenComplete((optionalTargetUUID, uuidThrowable) -> {
            if (optionalTargetUUID.isEmpty()) {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.find.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[0]))));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.find.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.find.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.find.sounds.error.pitch"));
                return;
            }
            final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(optionalTargetUUID.get());
            final ArrayList<UUID> playerUUIDs = new ArrayList<UUID>(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getUniqueId()).collect(Collectors.toList()));
            this.plugin.getComponentManager().getNetworkComponent().getServers().forEach(networkServer -> playerUUIDs.addAll(networkServer.getOnlinePlayerUUIDs()));
            if (!playerUUIDs.contains(offlineTarget.getUniqueId())) {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.find.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.find.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.find.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.find.sounds.error.pitch"));
                return;
            }
            String preServer = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.servers.internal.prettified-name");
            if (!Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getUniqueId()).toList().contains(offlineTarget.getUniqueId())) {
                final NetworkServer targetsCurrentNetworkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getOnlinePlayerUUIDs().contains(offlineTarget.getUniqueId())).findFirst().get();
                preServer = targetsCurrentNetworkServer.getPrettifiedName();
            }
            final String server = preServer;
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.find.messages.success").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%server%", server).replace("%player%", offlineTarget.getName()))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.find.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.find.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.find.sounds.success.pitch"));
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
                final ArrayList<UUID> playerUUIDs = new ArrayList<UUID>(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getUniqueId()).collect(Collectors.toList()));
                this.plugin.getComponentManager().getNetworkComponent().getServers().forEach(networkServer -> playerUUIDs.addAll(networkServer.getOnlinePlayerUUIDs()));
                playerUUIDs.remove(player.getUniqueId());
                tabCompletion.addAll(playerUUIDs.stream().map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).collect(Collectors.toList()));
            }
        }
        return tabCompletion;
    }

}
