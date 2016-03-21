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

package apps.trainingDataCleaning;

public class IdScorePair implements Comparable<IdScorePair> {

    private double score;
    private int id;

    public IdScorePair(int id, double score) {
        this.id = id;
        this.score = score;
    }

    public int Id() {
        return id;
    }

    public double Score() {
        return score;
    }

    public int compareTo(IdScorePair arg0) {
        double delta = score - arg0.score;
        if (delta < 0.0)
            return -1;
        if (delta > 0.0)
            return 1;
        else
            return 0;
    }

}
