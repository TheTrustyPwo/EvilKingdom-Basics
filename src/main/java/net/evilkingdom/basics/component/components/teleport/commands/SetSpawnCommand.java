package net.evilkingdom.basics.component.components.teleport.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.basics.component.components.data.objects.SelfData;
import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.number.NumberUtilities;
import net.evilkingdom.commons.utilities.number.enums.NumberFormatType;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class SetSpawnCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public SetSpawnCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "setspawn");
        command.setHandler(this);
        command.register();
    }

    /**
     * The execution of the command.
     * Just uses the bukkit arguments since bukkit handles the magic.
     */
    @Override
    public boolean onExecution(final CommandSender commandSender, final String[] arguments) {
        if (!(commandSender instanceof Player)) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.teleport.commands.setspawn.messages.invalid-executor").forEach(string -> commandSender.sendMessage(StringUtilities.colorize(string)));
            return false;
        }
        final Player player = (Player) commandSender;
        if (arguments.length != 1) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.teleport.commands.setspawn.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.teleport.commands.setspawn.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.setspawn.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.setspawn.sounds.error.pitch"));
            return false;
        }
        LuckPermsUtilities.getPermissions(player.getUniqueId()).whenComplete((playerPermissions, playerPermissionsThrowable) -> {
            if (!playerPermissions.contains("basics.teleport.commands.setspawn")) {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.teleport.commands.setspawn.messages.invalid-permissions").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.teleport.commands.setspawn.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.setspawn.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.setspawn.sounds.error.pitch"));
                return;
            }
            SelfData.get().whenComplete((selfData, selfDataThrowable) -> {
                selfData.setSpawn(player.getLocation());
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.teleport.commands.setspawn.messages.success").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.teleport.commands.setspawn.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.setspawn.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.teleport.commands.setspawn.sounds.success.pitch"));
            });
        });
        return true;
    }

    /**
     * The tab completion of the command.
     * Just uses the bukkit arguments since bukkit handles the magic and the converter filters the options returned.
     */
    @Override
    public ArrayList<String> onTabCompletion(final CommandSender commandSender, final String[] arguments) {
        if (!(commandSender instanceof Player)) {
            return new ArrayList<String>();
        }
        final Player player = (Player) commandSender;
        ArrayList<String> tabCompletion = new ArrayList<String>();
        switch (arguments.length) {
            case 1 ->  tabCompletion.addAll(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
            case 2 -> tabCompletion.addAll(Arrays.asList("gems", "tokens"));
        }
        return tabCompletion;
    }

}
