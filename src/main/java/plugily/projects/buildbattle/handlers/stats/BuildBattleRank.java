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

// Builder-themed rank ladder over POINTS_TOTAL that borrows from Inception up top.
// Dreamweaver is reserved for the top scorer.
public enum BuildBattleRank {

    APPRENTICE("&7Apprentice", 0), BUILDER("&fBuilder", 100), BRICKLAYER("&eBricklayer", 300),
    CARPENTER("&6Carpenter", 750), MASON("&bMason", 1500), CRAFTSMAN("&3Craftsman", 3000),
    ARCHITECT("&aArchitect", 6000), ENGINEER("&2Engineer", 10000), SCULPTOR("&dSculptor", 15000),
    MASTER_BUILDER("&6&lMaster Builder", 25000), FORGER("&5Forger", 40000), DREAMER("&bDreamer", 60000),
    EXTRACTOR("&c&lExtractor", 100000), DOM("&d&lDom", 150000),
    DREAMWEAVER("&4&k# &r&c&lDreamweaver", Integer.MAX_VALUE);

    private final String display;
    private final int lowBound;

    BuildBattleRank(String display, int lowBound) {

        this.display = display;
        this.lowBound = lowBound;

    }

    public String getDisplay() {

        return display;

    }

    public int getLowBound() {

        return lowBound;

    }

    // The reserved rank needs the top score and the highest regular rank, so #1
    // still climbs the ladder.
    public static int topPlayerGate() {

        int gate = 0;
        for (BuildBattleRank rank : values()) {

            if (rank != DREAMWEAVER) {

                gate = Math.max(gate, rank.getLowBound());

            }

        }

        return gate;

    }

    // Highest ladder rank whose threshold the points meet; never the reserved one.
    public static BuildBattleRank findRank(int points) {

        BuildBattleRank[] ranks = values();
        for (int i = ranks.length - 1; i >= 0; i--) {

            if (ranks[i] == DREAMWEAVER) {

                continue;

            }

            if (points >= ranks[i].getLowBound()) {

                return ranks[i];

            }

        }

        return APPRENTICE;

    }

}
