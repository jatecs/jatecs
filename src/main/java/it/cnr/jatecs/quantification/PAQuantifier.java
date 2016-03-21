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

import gnu.trove.TShortDoubleHashMap;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.quantification.interfaces.IQuantifier;
import it.cnr.jatecs.quantification.interfaces.IScalingFunction;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Hashtable;

public class PAQuantifier implements IQuantifier {

    private IClassifier classifier;
    private IClassifierRuntimeCustomizer classifierCustomizer;
    private ClassificationMode classificationMode;
    private IScalingFunction scalingFunction;
    private TShortDoubleHashMap scaledTPRs;
    private TShortDoubleHashMap scaledFPRs;

    public PAQuantifier(IClassifier classifier,
                        IClassifierRuntimeCustomizer classifierCustomizer,
                        ClassificationMode classificationMode,
                        IScalingFunction scalingFunction) {
        this.classifier = classifier;
        this.classifierCustomizer = classifierCustomizer;
        this.classificationMode = classificationMode;
        this.scalingFunction = scalingFunction;
        scaledTPRs = null;
        scaledFPRs = null;
    }

    @Override
    public Quantification quantify(IIndex index) {
        scaledTPRs = new TShortDoubleHashMap();
        scaledFPRs = new TShortDoubleHashMap();
        classifier.setRuntimeCustomizer(classifierCustomizer);

        Classifier classificationModule = new Classifier(index, classifier,
                true);

        classificationModule.setClassificationMode(classificationMode);

        classificationModule.exec();

        ClassificationScoreDB confidences = classificationModule.getConfidences();

        Quantification quantification = new Quantification(getName(),
                index.getCategoryDB());

        IShortIterator categories = index.getCategoryDB().getCategories();
        while (categories.hasNext()) {
            short category = categories.next();
            double pp = 0;
            int document = 0;
            double ppp = 0;
            double pnp = 0;
            int pDocument = 0;
            int nDocument = 0;

            while (document < confidences.getDocumentCount()) {
                Hashtable<Short, ClassifierRangeWithScore> docScores = confidences
                        .getDocumentScoresAsHashtable(document);
                double scaledScore = scalingFunction.scale(docScores
                        .get(category).score);
                ;
                pp += scaledScore;
                ++document;
                if (index.getClassificationDB().hasDocumentCategory(document,
                        category)) {
                    ppp += scaledScore;
                    ++pDocument;
                } else {
                    pnp += scaledScore;
                    ++nDocument;
                }
            }

            scaledTPRs.put(category, ppp / pDocument);
            scaledFPRs.put(category, pnp / nDocument);

            quantification.setQuantification(category, pp / document);
        }
        return quantification;
    }

    public IClassifier getClassifier() {
        return classifier;
    }

    public ClassificationMode getClassificationMode() {
        return classificationMode;
    }

    public void setClassificationMode(ClassificationMode classificationMode) {
        this.classificationMode = classificationMode;
    }

    public void setClassifierRuntimeCustomizer(
            IClassifierRuntimeCustomizer classifierCustomizer) {
        this.classifierCustomizer = classifierCustomizer;

    }

    public IScalingFunction getScalingFunction() {
        return scalingFunction;
    }

    public void setScalingFunction(IScalingFunction scalingFunction) {
        this.scalingFunction = scalingFunction;
    }

    public CCQuantifier getCCQuantifier() {
        return new CCQuantifier(classifier, classifierCustomizer,
                classificationMode);
    }

    @Override
    public String getName() {
        return "PA";
    }

    public TShortDoubleHashMap getScaledTPRs() {
        return scaledTPRs;
    }

    public TShortDoubleHashMap getScaledFPRs() {
        return scaledFPRs;
    }
}
