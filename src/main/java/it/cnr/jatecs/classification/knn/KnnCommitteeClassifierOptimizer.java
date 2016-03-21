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
import it.cnr.jatecs.classification.ThresholdOptimizerType;
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
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class KnnCommitteeClassifierOptimizer implements IClassifierOptimizer {

    protected double _minimumMargin, _maximumMargin, _stepMargin;


    protected ThresholdOptimizerType _optimizerType;

    protected int _decimalPrecision;

    public KnnCommitteeClassifierOptimizer() {
        _minimumMargin = -1;
        _maximumMargin = 1;
        _stepMargin = 0.05;
        _optimizerType = ThresholdOptimizerType.F1;
        _decimalPrecision = 3;
    }


    public void setMarginThresholds(double minimum, double maximum, double step) {
        _minimumMargin = minimum;
        _maximumMargin = maximum;
        _stepMargin = step;
    }


    public void setOptimizationType(ThresholdOptimizerType t) {
        _optimizerType = t;
    }

    public OptimalConfiguration optimizeFor(ILearner learner,
                                            IIndex training, IIndex validation, TShortArrayList catsValid) {
        // Construct classification model.
        KnnCommitteeClassifier classifier = (KnnCommitteeClassifier) learner.build(training);


        KnnCommitteeClassifierCustomizer cust = (KnnCommitteeClassifierCustomizer) classifier.getRuntimeCustomizer().cloneObject();

        JatecsLogger.status().println("Optiming parameters...");

        // For each category in index, perform validation.
        IShortIterator cats = new TShortArrayListIterator(catsValid);
        while (cats.hasNext()) {
            short catID = cats.next();


            JatecsLogger.status().println("Optimizing threshold for category " + training.getCategoryDB().getCategoryName(catID) + "...");

            optimizeThreshold(validation, catID, classifier, cust);

        }


        OptimalConfiguration conf = new OptimalConfiguration();
        conf.learnerCustomizer = null;
        conf.classifierCustomizer = cust;

        return conf;
    }


    protected void optimizeThreshold(IIndex validation, short catID, KnnCommitteeClassifier c, KnnCommitteeClassifierCustomizer cust) {

        TShortArrayList categories = new TShortArrayList();
        categories.add(catID);
        TShortArrayListIterator validCategories = new TShortArrayListIterator(categories);


        double bestThreshold = _minimumMargin;
        double bestEffectiveness = Double.MIN_VALUE;


        // Classify validation documents using current configuration.
        ClassificationResult[] results = c.classify(validation, catID);

        JatecsLogger.status().println("Testing classifier using minimum=" + _minimumMargin + ", maximum=" + _maximumMargin + ", step=" + _stepMargin);

        double threshold = Os.generateDouble(_minimumMargin, _decimalPrecision);
        while (threshold <= _maximumMargin) {

            ClassifierRange cr = new ClassifierRange();
            cr.border = threshold;
            cr.minimum = _minimumMargin;
            cr.maximum = _maximumMargin;


            IClassificationDBBuilder builder = new TroveClassificationDBBuilder(validation.getDocumentDB(), validation.getCategoryDB());

            for (int j = 0; j < results.length; j++) {
                ClassificationResult res = results[j];

                if (res.score.get(0) >= cr.border)
                    builder.setDocumentCategory(res.documentID, catID);
            }

            validCategories.begin();
            ClassificationComparer cc = new ClassificationComparer(builder.getClassificationDB(), validation.getClassificationDB(), validCategories);
            ContingencyTableSet tableSet = cc.evaluate();
            ContingencyTable ct = tableSet.getCategoryContingencyTable(catID);

            double effectiveness = 0;

            if (_optimizerType == ThresholdOptimizerType.ACCURACY)
                effectiveness = ct.accuracy();
            else if (_optimizerType == ThresholdOptimizerType.ERROR)
                effectiveness = ct.error();
            else if (_optimizerType == ThresholdOptimizerType.F1)
                effectiveness = ct.f1();
            else if (_optimizerType == ThresholdOptimizerType.PRECISION)
                effectiveness = ct.precision();
            else if (_optimizerType == ThresholdOptimizerType.RECALL)
                effectiveness = ct.recall();

            if (effectiveness > bestEffectiveness) {
                bestThreshold = threshold;
                bestEffectiveness = effectiveness;
            }

            threshold = Os.generateDouble(threshold + _stepMargin, _decimalPrecision);
        }

        // DEBUG
        JatecsLogger.execution().info("Selected threshold " + bestThreshold + " for effectiveness " + bestEffectiveness);

        ClassifierRange old = cust.getClassifierRange(catID);
        ClassifierRange cr = new ClassifierRange();
        cr.border = bestThreshold;
        cr.minimum = old.minimum;
        cr.maximum = old.maximum;
        cust._ranges.put(catID, cr);

    }


    public void assignBestClassifierConfiguration(IClassifierRuntimeCustomizer target, TShortArrayList externalCategories, Vector<IClassifierRuntimeCustomizer> customizers, TShortArrayList internalCategories) {
        IShortIterator cats = new TShortArrayListIterator(internalCategories);

        if (externalCategories.size() != internalCategories.size())
            throw new RuntimeException("The internal and external category array must have the same length");

        IClassifierRuntimeCustomizer customizer = target;

        KnnCommitteeClassifierCustomizer cValid = (KnnCommitteeClassifierCustomizer) customizer;
        int count = 0;
        while (cats.hasNext()) {
            short catID = cats.next();
            short externalCatID = externalCategories.get(count++);

            double t = 0;

            for (int i = 0; i < customizers.size(); i++) {
                IClassifierRuntimeCustomizer c = customizers.get(i);

                KnnCommitteeClassifierCustomizer cust = (KnnCommitteeClassifierCustomizer) c;
                t += cust.getClassifierRange(catID).border;
            }

            t /= customizers.size();

            ClassifierRange range = new ClassifierRange();
            range.border = Os.generateDouble(t, _decimalPrecision);
            range.minimum = _minimumMargin;
            range.maximum = _maximumMargin;
            cValid.setClassifierRange(externalCatID, range);

        }

    }

    public void assignBestLearnerConfiguration(ILearnerRuntimeCustomizer target, TShortArrayList externalCategories, Vector<ILearnerRuntimeCustomizer> customizers, TShortArrayList internalCategories) {

    }

}
