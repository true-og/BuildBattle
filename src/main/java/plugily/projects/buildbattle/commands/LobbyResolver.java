package plugily.projects.buildbattle.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import plugily.projects.buildbattle.Main;
import plugily.projects.buildbattle.arena.BaseArena;
import plugily.projects.minigamesbox.api.arena.IPluginArena;

// Resolves the lobby id a player typed (BB1) to an arena. Bare numbers are
// accepted so /bbjoin 1 matches BB1, and matching is case-insensitive.
// Map names and themes are never resolvable -- only lobby ids.
public final class LobbyResolver {

    private LobbyResolver() {

    }

    public static BaseArena resolve(Main plugin, String input) {

        if (input == null) {

            return null;

        }

        final String query = input.trim();
        if (query.isEmpty()) {

            return null;

        }

        for (IPluginArena arena : plugin.getArenaRegistry().getArenas()) {

            if (arena.getId().equalsIgnoreCase(query)) {

                return plugin.getArenaRegistry().getArena(arena.getId());

            }

        }

        if (!isNumber(query)) {

            return null;

        }

        // Bare number: match the trailing digits of a lobby id so "1" finds BB1
        // whatever prefix the server configured. Ambiguity is a miss, not a guess.
        final int wanted = Integer.parseInt(query);
        BaseArena match = null;
        for (IPluginArena arena : plugin.getArenaRegistry().getArenas()) {

            if (trailingNumber(arena.getId()) != wanted) {

                continue;

            }

            if (match != null) {

                return null;

            }

            match = plugin.getArenaRegistry().getArena(arena.getId());

        }

        return match;

    }

    public static List<String> lobbyIds(Main plugin) {

        final List<String> ids = new ArrayList<>();
        for (IPluginArena arena : plugin.getArenaRegistry().getArenas()) {

            ids.add(arena.getId());

        }

        return ids;

    }

    // Returns the number a lobby id ends with, or -1 when it ends in a letter.
    private static int trailingNumber(String id) {

        int index = id.length();
        while (index > 0 && Character.isDigit(id.charAt(index - 1))) {

            index--;

        }

        if (index == id.length()) {

            return -1;

        }

        try {

            return Integer.parseInt(id.substring(index));

        } catch (NumberFormatException error) {

            return -1;

        }

    }

    private static boolean isNumber(String value) {

        if (value.length() > 9) {

            return false;

        }

        for (int i = 0; i < value.length(); i++) {

            if (!Character.isDigit(value.charAt(i))) {

                return false;

            }

        }

        return true;

    }

    public static String normalize(String value) {

        return value == null ? "" : value.toLowerCase(Locale.ROOT);

    }

}
