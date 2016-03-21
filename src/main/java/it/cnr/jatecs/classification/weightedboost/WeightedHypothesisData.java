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

package it.cnr.jatecs.classification.weightedboost;

public class WeightedHypothesisData implements Comparable<WeightedHypothesisData> {
    /**
     * The pivot (feature) real ID.
     */
    public int pivot;

    /**
     * The set of C1 constant values.
     */
    public double[] c1ConstantValues;

    /**
     * The C0 negative value.
     */
    public double c0;


    public WeightedHypothesisData(int numPositiveSteps) {
        c1ConstantValues = new double[numPositiveSteps];
    }


    /**
     * This comparison method, sort HypothesisData by their pivot
     */
    public int compareTo(WeightedHypothesisData arg0) {
        return pivot - arg0.pivot;
    }

    public boolean equals(Object aThat) {
        if (this == aThat)
            return true;

        if (!(aThat instanceof WeightedHypothesisData))
            return false;
        WeightedHypothesisData that = (WeightedHypothesisData) aThat;
        return pivot == that.pivot;
    }


    @Override
    public int hashCode() {
        return new Integer(pivot).hashCode();
    }
}
