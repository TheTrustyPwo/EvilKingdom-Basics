package net.evilkingdom.basics.component.components.data.listeners;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.NetworkPlayerData;
import net.evilkingdom.basics.component.components.data.objects.PlayerData;
import net.evilkingdom.commons.constructor.objects.ConstructorRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.Optional;

public class ConnectionListener implements Listener {

    private final Basics plugin;;

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
        NetworkPlayerData.get(player.getUniqueId()).whenComplete((networkPlayerData, networkPlayerDataThrowable) -> {
            networkPlayerData.cache();
            final PermissionAttachment permissionAttachment = player.addAttachment(this.plugin);
            networkPlayerData.getRefinedPermissions().forEach(refinedPermission -> permissionAttachment.setPermission(refinedPermission, true));
            networkPlayerData.getRefinedNegatedPermissions().forEach(refinedNegatedPermission -> permissionAttachment.setPermission(refinedNegatedPermission, false));
            networkPlayerData.setPermissionAttachment(Optional.of(permissionAttachment));
        });
    }

    /**
     * The listener for player quits.
     */
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent playerQuitEvent) {
        final Player player = playerQuitEvent.getPlayer();
        if (!PlayerData.getViaCache(player.getUniqueId()).isEmpty()) {
            final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
            playerData.save(true);
            playerData.uncache();
        }
        if (!NetworkPlayerData.getViaCache(player.getUniqueId()).isEmpty()) {
            final NetworkPlayerData networkPlayerData = NetworkPlayerData.getViaCache(player.getUniqueId()).get();
            networkPlayerData.getPermissionAttachment().ifPresent(permissionAttachment -> player.removeAttachment(permissionAttachment));
            networkPlayerData.save(true);
            networkPlayerData.uncache();
        }
    }

}
