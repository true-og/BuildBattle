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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the startup ordering that makes the colorizer reach everything.
 *
 * <p>
 * {@code SignManager} reads {@code Signs.Lines} and the game-state names into
 * final fields while the base plugin enables, and never re-reads them — not
 * even on {@code /bba reload}. So if the colorizer is installed after
 * {@code super.onEnable()}, join signs are permanently stuck on uncolorized
 * text while every other surface works. Nothing fails loudly when that happens,
 * which is exactly why it needs a test.
 *
 * <p>
 * This asserts against the source because the invariant is a call ordering
 * inside {@code onEnable()}, and standing up {@code PluginMain} far enough to
 * observe it would need a live server. That makes it a canary, not a proof: it
 * catches the call being moved or the eager delegate coming back, but it would
 * still pass if the call were made unreachable, and it is coupled to the two
 * method names. A behavioural test with MockBukkit asserting that
 * {@code SignManager}'s cached lines are colorized would be strictly better if
 * the harness is ever stood up.
 */
class MainStartupOrderTest {

    private static final Path MAIN_SOURCE = Paths.get("src/main/java/plugily/projects/buildbattle/Main.java");

    /**
     * Strips block and line comments so prose mentioning a call is not mistaken for
     * the call.
     */
    private static String sourceWithoutComments() throws IOException {

        String source = new String(Files.readAllBytes(MAIN_SOURCE), StandardCharsets.UTF_8);
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");

    }

    @Test
    @DisplayName("the colorizer is installed before super.onEnable()")
    void colorizerIsInstalledBeforeBasePluginEnables() throws IOException {

        String source = sourceWithoutComments();

        int install = source.indexOf("installTrueOGColorizer();");
        int superEnable = source.indexOf("super.onEnable();");

        assertTrue(install >= 0, "installTrueOGColorizer() is no longer called from Main");
        assertTrue(superEnable >= 0, "super.onEnable() is no longer called from Main");
        assertTrue(install < superEnable,
                "installTrueOGColorizer() must run before super.onEnable(), otherwise SignManager "
                        + "caches uncolorized sign lines and game-state names for the lifetime of the server");

    }

    @Test
    @DisplayName("the decorator is handed a lazy delegate, not a resolved one")
    void colorizerReceivesALazyDelegate() throws IOException {

        String source = sourceWithoutComments();

        // Installing before super.onEnable() is only safe because the delegate is
        // resolved per call:
        // PluginMain has not built its LanguageManager yet at that point, so eagerly
        // passing
        // super.getLanguageManager() would capture null forever.
        assertTrue(source.contains("new TrueOGLanguageManager(super::getLanguageManager)"),
                "the decorator must be constructed with the super::getLanguageManager method reference "
                        + "so the delegate resolves lazily; passing super.getLanguageManager() would capture null");

    }

}
