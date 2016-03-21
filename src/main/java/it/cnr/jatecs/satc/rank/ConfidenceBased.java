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
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.utils.Ranker;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class ConfidenceBased extends StaticRank {

    protected TIntHashSet[] docCategoriesFilter;
    /**
     * Slope parameter of the logit function in the probability function
     */
    protected double[] slopes;

    public ConfidenceBased(int trainSize, ClassificationScoreDB classification, TIntHashSet categoriesFilter, double[] probabilitySlopes, int topProbabilities) {
        super(trainSize, classification, categoriesFilter);
        //FIXME asser probabilitySlopes length
        this.slopes = probabilitySlopes;
        if (topProbabilities > this.categoriesFilter.size()) {
            topProbabilities = this.categoriesFilter.size();
        }
        docCategoriesFilter = new TIntHashSet[testSize];
        for (int docId = 0; docId < testSize; docId++) {
            docCategoriesFilter[docId] = filterByTopProbabilities(docId, topProbabilities);
        }
    }

    public ConfidenceBased(int trainSize, ClassificationScoreDB classification, TIntHashSet categoriesFilter, double[] probabilitySlopes) {
        this(trainSize, classification, categoriesFilter, probabilitySlopes, classification.getDocumentScoresAsSet(0).size());
    }

    public double probability(double x, int catId) {
        x = Math.exp(x / slopes[catId]);
        x = 1.0 - (x / (x + 1.0));
        if (Double.isNaN(x)) {
            return 0.0;
        } else {
            return x;
        }
    }

    public TIntDoubleHashMap getTable() {
        TIntDoubleHashMap rank = new TIntDoubleHashMap((int) (testSize + testSize * 0.25), (float) 0.75);
        for (int docId = 0; docId < testSize; docId++) {
            Set<Entry<Short, ClassifierRangeWithScore>> entries = classification.getDocumentScoresAsSet(docId);
            Iterator<Entry<Short, ClassifierRangeWithScore>> iterator = entries.iterator();
            double sum = 0.0;
            while (iterator.hasNext()) {
                Entry<Short, ClassifierRangeWithScore> next = iterator.next();
                if (categoriesFilter.contains(next.getKey()) && docCategoriesFilter[docId].contains(next.getKey())) {
                    ClassifierRangeWithScore value = next.getValue();
                    sum += probability(Math.abs(value.score - value.border), next.getKey());
                    //System.out.println(docId + " " + next.getKey() + " " + probability(Math.abs(value.score - value.border), next.getKey()));
                    //System.out.println(next.getKey() + " " + slopes[next.getKey()] + " " + value.score);
                }
            }
            rank.put(docId, sum);
        }
        return rank;
    }

    @Override
    public TIntDoubleHashMap getMacroTable() {
        return getTable();
    }

    @Override
    public TIntDoubleHashMap getMicroTable() {
        return getTable();
    }

    private TIntHashSet filterByTopProbabilities(int docId, int topK) {
        TIntDoubleHashMap topProbRank = new TIntDoubleHashMap((int) (testSize + testSize * 0.25), (float) 0.75);
        Set<Entry<Short, ClassifierRangeWithScore>> entries = classification.getDocumentScoresAsSet(docId);
        Iterator<Entry<Short, ClassifierRangeWithScore>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Entry<Short, ClassifierRangeWithScore> next = iterator.next();
            if (categoriesFilter.contains(next.getKey())) {
                ClassifierRangeWithScore value = next.getValue();
                topProbRank.put(next.getKey(), probability(Math.abs(value.score - value.border), next.getKey()));
            }
        }
        Ranker r = new Ranker();
        return new TIntHashSet(r.get(topProbRank).toNativeArray(0, topK));
    }

    public void setSlope(double slope, int catId) {
        this.slopes[catId] = slope;
    }
}
