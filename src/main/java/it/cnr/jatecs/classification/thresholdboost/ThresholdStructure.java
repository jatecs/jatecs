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

package it.cnr.jatecs.classification.thresholdboost;

import it.cnr.jatecs.evaluation.ContingencyTable;

public class ThresholdStructure implements Comparable<ThresholdStructure> {

    public double threshold;
    public double efficiency;
    public ContingencyTable table;
    private long t = System.currentTimeMillis();

    public ThresholdStructure(long count) {
        t = count;
    }


    public int compareTo(ThresholdStructure o) {
        if (o.efficiency > efficiency)
            return 1;
        else if (o.efficiency == efficiency) {
            if (o.threshold > threshold)
                return 1;
            else if (o.threshold < threshold)
                return -1;
            else {
                if (o.t > t)
                    return 1;
                else if (o.t < t)
                    return -1;
                else {
                    System.out.println("Non è possibile");
                    return 0;
                }
            }
        } else {
            return -1;
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ThresholdStructure))
            return false;

        ThresholdStructure s = (ThresholdStructure) obj;
        return (compareTo(s) == 0);
    }


}
