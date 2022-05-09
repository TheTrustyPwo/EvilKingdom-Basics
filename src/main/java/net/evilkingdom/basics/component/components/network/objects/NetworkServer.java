package net.evilkingdom.basics.component.components.network.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
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
import org.bukkit.OfflinePlayer;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NetworkServer {

    private final Basics plugin;

    private final String name, ip;
    private final int port;
    private boolean online, lastRequestFailed;
    private final ArrayList<UUID> onlinePlayerUUIDs;

    public NetworkServer(final String name, final String ip, final int port) {
        this.plugin = Basics.getPlugin();

        this.name = name;
        this.ip = ip;
        this.port = port;
        this.online = false;
        this.lastRequestFailed = true;
        this.onlinePlayerUUIDs = new ArrayList<UUID>();
    }

    /**
     * Allows you to retrieve the server's online player uuids.
     *
     * @return The server's online player uuids.
     */
    public ArrayList<UUID> getOnlinePlayerUUIDs() {
        return this.onlinePlayerUUIDs;
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
                    this.onlinePlayerUUIDs.clear();
                    Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.server-status.messages.offline").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%server%", this.name)))));
                }
            }
            if (this.online) {
                final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
                final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
                final Transmission transmission = new Transmission(transmissionSite, TransmissionType.REQUEST, this.name, "basics", UUID.randomUUID(), "request=online_players");
                transmission.send().whenComplete((onlinePlayers, onlinePlayerCountThrowable) -> {
                    final ArrayList<UUID> previousOnlinePlayerUUIDs = new ArrayList<UUID>(this.onlinePlayerUUIDs);
                    this.onlinePlayerUUIDs.clear();
                    this.lastRequestFailed = onlinePlayers.equals("response=request_failed");
                    if (!onlinePlayers.equals("response=request_failed")) {
                        final JsonArray jsonArray = JsonParser.parseString(onlinePlayers.replaceFirst("response=", "")).getAsJsonArray();
                        jsonArray.forEach(jsonElement -> this.onlinePlayerUUIDs.add(UUID.fromString(jsonElement.getAsString())));
                    }
                    if (!this.lastRequestFailed) {
                        previousOnlinePlayerUUIDs.stream().filter(uuid -> !this.onlinePlayerUUIDs.contains(uuid)).map(uuid -> Bukkit.getOfflinePlayer(uuid)).forEach(offlinePlayer -> Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.connection.messages.quit.external").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", offlinePlayer.getName()).replace("%server%", this.name))))));
                        this.onlinePlayerUUIDs.stream().filter(uuid -> !previousOnlinePlayerUUIDs.contains(uuid)).map(uuid -> Bukkit.getOfflinePlayer(uuid)).forEach(offlinePlayer -> Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.connection.messages.join.external").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", offlinePlayer.getName()).replace("%server%", this.name))))));
                    }
                });
            }
        });
    }

}
