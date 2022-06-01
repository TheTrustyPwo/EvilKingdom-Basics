package net.evilkingdom.basics.component.components.chat.listeners;

/*
 * Made with love by https://kodirati.com/.
 */

import io.papermc.paper.event.player.AsyncChatEvent;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.PlayerData;
import net.evilkingdom.basics.component.components.data.objects.SelfData;
import net.evilkingdom.commons.cooldown.objects.Cooldown;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.number.NumberUtilities;
import net.evilkingdom.commons.utilities.number.enums.NumberFormatType;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.commons.utilities.time.TimeUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Optional;

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
     * The listener for player quits.
     */
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent playerQuitEvent) {
        final Player player = playerQuitEvent.getPlayer();
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            final PlayerData onlinePlayerData = PlayerData.getViaCache(onlinePlayer.getUniqueId()).get();
            if (onlinePlayerData.getReply().isPresent() && onlinePlayerData.getReply().get() == player.getUniqueId()) {
                onlinePlayerData.setReply(Optional.empty());
            }
        });
    }

}
