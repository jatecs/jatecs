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

package it.cnr.jatecs.satc.parameterTuning;

import gnu.trove.TIntArrayList;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.classification.adaboost.*;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.classification.mpboost.MPWeakLearner;
import it.cnr.jatecs.classification.mpboost.MPWeakLearnerMultiThread;
import it.cnr.jatecs.classification.svm.SvmClassifierCustomizer;
import it.cnr.jatecs.classification.svm.SvmLearner;
import it.cnr.jatecs.classification.svm.SvmLearnerCustomizer;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.MutablePair;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.FilteredIntIterator;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class ProbabilitySlope extends JatecsModule {

    LearnerType currentLearner = LearnerType.MPBoost;
    boolean isMacro = true;
    private int boostThreadCount = 2;
    private int kValue = 10;
    private double[] parameters = {0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07,
            0.08, 0.09, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 2.0,
            3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 20.0, 30.0, 40.0, 50.0,
            60.0, 70.0, 80.0, 90.0, 100.0, 200.0};
    private int boostIterationCount = 1000;
    private IIndex index;
    private Pair<Vector<Pair<Double, Double>>, Pair<Double, Double>> result;
    private boolean evaluate = false;
    private ContingencyTableSet globalCT = null;

    public ProbabilitySlope(IIndex index, String name) {
        super(index, name);
        this.index = index;
    }

    public ProbabilitySlope(IIndex index) {
        this(index, "");
    }

    public int getBoostThreadCount() {
        return boostThreadCount;
    }

    public void setBoostThreadCount(int boostThreadCount) {
        this.boostThreadCount = boostThreadCount;
    }

    public int getkValue() {
        return kValue;
    }

    public void setkValue(int kValue) {
        this.kValue = kValue;
    }

    public double[] getParameters() {
        return parameters;
    }

    public void setParameters(double[] parameters) {
        this.parameters = parameters;
    }

    ;

    public boolean isMacro() {
        return isMacro;
    }

    public boolean isMicro() {
        return !isMacro;
    }

    public void setMacro() {
        this.isMacro = true;
    }

    public void setMicro() {
        this.isMacro = false;
    }

    public LearnerType getCurrentLearner() {
        return currentLearner;
    }

    public void setCurrentLearner(LearnerType currentLearner) {
        this.currentLearner = currentLearner;
    }

    public int getBoostIterationCount() {
        return boostIterationCount;
    }

    public void setBoostIterationCount(int boostIterationCount) {
        this.boostIterationCount = boostIterationCount;
    }

    public boolean isEvaluate() {
        return evaluate;
    }

    public void setEvaluate(boolean evaluate) {
        this.evaluate = evaluate;
    }

    public ContingencyTableSet getGlobalCT() {
        return globalCT;
    }

    public Pair<Vector<Pair<Double, Double>>, Pair<Double, Double>> getResult() {
        return result;
    }

    public double getOptimumParameter() {
        return result.getSecond().getFirst();
    }

    @Override
    protected void processModule() {

        ILearner learner = null;
        ILearnerRuntimeCustomizer customizer = null;
        IClassifierRuntimeCustomizer classifierCustomizer = null;

        // LEARNING
        if (currentLearner == LearnerType.SVMlib) {
            learner = new SvmLearner();
            customizer = new SvmLearnerCustomizer();
            learner.setRuntimeCustomizer(customizer);
            classifierCustomizer = new SvmClassifierCustomizer();
        } else if (currentLearner == LearnerType.MPBoost) {
            learner = new AdaBoostLearner();
            AdaBoostLearnerCustomizer customizer2 = new AdaBoostLearnerCustomizer();
            customizer2.setNumIterations(boostIterationCount);
            customizer2
                    .setWeakLearner(boostThreadCount > 1 ? new MPWeakLearnerMultiThread(
                            boostThreadCount) : new MPWeakLearner());
            customizer2.setPerCategoryNormalization(true);
            customizer2.setLossFunction(new ExponentialLoss());
            customizer2.keepDistributionMatrix(false);
            customizer2
                    .setInitialDistributionType(InitialDistributionMatrixType.UNIFORM);
            learner.setRuntimeCustomizer(customizer2);
            AdaBoostClassifierCustomizer classifierCustomizer2 = new AdaBoostClassifierCustomizer();
            classifierCustomizer2.groupHypothesis(true);
            customizer = customizer2;
            classifierCustomizer = classifierCustomizer2;
        }

        int realFolds = Math.min(kValue, index.getDocumentDB()
                .getDocumentsCount());

        TIntArrayList[] folds = new TIntArrayList[realFolds];

        int docsCount = index.getDocumentDB().getDocumentsCount();
        IIntIterator docs = index.getDocumentDB().getDocuments();

        int foldSize = docsCount / realFolds;

        for (int j = 0; j < realFolds; ++j) {
            TIntArrayList fold = new TIntArrayList();
            while (fold.size() < foldSize && docs.hasNext())
                fold.add(docs.next());
            folds[j] = fold;
        }
        while (docs.hasNext())
            folds[realFolds - 1].add(docs.next());

        // generate a vector of categories with number of true classifications e
        // predicted confidences

        int categoriesCount = index.getCategoryDB().getCategoriesCount();

        Vector<MutablePair<Integer, Vector<Double>>> classificationInfo = new Vector<MutablePair<Integer, Vector<Double>>>(
                categoriesCount);

        for (int i = 0; i < categoriesCount; i++) {
            classificationInfo.add(new MutablePair<Integer, Vector<Double>>(0,
                    new Vector<Double>()));
        }

        for (int e = 0; e < realFolds; ++e) {

            TIntArrayList trainingDocs = new TIntArrayList();
            TIntArrayList testDocs = folds[e];
            for (int j = 0; j < realFolds; ++j) {
                if (j != e) {
                    TIntArrayList fold = folds[j];
                    for (int k = 0; k < fold.size(); ++k)
                        trainingDocs.add(fold.get(k));
                }
            }
            IIndex trainingIndex = index.cloneIndex();

            FilteredIntIterator trainingRemovedDocuments = new FilteredIntIterator(
                    docs, new TIntArrayListIterator(trainingDocs), true);
            trainingIndex.removeDocuments(trainingRemovedDocuments, false);
            assert (trainingIndex.getDocumentDB().getDocumentsCount() == trainingDocs
                    .size());

            IIndex testIndex = index.cloneIndex();

            docs.begin();
            FilteredIntIterator testRemovedDocuments = new FilteredIntIterator(
                    docs, new TIntArrayListIterator(testDocs), true);
            testIndex.removeDocuments(testRemovedDocuments, false);
            assert (testIndex.getDocumentDB().getDocumentsCount() == testDocs
                    .size());

            IClassifier cl = learner.build(trainingIndex);
            cl.setRuntimeCustomizer(classifierCustomizer);
            Classifier classifier = new Classifier(testIndex, cl, true);
            classifier.exec();
            cl.destroy();

            IClassificationDB trueClassification = testIndex
                    .getClassificationDB();
            ClassificationScoreDB predConfidences = classifier.getConfidences();

            IShortIterator catsIter = trueClassification.getCategoryDB()
                    .getCategories();
            IIntIterator docsIter = trueClassification.getDocumentDB()
                    .getDocuments();

            while (catsIter.hasNext()) {
                short catId = catsIter.next();
                classificationInfo.get(catId).setFirst(
                        classificationInfo.get(catId).getFirst()
                                + trueClassification
                                .getCategoryDocumentsCount(catId));
                Vector<Double> catConfidencesVect = classificationInfo.get(
                        catId).getSecond();
                docsIter.begin();
                while (docsIter.hasNext()) {
                    int docId = docsIter.next();
                    Hashtable<Short, ClassifierRangeWithScore> catConfidences = predConfidences
                            .getDocumentScoresAsHashtable(docId);
                    ClassifierRangeWithScore value = catConfidences.get(catId);
                    catConfidencesVect.add(value.score - value.border);
                }
            }

            if (isEvaluate()) {
                if (globalCT == null) {
                    globalCT = new ContingencyTableSet();
                }
                ContingencyTableSet tableSet;
                IClassificationDB predictions = classifier
                        .getClassificationDB();
                ClassificationComparer cc = new ClassificationComparer(
                        predictions, trueClassification);
                tableSet = cc.evaluate();

                IShortIterator evalCats = tableSet.getEvaluatedCategories();
                while (evalCats.hasNext()) {
                    short category = evalCats.next();
                    globalCT.addContingenyTable(category,
                            tableSet.getCategoryContingencyTable(category));
                }
            }
        }

        result = searchOptimum(classificationInfo, parameters.clone());

    }

    private double probability(double x, double sigma) {
        double y = Math.exp(x / sigma);
        y = y / (y + 1.0);
        if (Double.isNaN(y)) {
            return x > 0 ? 1.0 : 0.0;
        } else {
            return y;
        }
    }

    private Pair<Vector<Pair<Double, Double>>, Pair<Double, Double>> searchOptimum(
            Vector<MutablePair<Integer, Vector<Double>>> classificationInfo,
            double[] parameters) {
        return searchOptimum(classificationInfo, parameters, -1);
    }

    private Pair<Vector<Pair<Double, Double>>, Pair<Double, Double>> searchOptimum(
            Vector<MutablePair<Integer, Vector<Double>>> classificationInfo,
            double[] parameters, int category) {

        Vector<Pair<Double, Double>> result = new Vector<Pair<Double, Double>>();

        double minDiff = Double.POSITIVE_INFINITY;
        int optParameterIndex = -1;
        double optParameter = 1.0;

        for (int n = 1; n <= 2; n++) {

            if (n == 2) {
                if (optParameterIndex == 0) {
                    optParameterIndex++;
                }
                double start = parameters[optParameterIndex - 1];
                if (optParameterIndex == parameters.length - 1) {
                    optParameterIndex--;
                }
                double stop = parameters[optParameterIndex + 1];
                double step = (stop - start) / 1000.0;
                parameters = new double[1001];
                for (int i = 0; i < parameters.length; i++) {
                    parameters[i] = start + (i * step);
                }
            }

            for (int i = 0; i < parameters.length; i++) {
                double sigma = parameters[i];
                double diff = 0.0;
                if (isMacro() || category != -1) {
                    for (int catId = 0; catId < classificationInfo.size(); catId++) {
                        if (catId == category || category == -1) {
                            double trues = classificationInfo.get(catId)
                                    .getFirst();
                            Vector<Double> catConfidencesVect = classificationInfo
                                    .get(catId).getSecond();
                            double probabilities = 0.0;
                            for (Iterator<Double> iterator = catConfidencesVect
                                    .iterator(); iterator.hasNext(); ) {
                                probabilities += probability(iterator.next(),
                                        sigma);
                            }
                            diff += Math.abs(trues - probabilities);
                        }
                    }
                } else if (isMicro()) {
                    double trues = 0.0;
                    double probabilities = 0.0;
                    for (int catId = 0; catId < classificationInfo.size(); catId++) {
                        trues += classificationInfo.get(catId).getFirst();
                        Vector<Double> catConfidencesVect = classificationInfo
                                .get(catId).getSecond();
                        for (Iterator<Double> iterator = catConfidencesVect
                                .iterator(); iterator.hasNext(); ) {
                            probabilities += probability(iterator.next(), sigma);
                        }
                    }
                    diff = Math.abs(trues - probabilities);
                }
                if (diff < minDiff) {
                    optParameterIndex = i;
                    optParameter = parameters[i];
                    minDiff = diff;
                }
                result.add(new Pair<Double, Double>(parameters[i], diff));
            }
        }
        return new Pair<Vector<Pair<Double, Double>>, Pair<Double, Double>>(
                result, new Pair<Double, Double>(optParameter, minDiff));
    }

    public enum LearnerType {
        MPBoost, SVMlib
    }

}
