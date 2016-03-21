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
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;
import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.classification.svm.SvmClassifierCustomizer;
import it.cnr.jatecs.classification.svm.SvmLearner;
import it.cnr.jatecs.classification.svm.SvmLearnerCustomizer;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTableDataManager;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.evaluation.measures.CTmeasure;
import it.cnr.jatecs.evaluation.measures.F;
import it.cnr.jatecs.evaluation.measures.K;
import it.cnr.jatecs.evaluation.util.EvaluationReport;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.satc.evaluation.Reliability;
import it.cnr.jatecs.satc.gain.*;
import it.cnr.jatecs.satc.interfaces.IIncrementalRank;
import it.cnr.jatecs.satc.interfaces.IStaticRank;
import it.cnr.jatecs.satc.rank.*;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

public class SATCsimulation {

    public static void main(String[] args) throws IOException {
        if (args.length < 10) {
            System.err
                    .println("Usage: SATCsimulation "
                            + "<trueClassification> <predictedClassification> "
                            + "<estimatedEvaluation> <evaluationResultsPath> "
                            + "<rankPolicy> <trainingSetSize> <'f1'|'f05'|'f2'|'k'> <'macro'|'micro'> "
                            + "<slopeParameterForProbability> "
                            + "<prevalencies>"
                            // + "[FPgainWeight] [FNgainWeight] "
                            // + "[topCategoriesPerDocument] "
                            + "[categoriesToUseDividedByComma|'binary']");
            return;
        }

        // FIXME must be a parameter
        // If true dump only macro and micro F, not all the Contingency Tables
        boolean light = false;

        String filename = args[0];

        File fileTrues = new File(filename);

        String trueName = fileTrues.getName();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                fileTrues.getParent(), false);
        storageManager.open();
        IClassificationDB trueClassification = TroveReadWriteHelper
                .readClassification(storageManager, trueName);
        storageManager.close();

        filename = args[1];

        File filePred = new File(filename);

        String predName = filePred.getName();
        String path = filePred.getParent();

        storageManager = new FileSystemStorageManager(path, false);
        storageManager.open();
        IClassificationDB predClassification = TroveReadWriteHelper
                .readClassification(storageManager, predName);
        storageManager.close();

        ClassificationScoreDB predConfidences = ClassificationScoreDB
                .read(path + Os.pathSeparator()
                        + predName.substring(0, predName.length() - 4)
                        + ".confidences");

        String estimatedEvaluationPath = args[2];

        ContingencyTableSet estimatedEvaluation = ContingencyTableDataManager
                .readContingencyTableSet(estimatedEvaluationPath);

        String policy = args[4];

        int trainingSize = Integer.parseInt(args[5]);

        String measureArg = args[6].toLowerCase();

        CTmeasure measure = null;

        if (measureArg.equals("f1")) {
            measure = new F(1.0);
        } else if (measureArg.equals("f05")) {
            measure = new F(0.5);
            light = true;
        } else if (measureArg.equals("f2")) {
            measure = new F(2.0);
            light = true;
        } else if (measureArg.equals("k")) {
            measure = new K();
            light = true;
        }

        // TODO add a third value for creating both macro and micro rankings
        String rankType = args[7];

        // computation time of a method
        long time = 0;

        int categoriesCount = predClassification.getCategoryDB()
                .getCategoriesCount();
        int testSize = trueClassification.getDocumentDB().getDocumentsCount();

        String[] parametersStrings = args[8].split(":");
        double[] probabilitySlopes = new double[categoriesCount];
        for (int i = 0; i < categoriesCount; i++) {
            if (categoriesCount == parametersStrings.length) {
                probabilitySlopes[i] = Double.parseDouble(parametersStrings[i]);
            } else {
                probabilitySlopes[i] = Double.parseDouble(parametersStrings[0]);
            }
        }

        String[] prevalenciesStrings = args[9].split(":");
        double[] prevalencies = new double[categoriesCount];
        if (categoriesCount == prevalenciesStrings.length) {
            for (int i = 0; i < categoriesCount; i++) {
                prevalencies[i] = Double.parseDouble(prevalenciesStrings[i]);
            }
        } else {
            prevalencies = new double[0];
        }

        Double gainFPweight = 1.0;
        Double gainFNweight = 1.0;
        int topProbabilities = categoriesCount;

        boolean binary = false;
        TIntHashSet categoriesFilter = new TIntHashSet(
                (int) (categoriesCount + categoriesCount * 0.25), (float) 0.75);

        // if (args.length > 8) {
        // gainFPweight = Double.parseDouble(args[8]);
        // gainFNweight = Double.parseDouble(args[9]);
        // if (args.length > 10) {
        // topProbabilities = Integer.parseInt(args[10]);
        if (args.length > 10) {
            if (args[10].equals("binary")) {
                binary = true;
            } else {
                String[] categoriesStrings = args[10].split(":");
                int[] categories = new int[categoriesStrings.length];
                for (int i = 0; i < categories.length; i++) {
                    categories[i] = Integer.parseInt(categoriesStrings[i]);
                }
                categoriesFilter = new TIntHashSet(categories);
            }
        } else {
            for (int i = 0; i < categoriesCount; i++) {
                categoriesFilter.add(i);
            }
        }
        // }
        // }

        int categoryIterations = 1;
        if (binary) {
            categoryIterations = categoriesCount;
        }

        int randomIterations = 1;
        if (policy.indexOf("random") > -1) {
            randomIterations *= Integer.parseInt(policy.substring(6));
        }
        if (policy.indexOf("PM-superOracle") > -1) {
            randomIterations *= Integer.parseInt(policy.substring(14));
        }

        Reliability reliability = new Reliability(predConfidences,
                trueClassification, probabilitySlopes, categoriesFilter);

        for (int cat = 0; cat < categoryIterations; cat++) {
            for (int ran = 0; ran < randomIterations; ran++) {

                long totalTime = System.currentTimeMillis();

                String resultsPath = args[3];

                if (policy.indexOf("random") > -1 || binary) {
                    resultsPath += Os.pathSeparator();
                    (new File(resultsPath)).mkdirs();
                }

                if (policy.indexOf("random") > -1) {
                    resultsPath += "r" + ran;
                }

                if (binary) {
                    categoriesFilter = new TIntHashSet();
                    categoriesFilter.add(cat);
                    resultsPath += "c" + cat;
                }

                if (policy.indexOf("PM-superOracle") > -1) {
                    resultsPath += Os.pathSeparator();
                    (new File(resultsPath)).mkdirs();
                    resultsPath += "s" + ran;
                }

                TShortArrayList validCategoriesList = new TShortArrayList(0);
                for (TIntIterator it = categoriesFilter.iterator(); it
                        .hasNext(); ) {
                    validCategoriesList.add((short) it.next());
                }
                TShortArrayListIterator validCategories = new TShortArrayListIterator(
                        validCategoriesList);

                ClassificationComparer flatComparer = new ClassificationComparer(
                        predClassification, trueClassification,
                        validCategories, true);
                ContingencyTableSet evaluation = flatComparer.evaluate(false);

                if (evaluation.macroF1() == 1.0) {
                    System.out.println(predClassification.getName() + " "
                            + policy + " " + cat
                            + "\nNo classification error to correct.");
                    continue;
                }

                // ContingencyTableSet[] evaluations = new
                // ContingencyTableSet[testSize];
                // ExpectedErrorReduction eer = new ExpectedErrorReduction(
                // evaluation, evaluations);

                // used by the active learning rankers
                IIndex trainingSet = null;

                IStaticRank rankObject = null;

                long startTime = System.currentTimeMillis();
                if (policy.indexOf("random") > -1) {
                    rankObject = new Random(trainingSize, predConfidences,
                            categoriesFilter);
                } else if ("PM".equals(policy)) {
                    rankObject = new ConfidenceBased(trainingSize,
                            predConfidences, categoriesFilter,
                            probabilitySlopes, topProbabilities);
                } else if ("greedy".equals(policy)) {
                    rankObject = new UtilityBased(trainingSize,
                            predConfidences, categoriesFilter,
                            UtilityBased.EstimationType.TEST,
                            estimatedEvaluation, new Gain(measure),
                            probabilitySlopes, prevalencies, topProbabilities,
                            gainFPweight, gainFNweight);
                } else if ("greedy-oracle".equals(policy)) {
                    rankObject = new UtilityBased(trainingSize,
                            predConfidences, categoriesFilter,
                            UtilityBased.EstimationType.NONE, evaluation,
                            new GainOracle(measure), probabilitySlopes,
                            prevalencies, topProbabilities, gainFPweight,
                            gainFNweight);
                } else if ("greedy-oracle-avg".equals(policy)) {
                    rankObject = new UtilityBased(trainingSize,
                            predConfidences, categoriesFilter,
                            UtilityBased.EstimationType.NONE, evaluation,
                            new GainAvg(measure), probabilitySlopes,
                            prevalencies, topProbabilities, gainFPweight,
                            gainFNweight);
                } else if (policy.indexOf("PM-superOracle") > -1) {
                    rankObject = new ConfidenceBasedOracle(trainingSize,
                            getTrueConfidence(trueClassification,
                                    predClassification, predConfidences),
                            categoriesFilter
                    /* use weight? */);
                } else if ("greedy-superOracle".equals(policy)) {
                    rankObject = new UtilityBasedOracle(trainingSize,
                            getTrueConfidence(trueClassification,
                                    predClassification, predConfidences),
                            categoriesFilter, UtilityBased.EstimationType.NONE,
                            evaluation, new GainAvg(measure)
					/* use weight? */);
                } else if ("greedy-smooth1".equals(policy)) {
                    rankObject = new UtilityBased(trainingSize,
                            predConfidences, categoriesFilter,
                            UtilityBased.EstimationType.TEST,
                            estimatedEvaluation, new GainSmooth1(measure),
                            probabilitySlopes, prevalencies, topProbabilities,
                            gainFPweight, gainFNweight);
                } else if ("greedy-smooth1-avg".equals(policy)) {
                    rankObject = new UtilityBased(trainingSize,
                            predConfidences, categoriesFilter,
                            UtilityBased.EstimationType.TEST,
                            estimatedEvaluation, new GainSmooth1Avg(measure),
                            probabilitySlopes, prevalencies, topProbabilities,
                            gainFPweight, gainFNweight);
                } else if ("greedy-smoothE".equals(policy)) {
                    // to do
                } else if ("incremental-smooth1".equals(policy)) {
                    rankObject = new Incremental(trainingSize, predConfidences,
                            categoriesFilter, UtilityBased.EstimationType.TEST,
                            estimatedEvaluation, new GainSmooth1(measure),
                            new GainSmooth1(measure), probabilitySlopes,
                            prevalencies);
                } else if ("incremental-oracle".equals(policy)) {
                    rankObject = new Incremental(trainingSize, predConfidences,
                            categoriesFilter, UtilityBased.EstimationType.NONE,
                            evaluation, new Gain(measure), new Gain(measure),
                            probabilitySlopes, prevalencies);
                } else if ("incremental-superOracle".equals(policy)) {
                    rankObject = new IncrementalOracle(trainingSize,
                            getTrueConfidence(trueClassification,
                                    predClassification, predConfidences),
                            categoriesFilter, UtilityBased.EstimationType.NONE,
                            evaluation, new Gain(measure), new Gain(measure));
                } else if ("MMU".equals(policy)) {
                    if (trainingSet == null) {
                        storageManager = new FileSystemStorageManager(
                                fileTrues.getParent(), false);
                        storageManager.open();
                        trainingSet = TroveReadWriteHelper.readIndex(
                                storageManager,
                                trueName.replaceAll("test.*", "training"),
                                TroveContentDBType.Full,
                                TroveClassificationDBType.Full);
                        storageManager.close();
                    }
                    rankObject = new ALMMU(predConfidences, trainingSet);
                } else if ("LCI".equals(policy)) {
                    if (trainingSet == null) {
                        storageManager = new FileSystemStorageManager(
                                fileTrues.getParent(), false);
                        storageManager.open();
                        trainingSet = TroveReadWriteHelper.readIndex(
                                storageManager,
                                trueName.replaceAll("test.*", "training"),
                                TroveContentDBType.Full,
                                TroveClassificationDBType.Full);
                        storageManager.close();
                    }
                    rankObject = new ALLCI(predConfidences, trainingSet,
                            predClassification);
                } else if ("adaptive".equals(policy)) {
                    if (trainingSet == null) {
                        storageManager = new FileSystemStorageManager(
                                fileTrues.getParent(), false);
                        storageManager.open();
                        trainingSet = TroveReadWriteHelper.readIndex(
                                storageManager,
                                trueName.replaceAll("test.*", "training"),
                                TroveContentDBType.Full,
                                TroveClassificationDBType.Full);
                        storageManager.close();
                    }
                    SvmLearner learner = new SvmLearner();
                    SvmLearnerCustomizer customizer = new SvmLearnerCustomizer();
                    learner.setRuntimeCustomizer(customizer);
                    SvmClassifierCustomizer classifierCustomizer = new SvmClassifierCustomizer();
                    storageManager = new FileSystemStorageManager(
                            fileTrues.getParent(), false);
                    storageManager.open();
                    IIndex index = TroveReadWriteHelper.readIndex(
                            storageManager, trueName);
                    storageManager.close();
                    rankObject = new ALAdaptive(predConfidences, trainingSet,
                            predClassification, index,
                            new double[]{0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6,
                                    0.7, 0.8, 0.9, 1.0}, learner,
                            classifierCustomizer);
                }
                time += System.currentTimeMillis() - startTime;

                FileWriter outFile = new FileWriter(resultsPath
                        + (light ? "_LIGHT" : ""));
                BufferedWriter writer = new BufferedWriter(outFile);

                float[] evals = new float[testSize + 1];

                if (light) {
                    if (rankType.equals("macro")) {
                        evals[0] = (float) getEval(evaluation, measureArg, true);
                    } else if (rankType.equals("micro")) {
                        evals[0] = (float) getEval(evaluation, measureArg,
                                false);
                    }
                } else {
                    writer.write("\t0\n\n"
                            + EvaluationReport.printReport(evaluation,
                            trueClassification.getCategoryDB()));
                }

                TIntArrayList rank = null;
                TIntHashSet currRank = new TIntHashSet(
                        (int) (testSize + testSize * 0.25), (float) 0.75);

                if (policy.indexOf("incremental") == -1) {

                    startTime = System.currentTimeMillis();
                    if (rankType.equals("macro")) {
                        rank = rankObject.getMacroRank();
                    } else if (rankType.equals("micro")) {
                        rank = rankObject.getMicroRank();
                    }
                    time += System.currentTimeMillis() - startTime;
                    for (int i = 0; i < testSize; i++) {
                        currRank.add(rank.getQuick(i));
                        // System.out.println(rank.getQuick(i)
                        // + " "
                        // + cat
                        // + " "
                        // + +(flatComparer
                        // .evaluate(false)
                        // .getCategoryContingencyTable(
                        // (short) cat).tp() - evaluation
                        // .getCategoryContingencyTable(
                        // (short) cat).tp())
                        // + " "
                        // + (flatComparer
                        // .evaluate(false)
                        // .getCategoryContingencyTable(
                        // (short) cat).tn() - evaluation
                        // .getCategoryContingencyTable(
                        // (short) cat).tn()));

                        evaluation = flatComparer.evaluateMixedFast(currRank);
                        if (light) {
                            if (rankType.equals("macro")) {
                                evals[i + 1] = (float) getEval(evaluation,
                                        measureArg, true);
                            } else if (rankType.equals("micro")) {
                                evals[i + 1] = (float) getEval(evaluation,
                                        measureArg, false);
                            }
                        } else {
                            writer.write("\n\n\t"
                                    + String.valueOf(i + 1)
                                    + "\n\n"
                                    + EvaluationReport.printReport(evaluation,
                                    trueClassification.getCategoryDB()));
                        }
                        // evaluations[i] = evaluation;
                    }

                } else {

                    IIncrementalRank incrRankObject = (IIncrementalRank) rankObject;
                    ContingencyTableSet newEvaluation = evaluation;
                    for (int i = 0; i < testSize; i++) {
                        // System.out.println(i + ")");
                        startTime = System.currentTimeMillis();
                        if (rankType.equals("macro")) {
                            currRank.add(incrRankObject.nextMacroRankDocument(
                                    evaluation, newEvaluation));
                        } else if (rankType.equals("micro")) {
                            currRank.add(incrRankObject.nextMicroRankDocument(
                                    evaluation, newEvaluation));
                        }
                        time += System.currentTimeMillis() - startTime;
                        // System.out.println(currRank.toString());
                        evaluation = newEvaluation;
                        newEvaluation = flatComparer
                                .evaluateMixedFast(currRank);
                        if (light) {
                            if (rankType.equals("macro")) {
                                evals[i + 1] = (float) getEval(evaluation,
                                        measureArg, true);
                                ;
                            } else if (rankType.equals("micro")) {
                                evals[i + 1] = (float) getEval(evaluation,
                                        measureArg, false);
                                ;
                            }
                        } else {
                            writer.write("\n\n\t"
                                    + String.valueOf(i + 1)
                                    + "\n\n"
                                    + EvaluationReport.printReport(
                                    newEvaluation,
                                    trueClassification.getCategoryDB()));
                        }
                        // evaluations[i] = newEvaluation;
                    }
                }

                if (light) {
                    writer.write(Arrays.toString(evals));
                }

                writer.close();

                System.out.println(predName + " " + policy + " CAT: " + cat
                        + " RANKING TIME: " + time + " TOTAL TIME: "
                        + (System.currentTimeMillis() - totalTime)
                        + " RELIABILITY: " + reliability.get());

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
    }

    private static double getEval(ContingencyTableSet evaluation,
                                  String measureArg, boolean isMacro) {
        if (measureArg.equals("f1")) {
            return isMacro ? evaluation.macroF(1.0) : evaluation
                    .getGlobalContingencyTable().f(1.0);
        } else if (measureArg.equals("f05")) {
            return isMacro ? evaluation.macroF(0.5) : evaluation
                    .getGlobalContingencyTable().f(0.5);
        } else if (measureArg.equals("f2")) {
            return isMacro ? evaluation.macroF(2.0) : evaluation
                    .getGlobalContingencyTable().f(2.0);
        } else if (measureArg.equals("k")) {
            return isMacro ? evaluation.macroRoc() : evaluation
                    .getGlobalContingencyTable().roc();
        } else {
            return 0.0;
        }
    }

    @SuppressWarnings("unused")
    private static IClassificationDB mixClassifications(
            IClassificationDB trueClassification,
            IClassificationDB predictedClassification,
            TIntHashSet fromTrueClassification) {
        TroveClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                trueClassification.getDocumentDB(),
                trueClassification.getCategoryDB());

        IIntIterator documents = trueClassification.getDocumentDB()
                .getDocuments();
        while (documents.hasNext()) {
            int document = documents.next();
            if (fromTrueClassification.contains(document))
                copyClassification(document, trueClassification, builder);
            else
                copyClassification(document, predictedClassification, builder);
        }
        return builder.getClassificationDB();
    }

    private static void copyClassification(int document,
                                           IClassificationDB trueClassification,
                                           TroveClassificationDBBuilder builder) {
        IShortIterator categories = trueClassification
                .getDocumentCategories(document);
        while (categories.hasNext()) {
            short category = categories.next();
            builder.setDocumentCategory(document, category);
        }
    }

    private static ClassificationScoreDB getTrueConfidence(
            IClassificationDB trueClassification,
            IClassificationDB predictedClassification,
            ClassificationScoreDB predictedConfidences) {
        ClassificationScoreDB resultConfidence = new ClassificationScoreDB(trueClassification
                .getDocumentDB().getDocumentsCount());
        IIntIterator documents = trueClassification.getDocumentDB()
                .getDocuments();

        while (documents.hasNext()) {
            int document = documents.next();
            HashSet<Short> trueCategories = new HashSet<Short>(
                    trueClassification.getDocumentCategoriesCount(document));
            for (IShortIterator it = trueClassification
                    .getDocumentCategories(document); it.hasNext(); ) {
                trueCategories.add(it.next());
            }
            HashSet<Short> predictedCategories = new HashSet<Short>(
                    predictedClassification
                            .getDocumentCategoriesCount(document));
            for (IShortIterator it = predictedClassification
                    .getDocumentCategories(document); it.hasNext(); ) {
                predictedCategories.add(it.next());
            }

            Hashtable<Short, ClassifierRangeWithScore> predConfidence = predictedConfidences
                    .getDocumentScoresAsHashtable(document);
            int categoriesCount = trueClassification.getCategoryDB()
                    .getCategoriesCount();
            for (short category = 0; category < categoriesCount; category++) {
                resultConfidence.insertScore(document, category,
                        predConfidence.get(category).border,
                        predConfidence.get(category));
            }

            if (!trueCategories.isEmpty()) {
                for (Iterator<Short> it = trueCategories.iterator(); it
                        .hasNext(); ) {
                    Short category = it.next();
                    if (predictedCategories.isEmpty()
                            || !predictedCategories.contains(category)) {
                        resultConfidence.insertScore(document,
                                category, Double.NEGATIVE_INFINITY,
                                predConfidence.get(category));
                    }
                }
            }
            if (!predictedCategories.isEmpty()) {
                for (Iterator<Short> it = predictedCategories.iterator(); it
                        .hasNext(); ) {
                    Short category = it.next();
                    if (trueCategories.isEmpty()
                            || !trueCategories.contains(category)) {
                        resultConfidence.insertScore(document,
                                category, Double.POSITIVE_INFINITY,
                                predConfidence.get(category));
                    }
                }
            }
        }
        return resultConfidence;
    }
}
