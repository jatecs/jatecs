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

package apps.satc;

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
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.MutablePair;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.FilteredIntIterator;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class SATCparametersTuningGeneric {

    public static double probability(double x, double sigma) {
        double y = Math.exp(x / sigma);
        y = y / (y + 1.0);
        if (Double.isNaN(y)) {
            return x > 0 ? 1.0 : 0.0;
        } else {
            return y;
        }
    }

    public static Pair<Vector<Pair<Double, Double>>, Pair<Double, Double>> searchOptimum(
            Vector<MutablePair<Integer, Vector<Double>>> classificationInfo,
            double[] parameters, String rankType) {
        return searchOptimum(classificationInfo, parameters, rankType, -1);
    }

    public static Pair<Vector<Pair<Double, Double>>, Pair<Double, Double>> searchOptimum(
            Vector<MutablePair<Integer, Vector<Double>>> classificationInfo,
            double[] parameters, String rankType, int category) {

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
                if (rankType.equals("macro") || category != -1) {
                    for (int catId = 0; catId < classificationInfo.size(); catId++) {
                        if (catId == category || category == -1) {
                            double trues = classificationInfo.get(catId)
                                    .getFirst();
                            Vector<Double> catConfidencesVect = classificationInfo
                                    .get(catId).getSecond();
                            double probabilities = 0.0;
                            for (Iterator<Double> iterator = catConfidencesVect
                                    .iterator(); iterator.hasNext(); ) {
                                probabilities += probability(
                                        (Double) iterator.next(), sigma);
                            }
                            diff += Math.abs(trues - probabilities);
                        }
                    }
                } else if (rankType.equals("micro")) {
                    double trues = 0.0;
                    double probabilities = 0.0;
                    for (int catId = 0; catId < classificationInfo.size(); catId++) {
                        trues += classificationInfo.get(catId).getFirst();
                        Vector<Double> catConfidencesVect = classificationInfo
                                .get(catId).getSecond();
                        for (Iterator<Double> iterator = catConfidencesVect.iterator(); iterator
                                .hasNext(); ) {
                            probabilities += probability(
                                    (Double) iterator.next(), sigma);
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

    public static void main(String[] args) throws Exception {
        if (args.length < 7) {
            System.err
                    .println("Usage: SATCparametersTuningGeneric <'MPBoost-1000'|'SVMlib'> <MPboostThreadCount> <k-value> "
                            + "<indexDirectory> <listOfPrametersDividedBy:> <outputFilePath> <'macro'|'micro'> ['overwrite']");
            return;
        }

        String classifierType = args[0];

        int threadCount = Integer.parseInt(args[1]);

        int kFold = Integer.parseInt(args[2]);

        String dataPath = args[3];

        String[] parametersStrings = args[4].split(":");
        double[] parameters = new double[parametersStrings.length];
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = Double.parseDouble(parametersStrings[i]);
        }

        FileWriter outFile = new FileWriter(args[5]);
        BufferedWriter output = new BufferedWriter(outFile);

        String rankType = args[6];

        boolean overwrite = false;
        if (args.length > 7 && args[7].equals("overwrite")) {
            overwrite = true;
        }

        File file = new File(dataPath);

        String indexName = file.getName();
        dataPath = file.getParent();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                dataPath, false);
        storageManager.open();
        IIndex index = TroveReadWriteHelper.readIndex(storageManager,
                indexName, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        ILearner learner = null;
        ILearnerRuntimeCustomizer customizer = null;
        IClassifierRuntimeCustomizer classifierCustomizer = null;

        // LEARNING
        if (classifierType.equals("SVMlib")) {
            learner = new SvmLearner();
            customizer = new SvmLearnerCustomizer();
            learner.setRuntimeCustomizer(customizer);
            classifierCustomizer = new SvmClassifierCustomizer();
        } else if (classifierType.equals("MPBoost-1000")) {
            learner = new AdaBoostLearner();
            AdaBoostLearnerCustomizer customizer2 = new AdaBoostLearnerCustomizer();
            customizer2.setNumIterations(1000);
            customizer2
                    .setWeakLearner(threadCount > 1 ? new MPWeakLearnerMultiThread(
                            threadCount) : new MPWeakLearner());
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

        int realFolds = Math.min(kFold, index.getDocumentDB()
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

            String trueClassificationName = indexName + "_" + classifierType
                    + "_SATCparametersTuning-trueClassification-" + e + ":"
                    + kFold;

            String predConfidencesPath = dataPath + Os.pathSeparator()
                    + indexName + "_" + classifierType
                    + "_SATCparametersTuning-predConfidences-" + e + ":"
                    + kFold;

            IClassificationDB trueClassification = null;
            ClassificationScoreDB predConfidences = null;

            if (overwrite
                    || !(new File(dataPath + Os.pathSeparator()
                    + trueClassificationName)).exists()
                    || !(new File(predConfidencesPath)).exists()) {

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

                trueClassification = testIndex.getClassificationDB();
                predConfidences = classifier.getConfidences();

                storageManager = new FileSystemStorageManager(dataPath, false);
                storageManager.open();
                TroveReadWriteHelper.writeClassification(storageManager,
                        trueClassification, trueClassificationName, true);
                storageManager.close();
                ClassificationScoreDB.write(predConfidencesPath, predConfidences);

            } else {
                storageManager = new FileSystemStorageManager(dataPath, false);
                storageManager.open();
                trueClassification = TroveReadWriteHelper.readClassification(
                        storageManager, trueClassificationName);
                storageManager.close();
                predConfidences = ClassificationScoreDB.read(predConfidencesPath);
            }

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
        }

        Pair<Vector<Pair<Double, Double>>, Pair<Double, Double>> searchResult = searchOptimum(
                classificationInfo, parameters.clone(), rankType);
        output.write("category\tsigma\ttrueValues - probabilities\n");
        output.write("multi-label:\t" + searchResult.getSecond().getFirst()
                + "\t" + searchResult.getSecond().getSecond() + "\n");
        for (int catId = 0; catId < categoriesCount; catId++) {
            searchResult = searchOptimum(classificationInfo,
                    parameters.clone(), rankType, catId);
            output.write(catId + ":\t" + searchResult.getSecond().getFirst()
                    + "\t" + searchResult.getSecond().getSecond() + "\n");
        }
        output.close();

    }

}
