/*
 * This file is part of JaTeCS.
 *
 * JaTeCS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JaTeCS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JaTeCS.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The software has been mainly developed by (in alphabetical order):
 * - Andrea Esuli (andrea.esuli@isti.cnr.it)
 * - Tiziano Fagni (tiziano.fagni@isti.cnr.it)
 * - Alejandro Moreo Fernández (alejandro.moreo@isti.cnr.it)
 * Other past contributors were:
 * - Giacomo Berardi (giacomo.berardi@isti.cnr.it)
 */

package it.cnr.jatecs.indexing.tsr.ranks;

/**
 * Implements the Ylmaz Tau AP simetric measure (as the average of the two TauAP asimetric measures)
 *
 * @author Alejandro Moreo Fernandez (using Jatecs)
 */
public class YilmazTauAPsim extends RankSimilarityMethod {

    public YilmazTauAPsim(Rank rank1, Rank rank2) {
        this.rank1 = rank1;
        this.rank2 = rank2;
    }

    public static String getName() {
        return "TAUAPS";
    }

    @Override
    public double rankSimilarity() {
        YilmazTauAPasim sim_12 = new YilmazTauAPasim(rank1, rank2);
        YilmazTauAPasim sim_21 = new YilmazTauAPasim(rank2, rank1);

        double simRank_12 = sim_12.rankSimilarity();
        double simRank_21 = sim_21.rankSimilarity();

        return (simRank_12 + simRank_21) * 0.5;
    }
}
