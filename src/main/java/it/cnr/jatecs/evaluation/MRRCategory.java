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

package it.cnr.jatecs.evaluation;

public class MRRCategory {

    final private int maxNumRanks;
    private final int[] positions;
    private String name;

    public MRRCategory(int maxNumRanks) {
        this("unknown", maxNumRanks);
    }

    public MRRCategory(String name, int maxNumRanks) {
        this.setName(name);
        this.maxNumRanks = maxNumRanks;
        this.positions = new int[maxNumRanks + 1];
        for (int i = 0; i < maxNumRanks + 1; i++)
            this.positions[i] = 0;
    }

    public int getMaxNumRanks() {
        return maxNumRanks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    /**
     * @param rank Goes from 0 to "maxNumRanks" included (use this last one when a result is out
     *             of the first "maxNumRanks" results).
     */
    public void add1ToRank(int rank) {
        positions[rank] = positions[rank] + 1;
    }

    /**
     * @param rank Goes from 0 to "maxNumRanks" included (use this last one when a result is out
     *             of the first "maxNumRanks" results).
     */
    public void addNToRank(int rank, int n) {
        positions[rank] = positions[rank] + n;
    }


    public int getNumberOfRanked() {
        int sum = 0;
        for (int i = 0; i < maxNumRanks + 1; i++)
            sum += this.positions[i];
        return sum;
    }

    public int getRank(int rank) {
        return positions[rank];
    }


    public double getMRRValue() {
        double mrr = 0;
        for (int i = 0; i < maxNumRanks; i++) {
            mrr += (positions[i] * (1.0 / (i + 1)));
        }
        mrr /= getNumberOfRanked();
        return mrr;
    }
}
