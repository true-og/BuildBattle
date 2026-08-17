package plugily.projects.buildbattle.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.trueog.utilitiesog.UtilitiesOG;
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
//
// The name segment mirrors TheHerobrine-OG's formatter: the standard TrueOG
// union bracket tag, display name, and LuckPerms suffix, expanded through
// MiniPlaceholders with the sender as the audience so the same content reaches
// the in-game view and the Discord relay.
public class BuildBattleChatFormatter implements WorldChatFormatter {

    private static final String NAME_SEGMENT = "<simpleclans_union_bracket_tag><player_display_name><luckperms_suffix> ";

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

        final Component prefix = UtilitiesOG.trueogExpand(buildPrefix(sender, arena), sender);

        // The caret never inherits bold or colors from the prefix.
        final Component caret = UtilitiesOG.trueogExpand(getCaretColor(sender) + "> &r", sender)
                .decoration(TextDecoration.BOLD, false);

        // The message is already sanitised by Chat-OG, so it is composed with rather
        // than re-parsed.
        return Component.join(JoinConfiguration.noSeparators(), prefix, caret, message);

    }

    private String buildPrefix(Player player, BaseArena arena) {

        if (arena.getSpectators().contains(player)) {

            return "&4SPEC &8▏ " + NAME_SEGMENT;

        }

        IArenaState state = arena.getArenaState();
        if (state == IArenaState.WAITING_FOR_PLAYERS || state == IArenaState.STARTING
                || state == IArenaState.FULL_GAME)
        {

            return "&e" + arena.getPlayers().size() + "&7/&e" + arena.getMaximumPlayers() + "&8 ▏ " + NAME_SEGMENT;

        }

        if (state != IArenaState.IN_GAME) {

            return NAME_SEGMENT;

        }

        switch (arena.getArenaInGameState()) {

            case THEME_VOTING:
                return "&6VOTE &8▏ " + NAME_SEGMENT;
            case PLOT_VOTING:
                return "&6JUDGING &8▏ " + NAME_SEGMENT;
            case BUILD_TIME:
                // Only a classic/team arena has a theme worth showing; leaking a Guess the
                // Build theme here would hand the answer to the world and to Discord.
                String theme = arena instanceof BuildArena ? arena.getTheme() : null;
                return theme == null || theme.isEmpty() ? NAME_SEGMENT : "&a" + theme + " &8▏ " + NAME_SEGMENT;
            default:
                return NAME_SEGMENT;

        }

    }

    // TODO: Expose PlayerUtils.getMessageColor in Chat-OG as a supported API.
    private String getCaretColor(Player player) {

        try {

            final net.kyori.adventure.text.format.TextColor color = nl.skbotnl.chatog.util.PlayerUtils.INSTANCE
                    .getMessageColor(player.getUniqueId());
            if (color != null) {

                if (color.equals(NamedTextColor.WHITE)) {

                    return "&f";

                } else if (color.equals(NamedTextColor.GRAY)) {

                    return "&7";

                }

            }

        } catch (Throwable ignored) {

            // Chat-OG internals moved or are absent -- fall back to dark gray.

        }

        return "&8";

    }

}
