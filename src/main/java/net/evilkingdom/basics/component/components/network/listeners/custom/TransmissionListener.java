package net.evilkingdom.basics.component.components.network.listeners.custom;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.JsonArray;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.commons.transmission.TransmissionImplementor;
import net.evilkingdom.commons.transmission.abstracts.TransmissionHandler;
import net.evilkingdom.commons.transmission.enums.TransmissionType;
import net.evilkingdom.commons.transmission.objects.Transmission;
import net.evilkingdom.commons.transmission.objects.TransmissionSite;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class TransmissionListener extends TransmissionHandler {

    private final Basics plugin;

    /**
     * Allows you to create the listener.
     */
    public TransmissionListener() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * The receiving of a transmission.
     *
     * @param serverName ~ The transmission's server name.
     * @param siteName ~ The transmission's site name.
     * @param type ~ The transmission's transmission type.
     * @param uuid ~ The transmission's uuid.
     * @param data ~ The transmission's data.
     */
    public void onReceive(final String serverName, final String siteName, final TransmissionType type, final UUID uuid, final String data) {
        switch (type) {
            case REQUEST -> {
                final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
                final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
                switch (data) {
                    case "request=online_players" -> {
                        final JsonArray jsonArray = new JsonArray();
                        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> jsonArray.add(onlinePlayer.getUniqueId().toString()));
                        final Transmission transmission = new Transmission(transmissionSite, TransmissionType.RESPONSE, serverName, siteName, uuid, "response=" + jsonArray);
                        transmission.send();
                    }
                    case "request=online_staff" -> {
                        final JsonArray jsonArray = new JsonArray();
                        Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> jsonArray.add(onlinePlayer.getUniqueId().toString()));
                        final Transmission transmission = new Transmission(transmissionSite, TransmissionType.RESPONSE, serverName, siteName, uuid, "response=" + jsonArray);
                        transmission.send();
                    }
                    case "request=online_player_count" -> {
                        final Transmission transmission = new Transmission(transmissionSite, TransmissionType.RESPONSE, serverName, siteName, uuid, "response=" + Bukkit.getOnlinePlayers().size());
                        transmission.send();
                    }
                    case "request=online_staff_count" -> {
                        final Transmission transmission = new Transmission(transmissionSite, TransmissionType.RESPONSE, serverName, siteName, uuid, "response=" + Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).toList().size());
                        transmission.send();
                    }
                    case "request=ip_address" -> {
                        final Transmission transmission = new Transmission(transmissionSite, TransmissionType.RESPONSE, serverName, siteName, uuid, "response=" + Bukkit.getIp() + ":" + Bukkit.getPort());
                        transmission.send();
                    }
                }
            }
            case MESSAGE -> {
                final String dataType = data.split("=")[0];
                switch (dataType) {
                    case "staff_join" -> {
                        final UUID staffUUID = UUID.fromString(data.split("=")[1]);
                        final OfflinePlayer offlineStaff = Bukkit.getOfflinePlayer(staffUUID);
                        Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.connection.messages.join.external").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineStaff.getName()).replace("%server%", serverName)))));
                    }
                    case "staff_quit" -> {
                        final UUID staffUUID = UUID.fromString(data.split("=")[1]);
                        final OfflinePlayer offlineStaff = Bukkit.getOfflinePlayer(staffUUID);
                        Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.connection.messages.quit.external").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineStaff.getName()).replace("%server%", serverName)))));
                    }
                }
            }
        }
    }

}
