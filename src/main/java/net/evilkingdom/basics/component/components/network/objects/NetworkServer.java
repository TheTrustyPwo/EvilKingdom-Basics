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
import net.evilkingdom.commons.utilities.mojang.MojangUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.minecraft.server.packs.repository.Pack;
import org.bukkit.Bukkit;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NetworkServer {

    private final Basics plugin;

    private final String name, ip;
    private final int port;
    private boolean online;
    private int playerCount;

    public NetworkServer(final String name, final String ip, final int port) {
        this.plugin = Basics.getPlugin();

        this.name = name;
        this.ip = ip;
        this.port = port;
        this.online = false;
        this.playerCount = -1;
    }

    /**
     * Allows you to retrieve the server's player count.
     *
     * @return The server's player count.
     */
    public int getPlayerCount() {
        return this.playerCount;
    }

    /**
     * Allows you to retrieve the if the server is online.
     *
     * @return If the server is online.
     */
    public boolean isOnline() {
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
     * Allows you to retrieve the server's ip.
     *
     * @return The server's ip.
     */
    public String getIP() {
        return this.ip;
    }

    /**
     * Allows you to retrieve the server's port.
     *
     * @return The server's port.
     */
    public Integer getPort() {
        return this.port;
    }

    /**
     * Allows you to update the server's data.
     */
    public void updateData() {
        MojangUtilities.isOnline(this.ip, this.port).whenComplete((online, onlineThrowable) -> {
            if (online != this.online) {
                if (online) {
                    this.online = true;
                    Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.server-status.messages.online").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%server%", this.name)))));
                } else {
                    this.online = false;
                    this.playerCount = -1;
                    Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.server-status.messages.offline").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%server%", this.name)))));
                }
            }
            if (this.online) {
                final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
                final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
                final Transmission transmission = new Transmission(transmissionSite, TransmissionType.REQUEST, this.name, "basics", UUID.randomUUID(), "request=online_player_count");
                transmission.send().whenComplete((onlinePlayerCount, onlinePlayerCountThrowable) -> {
                    if (onlinePlayerCount.equals("response=request_failed")) {
                        this.playerCount = 0;
                    } else {
                        this.playerCount = Integer.parseInt(onlinePlayerCount.replace("response=", ""));
                    }
                });
            }
        });
    }

}
