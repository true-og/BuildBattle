package plugily.projects.buildbattle.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.skbotnl.chatog.api.WorldChatFormatter;
import org.bukkit.entity.Player;

import plugily.projects.buildbattle.Main;
import plugily.projects.buildbattle.arena.BaseArena;
import plugily.projects.buildbattle.arena.BuildArena;
import plugily.projects.minigamesbox.api.arena.IArenaState;

// Renders BuildBattle chat now that Chat-OG owns delivery and the Discord relay.
//
// The arena is resolved from the sender rather than the lobby id Chat-OG passes, so a spectator or
// a staff member standing in an arena world without having joined still falls through to Chat-OG's
// default format instead of being labelled as a competitor.
//
// The theme is never rendered for Guess the Build: it is the answer, and Discord relays this line.
public class BuildBattleChatFormatter implements WorldChatFormatter {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final Main plugin;

    public BuildBattleChatFormatter(Main plugin) {

        this.plugin = plugin;

    }

    @Override
    public Component format(Player sender, Component message, String worldName, String lobbyId) {

        BaseArena arena = plugin.getArenaRegistry().getArena(sender);
        if (arena == null) {

            return null;

        }

        // The message is already sanitised by Chat-OG, so it is composed with rather
        // than re-parsed.
        return Component.join(JoinConfiguration.noSeparators(),
                LEGACY_SERIALIZER.deserialize(buildPrefix(sender, arena)), message);

    }

    private String buildPrefix(Player player, BaseArena arena) {

        String plainName = PlainTextComponentSerializer.plainText().serialize(player.displayName());
        String name = "&9" + plainName + "&8 » &r";

        if (arena.getSpectators().contains(player)) {

            return "&8&lSPEC &8▏ &7" + plainName + "&8 » &7";

        }

        IArenaState state = arena.getArenaState();
        if (state == IArenaState.WAITING_FOR_PLAYERS || state == IArenaState.STARTING
                || state == IArenaState.FULL_GAME)
        {

            return "&e" + arena.getPlayers().size() + "&7/&e" + arena.getMaximumPlayers() + "&8 ▏ " + name;

        }

        if (state != IArenaState.IN_GAME) {

            return name;

        }

        switch (arena.getArenaInGameState()) {

            case THEME_VOTING:
                return "&6VOTE &8▏ " + name;
            case PLOT_VOTING:
                return "&6JUDGING &8▏ " + name;
            case BUILD_TIME:
                // Only a classic/team arena has a theme worth showing; leaking a Guess the
                // Build theme here would hand the answer to the world and to Discord.
                String theme = arena instanceof BuildArena ? arena.getTheme() : null;
                return theme == null || theme.isEmpty() ? name : "&a" + theme + " &8▏ " + name;
            default:
                return name;

        }

    }

}
