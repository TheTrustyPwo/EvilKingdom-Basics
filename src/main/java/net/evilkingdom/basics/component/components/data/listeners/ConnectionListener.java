package net.evilkingdom.basics.component.components.data.listeners;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
    }

    /**
     * The listener for player quits.
     */
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent playerQuitEvent) {
        final Player player = playerQuitEvent.getPlayer();
        PlayerData.get(player.getUniqueId()).whenComplete((playerData, playerDataThrowable) -> {
            if (!playerData.isCached()) {
                return;
            }
            playerData.save(true);
            playerData.uncache();
        });
    }

}