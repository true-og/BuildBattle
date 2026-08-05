/*
 *
 * BuildBattle - Ultimate building competition minigame
 * Copyright (C) 2022 Plugily Projects - maintained by Tigerpanzer_02 and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package plugily.projects.buildbattle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards {@code plugin.yml}'s permission tree against the two ways it rots.
 *
 * <p>
 * Before 5.1.5 it declared seven nodes that nothing anywhere read (so granting
 * them did nothing) and omitted several that the code does check (so
 * {@code buildbattle.admin.*} did not actually grant the whole admin surface).
 * Both failures are silent at runtime — a server owner only finds out when a
 * permission mysteriously has no effect — so they are pinned here instead.
 */
class PluginPermissionsTest {

    private static final Path PLUGIN_YML = Paths.get("src/main/resources/plugin.yml");
    private static final Path JAVA_SOURCES = Paths.get("src/main/java");

    /** Permission literals appearing in this fork's own source. */
    private static final Pattern OWN_PERMISSION = Pattern.compile("\"(buildbattle\\.[a-z][a-z.]*)\"");

    /**
     * Nodes checked by the shaded MiniGamesBox-Classic rather than by this fork,
     * recovered from the constant pools of {@code MiniGamesBox-Classic-1.4.5.jar}.
     * Re-verify when that version changes.
     */
    private static final Set<String> INHERITED_NODES = new HashSet<>(
            Arrays.asList("buildbattle.admin", "buildbattle.admin.delete", "buildbattle.admin.forcestart",
                    "buildbattle.admin.kitfile", "buildbattle.admin.leaderboard.manage", "buildbattle.admin.list",
                    "buildbattle.admin.locale", "buildbattle.admin.locales", "buildbattle.admin.locwand",
                    "buildbattle.admin.placeholders", "buildbattle.admin.reload", "buildbattle.admin.setup",
                    "buildbattle.admin.sign.break", "buildbattle.admin.sign.create", "buildbattle.admin.spychat",
                    "buildbattle.admin.statistic", "buildbattle.admin.statistic.others", "buildbattle.admin.stop",
                    "buildbattle.admin.teleport", "buildbattle.command.override"));

    /**
     * Player-facing nodes the base plugin gates commands on. Broken out because
     * they are the easiest to lose: no literal for them appears anywhere in this
     * repository, and an undeclared node silently defaults to OP, so the command
     * looks broken to every non-operator.
     */
    private static final Set<String> INHERITED_PLAYER_NODES = new HashSet<>(
            Arrays.asList("buildbattle.arenas", "buildbattle.command.selectkit", "buildbattle.fullgames"));

    /**
     * Nodes read from bundled configuration rather than from a Java string literal.
     */
    private static final Set<String> CONFIG_DRIVEN_NODES = new HashSet<>(
            Arrays.asList("buildbattle.heads", "buildbattle.updatenotify"));

    /**
     * Nodes that were declared historically but that nothing has ever read. Each
     * one has a working counterpart, so re-adding them only misleads whoever grants
     * them.
     */
    private static final Map<String, String> DEAD_NODES = Map.of("buildbattle.admin.stopgame", "buildbattle.admin.stop",
            "buildbattle.admin.plotwand", "buildbattle.admin.locwand", "buildbattle.admin.create",
            "buildbattle.admin.setup", "buildbattle.command.bypass", "buildbattle.command.override",
            "buildbattle.admin.addsign", "buildbattle.admin.sign.create", "buildbattle.admin.forcestart.theme",
            "buildbattle.admin.forcestart", "buildbattle.admin.supervotes.manage",
            "nothing - the feature does not exist");

    @SuppressWarnings("unchecked")
    private static Set<String> declaredNodes() throws IOException {

        try (InputStream in = Files.newInputStream(PLUGIN_YML)) {

            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> permissions = (Map<String, Object>) root.get("permissions");
            Set<String> declared = new TreeSet<>();
            for (Map.Entry<String, Object> entry : permissions.entrySet()) {

                declared.add(entry.getKey());
                Object body = entry.getValue();
                if (body instanceof Map) {

                    Object children = ((Map<String, Object>) body).get("children");
                    if (children instanceof Map) {

                        declared.addAll(((Map<String, Object>) children).keySet());

                    }

                }

            }

            return declared;

        }

    }

    private static Set<String> nodesCheckedInSource() throws IOException {

        try (Stream<Path> files = Files.walk(JAVA_SOURCES)) {

            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
            Set<String> found = new TreeSet<>();
            for (Path file : javaFiles) {

                Matcher matcher = OWN_PERMISSION.matcher(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
                while (matcher.find()) {

                    found.add(matcher.group(1));

                }

            }

            return found;

        }

    }

    @Test
    @DisplayName("every permission the code checks is declared in plugin.yml")
    void allCheckedNodesAreDeclared() throws IOException {

        Set<String> declared = declaredNodes();
        Set<String> checked = nodesCheckedInSource();

        assertFalse(checked.isEmpty(), "found no permission literals at all - the scanner is broken");

        // The source scan only sees this fork's own literals. Nodes the shaded
        // MiniGamesBox checks
        // have no literal here at all, so they must be asserted explicitly or they rot
        // unnoticed.
        Set<String> mustBeDeclared = new TreeSet<>(checked);
        mustBeDeclared.addAll(INHERITED_PLAYER_NODES);

        Set<String> undeclared = new TreeSet<>(mustBeDeclared);
        undeclared.removeAll(declared);
        assertTrue(undeclared.isEmpty(),
                "these permissions are checked in src/main/java but missing from plugin.yml, so "
                        + "buildbattle.admin.* will not grant them: " + undeclared);

    }

    @Test
    @DisplayName("plugin.yml declares no permission that nothing reads")
    void noDeclaredNodeIsDead() throws IOException {

        Set<String> declared = declaredNodes();
        Set<String> live = new TreeSet<>(nodesCheckedInSource());
        live.addAll(INHERITED_NODES);
        live.addAll(INHERITED_PLAYER_NODES);
        live.addAll(CONFIG_DRIVEN_NODES);

        Set<String> unread = new TreeSet<>(declared);
        unread.removeAll(live);
        // The wildcard parent is a Bukkit construct, not something the plugin resolves.
        unread.removeIf(node -> node.endsWith(".*"));

        assertTrue(unread.isEmpty(), "these permissions are declared but nothing reads them; granting one "
                + "silently does nothing: " + unread);

    }

    @Test
    @DisplayName("the specific nodes removed in 5.1.5 do not come back")
    void knownDeadNodesStayRemoved() throws IOException {

        Set<String> declared = declaredNodes();
        for (Map.Entry<String, String> dead : DEAD_NODES.entrySet()) {

            assertFalse(declared.contains(dead.getKey()),
                    dead.getKey() + " is declared again, but nothing reads it. Use " + dead.getValue() + " instead.");

        }

    }

}
