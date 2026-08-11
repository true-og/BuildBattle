package plugily.projects.buildbattle.commands;

import java.util.List;
import java.util.Locale;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import plugily.projects.buildbattle.Main;
import plugily.projects.buildbattle.arena.BaseArena;
import plugily.projects.buildbattle.arena.BuildArena;
import plugily.projects.buildbattle.handlers.themes.vote.VotePoll;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;

// Routes /v and /vote to the arena's existing theme poll inside BuildBattle
// worlds only. BuildBattle has no map pool, so this drives the poll the vote
// menu already owns -- it exists so VotingPlugin cannot own /vote in a lobby.
public class VoteCommandListener implements Listener {

    private final Main plugin;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public VoteCommandListener(Main plugin) {

        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {

        final String message = event.getMessage();
        if (message == null || message.length() < 2 || message.charAt(0) != '/') {

            return;

        }

        final String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {

            return;

        }

        // Drop any namespace prefix (e.g. votingplugin:v) before matching.
        String label = parts[0].toLowerCase(Locale.ROOT);
        final int colon = label.indexOf(':');
        if (colon >= 0) {

            label = label.substring(colon + 1);

        }

        if (!label.equals("v") && !label.equals("vote")) {

            return;

        }

        final Player player = event.getPlayer();
        final BaseArena arena = resolveArena(player);
        if (arena == null) {

            return;

        }

        event.setCancelled(true);
        handleVote(player, arena, parts.length > 1 ? parts[1] : null);

    }

    // Claims the command for an arena member, or for anyone standing in a
    // configured BuildBattle world. Defers everywhere else.
    private BaseArena resolveArena(Player player) {

        final BaseArena arena = plugin.getArenaRegistry().getArena(player);
        if (arena != null) {

            return arena;

        }

        return plugin.getMyWorldsManager().isConfiguredArenaWorld(player.getWorld().getName()) ? findWorldArena(player)
                : null;

    }

    private BaseArena findWorldArena(Player player) {

        final String worldName = player.getWorld().getName();
        for (String id : LobbyResolver.lobbyIds(plugin)) {

            final BaseArena candidate = plugin.getArenaRegistry().getArena(id);
            if (candidate == null) {

                continue;

            }

            if (matchesWorld(candidate.getLobbyLocation(), worldName)
                    || matchesWorld(candidate.getStartLocation(), worldName))
            {

                return candidate;

            }

        }

        return null;

    }

    private boolean matchesWorld(org.bukkit.Location location, String worldName) {

        return location != null && location.getWorld() != null && location.getWorld().getName().equals(worldName);

    }

    private void handleVote(Player player, BaseArena arena, String argument) {

        if (!(arena instanceof BuildArena buildArena)) {

            send(player, "&cThere is nothing to vote for in this game mode.");
            return;

        }

        if (arena.getArenaState() != IArenaState.IN_GAME
                || arena.getArenaInGameState() != BaseArena.ArenaInGameState.THEME_VOTING)
        {

            send(player, "&cYou cannot run this command right now.");
            return;

        }

        final VotePoll votePoll = buildArena.getVotePoll();
        final List<String> themes = buildArena.getVoteMenu().getThemeSelection();
        if (votePoll == null || themes.isEmpty()) {

            send(player, "&cThere is nothing to vote for right now.");
            return;

        }

        if (argument == null) {

            sendThemeList(player, votePoll, themes);
            buildArena.getVoteMenu().updateInventory(player);
            return;

        }

        final int choice;
        try {

            choice = Integer.parseInt(argument);

        } catch (NumberFormatException error) {

            send(player, "&cCorrect Usage: /vote <theme number>");
            sendThemeList(player, votePoll, themes);
            return;

        }

        if (choice < 1 || choice > themes.size()) {

            send(player, "&cInvalid theme!");
            sendThemeList(player, votePoll, themes);
            return;

        }

        final String theme = themes.get(choice - 1);
        new MessageBuilder(votePoll.addVote(player, theme) ? "MENU_THEME_VOTE_SUCCESS" : "MENU_THEME_VOTE_ALREADY")
                .asKey().player(player).value(theme).sendPlayer();
        buildArena.getVoteMenu().updateInventory(player);

    }

    private void sendThemeList(Player player, VotePoll votePoll, List<String> themes) {

        send(player, "&6Vote for a theme with /v #.");
        send(player, "&6Theme choices up for voting:");
        for (int i = 0; i < themes.size(); i++) {

            final String theme = themes.get(i);
            send(player, "&6&l" + (i + 1) + ". &6" + theme + " (&b" + votePoll.getVoteAmount(theme) + "&6 votes)");

        }

    }

    private void send(Player player, String message) {

        player.sendMessage(LEGACY_SERIALIZER.deserialize(plugin.getPluginMessagePrefix() + message));

    }

}
