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

import java.util.*;

public class InformationGainEntropyDiscretizer implements IValuesDiscretizer {

    private double threshold;
    private int maxNumPartitions;
    private boolean useThreshold;
    private IStatsProvider statsProvider;

    public InformationGainEntropyDiscretizer(IStatsProvider provider) {
        if (provider == null)
            throw new NullPointerException(
                    "The specified stats provider is 'null'");
        this.statsProvider = provider;
        threshold = 2;
        maxNumPartitions = 5;
        useThreshold = false;
    }

    public IStatsProvider getStatsProvider() {
        return statsProvider;
    }

    public void setStatsProvider(IStatsProvider statsProvider) {
        this.statsProvider = statsProvider;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public int getMaxNumPartitions() {
        return maxNumPartitions;
    }

    public void setMaxNumPartitions(int maxNumPartitions) {
        this.maxNumPartitions = maxNumPartitions;
    }

    public boolean isUseThreshold() {
        return useThreshold;
    }

    public void setUseThreshold(boolean useThreshold) {
        this.useThreshold = useThreshold;
    }

    @Override
    public TreeSet<DiscreteBin> discretizeValues(double[] valuesUnordered) {
        if (valuesUnordered == null)
            throw new NullPointerException("The set of values is 'null'");

        if (valuesUnordered.length == 0)
            throw new IllegalArgumentException("The set of values is empty");

        HashMap<Double, Double> mapValues = new HashMap<Double, Double>();
        for (int i = 0; i < valuesUnordered.length; i++) {
            if (mapValues.containsKey(valuesUnordered[i]))
                continue;
            mapValues.put(valuesUnordered[i], valuesUnordered[i]);
        }

        double[] valuesNoDuplicates = new double[mapValues.size()];
        Iterator<Double> it = mapValues.keySet().iterator();
        int cont = 0;
        while (it.hasNext()) {
            valuesNoDuplicates[cont] = it.next();
            cont++;
        }

        double[] values = Arrays
                .copyOf(valuesNoDuplicates, valuesNoDuplicates.length);
        Arrays.sort(values);

        TreeSet<DiscreteBin> bins = new TreeSet<DiscreteBin>();
        if (valuesNoDuplicates.length <= 3) {

            DiscreteBin db = new DiscreteBin(values[0],
                    values[values.length - 1]);
            bins.add(db);
            return bins;
        }


        ArrayList<Double> toProcess = new ArrayList<Double>(values.length);
        for (int i = 0; i < values.length; i++) {
            toProcess.add(values[i]);
        }

        // Compute recursive discretization.
        ArrayList<Double> splitPoints = new ArrayList<Double>();
        computeDiscretization(toProcess, splitPoints);

        if (splitPoints.size() == 0) {
            DiscreteBin db = new DiscreteBin(values[0],
                    values[values.length - 1]);
            bins.add(db);
            return bins;
        }

        // Sort split points.
        double[] points = new double[splitPoints.size()];
        for (int i = 0; i < splitPoints.size(); i++) {
            points[i] = splitPoints.get(i);
        }
        Arrays.sort(points);

        // Compute resulting bins.
        DiscreteBin bin = new DiscreteBin(values[0], points[0]);
        bins.add(bin);
        for (int i = 0; i < points.length - 1; i++) {
            bin = new DiscreteBin(points[i], points[i + 1]);
            bins.add(bin);
        }
        bin = new DiscreteBin(points[points.length - 1],
                values[values.length - 1]);
        bins.add(bin);

        return bins;
    }

    private void computeDiscretization(List<Double> toProcess,
                                       ArrayList<Double> splitPoints) {
        if (splitPoints.size() >= (maxNumPartitions + 1))
            return;

        double currentGain = 0;
        if (toProcess.size() > 2 || toProcess.size() < maxNumPartitions
                || currentGain > getThreshold()) {

            DiscreteBin binRef = new DiscreteBin(toProcess.get(0),
                    toProcess.get(toProcess.size() - 1));

            double maxIG = -Double.MAX_VALUE;
            int idxRef = 0;
            for (int i = 1; i < toProcess.size() - 1; i++) {
                DiscreteBin splitFirst = new DiscreteBin(
                        binRef.getStartValue(), toProcess.get(i));
                DiscreteBin splitSecond = new DiscreteBin(toProcess.get(i),
                        binRef.getEndValue());
                double ig = IG(binRef, splitFirst, splitSecond);
                if (ig > maxIG) {
                    maxIG = ig;
                    idxRef = i;
                }
            }

            if (maxIG < threshold)
                return;

            splitPoints.add(toProcess.get(idxRef));

            // Partitioning values in two sets.
            List<Double> set1 = toProcess.subList(0, idxRef + 1);
            List<Double> set2 = toProcess.subList(idxRef + 1, toProcess.size());

            computeDiscretization(set1, splitPoints);
            computeDiscretization(set2, splitPoints);
        }
    }

    private double IG(DiscreteBin binRef, DiscreteBin splitFirst,
                      DiscreteBin splitSecond) {
        double value = H(binRef);
        int numExamples = getStatsProvider().getNumTotalExamples();
        int numSplit1 = getStatsProvider().getNumExamplesInInterval(splitFirst);
        int numSplit2 = getStatsProvider()
                .getNumExamplesInInterval(splitSecond);
        double tmp1 = (numSplit1 / (double) numExamples) * H(splitFirst);
        double tmp2 = (numSplit2 / (double) numExamples) * H(splitSecond);
        value = value - tmp1 - tmp2;

        assert (!Double.isNaN(value) && !Double.isInfinite(value));

        return value;
    }

    private double H(DiscreteBin bin) {
        double sum = 0;
        for (int i = 0; i < getStatsProvider().getNumOfClasses(); i++) {
            double set_c_in = getStatsProvider().getNumExamplesInIntervalClass(
                    bin, i);
            double set_in = getStatsProvider().getNumExamplesInInterval(bin);

            if (set_in != 0) {
                double partial = (set_c_in / set_in);
                if (partial != 0) {
                    double logValue = Math.log(partial) / Math.log(2);
                    partial *= logValue;
                }
                sum += partial;
            }
        }

        assert (!Double.isNaN(sum) && !Double.isInfinite(sum));
        return -sum;
    }
}
