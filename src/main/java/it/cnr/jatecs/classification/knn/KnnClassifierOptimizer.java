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

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.OptimalConfiguration;
import it.cnr.jatecs.classification.interfaces.IClassifierOptimizer;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class KnnClassifierOptimizer implements IClassifierOptimizer {

    protected double _minimumMargin, _maximumMargin, _stepMargin;
    protected int _minimumK, _maximumK, _stepK;
    protected KnnOptimizationType _optimizerType;
    protected int _decimalPrecision;

    public KnnClassifierOptimizer() {
        _minimumMargin = -1;
        _maximumMargin = 1;
        _stepMargin = 0.05;
        _optimizerType = KnnOptimizationType.F1;
        _decimalPrecision = 3;
        _minimumK = 2;
        _maximumK = 40;
        _stepK = 2;
    }

    public void setMarginThresholds(double minimum, double maximum, double step) {
        _minimumMargin = minimum;
        _maximumMargin = maximum;
        _stepMargin = step;
    }

    public void setKThresholds(int minimum, int maximum, int step) {
        _minimumK = minimum;
        _maximumK = maximum;
        _stepK = step;
    }

    public void setOptimizationType(KnnOptimizationType t) {
        _optimizerType = t;
    }

    public OptimalConfiguration optimizeFor(ILearner learner, IIndex training,
                                            IIndex validation, TShortArrayList catsValid) {

        // Construct classification model.
        KnnClassifier classifier = (KnnClassifier) learner.build(training);

        KnnClassifierCustomizer cust = (KnnClassifierCustomizer) classifier
                .getRuntimeCustomizer().cloneObject();

        JatecsLogger.status().println("Optiming parameters...");

        short catID = 0;
        TShortArrayList categories = new TShortArrayList();
        categories.add(catID);
        TShortArrayListIterator validCategories = new TShortArrayListIterator(
                categories);

        int bestK = -1;
        double bestThreshold = _minimumMargin;
        double bestEffectiveness = -Double.MAX_VALUE;

        for (int currentK = _minimumK; currentK <= _maximumK; currentK += _stepK) {

            KnnClassifierCustomizer cu = (KnnClassifierCustomizer) cust
                    .cloneObject();

            cu.setK((short) 0, currentK);

            classifier.setRuntimeCustomizer(cu);

            // Classify validation documents using current configuration.
            ClassificationResult[] results = classifier.classify(validation,
                    catID);

            // double threshold = Os.generateDouble(_minimumMargin,
            // _decimalPrecision);
            double threshold = _minimumMargin;
            while (threshold <= _maximumMargin) {

                ClassifierRange cr = new ClassifierRange();
                cr.border = threshold;
                cr.minimum = _minimumMargin;
                cr.maximum = _maximumMargin;

                // System.out.println("Optimizing KNN for k="+currentK+",
                // threshold="+threshold);

                IClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                        validation.getDocumentDB(), validation.getCategoryDB());

                for (int j = 0; j < results.length; j++) {
                    ClassificationResult res = results[j];

                    if (res.score.get(0) >= cr.border)
                        builder.setDocumentCategory(res.documentID, catID);
                }

                validCategories.begin();
                ClassificationComparer cc = new ClassificationComparer(
                        builder.getClassificationDB(),
                        validation.getClassificationDB(), validCategories);
                ContingencyTableSet tableSet = cc.evaluate();
                ContingencyTable ct = tableSet
                        .getCategoryContingencyTable(catID);

                double effectiveness = 0;

                if (_optimizerType == KnnOptimizationType.ACCURACY)
                    effectiveness = ct.accuracy();
                else if (_optimizerType == KnnOptimizationType.ERROR)
                    effectiveness = ct.error();
                else if (_optimizerType == KnnOptimizationType.F1)
                    effectiveness = ct.f1();
                else if (_optimizerType == KnnOptimizationType.PRECISION)
                    effectiveness = ct.precision();
                else if (_optimizerType == KnnOptimizationType.RECALL)
                    effectiveness = ct.recall();
                else if (_optimizerType == KnnOptimizationType.CONFIDENCE_F1)
                    effectiveness = computeConfidenceF1(results, cr, validation);

                if (effectiveness > bestEffectiveness) {
                    bestThreshold = threshold;
                    bestEffectiveness = effectiveness;
                    bestK = currentK;
                }

                // threshold = Os.generateDouble(threshold+_stepMargin,
                // _decimalPrecision);
                threshold = threshold + _stepMargin;
            }
        }

        System.out.println("The best KNN found condifguration is k=" + bestK
                + ", threshold=" + bestThreshold + " Obtained effectiveness: "
                + bestEffectiveness);

        ClassifierRange old = cust.getClassifierRange(catID);
        ClassifierRange cr = new ClassifierRange();
        cr.border = bestThreshold;
        cr.minimum = old.minimum;
        cr.maximum = old.maximum;
        cust._ranges.put(catID, cr);
        cust.setK((short) 0, bestK);
        cust.setEfficacy(catID, bestEffectiveness);

        OptimalConfiguration conf = new OptimalConfiguration();
        conf.learnerCustomizer = null;
        conf.classifierCustomizer = cust;

        return conf;
    }

    protected double computeConfidenceF1(ClassificationResult[] results,
                                         ClassifierRange cr, IIndex validation) {
        double effectiveness = 0;

        double tp = 0;
        double fp = 0;
        double fn = 0;

        IIntIterator docs = validation.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int docID = docs.next();
            ClassificationResult res = results[docID];
            double score = res.score.get(0) - cr.border;
            boolean realPositive = validation.getClassificationDB()
                    .hasDocumentCategory(docID, (short) 0);
            boolean predictedPositive = score >= 0 ? true : false;
            score = score >= 0 ? score : -score;
            if (realPositive && predictedPositive)
                // TP
                tp += 1 * score;
            else if (realPositive && !predictedPositive)
                // FN
                fn += 1 * score;
            else if (!realPositive && predictedPositive)
                // FP
                fp += 1 * score;
        }

        double numerator = 2 * tp;
        double denominator = (2 * tp) + fp + fn;
        if (denominator == 0)
            effectiveness = 1;
        else
            effectiveness = numerator / denominator;

        return effectiveness;
    }

    public void assignBestClassifierConfiguration(
            IClassifierRuntimeCustomizer target,
            TShortArrayList externalCategories,
            Vector<IClassifierRuntimeCustomizer> customizers,
            TShortArrayList internalCategories) {
        IShortIterator cats = new TShortArrayListIterator(internalCategories);

        if (externalCategories.size() != internalCategories.size())
            throw new RuntimeException(
                    "The internal and external category array must have the same length");

        IClassifierRuntimeCustomizer customizer = target;

        KnnClassifierCustomizer cValid = (KnnClassifierCustomizer) customizer;

        int count = 0;
        while (cats.hasNext()) {
            short catID = cats.next();
            short externalCatID = externalCategories.get(count++);

            double t = 0;
            double effectiveness = 0;
            int k = 0;

            for (int i = 0; i < customizers.size(); i++) {
                IClassifierRuntimeCustomizer c = customizers.get(i);
                KnnClassifierCustomizer cust = (KnnClassifierCustomizer) c;

                t += cust.getClassifierRange(catID).border;
                effectiveness += cust.getEfficacy(catID);
                k += cust.getK(catID);
            }

            t /= customizers.size();
            effectiveness /= customizers.size();
            k /= customizers.size();
            if (k < 1)
                k = 1;

            cValid.setK(externalCatID, k);
            ClassifierRange range = new ClassifierRange();
            range.border = Os.generateDouble(t, _decimalPrecision);
            range.minimum = cValid.getClassifierRange((short) -1).minimum;
            range.maximum = cValid.getClassifierRange((short) -1).maximum;
            cValid.setClassifierRange(externalCatID, range);
            cValid.setEfficacy(externalCatID, effectiveness);

            System.out.println("Avg selected k: " + cValid.getK(externalCatID));
            System.out.println("Avg selected threshold: " + range.border);
        }

    }

    public void assignBestLearnerConfiguration(
            ILearnerRuntimeCustomizer target,
            TShortArrayList externalCategories,
            Vector<ILearnerRuntimeCustomizer> customizers,
            TShortArrayList internalCategories) {

    }

    public static enum KnnOptimizationType {
        /**
         * The accuracy meausure.
         */
        ACCURACY,

        /**
         * The error measure.
         */
        ERROR,

        /**
         * The F1 measure.
         */
        F1,

        /**
         * The precison measure.
         */
        PRECISION,

        /**
         * The recall measure.
         */
        RECALL,

        /**
         * The F1 computed by considering the confidence value in each decision.
         */
        CONFIDENCE_F1,
    }

}
