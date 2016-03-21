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

package it.cnr.jatecs.classification.adaboost;

/**
 * Decision stump weak hypothesis
 *
 * @author Andrea Esuli
 */
public class HypothesisData implements Comparable<HypothesisData> {
    /**
     * The pivot (feature) real ID.
     */
    public int pivot;

    /**
     * The C0 negative value.
     */
    public double c0;

    /**
     * The C1 positive value.
     */
    public double c1;

    /**
     * This comparison method sorts HypothesisData by their pivot
     */
    public int compareTo(HypothesisData arg0) {
        return pivot - arg0.pivot;
    }

    public boolean equals(Object aThat) {
        if (this == aThat)
            return true;

        if (!(aThat instanceof HypothesisData))
            return false;
        HypothesisData that = (HypothesisData) aThat;
        return pivot == that.pivot;
    }

    @Override
    public int hashCode() {
        return new Integer(pivot).hashCode();
    }
}
