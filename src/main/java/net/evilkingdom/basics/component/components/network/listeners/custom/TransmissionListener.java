package net.evilkingdom.basics.component.components.network.listeners.custom;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import io.papermc.paper.text.PaperComponents;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.network.objects.NetworkServer;
import net.evilkingdom.commons.transmission.TransmissionImplementor;
import net.evilkingdom.commons.transmission.abstracts.TransmissionHandler;
import net.evilkingdom.commons.transmission.enums.TransmissionType;
import net.evilkingdom.commons.transmission.objects.Transmission;
import net.evilkingdom.commons.transmission.objects.TransmissionServer;
import net.evilkingdom.commons.transmission.objects.TransmissionSite;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
     * @param server ~ The transmission's transmission server.
     * @param siteName ~ The transmission's transmission site's name.
     * @param type ~ The transmission's transmission type.
     * @param uuid ~ The transmission's uuid.
     * @param data ~ The transmission's data.
     */
    public void onReceive(final TransmissionServer server, final String siteName, final TransmissionType type, final UUID uuid, final String data) {
        final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
        final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
        switch (type) {
            case REQUEST -> {
                switch (data.replaceFirst("request=", "")) {
                    case "online_players" -> {
                        final JsonArray jsonArray = new JsonArray();
                        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> jsonArray.add(onlinePlayer.getUniqueId().toString()));
                        final Transmission transmission = new Transmission(transmissionSite, server, siteName, TransmissionType.RESPONSE, uuid, "response=" + new Gson().toJson(jsonArray));
                        transmission.send();
                    }
                    case "online_staff" -> {
                        final JsonArray jsonArray = new JsonArray();
                        Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> jsonArray.add(onlinePlayer.getUniqueId().toString()));
                        final Transmission transmission = new Transmission(transmissionSite, server, siteName, TransmissionType.RESPONSE, uuid, "response=" + new Gson().toJson(jsonArray));
                        transmission.send();
                    }
                }
            }
            case MESSAGE -> {
                switch (data.split("=")[0]) {
                    case "player_message" -> {
                        final Player player = Bukkit.getPlayer(UUID.fromString(data.split("=")[1].split("~")[0]));
                        final JsonArray jsonArray = JsonParser.parseString(data.split("=")[1].split("~")[1]).getAsJsonArray();
                        jsonArray.forEach(jsonElement -> player.sendMessage(StringUtilities.colorize(jsonElement.getAsString())));
                    }
                    case "player_sound" -> {
                        final Player player = Bukkit.getPlayer(UUID.fromString(data.split("=")[1].split("~")[0]));
                        final Sound sound = Sound.valueOf(data.split("=")[1].split("~")[1].split(":")[0]);
                        final float volume = Float.parseFloat(data.split("=")[1].split("~")[1].split(":")[1]);
                        final float pitch = Float.parseFloat(data.split("=")[1].split("~")[1].split(":")[2]);
                        player.playSound(player.getLocation(), sound, volume, pitch);
                    }
                    case "player_send" -> {
                        final Player player = Bukkit.getPlayer(UUID.fromString(data.split("=")[1].split("~")[0]));
                        final String sendServer = data.split("=")[1].split("~")[1].split(":")[0];
                        final TransmissionServer transmissionServer = transmissionSite.getServers().stream().filter(internalTransmissionServer -> internalTransmissionServer.getName().equals(sendServer)).findFirst().get();
                        transmissionSite.send(player, transmissionServer);
                    }
                    case "server_shutdown" -> {
                        final ArrayList<OfflinePlayer> offlinePlayers = new ArrayList<OfflinePlayer>();
                        final JsonArray jsonArray = JsonParser.parseString(data.split("=")[1]).getAsJsonArray();
                        jsonArray.forEach(jsonElement -> offlinePlayers.add(Bukkit.getOfflinePlayer(UUID.fromString(jsonElement.getAsString()))));
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> offlinePlayers.stream().filter(offlinePlayer -> offlinePlayer.isOnline()).forEach(offlinePlayer -> {
                            final Player onlinePlayer = offlinePlayer.getPlayer();
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.shutdowns.kick.server.message").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string)));
                            onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.shutdowns.kick.server.sound.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.shutdowns.kick.server.sound.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.shutdowns.kick.server.sound.pitch"));
                        }), 60L);
                    }
                    case "staff_chat" -> {
                        final NetworkServer networkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getName().equals(server.getName())).findFirst().get();
                        final Player player = Bukkit.getPlayer(UUID.fromString(data.split("=")[1].split("~")[0]));
                        final String message = data.split("=")[1].split("~")[1];
                        final String playerRank = WordUtils.capitalizeFully(LuckPermsUtilities.getRankViaCache(player.getUniqueId()).orElse(""));
                        Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> onlinePlayer.sendMessage(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.staff.chat.format").replace("%server%", networkServer.getPrettifiedName()).replace("%player_rank%", playerRank).replace("%player%", player.getName())).replace("%message%", message)));
                    }
                }
            }
        }
    }

}
