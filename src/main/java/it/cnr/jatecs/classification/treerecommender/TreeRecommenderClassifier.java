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

package it.cnr.jatecs.classification.treerecommender;

import gnu.trove.TShortArrayList;
import gnu.trove.TShortObjectHashMap;
import gnu.trove.TShortObjectIterator;
import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.treeboost.TreeBoostClassifierAddress;
import it.cnr.jatecs.classification.treeboost.TreeBoostClassifierCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.DoubleMappingShortObject;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Iterator;
import java.util.TreeSet;

public class TreeRecommenderClassifier extends BaseClassifier {

    protected TShortObjectHashMap<IClassifier> _map;
    protected DoubleMappingShortObject<TreeBoostClassifierAddress> _mapCatLevel;
    protected IClassifier _classifier;
    int atLeastResults;
    int checkAtLeastInternalNodes;
    boolean atLeastOne;
    public TreeRecommenderClassifier() {
        _map = new TShortObjectHashMap<IClassifier>();
        _mapCatLevel = new DoubleMappingShortObject<TreeBoostClassifierAddress>();
        _classifier = null;
        atLeastResults = 5;
        checkAtLeastInternalNodes = 3;
        atLeastOne = true;
    }

    public IClassifier internalClassifier() {
        return _classifier;
    }

    public TreeBoostClassifierCustomizer getRuntimeCustomizer() {
        return (TreeBoostClassifierCustomizer) _customizer;
    }

    public void setRuntimeCustomizer(IClassifierRuntimeCustomizer customizer) {
        super.setRuntimeCustomizer(customizer);

        if (getRuntimeCustomizer() != null) {
            // Set the customizer for all base classifier loaded.
            TShortObjectIterator<IClassifier> it = _map.iterator();
            while (it.hasNext()) {
                it.advance();
                IClassifier c = (IClassifier) it.value();

                // Set the wanted customizer.
                c.setRuntimeCustomizer(getRuntimeCustomizer()
                        .getInternalCustomizer());
            }
        }
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        ClassificationResult res = new ClassificationResult();
        res.documentID = docID;

        TreeSet<SingleClassificationResult> leafResults = new TreeSet<TreeRecommenderClassifier.SingleClassificationResult>();
        hierarchicallyClassification(Short.MIN_VALUE, testIndex, docID, 1, leafResults);

        int filled = 0;
        int numAutomaticallyAssigned = 0;
        SingleClassificationResult crBest = leafResults.last();
        while (filled < atLeastResults && leafResults.size() > 0) {
            SingleClassificationResult cr = leafResults.last();
            leafResults.remove(leafResults.last());
            res.categoryID.add(cr.catID);
            res.score.add(cr.score);
            if (cr.score >= cr.range.border) {
                numAutomaticallyAssigned++;
            }

            filled++;
        }

        if (atLeastOne && numAutomaticallyAssigned == 0) {
            // Force at least one category.
            res.score.set(0, crBest.range.border + 0.1);
        }

        return res;
    }

    protected void hierarchicallyClassification(short catID, IIndex idx,
                                                int docID, int level,
                                                TreeSet<SingleClassificationResult> leafResults) {
        IClassifier c = (IClassifier) _map.get(catID);
        if (c == null)
            // No more levels in the hierarchy.
            return;

        // Call recursively hierarchical classifier.
        ClassificationResult r = c.classify(idx, docID);

        // Order all results for classifiers.
        TreeSet<SingleClassificationResult> results = new TreeSet<TreeRecommenderClassifier.SingleClassificationResult>();
        TreeSet<SingleClassificationResult> resultsForced = new TreeSet<TreeRecommenderClassifier.SingleClassificationResult>();
        for (short i = 0; i < r.categoryID.size(); i++) {
            short curCatID = r.categoryID.get(i);

            TreeBoostClassifierAddress addr = new TreeBoostClassifierAddress();
            addr.level = catID;
            addr.categoryID = curCatID;
            short realCatID = _mapCatLevel.get2(addr);
            assert (realCatID != -1);

            ClassifierRange cr = c.getClassifierRange(curCatID);
            SingleClassificationResult singleRes = new SingleClassificationResult(
                    realCatID, r.score.get(i), cr);
            if (r.score.get(i) >= cr.border) {
                results.add(singleRes);
            } else {
                resultsForced.add(singleRes);
            }
        }

        if (level == 1) {
            // internal nodes.
            while (results.size() < checkAtLeastInternalNodes && resultsForced.size() > 0) {
                SingleClassificationResult cr = resultsForced.last();
                resultsForced.remove(resultsForced.last());
                results.add(cr);
            }

            Iterator<SingleClassificationResult> it = results.iterator();
            while (it.hasNext()) {
                SingleClassificationResult cr = it.next();
                if (!idx.getCategoryDB().hasChildCategories(cr.catID))
                    leafResults.add(cr);
                else {
                    int numChildren = idx.getCategoryDB().getChildCategoriesCount(cr.catID);
                    if (numChildren > 1)
                        hierarchicallyClassification(cr.catID, idx, docID, level + 1, leafResults);
                    else {
                        short childID = idx.getCategoryDB().getChildCategories(cr.catID).next();
                        SingleClassificationResult crChild = new SingleClassificationResult(childID, cr.score, cr.range);
                        leafResults.add(crChild);
                    }
                }
            }
        } else {
            // Leaf nodes.
            Iterator<SingleClassificationResult> it = results.iterator();
            while (it.hasNext()) {
                SingleClassificationResult cr = it.next();
                leafResults.add(cr);
            }

            it = resultsForced.iterator();
            while (it.hasNext()) {
                SingleClassificationResult cr = it.next();
                leafResults.add(cr);
            }
        }
    }

    public ClassifierRange getClassifierRange(short catID) {
        TreeBoostClassifierAddress addr = _mapCatLevel.get1(catID);
        if (addr != null) {
            IClassifier c = (IClassifier) _map.get(addr.level);
            return c.getClassifierRange(addr.categoryID);
        } else {
            ClassifierRange cr = new ClassifierRange();
            cr.border = 0;
            cr.minimum = -100;
            cr.maximum = 100;
            return cr;
        }
    }

    @Override
    public int getCategoryCount() {
        return _mapCatLevel.size();
    }

    @Override
    public IShortIterator getCategories() {
        TShortArrayList l = new TShortArrayList();
        for (short i = 0; i < _mapCatLevel.size(); i++)
            l.add(i);

        return new TShortArrayListIterator(l);
    }

    @Override
    public void destroy() {
        TShortObjectIterator<IClassifier> it = _map.iterator();
        while (it.hasNext()) {
            it.advance();
            IClassifier c = (IClassifier) it.value();
            c.destroy();
        }
    }

    static class SingleClassificationResult implements
            Comparable<SingleClassificationResult> {
        private short catID = -1;
        private double score = 0;
        private ClassifierRange range;

        public SingleClassificationResult(short catID, double score,
                                          ClassifierRange range) {
            this.catID = catID;
            this.score = score;
            this.range = range;
        }

        public short getCatID() {
            return catID;
        }

        public double getScore() {
            return score;
        }

        public ClassifierRange getRange() {
            return range;
        }

        @Override
        public int compareTo(SingleClassificationResult o) {
            if (score < o.score)
                return -1;
            else if (score > o.score)
                return 1;
            else
                return 0;
        }
    }

}
