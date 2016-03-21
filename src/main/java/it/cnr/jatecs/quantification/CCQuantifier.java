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
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.quantification.interfaces.IQuantifier;
import it.cnr.jatecs.quantification.interfaces.IScalingFunction;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import org.apache.log4j.Logger;

public class CCQuantifier implements IQuantifier {


    static Logger logger = Logger.getLogger(ScaledQuantifier.class);

    private IClassifier classifier;
    private IClassifierRuntimeCustomizer classifierCustomizer;
    private ClassificationMode classificationMode;
    private TShortDoubleHashMap thresholds;
    private boolean customThresholds;
    private TShortDoubleHashMap simpleTPRs;
    private TShortDoubleHashMap simpleFPRs;
    private ContingencyTableSet contingencytableSet;

    public CCQuantifier(IClassifier classifier,
                        IClassifierRuntimeCustomizer classifierCustomizer,
                        ClassificationMode classificationMode) {
        this.classifier = classifier;
        this.classifierCustomizer = classifierCustomizer;
        this.classificationMode = classificationMode;
        customThresholds = false;
        simpleTPRs = null;
        simpleFPRs = null;
    }

    public CCQuantifier(IClassifier classifier,
                        IClassifierRuntimeCustomizer classifierCustomizer,
                        ClassificationMode classificationMode,
                        TShortDoubleHashMap thresholds) {
        this(classifier, classifierCustomizer, classificationMode);
        this.thresholds = thresholds;
        customThresholds = true;
    }

    @Override
    public Quantification quantify(IIndex index) {
        simpleTPRs = new TShortDoubleHashMap();
        simpleFPRs = new TShortDoubleHashMap();
        classifier.setRuntimeCustomizer(classifierCustomizer);

        Classifier classificationModule = new Classifier(index, classifier);

        classificationModule.setClassificationMode(classificationMode);

        if (customThresholds) {
            logger.info(getName() + ": Setting custom thresholds");
            classificationModule.setCustomThresholds(thresholds);
        }

        classificationModule.exec();

        IClassificationDB classificationDB = classificationModule
                .getClassificationDB();

        ClassificationComparer comparer = new ClassificationComparer(
                classificationDB, index.getClassificationDB());

        contingencytableSet = comparer.evaluate();

        Quantification quantification = new Quantification(getName(),
                index.getCategoryDB());

        IShortIterator categories = index.getCategoryDB().getCategories();
        while (categories.hasNext()) {
            short category = categories.next();
            ContingencyTable categoryTable = contingencytableSet
                    .getCategoryContingencyTable(category);
            quantification.setQuantification(category,
                    ((double) categoryTable.tp() + categoryTable.fp())
                            / categoryTable.total());
            simpleTPRs.put(category, categoryTable.tpr());
            simpleFPRs.put(category, categoryTable.fpr());
        }
        return quantification;
    }

    public IClassifier getClassifier() {
        return classifier;
    }

    public boolean hasCustomThresholds() {
        return customThresholds;
    }

    public TShortDoubleHashMap getCustomThresholds() {
        return thresholds;
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

    public CCQuantifier getCCQuantifierWithoutCustomThresholds() {
        return new CCQuantifier(classifier, classifierCustomizer,
                classificationMode);
    }

    public PAQuantifier getPaQuantifier(IScalingFunction scalingFunction) {
        return new PAQuantifier(classifier, classifierCustomizer,
                classificationMode, scalingFunction);
    }

    @Override
    public String getName() {
        return "CC" + (customThresholds ? "-custTh" : "");
    }

    public TShortDoubleHashMap getSimpleTPRs() {
        return simpleTPRs;
    }

    public TShortDoubleHashMap getSimpleFPRs() {
        return simpleFPRs;
    }

    public ContingencyTableSet getContingencyTableSet() {
        return contingencytableSet;
    }

}
