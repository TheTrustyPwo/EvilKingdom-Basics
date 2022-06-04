package net.evilkingdom.basics.component.components.network.listeners.custom;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.text.PaperComponents;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.NetworkPlayerData;
import net.evilkingdom.basics.component.components.data.objects.NetworkRankData;
import net.evilkingdom.basics.component.components.data.objects.PlayerData;
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
import org.bukkit.permissions.PermissionAttachment;

import java.util.ArrayList;
import java.util.Optional;
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
                    case "announce" -> {
                        final JsonObject jsonObject = JsonParser.parseString(data.split("=")[1]).getAsJsonObject();
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(jsonObject.get("player").getAsString()));
                        final String message = jsonObject.get("message").getAsString();
                        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.announce.messages.success.online-players").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", offlinePlayer.getName()).replace("%message%", message))));
                            onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.announce.sounds.success.online-players.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.online-players.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.announce.sounds.success.online-players.pitch"));
                        });
                    }
                    case "server_stop" -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "stop");
                    case "server_restart" -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart");
                    case "staff_chat" -> {
                        final NetworkServer networkServer = this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(innerNetworkServer -> innerNetworkServer.getName().equals(server.getName())).findFirst().get();
                        final JsonObject jsonObject = JsonParser.parseString(data.split("=")[1]).getAsJsonObject();
                        final Player player = Bukkit.getPlayer(UUID.fromString(jsonObject.get("player").getAsString()));
                        final String message = jsonObject.get("message").getAsString();
                        final String playerRank = WordUtils.capitalizeFully(LuckPermsUtilities.getRankViaCache(player.getUniqueId()).orElse(""));
                        Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> onlinePlayer.sendMessage(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.staff.chat.format").replace("%server%", networkServer.getPrettifiedName()).replace("%player_rank%", playerRank).replace("%player%", player.getName())).replace("%message%", message)));
                    }
                    case "player_send" -> {
                        final JsonObject jsonObject = JsonParser.parseString(data.split("=")[1]).getAsJsonObject();
                        final Player player = Bukkit.getPlayer(UUID.fromString(jsonObject.get("player").getAsString()));
                        final String sendServer = jsonObject.get("server").getAsString();
                        final TransmissionServer transmissionServer = transmissionSite.getServers().stream().filter(internalTransmissionServer -> internalTransmissionServer.getName().equals(sendServer)).findFirst().get();
                        transmissionSite.send(player, transmissionServer);
                    }
                    case "update_rank" -> {
                        final String rank = data.split("=")[1];
                        final NetworkRankData preNetworkRankData = NetworkRankData.getViaCache(rank).get();
                        preNetworkRankData.uncache();
                        NetworkRankData.get(rank).whenComplete((networkRankData, networkRankDataThrowable) -> {
                            networkRankData.cache();
                            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                                final NetworkPlayerData networkPlayerData = NetworkPlayerData.getViaCache(onlinePlayer.getUniqueId()).get();
                                if (networkPlayerData.getRanks().contains(rank) && networkPlayerData.getPermissionAttachment().isPresent()) {
                                    final PermissionAttachment permissionAttachment = networkPlayerData.getPermissionAttachment().get();
                                    permissionAttachment.getPermissions().clear();
                                    networkPlayerData.getRefinedPermissions().forEach(refinedPermission -> permissionAttachment.setPermission(refinedPermission, true));
                                    networkPlayerData.getRefinedNegatedPermissions().forEach(refinedNegatedPermission -> permissionAttachment.setPermission(refinedNegatedPermission, false));
                                }
                            });
                        });
                    }
                    case "player_sent" -> {
                        final JsonObject jsonObject = JsonParser.parseString(data.split("=")[1]).getAsJsonObject();
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(jsonObject.get("player").getAsString()));
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                            if (!offlinePlayer.isOnline()) {
                                return;
                            }
                            final Player player = offlinePlayer.getPlayer();
                            final String reason = jsonObject.get("reason").getAsString();
                            switch (reason) {
                                case "server_stop" -> {
                                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.shutdowns.kick.server.message").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.shutdowns.kick.server.sound.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.shutdowns.kick.server.sound.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.shutdowns.kick.server.sound.pitch"));
                                }
                                case "send_command" -> {
                                    final OfflinePlayer sender = Bukkit.getOfflinePlayer(UUID.fromString(jsonObject.get("sender").getAsString()));
                                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.send.messages.success.target").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", sender.getName()).replace("%server%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.servers.internal.prettified-name")))));
                                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.send.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.send.sounds.success.target.pitch"));
                                }
                                case "server_command" -> {
                                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.server.messages.success").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%server%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.servers.internal.prettified-name")))));
                                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.server.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.server.sounds.success.pitch"));
                                }
                            }
                        }, 60L);
                    }
                }
            }
        }
    }

}
