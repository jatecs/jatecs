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

package it.cnr.jatecs.classification;

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.interfaces.*;
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

public class ThresholdClassifierOptimizer implements IClassifierOptimizer {

    protected double _minimum, _maximum, _step;

    protected ThresholdOptimizerType _optimizerType;

    protected int _decimalPrecision;

    public ThresholdClassifierOptimizer() {
        _minimum = -1;
        _maximum = 1;
        _step = 0.05;
        _optimizerType = ThresholdOptimizerType.F1;
        _decimalPrecision = 3;
    }

    public void setThresholds(double minimum, double maximum, double step) {
        _minimum = minimum;
        _maximum = maximum;
        _step = step;
    }

    public void setOptimizationType(ThresholdOptimizerType t) {
        _optimizerType = t;
    }

    public OptimalConfiguration optimizeFor(ILearner learner, IIndex training,
                                            IIndex validation, TShortArrayList catsValid) {
        // Construct classification model.
        IClassifier classifier = learner.build(training);
        IClassifierRuntimeCustomizer cust = classifier.getRuntimeCustomizer();

        // Create new instance of customizer.
        cust = cust.cloneObject();

        IThresholdClassifier c = (IThresholdClassifier) cust;

        // For each category in index, perform validation.
        IShortIterator cats = new TShortArrayListIterator(catsValid);
        while (cats.hasNext()) {
            short catID = cats.next();

            JatecsLogger.status().println("Optimizing category...");

            TShortArrayList categories = new TShortArrayList();
            categories.add(catID);
            TShortArrayListIterator validCategories = new TShortArrayListIterator(
                    categories);

            double bestThreshold = _minimum;
            double bestEffectiveness = Double.MIN_VALUE;

            // Classify validation documents using current configuration.
            ClassificationResult[] results = classifier.classify(validation,
                    catID);

            JatecsLogger.status().println(
                    "Testing classifier using minimum=" + _minimum
                            + ", maximum=" + _maximum + ", step=" + _step);

            double threshold = Os.generateDouble(_minimum, _decimalPrecision);
            while (threshold <= _maximum) {

                ClassifierRange cr = new ClassifierRange();
                cr.border = threshold;
                cr.minimum = _minimum;
                cr.maximum = _maximum;

                IClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                        validation.getDocumentDB(), validation
                        .getCategoryDB());

                for (int j = 0; j < results.length; j++) {
                    ClassificationResult res = results[j];

                    if (res.score.get(0) >= cr.border)
                        builder.setDocumentCategory(res.documentID, catID);
                }

                validCategories.begin();
                ClassificationComparer cc = new ClassificationComparer(builder
                        .getClassificationDB(), validation
                        .getClassificationDB(), validCategories);
                ContingencyTableSet tableSet = cc.evaluate();
                ContingencyTable ct = tableSet
                        .getCategoryContingencyTable(catID);

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

                threshold = Os.generateDouble(threshold + _step,
                        _decimalPrecision);
            }

            // DEBUG
            JatecsLogger.execution().info(
                    "Selected threshold " + bestThreshold
                            + " for effectiveness " + bestEffectiveness);

            ClassifierRange cr = new ClassifierRange();
            cr.border = bestThreshold;
            cr.minimum = _minimum;
            cr.maximum = _maximum;

            c.setClassifierRange(catID, cr);
        }

        classifier.setRuntimeCustomizer(cust);

        OptimalConfiguration conf = new OptimalConfiguration();
        conf.learnerCustomizer = learner.getRuntimeCustomizer();
        conf.classifierCustomizer = cust;

        return conf;
    }

    public void assignBestClassifierConfiguration(
            IClassifierRuntimeCustomizer target,
            TShortArrayList externalCategories,
            Vector<IClassifierRuntimeCustomizer> customizers,
            TShortArrayList internalCategories) {
        IShortIterator cats = new TShortArrayListIterator(internalCategories);

        assert (externalCategories.size() == internalCategories.size());

        IClassifierRuntimeCustomizer customizer = target;

        IThresholdClassifier cValid = (IThresholdClassifier) customizer;
        int count = 0;
        while (cats.hasNext()) {
            short catID = cats.next();
            short externalCatID = externalCategories.get(count++);

            double t = 0;

            for (int i = 0; i < customizers.size(); i++) {
                IClassifierRuntimeCustomizer c = customizers.get(i);

                IThresholdClassifier cust = (IThresholdClassifier) c;
                t += cust.getClassifierRange(catID).border;
            }

            t /= customizers.size();

            ClassifierRange range = new ClassifierRange();
            range.border = Os.generateDouble(t, _decimalPrecision);
            range.minimum = _minimum;
            range.maximum = _maximum;
            cValid.setClassifierRange(externalCatID, range);

        }

    }

    public void assignBestLearnerConfiguration(
            ILearnerRuntimeCustomizer target,
            TShortArrayList externalCategories,
            Vector<ILearnerRuntimeCustomizer> customizers,
            TShortArrayList internalCategories) {
        assert (customizers.size() != 0);
    }
}
