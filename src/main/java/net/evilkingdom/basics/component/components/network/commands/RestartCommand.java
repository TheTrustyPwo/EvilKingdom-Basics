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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
            if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("basics.network.commands.restart")) {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.restart.messages.invalid-permissions").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.restart.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.error.pitch"));
                return;
            }
        }
        if (arguments.length != 0) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.restart.messages.invalid-usage").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.restart.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.error.pitch"));
                return;
            }
            return;
        }
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.network.commands.restart.messages.success").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.network.commands.restart.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.network.commands.restart.sounds.success.pitch"));
        }
        Arrays.stream(Bukkit.getPluginManager().getPlugins()).filter(plugin -> plugin.getDescription().getDepend().contains("Commons")).forEach(dependingPlugin -> {
            try {
                final Method terminateMethod = dependingPlugin.getClass().getDeclaredMethod("terminate");
                final Field pluginField = dependingPlugin.getClass().getDeclaredField("plugin");
                terminateMethod.invoke(pluginField.get(null));
            } catch (final IllegalAccessException | NoSuchMethodException | NoSuchFieldException | InvocationTargetException exception) {
                exception.printStackTrace();
                //Pretty much nothing bad happens we get here! :>
            }
        });
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            Arrays.stream(Bukkit.getPluginManager().getPlugins()).filter(plugin -> plugin.getDescription().getDepend().contains("Commons")).forEach(dependingPlugin -> Bukkit.getPluginManager().disablePlugin(dependingPlugin));
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "spigot:restart");
        }, 100L);
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
