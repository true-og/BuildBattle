package plugily.projects.buildbattle.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import plugily.projects.buildbattle.Main;
import plugily.projects.minigamesbox.classic.arena.PluginArenaUtils;

// Starts the arena the sender is in even when the minimum player count is unmet.
// PluginArenaUtils.arenaForceStart owns the permission, state, and timer checks.
public class ForceStartCommand implements CommandExecutor {

    private final Main plugin;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public ForceStartCommand(Main plugin) {

        this.plugin = plugin;

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage("Only players can use this command.");
            return true;

        }

        int startTime = 0;
        if (args.length > 0) {

            try {

                startTime = Integer.parseInt(args[0]);

            } catch (NumberFormatException error) {

                send(player, "&cCorrect Usage: /bbforcestart [time]");
                return true;

            }

        }

        PluginArenaUtils.arenaForceStart(player, Math.max(0, startTime));
        return true;

    }

    private void send(Player player, String message) {

        player.sendMessage(LEGACY_SERIALIZER.deserialize(plugin.getPluginMessagePrefix() + message));

    }

}
