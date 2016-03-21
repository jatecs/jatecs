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

package it.cnr.jatecs.satc.rank;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIterator;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.satc.interfaces.IGain;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class UtilityBased extends ConfidenceBased {

    protected TIntIntHashMap catMap;

    ;
    ContingencyTableSet evaluation;
    IGain gain;
    double[][] macroUtilities;
    double[][] microUtilities;
    double[][] contingencies;
    double[] globalContingencies;
    int numOfCategories;

    public UtilityBased(int trainSize, ClassificationScoreDB classification,
                        TIntHashSet categoriesFilter, EstimationType estimation,
                        ContingencyTableSet evaluation, IGain gain,
                        double[] probabilitySlopes, double[] prevalencies) {
        this(trainSize, classification, categoriesFilter, estimation,
                evaluation, gain, probabilitySlopes, prevalencies,
                classification.getDocumentScoresAsSet(0).size(), 1.0, 1.0);
    }

    // FIXME trainSize redundant, use the total() method of each
    // ContingencyTable object
    public UtilityBased(int trainSize, ClassificationScoreDB classification,
                        TIntHashSet categoriesFilter, EstimationType estimation,
                        ContingencyTableSet evaluation, IGain gain,
                        double[] probabilitySlopes, double[] prevalencies,
                        int topProbabilities, double gainFPweight, double gainFNweight) {
        super(trainSize, classification, categoriesFilter, probabilitySlopes);

        // FIXME assert prevalencies length
        this.evaluation = evaluation;
        this.gain = gain;
        numOfCategories = categoriesFilter.size();
        catMap = new TIntIntHashMap(
                (int) (numOfCategories + numOfCategories * 0.25), (float) 0.75);
        int n = 0;
        for (TIntIterator it = categoriesFilter.iterator(); it.hasNext(); ) {
            catMap.put(it.next(), n);
            n++;
        }

        initContingencies(categoriesFilter, evaluation, prevalencies,
                estimation);

        double[] gainsFP = new double[numOfCategories];
        double[] gainsFN = new double[numOfCategories];
        for (TIntIterator it = categoriesFilter.iterator(); it.hasNext(); ) {
            int catId = it.next();
            int currCat = catMap.get(catId);
            gainsFP[currCat] = gainFPweight * gain.FP(contingencies[currCat]);
            gainsFN[currCat] = gainFNweight * gain.FN(contingencies[currCat]);
            // System.out.println(catId + " " + gainsFP[catMap.get(catId)] + " "
            // + gainsFN[catMap.get(catId)]);
        }

        // Micro gains
        double gainFP = gainFPweight * gain.FP(globalContingencies);
        double gainFN = gainFNweight * gain.FN(globalContingencies);

        macroUtilities = new double[testSize][numOfCategories];
        microUtilities = new double[testSize][numOfCategories];

        for (int docId = 0; docId < testSize; docId++) {

            Set<Entry<Short, ClassifierRangeWithScore>> entries = classification
                    .getDocumentScoresAsSet(docId);
            Iterator<Entry<Short, ClassifierRangeWithScore>> iterator = entries
                    .iterator();

            while (iterator.hasNext()) {
                Entry<Short, ClassifierRangeWithScore> next = iterator.next();
                if (categoriesFilter.contains(next.getKey())) {
                    int currCat = catMap.get(next.getKey());
                    ClassifierRangeWithScore value = next.getValue();
                    // System.out.println(next.getKey() + " " +
                    // slopes[next.getKey()] + " " + value.score);
                    // System.out.println(String.valueOf(docId) + ' ' +
                    // String.valueOf(next.getKey()) + ' ' +
                    // String.valueOf(value.score) + ' ' +
                    // String.valueOf(value.border) +
                    // ' ' + String.valueOf(p) + ' ' +
                    // String.valueOf(gainsFP[currCat]) + ' ' +
                    // String.valueOf(gainsFN[currCat]) + ' ' +
                    // String.valueOf(gainFP) + ' ' + String.valueOf(gainFN));
                    double p = probability(
                            Math.abs(value.score - value.border), next.getKey());
                    if (value.score > value.border) {
                        macroUtilities[docId][currCat] = gainsFP[currCat] * p;
                        // System.out.println(docId + " " + next.getKey() +
                        // " + " + gainsFP[currCat] + " " + p);
                        microUtilities[docId][currCat] = gainFP * p;
                    } else if (value.score < value.border) {
                        macroUtilities[docId][currCat] = gainsFN[currCat] * p;
                        // System.out.println(docId + " " + next.getKey() +
                        // " - " + gainsFN[currCat] + " " + p);
                        microUtilities[docId][currCat] = gainFN * p;
                    } else {
                        // what if the confidence is 0, FN or FP ?!
                        // macroUtilities[docId][currCat] = 0.5;
                        // microUtilities[docId][currCat] = 0.5;
                    }
                }
            }
        }
    }

    protected void initContingencies(TIntHashSet categoriesFilter,
                                     ContingencyTableSet evaluation, double[] prevalencies,
                                     EstimationType estimationType) {

        contingencies = new double[numOfCategories][4];
        for (TIntIterator it = categoriesFilter.iterator(); it.hasNext(); ) {
            int catId = it.next();
            int currCat = catMap.get(catId);
            ContingencyTable contingency = evaluation
                    .getCategoryContingencyTable((short) catId);
            double pr = 1.0;
            if (prevalencies.length > 0) {
                pr = prevalenceModifier(prevalencies[catId], contingency);
            }
            double estimation = 1.0;
            if (estimationType == EstimationType.TEST) {
                estimation = testSize
                        / (double) ((contingency.tp() + contingency.fn()) * pr
                        + contingency.tn() + contingency.fp());
            }
            contingencies[currCat][0] = estimation * contingency.tp() * pr;
            contingencies[currCat][1] = estimation * contingency.tn();
            contingencies[currCat][2] = estimation * contingency.fp();
            contingencies[currCat][3] = estimation * contingency.fn() * pr;
            // System.out.println(contingencies[currCat][0] + " " +
            // contingencies[currCat][1] + " " + contingencies[currCat][2] + " "
            // + contingencies[currCat][3]);
        }
        globalContingencies = new double[4];
        ContingencyTable contingency = evaluation.getGlobalContingencyTable();
        double estimation = 1.0;
        if (estimationType == EstimationType.TEST) {
            estimation = testSize / (double) contingency.total();
        }
        globalContingencies[0] = estimation * contingency.tp();
        globalContingencies[1] = estimation * contingency.tn();
        globalContingencies[2] = estimation * contingency.fp();
        globalContingencies[3] = estimation * contingency.fn();
    }

    private double prevalenceModifier(double prevalence, ContingencyTable ct) {
        double p = ct.tp() + ct.fn();
        double n = ct.tn() + ct.fp();
        return -(n * prevalence) / (p * (prevalence - 1.0));
    }

    public TIntDoubleHashMap getTable(double[][] utilities) {
        TIntDoubleHashMap rank = new TIntDoubleHashMap(
                (int) (testSize + testSize * 0.25), (float) 0.75);
        for (int docId = 0; docId < testSize; docId++) {
            double sum = 0.0;
            for (TIntIterator it = categoriesFilter.iterator(); it.hasNext(); ) {
                int catId = it.next();
                if (docCategoriesFilter[docId].contains(catId)) {
                    sum += utilities[docId][catMap.get(catId)];
                }
            }
            rank.put(docId, sum);
        }
        return rank;
    }

    @Override
    public TIntDoubleHashMap getMacroTable() {
        return getTable(macroUtilities);
    }

    @Override
    public TIntDoubleHashMap getMicroTable() {
        return getTable(microUtilities);
    }

    static public enum EstimationType {
        TEST, TRAIN, NONE
    }
}
