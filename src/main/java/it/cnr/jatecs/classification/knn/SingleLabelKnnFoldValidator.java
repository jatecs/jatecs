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

package it.cnr.jatecs.classification.knn;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.OptimalConfiguration;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.classification.validator.KFoldRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class SingleLabelKnnFoldValidator extends BaseLearner {

//	private static class SplitOperation {
//		IIndex training;
//		IIndex test;
//		float[][] similarityMatrix;
//	}

    protected SingleLabelKnnFakeLearner _learner;
    protected SingleLabelKnnClassifierOptimizer _optimizer;
    protected float[][] _globalSimMatrix;

    public SingleLabelKnnFoldValidator(SingleLabelKnnFakeLearner learner,
                                       SingleLabelKnnClassifierOptimizer optimizer) {
        _learner = learner;
        _optimizer = optimizer;
        _globalSimMatrix = null;
        _customizer = new KFoldRuntimeCustomizer();
    }

    protected static int findMinimumNumberOfPositives(IIndex index) {
        int minimum = Integer.MAX_VALUE;
        IShortIterator cats = index.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();
            int docs = index.getClassificationDB().getCategoryDocumentsCount(
                    catID);
            if (docs < minimum) {
                minimum = docs;
            }
        }

        return minimum;
    }

    public static Pair<IIndex, IIndex> splitIndex(int step, IIndex index,
                                                  int numValidationSteps) {
        int numPositives = index.getDocumentDB().getDocumentsCount();

        int numSteps = Math.min(numPositives, numValidationSteps);
        if (step >= numSteps)
            return null;

        TIntArrayList tr = new TIntArrayList();
        TIntArrayList va = new TIntArrayList();

        int numPositivesInValidation = numPositives / numSteps;
        int numPositivesInTraining = numPositives - numPositivesInValidation;
        int startTrainingID = (numPositives / numSteps) * step;
        int endTrainingID = (startTrainingID + numPositivesInTraining - 1);
        TIntIntHashMap map = new TIntIntHashMap();
        for (int i = startTrainingID; i <= endTrainingID; i++) {
            int v = i % numPositives;
            map.put(v, v);
        }

        int curDoc = 0;
        IIntIterator docs = index.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int docID = docs.next();
            if (map.containsKey(curDoc)) {
                tr.add(docID);
            } else {
                va.add(docID);
            }
            curDoc++;
        }

        tr.sort();
        va.sort();

        IIndex trIndex = index.cloneIndex();
        trIndex.removeDocuments(new TIntArrayListIterator(va), false);

        IIndex vaIndex = index.cloneIndex();
        vaIndex.removeDocuments(new TIntArrayListIterator(tr), false);

        JatecsLogger.status().println(
                "done. The training contains " + tr.size()
                        + " document(s) and the validation contains "
                        + va.size() + " document(s).");

        Pair<IIndex, IIndex> ret = new Pair<IIndex, IIndex>(trIndex, vaIndex);
        return ret;

    }

    protected float[][] buildSimilarityMatrix(IIndex trainingIndex)
            throws Exception {
        SingleLabelKnnFakeLearner learn = (SingleLabelKnnFakeLearner) _learner;
        KnnClassifierCustomizer customizer = (KnnClassifierCustomizer) learn._cl
                .getRuntimeCustomizer();
        if (!(customizer._searcher instanceof TextualKnnSearcher))
            return null;
        TextualKnnSearcher ts = (TextualKnnSearcher) customizer._searcher;

        int numDocs = trainingIndex.getDocumentDB().getDocumentsCount();
        float[][] matrix = new float[numDocs][numDocs];

        System.out.print("Computing global similarity matrix...");
        for (int i = 0; i < numDocs; i++) {
            for (int j = 0; j <= i; j++) {
                if (i != j) {
                    double score = ts._similarity.compute(i, j, trainingIndex);
                    matrix[i][j] = (float) score;
                    matrix[j][i] = (float) score;
                } else {
                    matrix[i][i] = 0;
                }
            }
        }
        System.out.println("done.");

        return matrix;
    }

    public IClassifier build(IIndex trainingIndex) {
        SingleLabelKnnFakeLearner learn = _learner;
        SingleLabelKnnClassifierCustomizer customizer = (SingleLabelKnnClassifierCustomizer) learn._cl
                .getRuntimeCustomizer();
        if (!(customizer._searcher instanceof TextualKnnSearcher))
            return buildNotTextual(trainingIndex);
        else
            // Da implementare il classificatore testuale?
            return null;
    }

    protected IClassifier buildNotTextual(IIndex trainingIndex) {
        KFoldRuntimeCustomizer cust = (KFoldRuntimeCustomizer) getRuntimeCustomizer();


        SingleLabelKnnFakeLearner learn = (SingleLabelKnnFakeLearner) _learner;
        SingleLabelKnnClassifierCustomizer customizer = (SingleLabelKnnClassifierCustomizer) learn._cl
                .getRuntimeCustomizer();
        boolean sameIndexesOld = customizer._searcher.useSameIndexesData();
        customizer._searcher.setUseSameIndexesData(true);

        ILearnerRuntimeCustomizer c = _learner.getRuntimeCustomizer();
        if (c != null) {
            c = c.cloneObject();
        }

        Vector<OptimalConfiguration> v = new Vector<OptimalConfiguration>();

        for (int i = 0; i < cust.getKFoldValidationSteps(); i++) {
            JatecsLogger.status().println(
                    "Doing validation " + (i + 1) + "/"
                            + cust.getKFoldValidationSteps() + "...");

            // Split the index in two parts: a training and a valiudation set.
            Pair<IIndex, IIndex> indexes = splitIndex(i, trainingIndex, cust
                    .getKFoldValidationSteps());
            if (indexes == null)
                // All possible steps was done.
                break;

            // Optimize classifier.
            OptimalConfiguration res = _optimizer.optimizeFor(_learner, indexes
                    .getFirst(), indexes.getSecond());

            // Keep track of current result.
            v.add(res);

        }

        customizer._searcher.setUseSameIndexesData(sameIndexesOld);

        // Choose optimal K.
        int kAvg = 0;
        for (int i = 0; i < v.size(); i++) {
            OptimalConfiguration conf = v.get(i);
            SingleLabelKnnClassifierCustomizer cu = (SingleLabelKnnClassifierCustomizer) conf.classifierCustomizer;
            kAvg += cu.getK();
        }

        kAvg /= v.size();

        System.out.println("Selected optimal value for k = " + kAvg);

        customizer.setK(kAvg);
        customizer._searcher.setUseSameIndexesData(sameIndexesOld);

        return _learner._cl;
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

}
