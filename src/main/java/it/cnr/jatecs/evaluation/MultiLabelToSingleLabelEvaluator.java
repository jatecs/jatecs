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

package it.cnr.jatecs.evaluation;

import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.*;
import java.util.Map.Entry;

public class MultiLabelToSingleLabelEvaluator {

    public ContingencyTableSet evaluateSingleLabelResultsForLeaves(
            IClassificationDB goldDB,
            ClassificationScoreDB scores) {
        IIntIterator docs = goldDB.getDocumentDB().getDocuments();
        ContingencyTableSet tset = new ContingencyTableSet();
        tset.setName("singlelabel");
        while (docs.hasNext()) {
            int docID = docs.next();
            Set<Entry<Short, ClassifierRangeWithScore>> entries = scores
                    .getDocumentScoresAsSet(docID);
            TreeSet<CatScore> ts = new TreeSet<CatScore>();
            Iterator<Entry<Short, ClassifierRangeWithScore>> docScores = entries
                    .iterator();
            while (docScores.hasNext()) {
                Entry<Short, ClassifierRangeWithScore> entry = docScores.next();
                short catID = entry.getKey();
                ClassifierRangeWithScore cr = entry.getValue();
                if (!goldDB.getCategoryDB().hasChildCategories(catID)) {
                    CatScore cs = new CatScore();
                    cs.catID = catID;
                    cs.score = cr.score;
                    cs.border = cr.border;
                    ts.add(cs);
                }
            }

            CatScore bestItem = null;
            if (ts.size() > 0)
                bestItem = ts.last();

            IShortIterator cats = goldDB.getCategoryDB().getCategories();
            while (cats.hasNext()) {
                short catID = cats.next();
                if (goldDB.getCategoryDB().hasChildCategories(catID))
                    continue;

                if (bestItem != null && catID == bestItem.catID) {

                    if (goldDB.hasDocumentCategory(docID, bestItem.catID)) {
                        if (bestItem.score > bestItem.border)
                            tset.addTP(catID);
                        else
                            tset.addFN(catID);
                    } else {
                        if (bestItem.score > bestItem.border)
                            tset.addFP(catID);
                        else
                            tset.addTN(catID);
                    }
                } else {
                    if (goldDB.hasDocumentCategory(docID, catID))
                        tset.addFN(catID);
                    else
                        tset.addTN(catID);
                }
            }

        }

        IShortIterator cats = goldDB.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();
            ContingencyTable ct = tset.getCategoryContingencyTable(catID);
            if (ct != null) {
                ct.setName(goldDB.getCategoryDB().getCategoryName(catID));
            }
        }

        return tset;
    }

    public MRRCategorySet evaluateMRRForLeaves(IClassificationDB goldDB, ClassificationScoreDB scores, int numRecommedations) {
        IIntIterator docs = goldDB.getDocumentDB().getDocuments();
        MRRCategorySet cset = new MRRCategorySet();
        while (docs.hasNext()) {
            int docID = docs.next();
            ArrayList<RecommendedCategory> catsRecommended = recommendCats(goldDB, docID, numRecommedations, scores);

            IShortIterator manualCats = goldDB.getDocumentCategories(docID);
            short manualCatID = -1;
            while (manualCats.hasNext()) {
                short c = manualCats.next();
                if (goldDB.getCategoryDB().hasChildCategories(c))
                    continue;
                else
                    manualCatID = c;
            }

            MRRCategory catMRR = cset.getMRR(manualCatID);
            if (catMRR == null) {
                catMRR = new MRRCategory(goldDB.getCategoryDB().getCategoryName(manualCatID), numRecommedations);
                cset.putMRR(manualCatID, catMRR);
            }


            boolean assigned = false;
            for (int i = 0; i < catsRecommended.size(); i++) {
                RecommendedCategory rc = catsRecommended.get(i);
                if (rc.leafCatID == manualCatID) {
                    assigned = true;
                    catMRR.add1ToRank(i);
                    break;
                }

            }

            if (!assigned) {
                catMRR.add1ToRank(numRecommedations);
            }
        }

        return cset;
    }

    /**
     * Recommend the most appropriate categories for the specified document ID and given
     * the set of classification scores contained in "confidences" parameter. The number
     * of recommended categories is limited by "maxNumCats" parameter.
     *
     * @param idx         The classificationDB to be used.
     * @param docID       The document ID to be checked.
     * @param maxNumCats  The maximum number of categories to recommend (-1 to recommend all categories).
     * @param confidences The set of classification scores given by a certain classifier.
     * @return The list of recommended categories from most recommended to least recommended.
     */
    private ArrayList<RecommendedCategory> recommendCats(IClassificationDB idx, int docID, int maxNumCats, ClassificationScoreDB confidences) {
        if (idx == null)
            throw new NullPointerException("The index is 'null'");
        if (confidences == null)
            throw new NullPointerException("The set of classification scores is 'null'");

        Iterator<Entry<Short, ClassifierRangeWithScore>> itResults = confidences.getDocumentScoresAsSet(docID).iterator();
        TreeMap<ClassifierRangeWithScore, Short> map = new TreeMap<ClassifierRangeWithScore, Short>();
        while (itResults.hasNext()) {
            Entry<Short, ClassifierRangeWithScore> item = itResults.next();
            if (idx.getCategoryDB().hasChildCategories(item.getKey()))
                continue;

            map.put(item.getValue(), item.getKey());
            if (maxNumCats != -1 && map.size() > maxNumCats) {
                map.remove(map.firstKey());
            }
        }

        ArrayList<RecommendedCategory> ret = new ArrayList<RecommendedCategory>();
        Iterator<ClassifierRangeWithScore> itOrdered = map.keySet().iterator();
        while (itOrdered.hasNext()) {
            ClassifierRangeWithScore score = itOrdered.next();
            short catID = map.get(score);
            RecommendedCategory rc = new RecommendedCategory();
            rc.leafCatID = catID;
            rc.score = score.score;
            if (score.score > score.border)
                rc.automaticallyAssigned = true;
            else
                rc.automaticallyAssigned = false;

            ret.add(rc);
        }

        ArrayList<RecommendedCategory> reverseRet = new ArrayList<MultiLabelToSingleLabelEvaluator.RecommendedCategory>();
        for (int i = ret.size() - 1; i >= 0; i--) {
            reverseRet.add(ret.get(i));
        }
        return reverseRet;
    }

    static class CatScore implements Comparable<CatScore> {
        short catID;
        double score;
        double border;

        @Override
        public boolean equals(Object arg0) {
            return compareTo((CatScore) arg0) == 0;
        }

        @Override
        public int compareTo(CatScore arg0) {
            if (score < arg0.score)
                return -1;
            else if (score > arg0.score)
                return 1;
            else {
                if (catID < arg0.catID)
                    return -1;
                else if (catID > arg0.catID)
                    return 1;
                else
                    return 0;
            }
        }

    }

    static class RecommendedCategory {
        short leafCatID;
        boolean automaticallyAssigned;
        double score;
    }
}
