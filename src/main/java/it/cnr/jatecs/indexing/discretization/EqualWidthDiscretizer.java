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

import java.util.TreeSet;

/**
 * A discretizer that divide the values space into an equal size number of bins.
 *
 * @author Tiziano Fagni
 */
public class EqualWidthDiscretizer implements IValuesDiscretizer {

    private int numBins;

    public EqualWidthDiscretizer() {
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
    public TreeSet<DiscreteBin> discretizeValues(double[] values) {

        TreeSet<DiscreteBin> bins = new TreeSet<DiscreteBin>();

        double minValue = Double.MAX_VALUE;
        double maxValue = -Double.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (values[i] < minValue)
                minValue = values[i];
            if (values[i] > maxValue)
                maxValue = values[i];
        }

        double intervalStep = (maxValue - minValue) / getNumBins();
        for (int i = 0; i < getNumBins(); i++) {
            DiscreteBin bin = new DiscreteBin(minValue + (i * intervalStep), minValue + ((i + 1) * intervalStep));
            bins.add(bin);
        }

        return bins;
    }

}
