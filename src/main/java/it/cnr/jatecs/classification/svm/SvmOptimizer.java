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

package it.cnr.jatecs.classification.svm;

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.OptimalConfiguration;
import it.cnr.jatecs.classification.ThresholdOptimizerType;
import it.cnr.jatecs.classification.interfaces.*;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.Vector;

public class SvmOptimizer implements IClassifierOptimizer {

    /**
     * The minimum and maximum cost for constraints violation.
     */
    private int _minVal;
    private int _maxVal;


    public SvmOptimizer() {
        _minVal = -4;
        _maxVal = 4;
    }


    public void setMinMaxCValues(int min, int max) {
        _minVal = min;
        _maxVal = max;
    }


    public void assignBestClassifierConfiguration(IClassifierRuntimeCustomizer target, TShortArrayList externalCategories, Vector<IClassifierRuntimeCustomizer> customizers, TShortArrayList internalCategories) {
    }

    public void assignBestLearnerConfiguration(ILearnerRuntimeCustomizer target, TShortArrayList externalCategories, Vector<ILearnerRuntimeCustomizer> customizers, TShortArrayList internalCategories) {
        short catID = externalCategories.get(0);

        double c_value = 0;
        for (int i = 0; i < customizers.size(); i++) {
            SvmLearnerCustomizer c = (SvmLearnerCustomizer) customizers.get(i);
            c_value += c.getSoftMarginCost((short) 0);
        }

        c_value = c_value / customizers.size();
        SvmLearnerCustomizer t = (SvmLearnerCustomizer) target;
        t.setSoftMarginCost(catID, c_value);
    }

    public OptimalConfiguration optimizeFor(ILearner learner, IIndex training, IIndex validation, TShortArrayList validCategories) {
        OptimalConfiguration conf = new OptimalConfiguration();


        double bestEffectiveness = -Double.MAX_VALUE;

        SvmLearnerCustomizer customizer = null;
        for (int i = _minVal; i <= _maxVal; i++) {
            double cost = Math.pow(10, i);
            Pair<SvmLearnerCustomizer, Double> res = evaluatePerformanceForLearner(learner, training, validation, validCategories, cost, customizer);

            if (res.getSecond() > bestEffectiveness) {
                bestEffectiveness = res.getSecond();
                customizer = res.getFirst();
            }
        }


        System.out.println("The best F1 obtained for category " + training.getCategoryDB().getCategoryName(validCategories.get(0)) + " is: " + bestEffectiveness);

        conf.classifierCustomizer = null;
        conf.learnerCustomizer = customizer;

        return conf;
    }


    protected Pair<SvmLearnerCustomizer, Double> evaluatePerformanceForLearner(ILearner learner, IIndex training, IIndex validation, TShortArrayList validCategories,
                                                                               double cost, SvmLearnerCustomizer customizer) {
        assert (validCategories.size() == 1);

        ThresholdOptimizerType optimizerType = ThresholdOptimizerType.F1;

        SvmLearnerCustomizer origCust = (SvmLearnerCustomizer) learner.getRuntimeCustomizer();
        SvmLearnerCustomizer cust = (SvmLearnerCustomizer) origCust.cloneObject();
        cust.setSoftMarginCost((short) 0, cost);

        learner.setRuntimeCustomizer(cust);
        IClassifier cl = learner.build(training);


        IIntIterator docs = validation.getDocumentDB().getDocuments();


        IClassificationDBBuilder builder = new TroveClassificationDBBuilder(validation.getDocumentDB(), validation.getCategoryDB());

        docs.begin();
        while (docs.hasNext()) {
            int docID = docs.next();
            ClassificationResult res = cl.classify(validation, docID);

            for (int i = 0; i < res.categoryID.size(); i++) {
                ClassifierRange cr = cl.getClassifierRange(res.categoryID.get(i));
                if (res.score.get(i) >= cr.border)
                    builder.setDocumentCategory(docID, res.categoryID.get(i));
            }

        }


        TShortArrayListIterator validCats = new TShortArrayListIterator(validCategories);
        ClassificationComparer cc = new ClassificationComparer(builder.getClassificationDB(), validation.getClassificationDB(), validCats);
        ContingencyTableSet tableSet = cc.evaluate();
        ContingencyTable ct = tableSet.getCategoryContingencyTable((short) 0);

        double effectiveness = 0;

        if (optimizerType == ThresholdOptimizerType.ACCURACY)
            effectiveness = ct.accuracy();
        else if (optimizerType == ThresholdOptimizerType.ERROR)
            effectiveness = ct.error();
        else if (optimizerType == ThresholdOptimizerType.F1)
            effectiveness = ct.f1();
        else if (optimizerType == ThresholdOptimizerType.PRECISION)
            effectiveness = ct.precision();
        else if (optimizerType == ThresholdOptimizerType.RECALL)
            effectiveness = ct.recall();

        learner.setRuntimeCustomizer(origCust);

        Pair<SvmLearnerCustomizer, Double> p = new Pair<SvmLearnerCustomizer, Double>(cust, effectiveness);
        return p;
    }

}
