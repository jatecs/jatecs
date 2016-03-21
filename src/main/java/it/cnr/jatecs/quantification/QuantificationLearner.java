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

package it.cnr.jatecs.quantification;

import gnu.trove.TDoubleHashSet;
import gnu.trove.TDoubleIterator;
import gnu.trove.TShortDoubleHashMap;
import gnu.trove.TShortObjectHashMap;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.classification.interfaces.*;
import it.cnr.jatecs.classification.validator.SimpleKFoldEvaluator;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.quantification.interfaces.IQuantifier;
import it.cnr.jatecs.quantification.interfaces.IScalingFunction;
import it.cnr.jatecs.utils.IOperationStatusListener;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

public class QuantificationLearner {

    private int folds;
    private ILearner learner;
    private ILearnerRuntimeCustomizer learnerCustomizer;
    private IClassifierRuntimeCustomizer classifierCustomizer;
    private ClassificationMode classificationMode;
    private IScalingFunction scalingFunction;
    private IOperationStatusListener status;
    private ContingencyTableSet tableSet;
    private TShortDoubleHashMap simpleTPRs;
    private TShortDoubleHashMap simpleFPRs;
    private TShortDoubleHashMap scaledTPRs;
    private TShortDoubleHashMap scaledFPRs;

    public QuantificationLearner(int folds, ILearner learner,
                                 ILearnerRuntimeCustomizer learnerCustomizer,
                                 IClassifierRuntimeCustomizer classifierCustomizer,
                                 ClassificationMode classificationMode,
                                 IScalingFunction scalingFunction, IOperationStatusListener status) {
        this.folds = folds;
        this.learner = learner;
        this.learnerCustomizer = learnerCustomizer;
        this.classifierCustomizer = classifierCustomizer;
        this.classificationMode = classificationMode;
        this.scalingFunction = scalingFunction;
        this.status = status;

        tableSet = null;
        simpleTPRs = null;
        simpleFPRs = null;
        scaledTPRs = null;
        scaledFPRs = null;
    }

    public static void write(IQuantifier[] quantifiers,
                             IStorageManager storageManager, IDataManager classifierDataManager)
            throws IOException {

        CCQuantifierDataManager ccQuantifierDataManager = new CCQuantifierDataManager(
                classifierDataManager);

        ccQuantifierDataManager.write(storageManager, "0", quantifiers[0]);

        PAQuantifierDataManager paQuantifierDataManager = new PAQuantifierDataManager(
                classifierDataManager);

        paQuantifierDataManager.write(storageManager, "1", quantifiers[1]);

        ScaledQuantifierDataManager scaledQuantifierDataManager = new ScaledQuantifierDataManager(
                ccQuantifierDataManager);

        scaledQuantifierDataManager.write(storageManager, "2", quantifiers[2]);
        scaledQuantifierDataManager.write(storageManager, "3", quantifiers[3]);
        scaledQuantifierDataManager.write(storageManager, "4", quantifiers[4]);

        scaledQuantifierDataManager = new ScaledQuantifierDataManager(
                paQuantifierDataManager);
        scaledQuantifierDataManager.write(storageManager, "5", quantifiers[5]);
    }

    public static IQuantifier[] read(IStorageManager storageManager,
                                     IDataManager classifierDataManager,
                                     ClassificationMode classificationMode) throws IOException {
        IQuantifier[] quantifiers = new IQuantifier[6];

        CCQuantifierDataManager ccQuantifierDataManager = new CCQuantifierDataManager(
                classifierDataManager);

        quantifiers[0] = ccQuantifierDataManager.read(storageManager, "0");
        ((CCQuantifier) quantifiers[0])
                .setClassificationMode(classificationMode);

        PAQuantifierDataManager paQuantifierDataManager = new PAQuantifierDataManager(
                classifierDataManager);

        quantifiers[1] = paQuantifierDataManager.read(storageManager, "1");
        ((PAQuantifier) quantifiers[1])
                .setClassificationMode(classificationMode);

        ScaledQuantifierDataManager scaledQuantifierDataManager = new ScaledQuantifierDataManager(
                ccQuantifierDataManager);

        quantifiers[2] = scaledQuantifierDataManager.read(storageManager, "2");
        ((CCQuantifier) ((ScaledQuantifier) quantifiers[2])
                .getInternalQuantifier())
                .setClassificationMode(classificationMode);

        quantifiers[3] = scaledQuantifierDataManager.read(storageManager, "3");
        ((CCQuantifier) ((ScaledQuantifier) quantifiers[3])
                .getInternalQuantifier())
                .setClassificationMode(classificationMode);

        quantifiers[4] = scaledQuantifierDataManager.read(storageManager, "4");
        ((CCQuantifier) ((ScaledQuantifier) quantifiers[4])
                .getInternalQuantifier())
                .setClassificationMode(classificationMode);

        scaledQuantifierDataManager = new ScaledQuantifierDataManager(
                paQuantifierDataManager);
        quantifiers[5] = scaledQuantifierDataManager.read(storageManager, "5");
        ((PAQuantifier) ((ScaledQuantifier) quantifiers[5])
                .getInternalQuantifier())
                .setClassificationMode(classificationMode);

        return quantifiers;
    }

    /**
     * @param index from which to learn the quantification models.
     * @return an array with, in order, CC, PA, ACC, MAX, SCC, SPA quantifiers.
     */
    public IQuantifier[] learn(IIndex index) {
        tableSet = null;
        simpleTPRs = null;
        simpleFPRs = null;
        scaledTPRs = null;
        scaledFPRs = null;
        ArrayList<IQuantifier> quantifiers = new ArrayList<IQuantifier>();
        status.operationStatus(0.0);
        learner.setRuntimeCustomizer(learnerCustomizer);
        IClassifier classifier = learner.build(index);

        CCQuantifier ccQuantifier = buildCCQuantifier(classifier,
                classifierCustomizer, classificationMode);
        quantifiers.add(ccQuantifier);

        PAQuantifier paQuantifier = buildPAQuantifier(classifier,
                classifierCustomizer, classificationMode, scalingFunction);
        quantifiers.add(paQuantifier);

        status.operationStatus(100.0 / (folds + 1));
        SimpleKFoldEvaluator kfoldevaluator = new SimpleKFoldEvaluator(learner,
                learnerCustomizer, classifierCustomizer, false);
        kfoldevaluator.setClassificationMode(classificationMode);
        kfoldevaluator.setSaveConfidences(true);
        kfoldevaluator.setEvaluateAllNodes(true);
        kfoldevaluator.setKFoldValue(folds);
        tableSet = kfoldevaluator.evaluate(index,
                new IOperationStatusListener() {
                    @Override
                    public void operationStatus(double percentage) {
                        status.operationStatus((100.0 / (folds + 1))
                                * +((1.0 - (1.0 / (folds + 1))) * percentage));
                    }
                });

        quantifiers.add(buildACCQuantifier(ccQuantifier, tableSet));
        quantifiers.add(buildMaxQuantifier(classifier, classifierCustomizer,
                index.getClassificationDB(), kfoldevaluator.getConfidences()));
        quantifiers.add(buildScaledQuantifier(ccQuantifier,
                index.getClassificationDB(), kfoldevaluator.getConfidences(),
                scalingFunction));
        quantifiers.add(buildScaledQuantifier(paQuantifier,
                index.getClassificationDB(), kfoldevaluator.getConfidences(),
                scalingFunction));

        return quantifiers.toArray(new IQuantifier[0]);
    }

    public ContingencyTableSet getContingencyTableSet() {
        return tableSet;
    }

    public CCQuantifier buildCCQuantifier(IClassifier classifier,
                                          IClassifierRuntimeCustomizer classifierCustomizer,
                                          ClassificationMode classificationMode) {
        return new CCQuantifier(classifier, classifierCustomizer,
                classificationMode);
    }

    public PAQuantifier buildPAQuantifier(IClassifier classifier,
                                          IClassifierRuntimeCustomizer classifierCustomizer,
                                          ClassificationMode classificationMode,
                                          IScalingFunction scalingFunction) {
        return new PAQuantifier(classifier, classifierCustomizer,
                classificationMode, scalingFunction);
    }

    public TShortDoubleHashMap getSimpleTPRs() {
        return simpleTPRs;
    }

    public TShortDoubleHashMap getSimpleFPRs() {
        return simpleFPRs;
    }

    public ScaledQuantifier buildACCQuantifier(IQuantifier internalIQuantifier,
                                               ContingencyTableSet tableSet) {
        simpleTPRs = new TShortDoubleHashMap();
        simpleFPRs = new TShortDoubleHashMap();

        IShortIterator categories = tableSet.getEvaluatedCategories();
        while (categories.hasNext()) {
            short category = categories.next();
            ContingencyTable table = tableSet
                    .getCategoryContingencyTable(category);
            simpleTPRs.put(category, table.tpr());
            simpleFPRs.put(category, table.fpr());
        }

        return new ScaledQuantifier(internalIQuantifier, simpleTPRs,
                simpleFPRs, "A");
    }

    public TShortDoubleHashMap getScaledTPRs() {
        return scaledTPRs;
    }

    public TShortDoubleHashMap getScaledFPRs() {
        return scaledFPRs;
    }

    public ScaledQuantifier buildScaledQuantifier(
            IQuantifier internalQuantifier,
            IClassificationDB trueClassification, ClassificationScoreDB confidences,
            IScalingFunction scalingFunction) {
        scaledTPRs = new TShortDoubleHashMap();
        scaledFPRs = new TShortDoubleHashMap();

        IShortIterator categories = trueClassification.getCategoryDB()
                .getCategories();
        while (categories.hasNext()) {
            short category = categories.next();
            double ppp = 0;
            double pnp = 0;
            int document = 0;
            int pDocument = 0;
            int nDocument = 0;
            while (document < confidences.getDocumentCount()) {
                Hashtable<Short, ClassifierRangeWithScore> docScores = confidences
                        .getDocumentScoresAsHashtable(document);
                double scaledScore = scalingFunction.scale(docScores
                        .get(category).score);
                if (trueClassification.hasDocumentCategory(document, category)) {
                    ppp += scaledScore;
                    ++pDocument;
                } else {
                    pnp += scaledScore;
                    ++nDocument;
                }
                ++document;
            }
            scaledTPRs.put(category, ppp / pDocument);
            scaledFPRs.put(category, pnp / nDocument);
        }

        return new ScaledQuantifier(internalQuantifier, scaledTPRs, scaledFPRs,
                "S");
    }

    ScaledQuantifier buildMaxQuantifier(IClassifier classifier,
                                        IClassifierRuntimeCustomizer classifierCustomizer,
                                        IClassificationDB trueClassification, ClassificationScoreDB confidences) {
        TShortDoubleHashMap TPRs = new TShortDoubleHashMap();
        TShortDoubleHashMap FPRs = new TShortDoubleHashMap();
        TShortDoubleHashMap thresholds = new TShortDoubleHashMap();

        TShortObjectHashMap<TDoubleHashSet> scores = new TShortObjectHashMap<TDoubleHashSet>();

        IShortIterator categories = trueClassification.getCategoryDB()
                .getCategories();
        while (categories.hasNext()) {
            short category = categories.next();
            scores.put(category, new TDoubleHashSet());
        }

        int document = 0;
        while (document < confidences.getDocumentCount()) {
            Hashtable<Short, ClassifierRangeWithScore> docScores = confidences
                    .getDocumentScoresAsHashtable(document);
            categories.begin();
            while (categories.hasNext()) {
                short category = categories.next();
                double score = docScores.get(category).score;
                scores.get(category).add(score);
            }
            ++document;
        }

        categories.begin();
        while (categories.hasNext()) {
            short category = categories.next();
            TDoubleIterator scoreIterator = scores.get(category).iterator();
            double maxDelta = Double.NEGATIVE_INFINITY;
            while (scoreIterator.hasNext()) {
                double currThreshold = scoreIterator.next();
                int tp = 0;
                int fp = 0;
                int tn = 0;
                int fn = 0;
                document = 0;
                while (document < confidences.getDocumentCount()) {
                    Hashtable<Short, ClassifierRangeWithScore> docScores = confidences
                            .getDocumentScoresAsHashtable(document);
                    double score = docScores.get(category).score;
                    if (trueClassification.hasDocumentCategory(document,
                            category)) {
                        if (score > currThreshold) {
                            ++tp;
                        } else {
                            ++fn;
                        }
                    } else {
                        if (score > currThreshold) {
                            ++fp;
                        } else {
                            ++tn;
                        }
                    }
                    ++document;
                }
                double tpr = ((double) tp) / (tp + fn);
                double fpr = ((double) fp) / (fp + tn);

                double currDelta = Math.abs(tpr - fpr);
                if (currDelta > maxDelta) {
                    maxDelta = currDelta;
                    thresholds.put(category, currThreshold);
                    TPRs.put(category, tpr);
                    FPRs.put(category, fpr);
                }
            }
        }

        CCQuantifier internalQuantifier = new CCQuantifier(classifier,
                classifierCustomizer, classificationMode, thresholds);
        ScaledQuantifier quantifier = new ScaledQuantifier(internalQuantifier,
                TPRs, FPRs, "MAX");
        return quantifier;
    }

}
