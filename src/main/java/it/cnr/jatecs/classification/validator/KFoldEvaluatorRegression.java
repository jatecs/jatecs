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
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.HashMap;
import java.util.Iterator;

public class KFoldEvaluatorRegression {


    protected ILearner _usedLearner;

    protected ILearnerRuntimeCustomizer _learnerCustomizer;
    protected IClassifierRuntimeCustomizer _classifierCustomizer;
    protected int _percentageToUse;
    protected boolean _evaluateAllNodes;


    /**
     * The k value for K-fold validation.
     */
    protected int _k;

    public KFoldEvaluatorRegression(ILearner learner, ILearnerRuntimeCustomizer learnerCustomizer,
                                    IClassifierRuntimeCustomizer classifierCustomizer) {
        _usedLearner = learner;
        _learnerCustomizer = learnerCustomizer;
        _classifierCustomizer = classifierCustomizer;
        _k = 5;
        _percentageToUse = 100;
        _evaluateAllNodes = false;
    }

    public KFoldEvaluatorRegression(ILearner learner) {
        this(learner, null, null);
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


    protected HashMap<Short, TIntArrayList> splitPerCategory(IIndex index) {
        HashMap<Short, TIntArrayList> map = new HashMap<Short, TIntArrayList>();
        IShortIterator cats = index.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();
            IIntIterator docs = index.getClassificationDB().getCategoryDocuments(catID);
            TIntArrayList l = new TIntArrayList();
            map.put(catID, l);
            while (docs.hasNext()) {
                int docID = docs.next();
                assert (index.getClassificationDB().getDocumentCategoriesCount(docID) == 1);
                l.add(docID);
            }
        }

        return map;
    }


    public RegressionResultSet evaluate(IIndex index, IOperationStatusListener status) {


        IIntIterator docs = index.getDocumentDB().getDocuments();

        HashMap<Short, TIntArrayList> originalSplit = splitPerCategory(index);
        Iterator<Short> it = originalSplit.keySet().iterator();
        int realFolds = _k;
        while (it.hasNext()) {
            short catID = it.next();
            TIntArrayList l = originalSplit.get(catID);
            realFolds = Math.min(realFolds, l.size());
        }


        TIntArrayList[] folds = new TIntArrayList[realFolds];

        // Now compute folds.
        for (int i = 0; i < realFolds; ++i) {
            TIntArrayList fold = new TIntArrayList();
            Iterator<Short> itCats = originalSplit.keySet().iterator();
            while (itCats.hasNext()) {
                short catID = itCats.next();
                TIntArrayList l = originalSplit.get(catID);
                int docsPerFold = l.size() / realFolds;

                int start = i * docsPerFold;
                int end = (i + 1) * docsPerFold;
                if (i + 1 == realFolds) {
                    if (l.size() % realFolds != 0)
                        end += l.size() % realFolds;
                }

                for (int x = start; x < end; x++)
                    fold.add(l.get(x));
            }

            fold.sort();
            folds[i] = fold;
        }


        RegressionResultSet globalTableSet = new RegressionResultSet(index.getCategoryDB().getCategoriesCount());

        for (int i = 0; i < realFolds; ++i) {
            JatecsLogger.status().println("Doing validation " + (i + 1) + "/" + realFolds + "...");

            TIntArrayList trainingDocs = new TIntArrayList();
            TIntArrayList testDocs = folds[i];
            for (int j = 0; j < realFolds; ++j) {
                if (j != i) {
                    TIntArrayList fold = folds[j];
                    for (int k = 0; k < fold.size(); ++k)
                        trainingDocs.add(fold.get(k));
                }
            }

            trainingDocs.sort();
            JatecsLogger.status().println("Building training set with " + trainingDocs.size() + " documents.");

//			IDocumentsDB trainingDocumentsDB = index.getDocumentsDB().cloneDB();
//			IClassificationDB trainingClassificationDB = index.getClassificationDB().cloneDB(index.getCategoriesDB(), trainingDocumentsDB);
//			IContentDB trainingContentDB = index.getContentDB().cloneDB(trainingDocumentsDB, index.getFeaturesDB());
//			IWeightingDB trainingWeightingDB = index .getWeightingDB().cloneDB(trainingContentDB);
//			IIndex trainingIndex = new GenericIndex(index.getFeaturesDB(),trainingDocumentsDB,index.getCategoriesDB(),index.getDomainDB(),
//					trainingContentDB,trainingWeightingDB,trainingClassificationDB);
            IIndex trainingIndex = index.cloneIndex();

            docs.begin();
            trainingIndex.removeDocuments(new TIntArrayListIterator(testDocs), false);
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
            testIndex.removeDocuments(new TIntArrayListIterator(trainingDocs), false);
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
