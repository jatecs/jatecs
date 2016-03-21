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

package it.cnr.jatecs.classification.validator;

import gnu.trove.TIntArrayList;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.evaluation.RegressionComparer;
import it.cnr.jatecs.evaluation.RegressionResult;
import it.cnr.jatecs.evaluation.RegressionResultSet;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.IOperationStatusListener;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.FilteredIntIterator;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class SimpleKFoldEvaluatorRegression {

    protected ILearner _usedLearner;

    protected ILearnerRuntimeCustomizer _learnerCustomizer;
    protected IClassifierRuntimeCustomizer _classifierCustomizer;
    protected int _percentageToUse;
    protected boolean _evaluateAllNodes;


    /**
     * The k value for K-fold validation.
     */
    protected int _k;

    public SimpleKFoldEvaluatorRegression(ILearner learner, ILearnerRuntimeCustomizer learnerCustomizer,
                                          IClassifierRuntimeCustomizer classifierCustomizer) {
        _usedLearner = learner;
        _learnerCustomizer = learnerCustomizer;
        _classifierCustomizer = classifierCustomizer;
        _k = 5;
        _percentageToUse = 100;
        _evaluateAllNodes = false;
    }

    public void setEvaluateAllNodes(boolean flag) {
        _evaluateAllNodes = flag;
    }

    public void setKFoldValue(int k) {
        _k = k;
    }

    public void setPercentageToUse(int percentage) {
        _percentageToUse = percentage;
    }

    public RegressionResultSet evaluate(IIndex index, IOperationStatusListener status) {
        int realFolds = Math.min(_k, index.getDocumentDB().getDocumentsCount());

        TIntArrayList[] folds = new TIntArrayList[realFolds];

        int docsCount = index.getDocumentDB().getDocumentsCount();
        IIntIterator docs = index.getDocumentDB().getDocuments();

        int foldSize = docsCount / realFolds;

        for (int i = 0; i < realFolds; ++i) {
            TIntArrayList fold = new TIntArrayList();
            while (fold.size() < foldSize && docs.hasNext())
                fold.add(docs.next());
            folds[i] = fold;
        }
        while (docs.hasNext())
            folds[realFolds - 1].add(docs.next());

        RegressionResultSet globalTableSet = new RegressionResultSet(index.getCategoryDB().getCategoriesCount());

        for (int i = 0; i < realFolds; ++i) {
            JatecsLogger.status().println("Doing validation " + (i + 1) + "/" + realFolds + "...");

            TIntArrayList trainingDocs = new TIntArrayList();
            TIntArrayList testDocs = folds[i];
            for (int j = 0; j < realFolds; ++j) {
                if (j != i) {
                    TIntArrayList fold = folds[j];
                    int partSize = Math.min(fold.size(), (fold.size() * _percentageToUse) / 100);
                    for (int k = 0; k < partSize; ++k)
                        trainingDocs.add(fold.get(k));
                }
            }

            JatecsLogger.status().println("Building training set with " + trainingDocs.size() + " documents.");

//			IDocumentsDB trainingDocumentsDB = index.getDocumentsDB().cloneDB();
//			IClassificationDB trainingClassificationDB = index.getClassificationDB().cloneDB(index.getCategoriesDB(), trainingDocumentsDB);
//			IContentDB trainingContentDB = index.getContentDB().cloneDB(trainingDocumentsDB, index.getFeaturesDB());
//			IWeightingDB trainingWeightingDB = index .getWeightingDB().cloneDB(trainingContentDB);
//			IIndex trainingIndex = new GenericIndex(index.getFeaturesDB(),trainingDocumentsDB,index.getCategoriesDB(),index.getDomainDB(),
//					trainingContentDB,trainingWeightingDB,trainingClassificationDB);
            IIndex trainingIndex = index.cloneIndex();

            docs.begin();
            FilteredIntIterator trainingRemovedDocuments = new FilteredIntIterator(docs, new TIntArrayListIterator(trainingDocs), true);
            trainingIndex.removeDocuments(trainingRemovedDocuments, false);
            assert (trainingIndex.getDocumentDB().getDocumentsCount() == trainingDocs.size());

            JatecsLogger.status().println("Building learner.");
            _usedLearner.setRuntimeCustomizer(_learnerCustomizer);
            IClassifier cl = _usedLearner.build(trainingIndex);

            JatecsLogger.status().println("Building test set with " + testDocs.size() + " documents.");

//			IDocumentsDB testDocumentsDB = index.getDocumentsDB().cloneDB();
//			IClassificationDB testClassificationDB = index.getClassificationDB().cloneDB(index.getCategoriesDB(), testDocumentsDB);
//			IContentDB testContentDB = index.getContentDB().cloneDB(testDocumentsDB, index.getFeaturesDB());
//			IWeightingDB testWeightingDB = index .getWeightingDB().cloneDB(testContentDB);
//			IIndex testIndex = new GenericIndex(index.getFeaturesDB(),testDocumentsDB,index.getCategoriesDB(),index.getDomainDB(),
//					testContentDB,testWeightingDB,testClassificationDB);
            IIndex testIndex = index.cloneIndex();

            docs.begin();
            FilteredIntIterator testRemovedDocuments = new FilteredIntIterator(docs, new TIntArrayListIterator(testDocs), true);
            testIndex.removeDocuments(testRemovedDocuments, false);
            assert (testIndex.getDocumentDB().getDocumentsCount() == testDocs.size());

            JatecsLogger.status().println("Testing.");
            cl.setRuntimeCustomizer(_classifierCustomizer);

            Classifier classifier = new Classifier(testIndex, cl);

//			ClassifierXofM classifier = new ClassifierXofM(testIndex, cl);
//			classifier.setX(1);
//			classifier.setBoundCondition(BoundCondition.ExactlyX);

            classifier.exec();


            RegressionComparer comparer = new RegressionComparer(
                    classifier.getClassificationDB(), testIndex.getClassificationDB());

            RegressionResultSet res = comparer.evaluate();

            IShortIterator evalCats = res.getEvaluatedCategories();
            while (evalCats.hasNext()) {
                short category = evalCats.next();
                RegressionResult resultCat = res.getRegressionResult(category);
                globalTableSet.addRegressionResult(category, resultCat);
            }
            double percentage = (i * 100) / (double) realFolds;
            if (status != null)
                status.operationStatus(percentage);
        }

        if (status != null)
            status.operationStatus(100);

        return globalTableSet;
    }
}
