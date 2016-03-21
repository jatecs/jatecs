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

package it.cnr.jatecs.classification.ddagsl;

import gnu.trove.TIntArrayList;
import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.classification.svm.SvmLearner;
import it.cnr.jatecs.classification.svm.SvmLearnerCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexing.tsr.ITsr;
import it.cnr.jatecs.indexing.weighting.BM25;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.indexing.weighting.TfNormalizedIdf;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.ArrayList;
import java.util.Vector;

/**
 * See Platt, J. C., Cristianini, N., & Shawe-Taylor, J. (1999). Large-margin
 * DAGs for multiclass classiﬁcation. In Proceedings of the 11th International
 * Conference on Neural Information Processing Systems (NIPS’99) (pp. 533–547).
 * Denver, USA.
 *
 * @author Tiziano Fagni (tiziano.fagni@isti.cnr.it)
 */
public class SvmDDagSingleLabelLearner extends BaseLearner {

    private ITsr tsrType;
    private WeightingType weightingType;

    public SvmDDagSingleLabelLearner() {
        super();
        _customizer = null;
        weightingType = WeightingType.TF_IDF;
    }

    public ITsr getTsrType() {
        return tsrType;
    }

    public void setTsrType(ITsr tsrType) {
        this.tsrType = tsrType;
    }

    public IClassifier build(IIndex trainingIndex) {

        TShortArrayList cats = new TShortArrayList();
        IShortIterator catsIt1 = trainingIndex.getCategoryDB()
                .getCategories();
        while (catsIt1.hasNext()) {
            short catID = catsIt1.next();
            cats.add(catID);
        }
        cats.sort();

        SvmDDagSingleLabelClassifier classifier = new SvmDDagSingleLabelClassifier();
        for (int i = 0; i < cats.size() - 1; i++) {
            for (int j = i + 1; j < cats.size(); j++) {
                ArrayList<Short> catsGood = new ArrayList<Short>();
                catsGood.add(cats.get(i));
                catsGood.add(cats.get(j));

                // Build local index.
                IIndex localIndex = buildBinaryLocalIndex(trainingIndex,
                        catsGood);

                // Build local classifier.
                SvmLearner learner = new SvmLearner();
                SvmLearnerCustomizer customizer = new SvmLearnerCustomizer();
                learner.setRuntimeCustomizer(customizer);
                IClassifier localClassifier = learner.build(localIndex);

                // Keep track of generated classifier.
                classifier.addLocalBinaryClassifier(cats.get(i), cats.get(j),
                        localClassifier, localIndex.getContentDB(),
                        weightingType);
            }
        }

        return classifier;
    }

    private IIndex buildBinaryLocalIndex(IIndex trainingIndex,
                                         ArrayList<Short> catsGood) {
        if (!(catsGood.size() == 2))
            throw new RuntimeException("The set of valid categories must be 2");

        // First create a new index.
        IIndex idx = trainingIndex.cloneIndex();

        // Remove unwanted categories.
        TShortArrayList toRemove = new TShortArrayList();
        IShortIterator allCats = idx.getCategoryDB().getCategories();
        while (allCats.hasNext()) {
            short catID = allCats.next();
            if (catsGood.contains(catID))
                continue;

            toRemove.add(catID);
        }
        toRemove.sort();
        idx.removeCategories(new TShortArrayListIterator(toRemove));

        // Remove unwanted documents.
        TIntArrayList docsToRemove = new TIntArrayList();
        IIntIterator docs = idx.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int docID = docs.next();
            IShortIterator curCats = idx.getClassificationDB()
                    .getDocumentCategories(docID);
            if (!curCats.hasNext())
                docsToRemove.add(docID);
        }

        docsToRemove.sort();
        idx.removeDocuments(new TIntArrayListIterator(docsToRemove), true);

        // If the case, apply TSR.
        if (tsrType != null) {
            tsrType.computeTSR(idx);

            // Apply weighting.
            IWeighting weighting = null;
            if (weightingType == WeightingType.TF_IDF) {
                weighting = new TfNormalizedIdf(idx);
            } else if (weightingType == WeightingType.BM25) {
                weighting = new BM25(idx);
            }

            idx = weighting.computeWeights(idx);
        }

        // Remove 2nd category to make an index for a binary classifier.
        toRemove.clear();
        String catNameToRemove = trainingIndex.getCategoryDB()
                .getCategoryName(catsGood.get(1));
        toRemove.add(idx.getCategoryDB().getCategory(catNameToRemove));
        idx.removeCategories(new TShortArrayListIterator(toRemove));

        return idx;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        throw new RuntimeException("To be implemented");
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {

    }

    public WeightingType getWeightingType() {
        return weightingType;
    }

    public void setWeightingType(WeightingType weightingType) {
        this.weightingType = weightingType;
    }

    public static enum WeightingType {
        TF_IDF, BM25
    }

}
