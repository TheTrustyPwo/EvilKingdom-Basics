package net.evilkingdom.basics.component.components.network.listeners;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.PlayerData;
import net.evilkingdom.commons.transmission.TransmissionImplementor;
import net.evilkingdom.commons.transmission.enums.TransmissionType;
import net.evilkingdom.commons.transmission.objects.Transmission;
import net.evilkingdom.commons.transmission.objects.TransmissionSite;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class ConnectionListener implements Listener {

    private final Basics plugin;

    /**
     * Allows you to create the listener.
     */
    public ConnectionListener() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the listener.
     */
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    /**
     * The listener for player joins.
     */
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent playerJoinEvent) {
        final Player player = playerJoinEvent.getPlayer();
        PlayerData.get(player.getUniqueId()).whenComplete((playerData, playerDataThrowable) -> playerData.cache());
        LuckPermsUtilities.getPermissions(player.getUniqueId()).whenComplete((permissions, permissionsThrowable) -> {
            if (permissions.contains("components.network.staff")) {
                final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
                final TransmissionSite transmissionSite = transmissionImplementor.getTransmissionSites().stream().findFirst().get();
                Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.connection.messages.join.internal").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()).replace("%server%", transmissionSite.getName())))));
                transmissionSite.getServers().forEach(transmissionServer -> {
                    final Transmission transmission = new Transmission(transmissionSite, transmissionServer, TransmissionType.MESSAGE, UUID.randomUUID(),"staff_join=" + player.getUniqueId());
                    transmission.send();
                });
            }
        });
    }

    /**
     * The listener for player quits.
     */
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent playerQuitEvent) {
        final Player player = playerQuitEvent.getPlayer();
        if (LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("components.network.staff")) {
            final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
            final TransmissionSite transmissionSite = transmissionImplementor.getTransmissionSites().stream().findFirst().get();
            Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.staff.connection.messages.quit.internal").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()).replace("%server%", transmissionSite.getName())))));
            transmissionSite.getServers().forEach(transmissionServer -> {
                final Transmission transmission = new Transmission(transmissionSite, transmissionServer, TransmissionType.MESSAGE, UUID.randomUUID(),"staff_quit=" + player.getUniqueId());
                transmission.send();
            });
        }
        if (PlayerData.getViaCache(player.getUniqueId()).isEmpty()) {
            return;
        }
        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
        playerData.save(true);
        playerData.uncache();
    }

}
