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

public class EqualFrequencyDiscretizer implements IValuesDiscretizer {

    private int numBins;

    public EqualFrequencyDiscretizer() {
        numBins = 10;
    }

    /**
     * Get the number of discrete bins to generate.
     *
     * @return The number of discrete bins to generate.
     */
    public int getNumBins() {
        return numBins;
    }

    /**
     * Set the number of discrete bins to generate.
     *
     * @param numBins The number of bins to generate.
     */
    public void setNumBins(int numBins) {
        this.numBins = numBins;
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

        assert (values[values.length - 1] <= 1);

        TreeSet<DiscreteBin> bins = new TreeSet<DiscreteBin>();

        int numBins = getNumBins();
        if (numBins >= values.length) {
            // DiscreteBin db = new DiscreteBin(values[0],
            // values[values.length-1]);
            // bins.add(db);
            // return bins;
            numBins = values.length - 1;
            if (numBins == 0) {
                DiscreteBin db = new DiscreteBin(values[0],
                        values[values.length - 1]);
                bins.add(db);
                return bins;
            }
        }

        double lastVal = values[0];
        int step = values.length / numBins;
        for (int i = 0; i < values.length; i += step) {
            if ((i + step) < values.length) {
                DiscreteBin db = new DiscreteBin(lastVal, values[i + step]);
                bins.add(db);
                lastVal = values[i + step];
            }
        }

        if (values.length % numBins != 0) {
            double startValue = bins.last().getStartValue();
            double endValue = values[values.length - 1];
            DiscreteBin db = new DiscreteBin(startValue, endValue);
            bins.remove(bins.last());
            bins.add(db);
        }

        if (bins.size() == 0) {
            DiscreteBin db = new DiscreteBin(values[0],
                    values[values.length - 1]);
            bins.add(db);
        }

        return bins;
    }

}
