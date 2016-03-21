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

package it.cnr.jatecs.indexing.discretization;

import java.util.Arrays;
import java.util.TreeSet;

/**
 * Implementation of Proportional k-Interval Discretization (PKID) as described
 * in http://www.csse.monash.edu/~webb/Files/YangWebb01.pdf.
 *
 * @author Tiziano Fagni
 */
public class PKIDDiscretizer implements IValuesDiscretizer {

    public PKIDDiscretizer() {
    }

    @Override
    public TreeSet<DiscreteBin> discretizeValues(double[] valuesUnordered) {

        if (valuesUnordered == null)
            throw new NullPointerException("The set of values is 'null'");

        if (valuesUnordered.length == 0)
            throw new IllegalArgumentException("The set of values is empty");

        double[] values = Arrays
                .copyOf(valuesUnordered, valuesUnordered.length);
        Arrays.sort(values);

        TreeSet<DiscreteBin> bins = new TreeSet<DiscreteBin>();

        int numBins = (int) Math.floor(Math.sqrt(values.length));
        int itemsInBins = numBins;
        double lastValue = values[0];
        int firstIdx = 0;
        int currentlyAdded = 0;
        for (int i = 1; i < values.length; i++) {
            if (currentlyAdded >= itemsInBins && lastValue != values[i]) {
                DiscreteBin db = new DiscreteBin(values[firstIdx],
                        values[i - 1]);
                bins.add(db);
                firstIdx = i - 1;
                currentlyAdded = 0;
            }

            currentlyAdded++;
            lastValue = values[i];
        }

        if (currentlyAdded > 0) {
            DiscreteBin db = new DiscreteBin(values[firstIdx],
                    values[values.length - 1]);
            bins.add(db);
        }

        if (bins.size() == 0) {
            DiscreteBin db = new DiscreteBin(values[0], values[0]);
            bins.add(db);
        }

        return bins;
    }
}
