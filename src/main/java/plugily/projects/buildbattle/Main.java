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

import java.util.UUID;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.TestOnly;
import plugily.projects.buildbattle.arena.ArenaEvents;
import plugily.projects.buildbattle.arena.ArenaManager;
import plugily.projects.buildbattle.arena.ArenaRegistry;
import plugily.projects.buildbattle.arena.BaseArena;
import plugily.projects.buildbattle.arena.managers.plots.PlotMenuHandler;
import plugily.projects.buildbattle.arena.vote.VoteEvents;
import plugily.projects.buildbattle.arena.vote.VoteItems;
import plugily.projects.buildbattle.boot.AdditionalValueInitializer;
import plugily.projects.buildbattle.boot.MessageInitializer;
import plugily.projects.buildbattle.boot.PlaceholderInitializer;
import plugily.projects.buildbattle.commands.HubCommand;
import plugily.projects.buildbattle.commands.arguments.ArgumentsRegistry;
import plugily.projects.buildbattle.handlers.LanguageMigrator;
import plugily.projects.buildbattle.handlers.language.TrueOGLanguageManager;
import plugily.projects.buildbattle.handlers.menu.OptionsRegistry;
import plugily.projects.buildbattle.handlers.misc.BlacklistManager;
import plugily.projects.buildbattle.handlers.misc.BuilderCreativeManager;
import plugily.projects.buildbattle.handlers.misc.HeadDatabaseManager;
import plugily.projects.buildbattle.handlers.misc.MyWorldsManager;
import plugily.projects.buildbattle.handlers.misc.PreJoinLocationStore;
import plugily.projects.buildbattle.handlers.misc.ReconnectToMainWorldListener;
import plugily.projects.buildbattle.handlers.setup.SetupCategoryManager;
import plugily.projects.buildbattle.handlers.themes.ThemeManager;
import plugily.projects.minigamesbox.api.handlers.language.ILanguageManager;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.handlers.setup.SetupInventory;
import plugily.projects.minigamesbox.classic.handlers.setup.categories.PluginSetupCategoryManager;

/**
 * Created by Tom on 17/08/2015. Updated by Tigerpanzer_02 on 03.12.2021
 */
public class Main extends PluginMain {

    private VoteItems voteItems;
    private HeadDatabaseManager headDatabaseManager;
    private ThemeManager themeManager;
    private BlacklistManager blacklistManager;
    private OptionsRegistry optionsRegistry;
    private ArenaRegistry arenaRegistry;
    private ArenaManager arenaManager;
    private ArgumentsRegistry argumentsRegistry;
    private PlotMenuHandler plotMenuHandler;
    private MyWorldsManager myWorldsManager;
    private PreJoinLocationStore preJoinLocationStore;
    private BuilderCreativeManager builderCreativeManager;
    private ILanguageManager languageManager;

    @TestOnly
    public Main() {

        super();

    }

    @Override
    public void onEnable() {

        long start = System.currentTimeMillis();
        installDebugNoiseFilter();
        // Write bundled powerups.yml to the data folder so the shaded
        // MiniGamesBox-Classic reader finds it instead of logging
        // "File powerups.yml does not exist!".
        if (!new java.io.File(getDataFolder(), "powerups.yml").exists()) {

            saveResource("powerups.yml", false);

        }

        new LanguageMigrator(this);
        MessageInitializer messageInitializer = new MessageInitializer(this);
        // Must precede super.onEnable(): SignManager caches Signs.Lines and the
        // game-state names into
        // final fields while the base plugin enables, and never re-reads them (not even
        // on /bba reload).
        installTrueOGColorizer();
        super.onEnable();
        getDebugger().debug("[System] [Plugin] Initialization start");
        arenaRegistry = new ArenaRegistry(this);
        myWorldsManager = new MyWorldsManager(this);
        if (!myWorldsManager.initialize()) {

            // super.onEnable() already ran, so a bare return would leave the plugin marked
            // enabled with no arenas, commands or listeners registered. Fail fast instead.
            // Note this runs onDisable() re-entrantly, inside onEnable(). That is safe
            // today only
            // because registerArenas() has not run yet, so onDisable()'s loop over the
            // arena
            // registry is empty and never dereferences the still-null arenaManager. If
            // arenas ever
            // become populated earlier, move this check above super.onEnable() instead.
            getServer().getPluginManager().disablePlugin(this);
            return;

        }

        preJoinLocationStore = new PreJoinLocationStore(this);
        new PlaceholderInitializer(this);
        messageInitializer.registerMessages();
        new AdditionalValueInitializer(this);
        initializePluginClasses();
        getDebugger().debug("Full {0} plugin enabled", getName());
        getDebugger().debug("[System] [Plugin] Initialization finished took {0}ms", System.currentTimeMillis() - start);

    }

    // Route language values through TrueOG's colorizer when Utilities-OG is
    // installed. It is a
    // compileOnly dependency, so only build the decorator once its classes are
    // known present.
    // Every plugin is loaded before any is enabled, so this check is already valid
    // pre-onEnable.
    private void installTrueOGColorizer() {

        if (getServer().getPluginManager().getPlugin("Utilities-OG") == null) {

            getLogger().info("Utilities-OG is not installed, so messages keep their default formatting.");
            return;

        }

        // Delegate resolved lazily: PluginMain has not built its LanguageManager yet at
        // this point.
        languageManager = new TrueOGLanguageManager(super::getLanguageManager);

    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Returns the colorizing decorator once {@link #installTrueOGColorizer()} has
     * run, which is before {@code super.onEnable()} so that values the base plugin
     * caches at startup (join sign lines, game-state names) are already colorized.
     */
    @Override
    public ILanguageManager getLanguageManager() {

        return languageManager != null ? languageManager : super.getLanguageManager();

    }

    // Suppress known non-actionable startup noise from shaded MiniGamesBox-Classic.
    private void installDebugNoiseFilter() {

        java.util.logging.Logger jul = java.util.logging.Logger.getLogger(getName());
        Filter previous = jul.getFilter();
        jul.setFilter(new Filter() {

            @Override
            public boolean isLoggable(LogRecord record) {

                String msg = record.getMessage();
                // Raw locale records contain color codes and concatenated values,
                // so match them by substring rather than prefix.
                if (msg != null && msg.contains("Loaded locale"))
                    return false;
                return previous == null || previous.isLoggable(record);

            }

        });

    }

    public void initializePluginClasses() {

        addFileName("themes");
        addFileName("vote_items");
        blacklistManager = new BlacklistManager(this);
        headDatabaseManager = new HeadDatabaseManager(this);
        themeManager = new ThemeManager(this);
        BaseArena.init(this);
        new ArenaEvents(this);
        arenaManager = new ArenaManager(this);
        builderCreativeManager = new BuilderCreativeManager(this);
        arenaRegistry.registerArenas();
        new ReconnectToMainWorldListener(this);
        PluginCommand hubCommand = getCommand("hub");
        if (hubCommand != null) {

            hubCommand.setExecutor(new HubCommand(this));

        } else {

            getLogger().warning("The 'hub' command is missing from plugin.yml, so /hub will not work.");

        }

        myWorldsManager.synchronizeArenaWorldInventories();
        getSignManager().loadSigns();
        getSignManager().updateSigns();
        argumentsRegistry = new ArgumentsRegistry(this);
        voteItems = new VoteItems(this);
        new VoteEvents(this);
        plotMenuHandler = new PlotMenuHandler(this);
        optionsRegistry = new OptionsRegistry(this);
        optionsRegistry.registerOptions();

    }

    public VoteItems getVoteItems() {

        return voteItems;

    }

    public HeadDatabaseManager getHeadDatabaseManager() {

        return headDatabaseManager;

    }

    public ThemeManager getThemeManager() {

        return themeManager;

    }

    public BlacklistManager getBlacklistManager() {

        return blacklistManager;

    }

    public OptionsRegistry getOptionsRegistry() {

        return optionsRegistry;

    }

    public PlotMenuHandler getPlotMenuHandler() {

        return plotMenuHandler;

    }

    public MyWorldsManager getMyWorldsManager() {

        return myWorldsManager;

    }

    public BuilderCreativeManager getBuilderCreativeManager() {

        return builderCreativeManager;

    }

    public void savePreJoinLocation(Player player) {

        if (preJoinLocationStore != null) {

            preJoinLocationStore.put(player.getUniqueId(), player.getLocation());

        }

    }

    public Location getAndRemovePreJoinLocation(UUID playerId) {

        if (preJoinLocationStore == null) {

            return null;

        }

        // Pulls the stored world back in through MyWorlds when it is unloaded.
        Location location = preJoinLocationStore.getOrLoadWorld(playerId);
        preJoinLocationStore.remove(playerId);
        return location;

    }

    @Override
    public void onDisable() {

        if (builderCreativeManager != null) {

            builderCreativeManager.revokeAll();

        }

        if (preJoinLocationStore != null) {

            preJoinLocationStore.shutdown();

        }

        super.onDisable();

    }

    @Override
    public ArenaRegistry getArenaRegistry() {

        return arenaRegistry;

    }

    @Override
    public ArenaManager getArenaManager() {

        return arenaManager;

    }

    @Override
    public ArgumentsRegistry getArgumentsRegistry() {

        return argumentsRegistry;

    }

    @Override
    public PluginSetupCategoryManager getSetupCategoryManager(SetupInventory setupInventory) {

        return new SetupCategoryManager(setupInventory);

    }

}
