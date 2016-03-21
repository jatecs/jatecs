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

import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.satc.interfaces.IGain;
import it.cnr.jatecs.satc.interfaces.IIncrementalRank;
import it.cnr.jatecs.utils.Ranker;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author giacomo
 */
public class Incremental extends UtilityBased implements IIncrementalRank {

    TIntDoubleHashMap macroRankTable;
    TIntDoubleHashMap microRankTable;
    double[][] probabilities;
    TIntHashSet macroAlreadySeen;
    TIntHashSet microAlreadySeen;
    private TIntArrayList macroRank;
    private TIntArrayList microRank;

    public Incremental(int trainSize, ClassificationScoreDB classification, TIntHashSet categoriesFilter,
                       EstimationType estimation, ContingencyTableSet evaluation, IGain gain, IGain firstRankGain, double[] probabilitySlope, double[] prevalencies) {
        super(trainSize, classification, categoriesFilter, estimation, evaluation, firstRankGain, probabilitySlope, prevalencies);
        macroRankTable = new TIntDoubleHashMap((int) (testSize + testSize * 0.25), (float) 0.75);
        microRankTable = new TIntDoubleHashMap((int) (testSize + testSize * 0.25), (float) 0.75);
        macroAlreadySeen = new TIntHashSet((int) (testSize + testSize * 0.25), (float) 0.75);
        microAlreadySeen = new TIntHashSet((int) (testSize + testSize * 0.25), (float) 0.75);
        probabilities = new double[testSize][numOfCategories];
        for (int docId = 0; docId < testSize; docId++) {
            Set<Entry<Short, ClassifierRangeWithScore>> entries = classification.getDocumentScoresAsSet(docId);
            Iterator<Entry<Short, ClassifierRangeWithScore>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Entry<Short, ClassifierRangeWithScore> next = iterator.next();
                ClassifierRangeWithScore value = next.getValue();
                if (categoriesFilter.contains(next.getKey())) {
                    probabilities[docId][catMap.get(next.getKey())] = probability(Math.abs(value.score - value.border), next.getKey());
                }
            }
        }
    }

    protected TIntDoubleHashMap nextMacroTable(ContingencyTableSet oldEvaluation, ContingencyTableSet newEvaluation) {
        if (oldEvaluation != null && newEvaluation != null && !oldEvaluation.equals(newEvaluation)) {
            TIntDoubleHashMap currGainsFP = new TIntDoubleHashMap();
            TIntDoubleHashMap currGainsFN = new TIntDoubleHashMap();
            for (TIntIterator it = categoriesFilter.iterator(); it.hasNext(); ) {
                int catId = it.next();
                ContingencyTable newContingency = newEvaluation.getCategoryContingencyTable((short) catId);
                ContingencyTable oldContingency = oldEvaluation.getCategoryContingencyTable((short) catId);
                TIntArrayList increment = new TIntArrayList(4);
                increment.setQuick(0, newContingency.tp() - oldContingency.tp());
                increment.setQuick(1, newContingency.tn() - oldContingency.tn());
                increment.setQuick(2, newContingency.fp() - oldContingency.fp());
                increment.setQuick(3, newContingency.fn() - oldContingency.fn());
                if (increment.getQuick(0) != 0 || increment.getQuick(1) != 0 || increment.getQuick(2) != 0 || increment.getQuick(3) != 0) {
                    int currCat = catMap.get(catId);
                    //System.out.println(catId + " " + Arrays.toString(contingencies[currCat]) + " " + increment.getQuick(0) + " " + increment.getQuick(1) + " " + increment.getQuick(2) + " " + increment.getQuick(3));
                    for (int j = 0; j < 4; j++) {
                        contingencies[currCat][j] += increment.getQuick(j);
//                        if (contingencies[currCat][j] < 0.0) {
//                            contingencies[currCat][j] = 0.0;
//                        }
                    }
                    currGainsFP.put(catId, gain.FP(contingencies[currCat]));
                    currGainsFN.put(catId, gain.FN(contingencies[currCat]));
                    //System.out.println(catId + " " + currGainsFP.get(catId) + " " + currGainsFN.get(catId));
                }
            }
            //FIXME iter only on the not seen documents
            for (int docId = 0; docId < testSize; docId++) {
                if (macroAlreadySeen.contains(docId)) {
                    continue;
                }
                // Iter only on the categories with changed contingencies
                int[] keys = currGainsFP.keys();
                Hashtable<Short, ClassifierRangeWithScore> entries = classification.getDocumentScoresAsHashtable(docId);
                for (int i = 0; i < keys.length; i++) {
                    short catId = (short) keys[i];
                    if (entries.containsKey(catId)) {
                        ClassifierRangeWithScore value = entries.get(catId);
                        if (value.score > value.border) {
                            macroUtilities[docId][catMap.get(catId)] = currGainsFP.get(catId) * probabilities[docId][catMap.get(catId)];
                            //System.out.println(docId + " " + catId + " + " + currGainsFP.get(catId) + " " + probabilities[docId][catMap.get(catId)]);
                        } else if (value.score < value.border) {
                            macroUtilities[docId][catMap.get(catId)] = currGainsFN.get(catId) * probabilities[docId][catMap.get(catId)];
                            //System.out.println(docId + " " + catId + " - " + currGainsFN.get(catId) + " " + probabilities[docId][catMap.get(catId)]);
                        } else {
                            //macroUtilities[docId][catMap.get(catId)] = 1.0 * probabilities[docId][catMap.get(catId)];
                        }
                    }
                }
            }
        }
        macroRankTable.clear();
        //FIXME iter only on the not seen documents
        for (int docId = 0; docId < testSize; docId++) {
            if (!macroAlreadySeen.contains(docId)) {
                double sum = 0.0;
                for (int i = 0; i < macroUtilities[docId].length; i++) {
                    sum += macroUtilities[docId][i];
                }
                macroRankTable.put(docId, sum);
            }
        }
        macroRank = null;
        return macroRankTable;
    }

    protected TIntDoubleHashMap nextMicroTable(ContingencyTableSet oldEvaluation, ContingencyTableSet newEvaluation) {
        if (oldEvaluation != null && newEvaluation != null && !oldEvaluation.equals(newEvaluation)) {
            ContingencyTable newContingency = newEvaluation.getGlobalContingencyTable();
            ContingencyTable oldContingency = oldEvaluation.getGlobalContingencyTable();
            TIntArrayList increment = new TIntArrayList(4);
            increment.setQuick(0, newContingency.tp() - oldContingency.tp());
            increment.setQuick(1, newContingency.tn() - oldContingency.tn());
            increment.setQuick(2, newContingency.fp() - oldContingency.fp());
            increment.setQuick(3, newContingency.fn() - oldContingency.fn());
            if (increment.getQuick(0) != 0 || increment.getQuick(1) != 0 || increment.getQuick(2) != 0 || increment.getQuick(3) != 0) {
                for (int j = 0; j < 4; j++) {
                    globalContingencies[j] += increment.getQuick(j);
//                    if (globalContingencies[j] < 0.0) {
//                        globalContingencies[j] = 0.0;
//                    }
                }
                double currGainFP = gain.FP(globalContingencies);
                double currGainFN = gain.FN(globalContingencies);

                //FIXME iter only on the not seen documents
                for (int docId = 0; docId < testSize; docId++) {
                    if (microAlreadySeen.contains(docId)) {
                        continue;
                    }
                    Iterator<Entry<Short, ClassifierRangeWithScore>> iterator = classification.getDocumentScoresAsSet(docId).iterator();
                    while (iterator.hasNext()) {
                        Entry<Short, ClassifierRangeWithScore> next = iterator.next();
                        int catId = catMap.get(next.getKey());
                        ClassifierRangeWithScore value = next.getValue();
                        // the current gain has to be set to allow an update of the utilities
                        if (value.score > value.border) {
                            microUtilities[docId][catId] = currGainFP * probabilities[docId][catId];
                        } else if (value.score < value.border) {
                            microUtilities[docId][catId] = currGainFN * probabilities[docId][catId];
                        } else {
                            //microUtilities[docId][catId] = 1.0 * probabilities[docId][catId];
                        }
                    }
                }
            }
        }
        microRankTable.clear();
        //FIXME iter only on the not seen documents
        for (int docId = 0; docId < testSize; docId++) {
            if (!microAlreadySeen.contains(docId)) {
                double sum = 0.0;
                for (int i = 0; i < microUtilities[docId].length; i++) {
                    sum += microUtilities[docId][i];
                }
                microRankTable.put(docId, sum);
            }
        }
        microRank = null;
        return microRankTable;
    }

    @Override
    public TIntDoubleHashMap getMacroTable() {
        return macroRankTable;
    }

    public TIntArrayList nextMacroRank(ContingencyTableSet oldEvaluation, ContingencyTableSet newEvaluation) {
        nextMacroTable(oldEvaluation, newEvaluation);
        if (macroRank == null) {
            macroRank = getMacroRank();
        }
        return macroRank;
    }

    @Override
    public int nextMacroRankDocument(ContingencyTableSet oldEvaluation, ContingencyTableSet newEvaluation) {
        nextMacroTable(oldEvaluation, newEvaluation);
        int docID = getFirstMacro();
        macroAlreadySeen.add(docID);
        return docID;
    }

    @Override
    public TIntDoubleHashMap getMicroTable() {
        return microRankTable;
    }

    public TIntArrayList nextMicroRank(ContingencyTableSet oldEvaluation, ContingencyTableSet newEvaluation) {
        nextMicroTable(oldEvaluation, newEvaluation);
        if (microRank == null) {
            microRank = getMacroRank();
        }
        return microRank;
    }

    @Override
    public int nextMicroRankDocument(ContingencyTableSet oldEvaluation, ContingencyTableSet newEvaluation) {
        nextMicroTable(oldEvaluation, newEvaluation);
        int docID = getFirstMicro();
        microAlreadySeen.add(docID);
        return docID;
    }

    public int getFirstMacro() {
        return Ranker.getMax(macroRankTable);
    }

    public int getFirstMicro() {
        return Ranker.getMax(microRankTable);
    }

}
