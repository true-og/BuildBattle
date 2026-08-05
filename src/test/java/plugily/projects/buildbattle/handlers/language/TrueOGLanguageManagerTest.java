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

package plugily.projects.buildbattle.handlers.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import plugily.projects.minigamesbox.api.handlers.language.ILanguageManager;
import plugily.projects.minigamesbox.api.utils.services.locale.ILocale;

/**
 * Covers {@link TrueOGLanguageManager}'s string transformation, which is pure
 * and therefore fully testable without a running server.
 *
 * <p>
 * The regression cases here are not hypothetical. An earlier revision
 * round-tripped every value through MiniMessage unconditionally, which silently
 * blanked the bare colour codes that define the plugin's colour scheme and
 * leaked decorations across colour boundaries in the rest of the file.
 */
class TrueOGLanguageManagerTest {

    /**
     * Stub delegate: echoes back whatever it was asked for, so tests control the
     * raw input.
     */
    private static final class EchoLanguageManager implements ILanguageManager {

        private final List<String> list;
        private boolean reloaded;

        EchoLanguageManager() {

            this(Collections.emptyList());

        }

        EchoLanguageManager(List<String> list) {

            this.list = list;

        }

        @Override
        public void setupLocale() {

            // Nothing to set up in tests.

        }

        @Override
        public boolean isDefaultLanguageUsed() {

            return true;

        }

        @Override
        public String getLanguageMessage(String message) {

            return message;

        }

        @Override
        public List<String> getLanguageList(String message) {

            return list;

        }

        @Override
        public List<String> getLanguageListFromKey(String key) {

            return list;

        }

        @Override
        public void reloadLanguage() {

            reloaded = true;

        }

        @Override
        public ILocale getPluginLocale() {

            return null;

        }

    }

    private static TrueOGLanguageManager decorating(ILanguageManager delegate) {

        return new TrueOGLanguageManager(() -> delegate);

    }

    private static String render(String raw) {

        return decorating(new EchoLanguageManager()).getLanguageMessage(raw);

    }

    // --- Regression: bare colour codes must survive
    // ------------------------------------------

    /**
     * These are the seven COLOR_* values in language.yml that MessageBuilder reads
     * on every construction. Blanking any of them strips colour from essentially
     * every message the plugin sends, so this is the highest-value assertion in the
     * file.
     */
    @ParameterizedTest(name = "bare colour code {0} survives")
    @ValueSource(strings =
    { "&b", "&a", "&7", "&c", "&8", "&6", "&r", "&l", "&k", "&o" })
    void bareColourCodesArePreserved(String code) {

        assertEquals(code, render(code));

    }

    // --- Regression: no decoration bleed
    // -----------------------------------------------------

    @Test
    @DisplayName("plain legacy text is returned byte-for-byte, so decorations cannot bleed")
    void plainLegacyTextIsUntouched() {

        // A legacy colour code resets decorations; a MiniMessage colour tag does not.
        // Round-tripping
        // this string used to move &l past the second &8 and embolden the trailing
        // separator.
        String raw = "&8+-------+ &b&lYOUR STATS &8+-------+";
        assertEquals(raw, render(raw));

        String prefixed = "[&7[&6&lBB&7] &r&fmessage]";
        assertEquals(prefixed, render(prefixed));

    }

    @Test
    @DisplayName("identical input is returned as the same instance when no colorizing is needed")
    void untouchedValuesAreNotCopied() {

        String raw = "&7plain text";
        assertSame(raw, render(raw));

    }

    // --- Regression: hex markers
    // -------------------------------------------------------------

    @ParameterizedTest(name = "pre-colorized hex {0} is left alone when nothing needs colorizing")
    @ValueSource(strings =
    { "§x§f§f§0§0§a§aLower", "§x§F§F§0§0§A§AUpperDigits", "§X§f§f§0§0§a§aUpperMarker" })
    void preColorizedHexIsNotCorrupted(String raw) {

        // Language lists arrive already colorized by MiniGamesBox, so these reach the
        // decorator.
        // With no MiniMessage tag present these short-circuit; the routed case is
        // covered below.
        assertEquals(raw, render(raw));

    }

    /**
     * The cases above never reach {@code normalizeLegacyHex}, because a value that
     * is only legacy text short-circuits. Pre-colorized hex only meets the
     * normalizer when the same value also carries a MiniMessage tag — which is the
     * one situation where an unnormalized {@code §x} marker would be shredded into
     * a single colour code. These inputs therefore exercise it for real.
     */
    @ParameterizedTest(name = "routed value keeps its pre-colorized hex prefix: {0}")
    @ValueSource(strings =
    { "§x§f§f§0§0§a§aHex <red>tail", "§x§F§F§0§0§A§AHex <red>tail", "§X§f§f§0§0§a§aHex <red>tail" })
    void preColorizedHexSurvivesWhenTheValueIsRouted(String raw) {

        String rendered = render(raw);
        assertNotEquals(raw, rendered, "expected this value to be routed through the colorizer");
        assertTrue(rendered.toLowerCase(Locale.ROOT).startsWith("§x§f§f§0§0§a§a"),
                "the pre-colorized hex prefix was corrupted, got: " + rendered);
        assertTrue(rendered.contains("Hex"), "the text was lost, got: " + rendered);

    }

    // --- TrueOG constructs actually render
    // ---------------------------------------------------

    // These assert only that the decorator ROUTES such values to the colorizer and
    // does not lose
    // their text. What the colorizer turns a tag into is Utilities-OG's contract,
    // not this class's,
    // so no exact output is pinned here.

    @ParameterizedTest(name = "{0} is routed to the colorizer")
    @ValueSource(strings =
    { "<#ff00aa>Hex text", "<gradient:red:blue>Gradient</gradient>", "&*Rainbow", "<rainbow>Rainbow tag",
            "<red>Named colour", "<dark_aqua>Underscored colour", "<color:red>Colour form", "<bold>Decoration",
            "<b>Decoration shorthand", "<!bold>Negated decoration", "<reset>After reset", "<bold:false>Boolean arg",
            "<italic:true>Boolean arg" })
    void trueOgConstructsAreRouted(String raw) {

        String rendered = render(raw);
        assertNotEquals(raw, rendered, "expected the construct to be consumed, got: " + rendered);

    }

    /**
     * The mirror of the test above, and the reason tag detection is a whitelist.
     * language.yml is full of literal placeholders like {@code <arena>}; if those
     * were treated as MiniMessage tags the whole value would be round-tripped, and
     * legacy decorations would bleed past the next colour code exactly as they did
     * before the short-circuit existed.
     */
    /**
     * The bleed case for values that ARE routed. A legacy colour code clears
     * decorations while a MiniMessage colour tag does not, so without an explicit
     * reset the {@code &l} would carry past the closing {@code &8} and onto the
     * tagged text.
     */
    @ParameterizedTest(name = "decorations do not bleed past a colour code in {0}")
    @ValueSource(strings =
    { "&8[&b&lBB&8] <red>x", "&8[&b&lBB&8] <#ff00aa>x", "&8[&b&lBB&8] &*x" })
    void decorationsDoNotBleedInRoutedValues(String raw) {

        String rendered = render(raw);
        int closingBracket = rendered.indexOf(']');
        assertTrue(closingBracket > 0, "expected the literal ] to survive, got: " + rendered);
        // §l immediately before the ] means bold leaked past the &8 that should have
        // cleared it.
        assertNotEquals('l', rendered.charAt(closingBracket - 1),
                "a decoration bled past the colour code, got: " + rendered);
        assertFalse(rendered.endsWith("§lx"), "a decoration bled onto the tagged text, got: " + rendered);

    }

    @ParameterizedTest(name = "{0} is left alone")
    @ValueSource(strings =
    { "&6Usage: /bb help <arena> <statistic>", "&8[&b&lBB&8] <arena>", "&7value <= 10 and >= 2",
            "&7a <plot> and a <theme>" })
    void nonMiniMessageAngleBracketsAreNotRouted(String raw) {

        assertEquals(raw, render(raw), "a literal placeholder was mistaken for a MiniMessage tag");

    }

    @Test
    @DisplayName("a style-only value does not come back empty")
    void styleOnlyTagIsNotBlanked() {

        // Adventure serializes a styled but text-less component to "". The sentinel
        // exists to stop
        // that, and this is the same failure mode that blanked the bare colour codes
        // above.
        assertFalse(render("<blue>").isEmpty(), "style-only value was blanked");

    }

    // --- Things that must pass through unharmed
    // ----------------------------------------------

    @Test
    @DisplayName("unknown angle-bracket tokens stay literal")
    void unknownTagsRemainLiteral() {

        // language.yml documents command usage with <arena> / <statistic> placeholders.
        String rendered = render("&6Usage: /bb help <arena> <statistic>");
        assertTrue(rendered.contains("<arena>"), "expected <arena> to stay literal, got: " + rendered);
        assertTrue(rendered.contains("<statistic>"), "expected <statistic> to stay literal, got: " + rendered);

    }

    @Test
    @DisplayName("placeholders survive colorizing so later substitution still works")
    void placeholdersSurvive() {

        String raw = "&aWelcome %player%, you have %value% points";
        assertEquals(raw, render(raw));

        String withTag = "<green>Welcome %player%, you have %value% points";
        String rendered = render(withTag);
        assertTrue(rendered.contains("%player%"), "expected %player% to survive, got: " + rendered);
        assertTrue(rendered.contains("%value%"), "expected %value% to survive, got: " + rendered);

    }

    @Test
    @DisplayName("null and empty values pass straight through")
    void nullAndEmptyArePassedThrough() {

        assertNull(render(null));
        assertEquals("", render(""));

    }

    // --- List handling and delegation
    // --------------------------------------------------------

    @Test
    @DisplayName("every element of a language list is colorized")
    void listsAreColorizedElementwise() {

        EchoLanguageManager delegate = new EchoLanguageManager(Arrays.asList("&7plain", "<#ff00aa>hex"));
        List<String> rendered = decorating(delegate).getLanguageList("ignored");

        assertEquals(2, rendered.size());
        assertEquals("&7plain", rendered.get(0), "plain elements must be left alone");
        assertNotEquals("<#ff00aa>hex", rendered.get(1), "tagged elements must be colorized");

    }

    @Test
    @DisplayName("reloadLanguage reaches the delegate")
    void reloadIsDelegated() {

        EchoLanguageManager delegate = new EchoLanguageManager();
        assertFalse(delegate.reloaded);

        decorating(delegate).reloadLanguage();

        assertTrue(delegate.reloaded);

    }

    @Test
    @DisplayName("the delegate is resolved per call, so it can arrive after construction")
    void delegateIsResolvedLazily() {

        // This is what lets Main install the decorator before super.onEnable():
        // PluginMain has not
        // built its LanguageManager yet, so a delegate captured at construction would
        // be null forever
        // and every join sign would render uncolorized.
        AtomicReference<ILanguageManager> slot = new AtomicReference<>(null);
        TrueOGLanguageManager manager = new TrueOGLanguageManager(slot::get);

        assertEquals("KEY", manager.getLanguageMessage("KEY"));

        slot.set(new EchoLanguageManager());

        assertFalse(manager.getLanguageMessage("<blue>").isEmpty(), "delegate that arrived later was not picked up");

    }

    @Test
    @DisplayName("a not-yet-available delegate degrades instead of throwing")
    void nullDelegateIsTolerated() {

        // The decorator is installed before super.onEnable(), so PluginMain has not
        // built its
        // LanguageManager yet and the supplier legitimately returns null for a window
        // at startup.
        TrueOGLanguageManager manager = new TrueOGLanguageManager(() -> null);

        assertEquals("KEY", manager.getLanguageMessage("KEY"));
        assertTrue(manager.getLanguageList("KEY").isEmpty());
        assertTrue(manager.getLanguageListFromKey("KEY").isEmpty());
        assertNull(manager.getPluginLocale());
        assertFalse(manager.isDefaultLanguageUsed());
        manager.reloadLanguage();
        manager.setupLocale();

    }

}
