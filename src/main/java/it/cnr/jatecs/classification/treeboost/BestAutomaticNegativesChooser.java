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

package it.cnr.jatecs.classification.treeboost;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TShortIntHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexing.similarity.CosineSimilarityFunction;
import it.cnr.jatecs.indexing.similarity.ISimilarityFunction;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

public class BestAutomaticNegativesChooser implements INegativesChooser {

    protected IIndex _index;

    protected ISimilarityFunction _similarity;

    protected Vector<TreeSet<DocumentSimilar>> _best;

    public BestAutomaticNegativesChooser() {
        _similarity = new CosineSimilarityFunction();
        _best = null;
    }

    protected TIntDoubleHashMap getDocumentAsMap(int docID, IIndex index) {
        TIntDoubleHashMap d2 = new TIntDoubleHashMap(index.getFeatureDB()
                .getFeaturesCount());
        IIntIterator features = index.getFeatureDB().getFeatures();
        while (features.hasNext()) {
            int featID = features.next();
            d2.put(featID,
                    index.getWeightingDB().getDocumentFeatureWeight(docID,
                            featID));
        }

        return d2;
    }

    public void initialize(IIndex index) {
        _index = index;

        JatecsLogger.status().print("Computing categories centroid...");
        // First compute categories centroid.
        TIntDoubleHashMap[] centroids = new TIntDoubleHashMap[_index
                .getCategoryDB().getCategoriesCount()];
        for (short i = 0; i < centroids.length; i++) {
            short catID = i;
            centroids[catID] = computeCentroid(catID, _index);
        }
        JatecsLogger.status().println("done.");

        // Next compute the number of negatives that must be choosed for each
        // category (based on sibling nodes).
        TShortIntHashMap numNegatives = new TShortIntHashMap(_index
                .getCategoryDB().getCategoriesCount());
        IShortIterator catsIt = _index.getCategoryDB().getCategories();
        while (catsIt.hasNext()) {
            short catID = catsIt.next();
            int numNeg = 0;

            IShortIterator parents = _index.getCategoryDB()
                    .getParentCategories(catID);
            if (parents.hasNext()) {
                while (parents.hasNext()) {
                    short parentID = parents.next();
                    numNeg += _index.getClassificationDB()
                            .getCategoryDocumentsCount(parentID);
                }
            } else {
                numNeg = _index.getDocumentDB().getDocumentsCount();
            }

            // Now consider positive documents for this category.
            numNeg = numNeg
                    - _index.getClassificationDB().getCategoryDocumentsCount(
                    catID);

            numNegatives.put(catID, numNeg);
        }

        // DEBUG
        System.out.println("Ho finito di calcolare il numero di negativi");

        _best = new Vector<TreeSet<DocumentSimilar>>(_index.getCategoryDB()
                .getCategoriesCount());
        for (int i = 0; i < _index.getCategoryDB().getCategoriesCount(); i++)
            _best.add(new TreeSet<DocumentSimilar>());

        IShortIterator cats = _index.getCategoryDB().getCategories();

        IIntIterator documents = _index.getDocumentDB().getDocuments();
        while (documents.hasNext()) {
            int docID = documents.next();
            cats.begin();

            TIntDoubleHashMap doc = getDocumentAsMap(docID, _index);

            while (cats.hasNext()) {
                short catID = cats.next();
                if (_index.getClassificationDB().hasDocumentCategory(docID,
                        catID))
                    continue;

                double similarity = _similarity.compute(centroids[catID], doc,
                        _index.getFeatureDB().getFeatures());

                DocumentSimilar ds = new DocumentSimilar();
                ds.docID = docID;
                ds.similarity = similarity;

                _best.get(catID).add(ds);

                if (_best.get(catID).size() > numNegatives.get(catID))
                    _best.get(catID).remove(_best.get(catID).first());
            }
        }

    }

    public void release() {
        _best = null;
    }

    public TIntArrayListIterator selectNegatives(String category) {
        short catID = _index.getCategoryDB().getCategory(category);

        TreeSet<DocumentSimilar> best = _best.get(catID);

        TIntArrayList neg = new TIntArrayList();
        Iterator<DocumentSimilar> it = best.iterator();
        while (it.hasNext()) {
            DocumentSimilar docS = it.next();
            neg.add(docS.docID);
        }

        neg.sort();

        return new TIntArrayListIterator(neg);
    }

    protected TIntDoubleHashMap computeCentroid(short catID, IIndex index) {
        TIntDoubleHashMap centroid = new TIntDoubleHashMap(index.getFeatureDB()
                .getFeaturesCount());
        IIntIterator it = index.getFeatureDB().getFeatures();
        while (it.hasNext()) {
            int featID = it.next();
            centroid.put(featID, 0);
        }

        IIntIterator docs = index.getClassificationDB().getCategoryDocuments(
                catID);
        while (docs.hasNext()) {
            int docID = docs.next();
            IIntIterator feats = index.getContentDB()
                    .getDocumentFeatures(docID);
            while (feats.hasNext()) {
                int featID = feats.next();

                centroid.put(
                        featID,
                        centroid.get(featID)
                                + index.getWeightingDB()
                                .getDocumentFeatureWeight(docID, featID));
            }
        }

        int numDoc = index.getClassificationDB().getCategoryDocumentsCount(
                catID);
        int[] keys = centroid.keys();
        for (int i = 0; i < keys.length; i++) {
            centroid.put(keys[i], centroid.get(keys[i]) / (double) numDoc);
        }

        return centroid;
    }
}
