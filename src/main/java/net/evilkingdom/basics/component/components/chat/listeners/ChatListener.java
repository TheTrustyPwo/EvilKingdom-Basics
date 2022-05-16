package net.evilkingdom.basics.component.components.chat.listeners;

/*
 * Made with love by https://kodirati.com/.
 */

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.text.PaperComponents;
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
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Optional;

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
    @EventHandler
    public void onPlayerChat(final AsyncChatEvent asyncChatEvent) {
        final Player player = asyncChatEvent.getPlayer();
        asyncChatEvent.setCancelled(true);
        final SelfData selfData = SelfData.getViaCache().get();
        if (!selfData.canChat() && !LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.chat.global.mutechat.bypass")) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.global.invalid-chat.server-chat-disabled.message").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.global.invalid-chat.server-chat-disabled.sound.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.global.invalid-chat.server-chat-disabled.sound.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.global.invalid-chat.server-chat-disabled.sound.pitch"));
            return;
        }
        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
        if (!playerData.canChat()) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.global.invalid-chat.player-chat-disabled.message").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.global.invalid-chat.player-chat-disabled.sound.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.global.invalid-chat.player-chat-disabled.sound.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.global.invalid-chat.player-chat-disabled.sound.pitch"));
            return;
        }
        final Optional<Cooldown> optionalCooldown = playerData.getCooldowns().stream().filter(cooldown -> cooldown.getIdentifier().equals("player-" + playerData.getUUID() + "-chat")).findFirst();
        if (optionalCooldown.isPresent() && !LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.chat.global.slowchat.bypass")) {
            final Cooldown cooldown = optionalCooldown.get();
            final String formattedTimeLeft = TimeUtilities.format((cooldown.getTimeLeft() * 50));
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.global.invalid-chat.player-on-cooldown.message").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%time_left%", formattedTimeLeft))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.global.invalid-chat.player-on-cooldown.sound.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.global.invalid-chat.player-on-cooldown.sound.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.global.invalid-chat.player-on-cooldown.sound.pitch"));
            return;
        }
        final String message = PaperComponents.plainTextSerializer().serialize(asyncChatEvent.originalMessage());
        if (StringUtilities.contains(message, new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.filtered-words")))) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.chat.global.invalid-chat.message-filtered.message").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.global.invalid-chat.message-filtered.sound.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.global.invalid-chat.message-filtered.sound.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.chat.global.invalid-chat.message-filtered.sound.pitch"));
            return;
        }
        final String playerPrefix = LuckPermsUtilities.getPrefixViaCache(player.getUniqueId()).orElse("");
        final String playerSuffix = LuckPermsUtilities.getSuffixViaCache(player.getUniqueId()).orElse("");
        String formattedSubMessage;
        if (this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.global.format").contains("%player_prison_rank%")) {
            final net.evilkingdom.prison.component.components.data.objects.PlayerData prisonPlayerData = net.evilkingdom.prison.component.components.data.objects.PlayerData.getViaCache(player.getUniqueId()).get();
            final String formattedPrisonRank = NumberUtilities.format(prisonPlayerData.getRank(), NumberFormatType.LETTERS);
            formattedSubMessage = StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.global.format").replace("%player_prefix%", playerPrefix).replace("%player%", player.getName()).replace("%player_suffix%", playerSuffix).replace("%player_prison_rank%", formattedPrisonRank));
        } else {
            formattedSubMessage = StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.global.format").replace("%player_prefix%", playerPrefix).replace("%player%", player.getName()).replace("%player_suffix%", playerSuffix));
        }
        String formattedMessage = message;
        if (LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.chat.global.colorized")) {
            formattedMessage = StringUtilities.colorize(message);
        }
        if (this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.chat.global.taggables.item.enabled") && ((message.contains("[i]") || message.contains("[item]")) && LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.chat.global.taggables.item"))) {
            final Component itemComponent;
            if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                itemComponent = Component.text(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.global.taggables.item.format.empty-hand").replace("%player%", player.getName())));
            } else {
                final ItemStack item = player.getInventory().getItemInMainHand();
                final String formattedItemAmount = NumberUtilities.format(item.getAmount(), NumberFormatType.LETTERS);
                String itemName = PaperComponents.plainTextSerializer().serialize(item.displayName());
                if (itemName.startsWith("[") && itemName.endsWith("]")) {
                    itemName = itemName.substring(1, (itemName.length() - 1));
                }
                final Component itemInformationComponent = Component.text(itemName).hoverEvent(item.asHoverEvent());
                itemComponent = Component.text(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.chat.global.taggables.item.format.not-empty-hand").replace("%amount%", formattedItemAmount))).replaceText(TextReplacementConfig.builder().match("%item%").replacement(itemInformationComponent).build());
            }
            final Component formatComponent = Component.text(formattedSubMessage.replace("%message%", formattedMessage)).replaceText(TextReplacementConfig.builder().matchLiteral("[i]").replacement(itemComponent).build()).replaceText(TextReplacementConfig.builder().matchLiteral("[item]").replacement(itemComponent).build());
            Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> {
                final PlayerData onlinePlayerData = PlayerData.getViaCache(onlinePlayer.getUniqueId()).get();
                return onlinePlayerData.canChat() && !onlinePlayerData.getIgnored().contains(player.getUniqueId());
            }).forEach(onlinePlayer -> onlinePlayer.sendMessage(formatComponent));
        } else {
            final String format = formattedSubMessage.replace("%message%", formattedMessage);
            Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> {
                final PlayerData onlinePlayerData = PlayerData.getViaCache(onlinePlayer.getUniqueId()).get();
                return onlinePlayerData.canChat() && !onlinePlayerData.getIgnored().contains(player.getUniqueId());
            }).forEach(onlinePlayer -> onlinePlayer.sendMessage(format));
        }
        if (selfData.getChatSlow() > -1L) {
            final Cooldown cooldown = new Cooldown(this.plugin, "player-" + playerData.getUUID() + "-chat", selfData.getChatSlow());
            cooldown.start();
        }
    }

}
