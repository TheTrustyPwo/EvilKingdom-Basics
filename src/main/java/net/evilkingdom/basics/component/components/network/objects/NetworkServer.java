package net.evilkingdom.basics.component.components.network.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.network.enums.NetworkServerStatus;
import net.evilkingdom.commons.transmission.TransmissionImplementor;
import net.evilkingdom.commons.transmission.enums.TransmissionType;
import net.evilkingdom.commons.transmission.objects.Transmission;
import net.evilkingdom.commons.transmission.objects.TransmissionServer;
import net.evilkingdom.commons.transmission.objects.TransmissionSite;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.mojang.MojangUtilities;
import net.evilkingdom.commons.utilities.pterodactyl.PterodactylUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.minecraft.server.packs.repository.Pack;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NetworkServer {

    private final Basics plugin;

    private final String name, prettifiedName, pterodactylId;
    private NetworkServerStatus status;
    private final ArrayList<UUID> onlinePlayerUUIDs;

    /**
     * Allows you to create a Network Server.
     *
     * @param name ~ The network server's name.
     * @param prettifiedName ~ The network server's prettified name.
     * @param pterodactylId ~ The network server's pterodactyl server id.
     */
    public NetworkServer(final String name, final String prettifiedName, final String pterodactylId) {
        this.plugin = Basics.getPlugin();

        this.name = name;
        this.prettifiedName = prettifiedName;
        this.pterodactylId = pterodactylId;
        this.status = NetworkServerStatus.OFFLINE;
        this.onlinePlayerUUIDs = new ArrayList<UUID>();
    }

    /**
     * Allows you to retrieve the network server's online player uuids.
     *
     * @return The network server's online player uuids.
     */
    public ArrayList<UUID> getOnlinePlayerUUIDs() {
        return this.onlinePlayerUUIDs;
    }

    /**
     * Allows you to retrieve the network server's status.
     *
     * @return The network server's status.
     */
    public NetworkServerStatus getStatus() {
        return this.status;
    }

    /**
     * Allows you to retrieve the network server's name.
     *
     * @return The network server's name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Allows you to retrieve the network server's prettified name.
     *
     * @return The network server's prettified name.
     */
    public String getPrettifiedName() {
        return this.prettifiedName;
    }

    /**
     * Allows you to retrieve the network server's pterodactyl server id.
     *
     * @return The network server's pterodactyl server id.
     */
    public String getPterodactylId() {
        return this.pterodactylId;
    }

    /**
     * Allows you to update the server's data.
     */
    public void updateData() {
        final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
        final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
        PterodactylUtilities.getStatus(transmissionSite.getPterodactylURL(), transmissionSite.getPterodactylToken(), this.pterodactylId).whenComplete((pterodactylStatus, pterodactylStatusThrowable) -> {
            NetworkServerStatus status = null;
            switch (pterodactylStatus.get()) {
                case "starting" -> status = NetworkServerStatus.STARTING;
                case "running" -> status = NetworkServerStatus.ONLINE;
                case "offline" -> status = NetworkServerStatus.OFFLINE;
            }
            if (status != this.status) {
                this.status = status;
                String preFormattedStatus = null;
                switch (status) {
                    case ONLINE -> preFormattedStatus = "&aonline";
                    case STARTING -> preFormattedStatus = "&6starting";
                    case OFFLINE -> preFormattedStatus = "&coffline";
                }
                final String formattedStatus = preFormattedStatus;
                Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.server-status.message").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%server%", this.prettifiedName).replace("%status%", formattedStatus))));
                    onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.staff.status-change.sound.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.staff.status-change.sound.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.staff.status-change.sound.pitch"));
                });
            }
            if (this.status == NetworkServerStatus.ONLINE) {
                final TransmissionServer transmissionServer = transmissionSite.getServers().stream().filter(innerTransmissionServer -> innerTransmissionServer.getName().equals(this.name)).findFirst().get();
                final Transmission transmission = new Transmission(transmissionSite, transmissionServer, "basics", TransmissionType.REQUEST, UUID.randomUUID(), "request=online_players");
                transmission.send().whenComplete((onlinePlayers, onlinePlayerCountThrowable) -> {
                    final ArrayList<UUID> previousOnlinePlayerUUIDs = new ArrayList<UUID>(this.onlinePlayerUUIDs);
                    this.onlinePlayerUUIDs.clear();
                    final JsonArray jsonArray = JsonParser.parseString(onlinePlayers.replaceFirst("response=", "")).getAsJsonArray();
                    jsonArray.forEach(jsonElement -> this.onlinePlayerUUIDs.add(UUID.fromString(jsonElement.getAsString())));
                    previousOnlinePlayerUUIDs.stream().filter(uuid -> !this.onlinePlayerUUIDs.contains(uuid)).map(uuid -> Bukkit.getOfflinePlayer(uuid)).forEach(offlinePlayer -> Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.connection.messages.quit.external").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", offlinePlayer.getName()).replace("%server%", this.prettifiedName))));
                        onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.staff.connection.sounds.join.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.staff.connection.sounds.join.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.staff.connection.sounds.join.pitch"));
                    }));
                    this.onlinePlayerUUIDs.stream().filter(uuid -> !previousOnlinePlayerUUIDs.contains(uuid)).map(uuid -> Bukkit.getOfflinePlayer(uuid)).forEach(offlinePlayer -> Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.connection.messages.join.external").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", offlinePlayer.getName()).replace("%server%", this.prettifiedName))));
                        onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.staff.connection.sounds.quit.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.staff.connection.sounds.quit.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.staff.connection.sounds.quit.pitch"));
                    }));
                });
            }
        });
    }

}
