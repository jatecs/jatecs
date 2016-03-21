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
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ConfusionMatrix;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class SingleLabelKnnClassifierOptimizer {
    protected int _minimumK, _maximumK, _stepK;

    protected int _decimalPrecision;

    public SingleLabelKnnClassifierOptimizer() {
        _decimalPrecision = 3;
        _minimumK = 2;
        _maximumK = 40;
        _stepK = 2;
    }

    public void setKThresholds(int minimum, int maximum, int step) {
        _minimumK = minimum;
        _maximumK = maximum;
        _stepK = step;
    }

    public OptimalConfiguration optimizeFor(ILearner learner, IIndex training,
                                            IIndex validation) {
        // Construct classification model.
        SingleLabelKnnClassifier classifier = (SingleLabelKnnClassifier) learner
                .build(training);

        SingleLabelKnnClassifierCustomizer cust = (SingleLabelKnnClassifierCustomizer) classifier
                .getRuntimeCustomizer().cloneObject();

        JatecsLogger.status().println("Optiming parameters...");

        int bestK = -1;
        double bestEffectiveness = -Double.MAX_VALUE;

        for (int currentK = _minimumK; currentK <= _maximumK; currentK += _stepK) {

            SingleLabelKnnClassifierCustomizer cu = (SingleLabelKnnClassifierCustomizer) cust
                    .cloneObject();

            cu.setK(currentK);

            classifier.setRuntimeCustomizer(cu);

            ClassificationResult[] results = new ClassificationResult[validation
                    .getDocumentDB().getDocumentsCount()];
            IIntIterator docs = validation.getDocumentDB().getDocuments();
            while (docs.hasNext()) {
                int docID = docs.next();

                // Classify validation documents using current configuration.
                ClassificationResult res = classifier.classify(validation,
                        docID);
                results[docID] = res;
            }

            IClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                    validation.getDocumentDB(), validation.getCategoryDB());

            for (int j = 0; j < results.length; j++) {
                ClassificationResult res = results[j];

                builder.setDocumentCategory(res.documentID, res.categoryID
                        .get(0));
            }

            ClassificationComparer cc = new ClassificationComparer(builder
                    .getClassificationDB(), validation.getClassificationDB());
            ConfusionMatrix cm = cc.evaluateSingleLabel();
            if (cm.getAccuracy() > bestEffectiveness) {
                bestEffectiveness = cm.getAccuracy();
                bestK = currentK;
            }

        }

        System.out.println("The best KNN found condifguration is k=" + bestK);

        cust.setK(bestK);
        // cust.setEfficacy(catID, bestEffectiveness);

        OptimalConfiguration conf = new OptimalConfiguration();
        conf.learnerCustomizer = null;
        conf.classifierCustomizer = cust;

        return conf;
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

}
