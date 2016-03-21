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

package it.cnr.jatecs.classification.rocchio;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntDoubleIterator;
import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

public class RocchioLearner extends BaseLearner {

    public RocchioLearner() {
        _customizer = new RocchioLearnerCustomizer();
    }

    public IClassifier build(IIndex trainingIndex) {
        TextualProgressBar pb = new TextualProgressBar(
                "Computing Rocchio learner");
        int catsCount = trainingIndex.getCategoryDB().getCategoriesCount();

        RocchioClassifier c = new RocchioClassifier();
        RocchioClassifierCustomizer custo = (RocchioClassifierCustomizer) c
                .getRuntimeCustomizer();
        custo._ranges = new Hashtable<Short, ClassifierRange>(trainingIndex
                .getCategoryDB().getCategoriesCount());
        RocchioLearnerCustomizer cust = (RocchioLearnerCustomizer) _customizer;

        c.vectors = new TIntDoubleHashMap[trainingIndex.getCategoryDB()
                .getCategoriesCount()];

        int count = 0;

        pb.signal(0);

        IShortIterator it = trainingIndex.getCategoryDB().getCategories();
        while (it.hasNext()) {
            short catID = it.next();

            ClassifierRange cr = new ClassifierRange();
            cr.border = 0;
            cr.minimum = Double.MIN_VALUE;
            cr.maximum = Double.MAX_VALUE;
            custo._ranges.put(catID, cr);

            c.vectors[catID] = new TIntDoubleHashMap();

            // Get positives for this category.
            IIntIterator positives = trainingIndex.getClassificationDB()
                    .getCategoryDocuments(catID);

            // Select Near-Positives for this category.
            TIntArrayList np = computeNearPositives(catID, trainingIndex, cust,
                    positives);
            TIntArrayListIterator npositives = new TIntArrayListIterator(np);

            // Compute weight for each feature considering the two previous
            // selected sets.
            IIntIterator features = trainingIndex.getDomainDB()
                    .getCategoryFeatures(catID);
            while (features.hasNext()) {
                int featID = features.next();

                double first = 0;
                double second = 0;

                positives.begin();
                while (positives.hasNext()) {
                    int docID = positives.next();

                    first += trainingIndex.getWeightingDB()
                            .getDocumentFeatureWeight(docID, featID);
                }

                first = (first / (double) trainingIndex.getClassificationDB()
                        .getCategoryDocumentsCount(catID))
                        * cust._beta;

                npositives.begin();
                while (npositives.hasNext()) {
                    int docID = npositives.next();

                    second += trainingIndex.getWeightingDB()
                            .getDocumentFeatureWeight(docID, featID);
                }

                second = (second / (double) np.size()) * cust._gamma;

                double weight = first - second;

                if (weight != 0)
                    c.vectors[catID].put(featID, weight);
            }

            count++;
            pb.signal((count * 100) / catsCount);
        }

        pb.signal(100);

        return c;
    }

    protected TIntArrayList computeNearPositives(short catID, IIndex index,
                                                 RocchioLearnerCustomizer cust, IIntIterator positives) {
        TreeSet<ItemOrdered> s = new TreeSet<ItemOrdered>();

        // First compute the centroid of positives.
        TIntDoubleHashMap centroid = new TIntDoubleHashMap(index
                .getFeatureDB().getFeaturesCount() / 3);
        positives.begin();
        int count = 0;
        while (positives.hasNext()) {
            int docID = positives.next();
            IIntIterator features = index.getContentDB().getDocumentFeatures(
                    docID);
            while (features.hasNext()) {
                int featID = features.next();
                if (centroid.containsKey(featID))
                    centroid.put(featID, centroid.get(featID)
                            + index.getWeightingDB().getDocumentFeatureWeight(
                            docID, featID));
                else
                    centroid.put(featID, index.getWeightingDB()
                            .getDocumentFeatureWeight(docID, featID));
            }

            count++;
        }
        TIntDoubleIterator it = centroid.iterator();
        while (it.hasNext()) {
            it.advance();
            it.setValue(it.value() / count);
        }

        IIntIterator docs = index.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int docID = docs.next();
            if (index.getClassificationDB().hasDocumentCategory(docID, catID))
                continue;

            TIntDoubleHashMap doc = new TIntDoubleHashMap(index.getContentDB()
                    .getDocumentFeaturesCount(docID));
            IIntIterator feats = index.getContentDB()
                    .getDocumentFeatures(docID);
            while (feats.hasNext()) {
                int featID = feats.next();
                doc.put(featID, index.getWeightingDB()
                        .getDocumentFeatureWeight(docID, featID));
            }

            ItemOrdered item = new ItemOrdered();
            item.distance = cust._func.compute(doc, centroid);
            item.docID = docID;

            s.add(item);
            if (s.size() > cust._numberNearPositives)
                s.remove(s.first());
        }

        TIntArrayList toReturn = new TIntArrayList();
        Iterator<ItemOrdered> el = s.iterator();
        while (el.hasNext()) {
            ItemOrdered item = el.next();
            toReturn.add(item.docID);
        }

        toReturn.sort();
        return toReturn;
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        return null;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {

    }

    @SuppressWarnings("rawtypes")
    class ItemOrdered implements Comparable {
        double distance;
        int docID;

        public int compareTo(Object o) {
            if (!(o instanceof ItemOrdered))
                throw new ClassCastException("The element is not of type "
                        + ItemOrdered.class.getName());

            ItemOrdered i = (ItemOrdered) o;
            if (distance < i.distance)
                return -1;
            else if (distance == i.distance)
                return 0;
            else
                return 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ItemOrdered))
                return false;
            ItemOrdered i = (ItemOrdered) obj;

            if (docID == i.docID)
                return true;
            else
                return false;
        }

    }

}
