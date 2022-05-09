package net.evilkingdom.basics.component.components.network.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.Basics;
import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RestartCommand extends CommandHandler {

    private final Basics plugin;

    /**
     * Allows you to create the command.
     */
    public RestartCommand() {
        this.plugin = Basics.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "restart", new ArrayList<String>(), this);
        command.register();
    }

    /**
     * The execution of the command.
     *
     * @param sender ~ The command's sender.
     * @param arguments ~ The command's arguments.
     */
    @Override
    public void onExecution(final CommandSender sender, final String[] arguments) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.commands.stop")) {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.stop.messages.invalid-permissions").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.stop.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.stop.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.stop.sounds.error.pitch"));
                return;
            }
        }
        if (arguments.length != 0) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.stop.messages.invalid-usage").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.stop.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.stop.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.stop.sounds.error.pitch"));
                return;
            }
            return;
        }
        Arrays.stream(Bukkit.getPluginManager().getPlugins()).toList().stream().filter(plugin -> plugin.getDescription().getDepend().contains("Commons")).forEach(dependingPlugin -> {
            try {
                dependingPlugin.getClass().getMethod("getPlugin", null).invoke(null).getClass().getMethod("terminate", null).invoke(null);
                Bukkit.getPluginManager().disablePlugin(dependingPlugin);
            } catch (final InvocationTargetException | IllegalAccessException | NoSuchMethodException exception) {
            }
        });
        Bukkit.getServer().shutdown();
    }

    /**
     * The tab completion of the command.
     *
     * @param sender ~ The command's sender.
     * @param arguments ~ The command's arguments.
     */
    @Override
    public ArrayList<String> onTabCompletion(final CommandSender sender, final String[] arguments) {
        if (!(sender instanceof Player)) {
            return new ArrayList<String>();
        }
        final Player player = (Player) sender;
        ArrayList<String> tabCompletion = new ArrayList<String>();
        switch (arguments.length) {
            case 1 ->  tabCompletion.addAll(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
            case 2 -> tabCompletion.addAll(Arrays.asList("gems", "tokens"));
        }
        return tabCompletion;
    }

}
