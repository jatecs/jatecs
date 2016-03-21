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

package apps.activeLearning;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import it.cnr.jatecs.activelearning.*;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.adaboost.*;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.classification.mpboost.MPWeakLearnerMultiThread;
import it.cnr.jatecs.classification.svm.SvmClassifierCustomizer;
import it.cnr.jatecs.classification.svm.SvmLearner;
import it.cnr.jatecs.classification.svm.SvmLearnerCustomizer;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.evaluation.measures.F;
import it.cnr.jatecs.evaluation.util.EvaluationReport;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.satc.gain.GainSmooth1Avg;
import it.cnr.jatecs.satc.parameterTuning.ProbabilitySlope;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Run active learning experiments on a training and test index pair. This
 * program allow to compare a number of different AL policies implemented in the
 * it.cnr.jatecs.activeLearning package, using two different learning algorithms
 * (boosting and SVMs).
 *
 * @author giacomo
 */
public class ActiveLearning {

    public static void main(String[] args) throws IOException {
        if (args.length != 9) {
            System.err
                    .println("Usage: ActiveLearning "
                            + "<trainingSet> "
                            + "<testSet> "
                            + "<evaluationResultsPath> "
                            + "<ActiveLearningPolicy> <trainingSamplesNumber> "
                            + " <samplesPerStep> <maxIterations> <macro|micro> <MPBoost-1000|SVMlib>");
            return;
        }

        // TODO should be a parameter
        // If true dump only macro and micro f1, not all the Contingency Tables
        boolean light = false;

        String filename = args[0];
        File file = new File(filename);
        FileSystemStorageManager storage = new FileSystemStorageManager(
                file.getParent(), false);
        storage.open();
        String trainName = file.getName();
        IIndex trainingSet = TroveReadWriteHelper.readIndex(storage, trainName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);
        storage.close();
        filename = args[1];
        file = new File(filename);
        storage = new FileSystemStorageManager(file.getParent(), false);
        storage.open();
        String testName = file.getName();
        IIndex testSet = TroveReadWriteHelper.readIndex(storage, testName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);
        storage.close();

        String policy = args[3];

        int trainSplit = Integer.parseInt(args[4]);
        int stepSamples = Integer.parseInt(args[5]);
        int maxIterations = Integer.parseInt(args[6]);

        String rankType = args[7];

        String classifierType = args[8];

        // computation time of a method
        long time = 0;

        int categoriesCount = trainingSet.getCategoryDB().getCategoriesCount();

        TIntHashSet categoriesFilter = new TIntHashSet(
                (int) (categoriesCount + categoriesCount * 0.25), (float) 0.75);
        for (int i = 0; i < categoriesCount; i++) {
            categoriesFilter.add(i);
        }

        int trainSize = trainingSet.getDocumentDB().getDocumentsCount();
        int testSize = testSet.getDocumentDB().getDocumentsCount();

        int randomIterations = 1;
        if (policy.indexOf("random") > -1) {
            randomIterations *= Integer.parseInt(policy.substring(6));
        }

        for (int ran = 0; ran < randomIterations; ran++) {

            long totalTime = System.currentTimeMillis();

            String resultsPath = args[2];

            if (policy.indexOf("random") > -1) {
                resultsPath += Os.pathSeparator() + "r" + ran;
                (new File(resultsPath)).mkdirs();
            }

            if (policy.indexOf("random") > -1) {
                resultsPath += "r" + ran;
            }

            FileWriter outFile = new FileWriter(resultsPath
                    + (light ? "_LIGHT" : ""));
            BufferedWriter writer = new BufferedWriter(outFile);

            float[] F1s = new float[testSize + 1];

            int stepCount = Math.min(
                    (int) Math.ceil((trainSize - trainSplit) / stepSamples),
                    maxIterations);

            TIntArrayList trainIds = new TIntArrayList();
            TIntArrayList unlabelIds = new TIntArrayList();

            // leave the first currTrainSize documents in the training set, the
            // rest in the unlabelled set
            for (int i = 0; i < trainSize; i++) {
                if (i < trainSplit) {
                    trainIds.add(i);
                } else {
                    unlabelIds.add(i);
                }
            }
            IIndex currTrainSet = trainingSet.cloneIndex();
            currTrainSet.removeDocuments(new TIntArrayListIterator(unlabelIds),
                    false);
            IIndex currUnlabelSet = trainingSet.cloneIndex();
            currUnlabelSet.removeDocuments(new TIntArrayListIterator(trainIds),
                    false);
            Adaptive.reWeight(currTrainSet, currTrainSet);
            Adaptive.reWeight(currUnlabelSet, currTrainSet);

            for (int i = 0; i < stepCount; i++) {

                long startTime = System.currentTimeMillis();

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
                    customizer2.setWeakLearner(new MPWeakLearnerMultiThread(2));
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

                IClassifier cl = learner.build(currTrainSet);
                cl.setRuntimeCustomizer(classifierCustomizer);

                Classifier classifier = new Classifier(currUnlabelSet, cl, true);
                classifier.exec();
                IClassificationDB unlabelClassification = classifier
                        .getClassificationDB();
                ClassificationScoreDB unlabelConfidences = classifier.getConfidences();

                PoolRank al = null;
                if (policy.equals("PM")) {
                    al = new SATCconfidenceRank(unlabelConfidences,
                            currTrainSet, categoriesFilter, parametersTuning(
                            currTrainSet, categoriesCount,
                            classifierType, false).getFirst());
                } else if (policy.equals("greedy-smooth1-avg")) {
                    Pair<double[], ContingencyTableSet> params = parametersTuning(
                            currTrainSet, categoriesCount, classifierType, true);
                    al = new SATCutilityRank(unlabelConfidences, currTrainSet,
                            params.getSecond(), categoriesFilter,
                            new GainSmooth1Avg(new F(1)), params.getFirst());
                } else if (policy.equals("MMU")) {
                    al = new MMU(unlabelConfidences, currTrainSet);
                } else if (policy.equals("LCI")) {
                    al = new LCI(unlabelConfidences, currTrainSet,
                            unlabelClassification);
                } else if (policy.equals("adaptive")) {
                    al = new Adaptive(unlabelConfidences, currTrainSet,
                            unlabelClassification, currUnlabelSet, trainingSet,
                            stepSamples, new double[]{0.0, 0.1, 0.2, 0.3,
                            0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0},
                            learner, classifierCustomizer);
                }
                //
                // else {
                // samplesToLabel = new TIntArrayList();
                // for (int j = 0; j <
                // currUnlabelSet.getDocumentsDB().getDocumentsCount(); j++) {
                // samplesToLabel.add(j);
                // }
                // }
                TIntArrayList samplesToLabel = null;
                if (rankType.equals("macro")) {
                    samplesToLabel = al.getFirstMacro(stepSamples);
                } else if (rankType.equals("micro")) {
                    samplesToLabel = al.getFirstMicro(stepSamples);
                }

                Pair<IIndex, IIndex> labelUnlabel = null;

                if (!samplesToLabel.isEmpty()) {
                    try {
                        labelUnlabel = Adaptive.moveDocuments(currTrainSet,
                                currUnlabelSet, samplesToLabel, trainingSet);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                    currTrainSet = labelUnlabel.getFirst();
                    currUnlabelSet = labelUnlabel.getSecond();
                }

                time += System.currentTimeMillis() - startTime;

                classifier = new Classifier(testSet, cl, true);
                classifier.exec();
                cl.destroy();

                ClassificationComparer flatComparer = new ClassificationComparer(
                        classifier.getClassificationDB(),
                        testSet.getClassificationDB());
                ContingencyTableSet evaluation = flatComparer.evaluate(false);
                if (light) {
                    if (rankType.equals("macro")) {
                        F1s[i + 1] = (float) evaluation.macroF1();
                    } else if (rankType.equals("micro")) {
                        F1s[i + 1] = (float) evaluation
                                .getGlobalContingencyTable().f1();
                    }
                } else {
                    writer.write("\n\n\t"
                            + String.valueOf(i + 1)
                            + "\n\n"
                            + EvaluationReport.printReport(evaluation,
                            testSet.getCategoryDB()));
                }

                if (samplesToLabel.isEmpty()) {
                    break;
                }

            }

            if (light) {
                writer.write(Arrays.toString(F1s));
            }

            writer.close();

            System.out.println(testName + " " + policy + " RANKING TIME: "
                    + time + " TOTAL TIME: "
                    + (System.currentTimeMillis() - totalTime));

            // Reliability reliability = new Reliability(predConfidences,
            // trueClassification, probabilitySlopes, categoriesFilter);

            // DecimalFormat df = new DecimalFormat(".###");
            // System.out.print(predClassification.getName()
            // + " " + policy + " " + cat
            // + "\n" + "reliability index: " //+ reliability.get()
            // + "\n" + df.format(eer.getMacro(0.05))
            // + " " + df.format(eer.getMacro(0.1)) + " "
            // + df.format(eer.getMacro(0.2)) + "\n");
        }

    }

    private static Pair<double[], ContingencyTableSet> parametersTuning(
            IIndex trainingSet, int categoriesCount, String learner,
            boolean evaluate) {
        ProbabilitySlope.LearnerType currLearner = null;
        if (learner.equals("SVMlib")) {
            currLearner = ProbabilitySlope.LearnerType.SVMlib;
        } else if (learner.equals("MPBoost-1000")) {
            currLearner = ProbabilitySlope.LearnerType.MPBoost;
        }
        double[] probabilitySlopes = new double[categoriesCount];
        ProbabilitySlope p = new ProbabilitySlope(trainingSet);
        p.setCurrentLearner(currLearner);
        p.setEvaluate(evaluate);
        p.exec();
        double sigma = p.getOptimumParameter();
        for (int i = 0; i < categoriesCount; i++) {
            probabilitySlopes[i] = sigma;
        }
        ContingencyTableSet evaluation = null;
        if (evaluate) {
            evaluation = p.getGlobalCT();
        }
        return new Pair<double[], ContingencyTableSet>(probabilitySlopes,
                evaluation);
    }

}