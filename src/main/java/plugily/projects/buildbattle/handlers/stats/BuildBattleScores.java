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

package plugily.projects.buildbattle.handlers.stats;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import plugily.projects.buildbattle.Main;
import plugily.projects.minigamesbox.api.events.player.PlugilyPlayerStatisticChangeEvent;
import plugily.projects.minigamesbox.api.stats.IStatisticType;

// Plugin-wide POINTS_TOTAL cache behind the bb_score and bb_rank placeholders.
// Seeded from the stats backend, then kept live by MiniGamesBox's statistic change event.
public class BuildBattleScores implements Listener {

    private static final String POINTS_TOTAL = "POINTS_TOTAL";

    private final Main plugin;
    private final Map<UUID, Integer> points = new ConcurrentHashMap<>();
    private final IStatisticType pointsType;

    private volatile UUID topPlayer;
    private volatile int topPoints;

    public BuildBattleScores(Main plugin) {

        this.plugin = plugin;
        this.pointsType = plugin.getStatsStorage().getStatisticType(POINTS_TOTAL);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadAll();

    }

    public int getPoints(UUID uuid) {

        return points.getOrDefault(uuid, 0);

    }

    // Same rule as TheHerobrine-OG's Death Bringer: the reserved rank belongs to
    // the top scorer.
    public BuildBattleRank getRank(UUID uuid) {

        int score = getPoints(uuid);
        if (uuid.equals(topPlayer) && score >= BuildBattleRank.topPlayerGate()) {

            return BuildBattleRank.DREAMWEAVER;

        }

        return BuildBattleRank.findRank(score);

    }

    public UUID getTopPlayer() {

        return topPlayer;

    }

    // Fires on the main thread with the new total, both on load at join and after
    // every game.
    @EventHandler
    public void onStatisticChange(PlugilyPlayerStatisticChangeEvent event) {

        if (!isPointsTotal(event.getStatisticType())) {

            return;

        }

        UUID uuid = event.getPlayer().getUniqueId();
        points.put(uuid, Math.max(event.getNumber(), 0));
        bumpTop(uuid);

    }

    private boolean isPointsTotal(IStatisticType type) {

        if (type == null) {

            return false;

        }

        return type == pointsType || (pointsType != null && pointsType.getName().equals(type.getName()));

    }

    private void bumpTop(UUID uuid) {

        int score = getPoints(uuid);
        if (uuid.equals(topPlayer) || score > topPoints) {

            topPlayer = uuid;
            topPoints = score;

        }

    }

    // Full leaderboard read off the main thread; live values already cached win.
    private void loadAll() {

        if (pointsType == null) {

            plugin.getLogger().warning("POINTS_TOTAL is not registered, so bb_score and bb_rank start empty.");
            return;

        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            Map<UUID, Integer> stored = plugin.getStatsStorage().getStats(pointsType);
            if (stored == null) {

                return;

            }

            stored.forEach((uuid, value) -> {

                if (uuid == null || value == null) {

                    return;

                }

                points.putIfAbsent(uuid, Math.max(value, 0));
                bumpTop(uuid);

            });

        });

    }

}
