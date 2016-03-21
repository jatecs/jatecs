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
import gnu.trove.TShortDoubleIterator;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.quantification.interfaces.IQuantifier;
import it.cnr.jatecs.quantification.interfaces.IQuantifierDataManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CCQuantifierDataManager implements IQuantifierDataManager {
    private static String thresholdSuffix = "_thresholds";
    private static String classifierSuffix = "_classifier";
    private IDataManager classifierDataManager;

    public CCQuantifierDataManager(IDataManager classifierDataManager) {
        this.classifierDataManager = classifierDataManager;
    }

    public void write(IStorageManager storageManager, String modelName,
                      IQuantifier quantifier) {
        try {
            CCQuantifier ccQuantifier = (CCQuantifier) quantifier;
            classifierDataManager.write(storageManager, modelName
                    + classifierSuffix, ccQuantifier.getClassifier());
            classifierDataManager.writeClassifierRuntimeConfiguration(
                    storageManager, modelName + classifierSuffix, ccQuantifier
                            .getClassifier().getRuntimeCustomizer());
            DataOutputStream output = new DataOutputStream(
                    storageManager.getOutputStreamForResource(modelName
                            + thresholdSuffix));
            if (ccQuantifier.getClassificationMode() == ClassificationMode.PER_DOCUMENT) {
                output.writeBoolean(true);
            } else {
                output.writeBoolean(false);
            }
            if (ccQuantifier.hasCustomThresholds()) {
                output.writeBoolean(true);
                TShortDoubleHashMap thresholds = ccQuantifier
                        .getCustomThresholds();
                output.writeInt(thresholds.size());
                TShortDoubleIterator iterator = thresholds.iterator();
                while (iterator.hasNext()) {
                    iterator.advance();
                    output.writeShort(iterator.key());
                    output.writeDouble(iterator.value());
                }
            } else {
                output.writeBoolean(false);
            }
            output.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public IQuantifier read(IStorageManager storageManager, String modelName) {
        IClassifier classifier = classifierDataManager.read(storageManager,
                modelName + classifierSuffix);
        IClassifierRuntimeCustomizer customizer = classifierDataManager
                .readClassifierRuntimeConfiguration(storageManager, modelName
                        + classifierSuffix);
        DataInputStream reader = new DataInputStream(
                storageManager.getInputStreamForResource(modelName
                        + thresholdSuffix));
        try {
            boolean perDocument = reader.readBoolean();
            ClassificationMode classificationMode = ClassificationMode.PER_DOCUMENT;
            if (perDocument) {
                classificationMode = ClassificationMode.PER_DOCUMENT;
            } else {
                classificationMode = ClassificationMode.PER_DOCUMENT;
            }
            boolean hasThresholds = reader.readBoolean();
            if (hasThresholds) {
                TShortDoubleHashMap thresholds = new TShortDoubleHashMap();
                int count = reader.readInt();
                for (int i = 0; i < count; ++i) {
                    short cat = reader.readShort();
                    double value = reader.readDouble();
                    thresholds.put(cat, value);
                }
                reader.close();
                return new CCQuantifier(classifier, customizer,
                        classificationMode, thresholds);
            } else {
                reader.close();
                return new CCQuantifier(classifier, customizer,
                        classificationMode);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
