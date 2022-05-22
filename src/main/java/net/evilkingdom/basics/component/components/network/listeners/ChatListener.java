package net.evilkingdom.basics.component.components.network.listeners;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.text.PaperComponents;
import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.PlayerData;
import net.evilkingdom.basics.component.components.data.objects.SelfData;
import net.evilkingdom.basics.component.components.network.enums.NetworkServerStatus;
import net.evilkingdom.commons.cooldown.objects.Cooldown;
import net.evilkingdom.commons.transmission.TransmissionImplementor;
import net.evilkingdom.commons.transmission.enums.TransmissionType;
import net.evilkingdom.commons.transmission.objects.Transmission;
import net.evilkingdom.commons.transmission.objects.TransmissionServer;
import net.evilkingdom.commons.transmission.objects.TransmissionSite;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.number.NumberUtilities;
import net.evilkingdom.commons.utilities.number.enums.NumberFormatType;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.commons.utilities.time.TimeUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class ChatListener implements Listener {

    private final Basics plugin;

    /**
     * Allows you to create the listener.
     */
    public ChatListener() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the listener.
     */
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    /**
     * The listener for player chats.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(final AsyncChatEvent asyncChatEvent) {
        final Player player = asyncChatEvent.getPlayer();
        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
        String preMessage = PaperComponents.plainTextSerializer().serialize(asyncChatEvent.originalMessage());
        if (!playerData.canStaffChat() && !(preMessage.startsWith("#") && LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.staff"))) {
            return;
        }
        if (preMessage.startsWith("#")) {
            preMessage = preMessage.substring(1);
        }
        asyncChatEvent.setCancelled(true);
        final String message = preMessage;
        final String playerRank = WordUtils.capitalizeFully(LuckPermsUtilities.getRankViaCache(player.getUniqueId()).orElse(""));
        Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> LuckPermsUtilities.getPermissionsViaCache(onlinePlayer.getUniqueId()).contains("basics.network.staff")).forEach(onlinePlayer -> onlinePlayer.sendMessage(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.staff.chat.format").replace("%player_server%", "Here").replace("%player_rank%", playerRank).replace("%player%", player.getName())).replace("%message%", message)));
        this.plugin.getComponentManager().getNetworkComponent().getServers().stream().filter(networkServer -> networkServer.getStatus() == NetworkServerStatus.ONLINE).forEach(networkServer -> {
            final TransmissionImplementor transmissionImplementor = TransmissionImplementor.get(this.plugin);
            final TransmissionSite transmissionSite = transmissionImplementor.getSites().stream().filter(innerTransmissionSite -> innerTransmissionSite.getName().equals("basics")).findFirst().get();
            final TransmissionServer transmissionServer = transmissionSite.getServers().stream().filter(innerTransmissionServer -> innerTransmissionServer.getName().equals(networkServer.getName())).findFirst().get();
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("player", player.getUniqueId().toString());
            jsonObject.addProperty("message", message);
            final Transmission staffChatTransmission = new Transmission(transmissionSite, transmissionServer, "basics", TransmissionType.MESSAGE, UUID.randomUUID(), "staff_chat=" + new Gson().toJson(jsonObject));
            staffChatTransmission.send();
        });
    }

}
