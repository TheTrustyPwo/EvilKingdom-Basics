package net.evilkingdom.basics.component.components.network.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.commons.transmission.TransmissionImplementor;
import net.evilkingdom.commons.transmission.enums.TransmissionType;
import net.evilkingdom.commons.transmission.objects.Transmission;
import net.evilkingdom.commons.transmission.objects.TransmissionSite;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NetworkServer {

    private final Basics plugin;

    private final String name;
    private boolean online, starting;
    private int playerCount;

    public NetworkServer(final String name) {
        this.plugin = Basics.getPlugin();

        this.name = name;
        this.updateData();
    }

    /**
     * Allows you to retrieve the server's player count.
     *
     * @return The server's player count.
     */
    private int getPlayerCount() {
        return this.playerCount;
    }

    /**
     * Allows you to retrieve the if the server is starting.
     *
     * @return If the server is starting.
     */
    private boolean isStarting() {
        return this.starting;
    }

    /**
     * Allows you to retrieve the if the server is online.
     *
     * @return If the server is online.
     */
    private boolean isOnline() {
        return this.online;
    }

    /**
     * Allows you to retrieve the server's name.
     *
     * @return The server's name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Allows you to update the server's data.
     */
    public void updateData() {
        CompletableFuture.supplyAsync(() -> {
            final String ip = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.transmissions.servers.external." + this.name + ".ip");
            final int port = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.network.transmissions.servers.external." + this.name + ".port");
            try {
                final Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 15);
                socket.close();
                return true;
            } catch (final Exception exception) {
                return false;
            }
        }).whenComplete((online, onlineThrowable) -> {
            if (online != this.online) {
                if (online) {
                    this.starting = true;
                    Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                        if (!this.starting) {
                            return;
                        }
                        this.online = true;
                        Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.server-status.messages.online").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%server%", this.name)))));
                    }, 160L);
                } else {
                    this.online = false;
                    this.starting = false;
                    Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.server-status.messages.offline").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%server%", this.name)))));
                }
            }
        });
        if (this.online) {
            final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
            final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
            final Transmission playerCountTransmission = new Transmission(transmissionSite, TransmissionType.REQUEST, this.name, "basics", UUID.randomUUID(),"request=online_player_count");
            playerCountTransmission.send().whenComplete((playerCount, playerCountThrowable) -> this.playerCount = Integer.parseInt(playerCount));
        }
    }

}
