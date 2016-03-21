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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

public class ChiMergeDiscretizer implements IValuesDiscretizer {

    private IStatsProvider statsProvider;

    private double stopCondition;
    private int minNumBins;
    private int maxNumBins;

    public ChiMergeDiscretizer(IStatsProvider statsProvider) {
        if (statsProvider == null)
            throw new NullPointerException("The stats provider is 'null'");

        this.statsProvider = statsProvider;

        // Should be computed dinamically.
        // See
        // http://www.unich.it/med/papers/Metodologia%20Medico-Scientifica%20di%20base/statistica/lezione%20-%20Il%20chi-quadrato.pdf
        // For
        this.stopCondition = 0.90;
        minNumBins = 1;
        maxNumBins = 20;
    }

    public int getMinNumBins() {
        return minNumBins;
    }

    public void setMinNumBins(int minNumBins) {
        this.minNumBins = minNumBins;
    }

    public int getMaxNumBins() {
        return maxNumBins;
    }

    public void setMaxNumBins(int maxNumBins) {
        this.maxNumBins = maxNumBins;
    }

    public IStatsProvider getStatsProvider() {
        return statsProvider;
    }

    public void setStatsProvider(IStatsProvider statsProvider) {
        this.statsProvider = statsProvider;
    }

    public double getStopCondition() {
        return stopCondition;
    }

    public void setStopCondition(double stopCondition) {
        this.stopCondition = stopCondition;
    }

    @Override
    public TreeSet<DiscreteBin> discretizeValues(double[] valuesUnordered) {
        if (valuesUnordered == null)
            throw new NullPointerException("The set of values is 'null'");

        if (valuesUnordered.length == 0)
            throw new IllegalArgumentException("The set of values is empty");

        TreeSet<DiscreteBin> bins = new TreeSet<DiscreteBin>();

        if (valuesUnordered.length == 1) {
            DiscreteBin db = new DiscreteBin(valuesUnordered[0],
                    valuesUnordered[0]);
            bins.add(db);
            return bins;
        }

        double[] values = Arrays
                .copyOf(valuesUnordered, valuesUnordered.length);
        Arrays.sort(values);
        assert (values[values.length - 1] <= 1);

        ArrayList<DiscreteBin> toProcess = new ArrayList<DiscreteBin>();
        for (int i = 1; i < values.length; i++) {
            DiscreteBin db = new DiscreteBin(values[i - 1], values[i]);
            toProcess.add(db);
        }

        int originalToProcess = toProcess.size();

        boolean done = false;
        while (!done) {
            double currentMinValue = Double.MAX_VALUE;
            int firstMinIndex = 0;
            for (int i = 0; i < toProcess.size() - 1; i++) {
                // Take 2 adjacent intervals and compute chi_square.
                double chi = computeChiSquare(toProcess.get(i),
                        toProcess.get(i + 1));
                if (chi < currentMinValue) {
                    currentMinValue = chi;
                    firstMinIndex = i;
                }
            }

            if (toProcess.size() == 1 || toProcess.size() <= getMinNumBins()) {
                done = true;
                continue;
            }

            double stopValue = 1 - ChiSquarePValueComputator.computePValue(
                    statsProvider.getNumOfClasses() - 1, currentMinValue);
            if (stopValue < getStopCondition()
                    && toProcess.size() <= getMaxNumBins()) {
                done = true;
                continue;
            }

            DiscreteBin db1 = toProcess.get(firstMinIndex);
            DiscreteBin db2 = toProcess.get(firstMinIndex + 1);
            toProcess.remove(firstMinIndex + 1);
            db1.setEndValue(db2.getEndValue());
        }

        if (originalToProcess > getMinNumBins()
                && toProcess.size() < getMinNumBins())
            assert (toProcess.size() >= getMinNumBins());
        assert (toProcess.size() <= getMaxNumBins());

        for (int i = 0; i < toProcess.size(); i++) {
            bins.add(toProcess.get(i));
        }

        return bins;
    }

    private double computeContributeForInterval(DiscreteBin discreteBin) {
        double sum = 0;
        int numExamplesInInterval = statsProvider
                .getNumExamplesInInterval(discreteBin);
        for (int j = 0; j < statsProvider.getNumOfClasses(); j++) {
            int numExamplesInIntervalClass = statsProvider
                    .getNumExamplesInIntervalClass(discreteBin, j);
            int numExamplesInClass = statsProvider.getNumExamplesInClass(j);
            double E_ij = (numExamplesInInterval * numExamplesInClass)
                    / (double) numExamplesInInterval;
            double contribute = 0;
            if (E_ij != 0 && !Double.isNaN(E_ij))
                contribute = Math.pow(numExamplesInIntervalClass - E_ij, 2)
                        / E_ij;
            if (Double.isNaN(contribute))
                assert (!Double.isNaN(contribute));
            sum += contribute;
        }

        return sum;
    }

    private double computeChiSquare(DiscreteBin discreteBin,
                                    DiscreteBin discreteBin2) {
        double sum = computeContributeForInterval(discreteBin)
                + computeContributeForInterval(discreteBin2);
        return sum;
    }
}
