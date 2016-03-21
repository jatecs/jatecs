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
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.evaluation.HierarchicalClassificationComparer;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.utils.IOperationStatusListener;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.FilteredIntIterator;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class SimpleKFoldEvaluator {

    protected ILearner _usedLearner;

    protected ILearnerRuntimeCustomizer _learnerCustomizer;
    protected IClassifierRuntimeCustomizer _classifierCustomizer;
    protected int _percentageToUse;
    protected boolean _evaluateAllNodes;
    protected boolean _saveClassification;
    protected TroveClassificationDBBuilder _classificationBuilder;

    /**
     * The k value for K-fold validation.
     */
    protected int _k;

    protected ClassificationMode classificationMode;

    protected boolean _saveConfidences;
    protected ClassificationScoreDB _confidences;

    public SimpleKFoldEvaluator(ILearner learner,
                                ILearnerRuntimeCustomizer learnerCustomizer,
                                IClassifierRuntimeCustomizer classifierCustomizer) {
        this(learner, learnerCustomizer, classifierCustomizer, false);
    }

    public SimpleKFoldEvaluator(ILearner learner,
                                ILearnerRuntimeCustomizer learnerCustomizer,
                                IClassifierRuntimeCustomizer classifierCustomizer,
                                boolean saveClassification) {
        _usedLearner = learner;
        _learnerCustomizer = learnerCustomizer;
        _classifierCustomizer = classifierCustomizer;
        _k = 5;
        _percentageToUse = 100;
        _evaluateAllNodes = false;
        _saveClassification = saveClassification;
        _classificationBuilder = null;
        classificationMode = ClassificationMode.PER_DOCUMENT;
        _saveConfidences = false;
        _confidences = null;
    }

    public void setSaveConfidences(boolean saveConfidences) {
        this._saveConfidences = saveConfidences;
    }

    public boolean isSavingConfidences() {
        return _saveConfidences;
    }

    public void setClassificationMode(ClassificationMode mode) {
        this.classificationMode = mode;
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

    public IClassificationDB getClassification() {
        if (_classificationBuilder != null)
            return _classificationBuilder.getClassificationDB();
        return null;
    }

    public ClassificationScoreDB getConfidences() {
        return _confidences;
    }

    public ContingencyTableSet evaluate(IIndex index,
                                        IOperationStatusListener status) {
        int realFolds = Math
                .min(_k, index.getDocumentDB().getDocumentsCount());

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

        ContingencyTableSet globalCT = new ContingencyTableSet();

        if (_saveClassification)
            _classificationBuilder = new TroveClassificationDBBuilder(
                    index.getDocumentDB(), index.getCategoryDB());
        if (isSavingConfidences())
            _confidences = new ClassificationScoreDB(index.getDocumentDB()
                    .getDocumentsCount());

        for (int i = 0; i < realFolds; ++i) {
            JatecsLogger.status().println(
                    "Doing validation " + (i + 1) + "/" + realFolds + "...");

            TIntArrayList trainingDocs = new TIntArrayList();
            TIntArrayList testDocs = folds[i];
            for (int j = 0; j < realFolds; ++j) {
                if (j != i) {
                    TIntArrayList fold = folds[j];
                    int partSize = Math.min(fold.size(),
                            (fold.size() * _percentageToUse) / 100);
                    for (int k = 0; k < partSize; ++k)
                        trainingDocs.add(fold.get(k));
                }
            }

            JatecsLogger.status().println(
                    "Building training set with " + trainingDocs.size()
                            + " documents.");

            // IDocumentsDB trainingDocumentsDB =
            // index.getDocumentsDB().cloneDB();
            // IClassificationDB trainingClassificationDB =
            // index.getClassificationDB().cloneDB(index.getCategoriesDB(),
            // trainingDocumentsDB);
            // IContentDB trainingContentDB =
            // index.getContentDB().cloneDB(trainingDocumentsDB,
            // index.getFeaturesDB());
            // IWeightingDB trainingWeightingDB = index
            // .getWeightingDB().cloneDB(trainingContentDB);
            // IIndex trainingIndex = new
            // GenericIndex(index.getFeaturesDB(),trainingDocumentsDB,index.getCategoriesDB(),index.getDomainDB(),
            // trainingContentDB,trainingWeightingDB,trainingClassificationDB);
            IIndex trainingIndex = index.cloneIndex();

            docs.begin();
            FilteredIntIterator trainingRemovedDocuments = new FilteredIntIterator(
                    docs, new TIntArrayListIterator(trainingDocs), true);
            trainingIndex.removeDocuments(trainingRemovedDocuments, false);
            assert (trainingIndex.getDocumentDB().getDocumentsCount() == trainingDocs
                    .size());

            IClassifier cl = null;
            if (trainingIndex.getDocumentDB().getDocumentsCount() > 0) {
                JatecsLogger.status().println("Building learner.");
                _usedLearner.setRuntimeCustomizer(_learnerCustomizer);
                cl = _usedLearner.build(trainingIndex);
            }
            JatecsLogger.status()
                    .println(
                            "Building test set with " + testDocs.size()
                                    + " documents.");

            // IDocumentsDB testDocumentsDB =
            // index.getDocumentsDB().cloneDB();
            // IClassificationDB testClassificationDB =
            // index.getClassificationDB().cloneDB(index.getCategoriesDB(),
            // testDocumentsDB);
            // IContentDB testContentDB =
            // index.getContentDB().cloneDB(testDocumentsDB,
            // index.getFeaturesDB());
            // IWeightingDB testWeightingDB = index
            // .getWeightingDB().cloneDB(testContentDB);
            // IIndex testIndex = new
            // GenericIndex(index.getFeaturesDB(),testDocumentsDB,index.getCategoriesDB(),index.getDomainDB(),
            // testContentDB,testWeightingDB,testClassificationDB);
            IIndex testIndex = index.cloneIndex();

            docs.begin();
            FilteredIntIterator testRemovedDocuments = new FilteredIntIterator(
                    docs, new TIntArrayListIterator(testDocs), true);
            testIndex.removeDocuments(testRemovedDocuments, false);
            assert (testIndex.getDocumentDB().getDocumentsCount() == testDocs
                    .size());

            IClassificationDB predictions = null;
            Classifier classifier = null;
            if (cl != null) {
                JatecsLogger.status().println("Testing.");
                cl.setRuntimeCustomizer(_classifierCustomizer);

                classifier = new Classifier(testIndex, cl,
                        isSavingConfidences());
                classifier.setClassificationMode(classificationMode);

                // ClassifierXofM classifier = new ClassifierXofM(testIndex,
                // cl);
                // classifier.setX(1);
                // classifier.setBoundCondition(BoundCondition.ExactlyX);

                classifier.exec();

                cl.destroy();

                predictions = classifier.getClassificationDB();
            } else {
                // We have not build a classifier. Simulate a trivial rejector.
                ICategoryDB catsDB = testIndex.getCategoryDB();
                TroveClassificationDBBuilder classificationBuilder = new TroveClassificationDBBuilder(
                        testIndex.getDocumentDB(), catsDB);
                predictions = classificationBuilder.getClassificationDB();
            }

            if (_saveClassification) {
                IIntIterator testDocIt = predictions.getDocumentDB()
                        .getDocuments();
                while (testDocIt.hasNext()) {
                    int doc = testDocIt.next();
                    int trueDoc = index.getDocumentDB().getDocument(
                            testIndex.getDocumentDB().getDocumentName(doc));
                    IShortIterator docCat = predictions
                            .getDocumentCategories(doc);
                    while (docCat.hasNext()) {
                        short cat = docCat.next();
                        _classificationBuilder
                                .setDocumentCategory(trueDoc, cat);
                    }
                }
            }

            if (isSavingConfidences() && cl != null) {
                ClassificationScoreDB confidences = classifier.getConfidences();
                IIntIterator testDocIt = predictions.getDocumentDB()
                        .getDocuments();
                while (testDocIt.hasNext()) {
                    int doc = testDocIt.next();
                    int trueDoc = index.getDocumentDB().getDocument(
                            testIndex.getDocumentDB().getDocumentName(doc));
                    Set<Entry<Short, ClassifierRangeWithScore>> res = confidences
                            .getDocumentScoresAsSet(doc);
                    Iterator<Entry<Short, ClassifierRangeWithScore>> it = res
                            .iterator();
                    while (it.hasNext()) {
                        Entry<Short, ClassifierRangeWithScore> entry = it
                                .next();
                        ClassifierRange cr = new ClassifierRange();
                        cr.border = entry.getValue().border;
                        cr.maximum = entry.getValue().maximum;
                        cr.minimum = entry.getValue().minimum;
                        _confidences.insertScore(trueDoc,
                                entry.getKey(), entry.getValue().score, cr);
                    }

                }
            }

            ContingencyTableSet tableSet;
            if (_evaluateAllNodes) {
                ClassificationComparer cc = new ClassificationComparer(
                        predictions, testIndex.getClassificationDB());
                tableSet = cc.evaluate();
            } else {
                HierarchicalClassificationComparer cc = new HierarchicalClassificationComparer(
                        predictions, testIndex.getClassificationDB());
                tableSet = cc.evaluate();
            }

            IShortIterator evalCats = tableSet.getEvaluatedCategories();
            while (evalCats.hasNext()) {
                short category = evalCats.next();
                globalCT.addContingenyTable(category,
                        tableSet.getCategoryContingencyTable(category));
            }
            double percentage = (i * 100) / (double) realFolds;
            if (status != null)
                status.operationStatus(percentage);
        }

        if (status != null)
            status.operationStatus(100);

        globalCT.setName(index.getName() + "_KFold-" + realFolds);

        return globalCT;
    }
}
