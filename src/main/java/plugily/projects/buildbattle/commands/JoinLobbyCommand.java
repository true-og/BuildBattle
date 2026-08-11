package plugily.projects.buildbattle.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import plugily.projects.buildbattle.Main;
import plugily.projects.buildbattle.arena.BaseArena;
import plugily.projects.minigamesbox.api.arena.IPluginArena;

// Joins a BuildBattle lobby by its id (BB1). Maps and themes are not accepted.
public class JoinLobbyCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public JoinLobbyCommand(Main plugin) {

        this.plugin = plugin;

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage("Only players can use this command.");
            return true;

        }

        if (args.length == 0) {

            sendLobbyList(player);
            return true;

        }

        final BaseArena arena = LobbyResolver.resolve(plugin, args[0]);
        if (arena == null) {

            send(player, "&c" + args[0] + " does not exist.");
            sendLobbyList(player);
            return true;

        }

        plugin.getArenaManager().joinAttempt(player, arena);
        return true;

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length != 1) {

            return null;

        }

        final String prefix = LobbyResolver.normalize(args[0]);
        final List<String> completions = new ArrayList<>();
        for (String id : LobbyResolver.lobbyIds(plugin)) {

            if (LobbyResolver.normalize(id).startsWith(prefix)) {

                completions.add(id);

            }

        }

        return completions;

    }

    private void sendLobbyList(Player player) {

        final List<IPluginArena> arenas = plugin.getArenaRegistry().getArenas();
        if (arenas.isEmpty()) {

            send(player, "&cThere are no BuildBattle lobbies available.");
            return;

        }

        send(player, "&6Join a lobby with /bbjoin <lobby>.");
        for (IPluginArena arena : arenas) {

            send(player, "&6&l" + arena.getId() + " &6(&b" + arena.getPlayers().size() + "&6/&b"
                    + arena.getMaximumPlayers() + "&6)");

        }

    }

    private void send(Player player, String message) {

        player.sendMessage(LEGACY_SERIALIZER.deserialize(plugin.getPluginMessagePrefix() + message));

    }

}
