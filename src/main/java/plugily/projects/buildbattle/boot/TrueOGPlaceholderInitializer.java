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

package plugily.projects.buildbattle.boot;

import org.bukkit.entity.Player;
import net.trueog.utilitiesog.UtilitiesOG;
import plugily.projects.buildbattle.handlers.stats.BuildBattleScores;

// Registers the bb_score and bb_rank MiniPlaceholders through Utilities-OG.
// Only constructed when Utilities-OG is installed, so its classes stay unloaded otherwise.
public class TrueOGPlaceholderInitializer {

    public TrueOGPlaceholderInitializer(BuildBattleScores scores) {

        UtilitiesOG.registerAudiencePlaceholder("bb_score",
                (Player player) -> String.valueOf(scores.getPoints(player.getUniqueId())));
        UtilitiesOG.registerAudiencePlaceholder("bb_rank",
                (Player player) -> scores.getRank(player.getUniqueId()).getDisplay());

    }

}
