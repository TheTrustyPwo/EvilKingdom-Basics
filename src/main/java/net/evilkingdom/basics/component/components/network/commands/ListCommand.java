package net.evilkingdom.basics.component.components.network.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.network.objects.NetworkServer;
import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.transmission.TransmissionImplementor;
import net.evilkingdom.commons.transmission.objects.TransmissionSite;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ListCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public ListCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "list", new ArrayList<String>(List.of("glist", "globallist", "players", "onlineplayers", "playercount", "onlineplayercount", "online")), this);
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
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.list.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            return;
        }
        final Player player = (Player) sender;
        if (arguments.length != 0) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.list.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.list.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.list.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.list.sounds.error.pitch"));
            return;
        }
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.list.messages.success").forEach(string -> {
            final Matcher statusMatcher = Pattern.compile("%server_([a-zA-Z0-9]*)_status%").matcher(string);
            while (statusMatcher.find()) {
                final String serverName = statusMatcher.group().replaceFirst("%server_", "").replaceFirst("_status%", "");
                String status;
                if (serverName.equals(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.servers.internal.name"))) {
                    status = "&aOnline";
                } else {
                    final NetworkServer networkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getName().equals(serverName)).findFirst().get();
                    if (networkServer.isOnline()) {
                        status = "&aOnline";
                    } else {
                        status = "&cOffline";
                    }
                }
                string.replace(statusMatcher.group(), status);
            }
            final Matcher playerCountMatcher = Pattern.compile("%server_([a-zA-Z0-9]*)_player_count%").matcher(string);
            while (playerCountMatcher.find()) {
                final String serverName = playerCountMatcher.group().replaceFirst("%server_", "").replaceFirst("_status%", "");
                int playerCount;
                if (serverName.equals(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.servers.internal.name"))) {
                    playerCount = Bukkit.getOnlinePlayers().size();
                } else {
                    final NetworkServer networkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getName().equals(serverName)).findFirst().get();
                    playerCount = networkServer.getOnlinePlayerUUIDs().size();
                }
                string.replace(playerCountMatcher.group(), String.valueOf(playerCount));
            }
            final Matcher globalPlayerCountMatcher = Pattern.compile("%global_player_count%").matcher(string);
            while (globalPlayerCountMatcher.find()) {
                final ArrayList<UUID> playerUUIDs = new ArrayList<UUID>(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getUniqueId()).collect(Collectors.toList()));
                this.plugin.getComponentManager().getNetworkComponent().getServers().forEach(networkServer -> playerUUIDs.addAll(networkServer.getOnlinePlayerUUIDs()));
                string.replace(globalPlayerCountMatcher.group(), String.valueOf(playerUUIDs.size()));
            }
            player.sendMessage(StringUtilities.colorize(string));
        });
        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.list.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.list.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.list.sounds.success.pitch"));
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
        return tabCompletion;
    }

}
