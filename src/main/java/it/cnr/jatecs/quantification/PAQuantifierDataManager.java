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

import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.quantification.interfaces.IQuantifier;
import it.cnr.jatecs.quantification.interfaces.IQuantifierDataManager;
import it.cnr.jatecs.quantification.interfaces.IScalingFunction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PAQuantifierDataManager implements IQuantifierDataManager {
    private static String parametersSuffix = "_parameters";
    private static String classifierSuffix = "_classifier";
    private IDataManager classifierDataManager;

    public PAQuantifierDataManager(IDataManager classifierDataManager) {
        this.classifierDataManager = classifierDataManager;
    }

    public void write(IStorageManager storageManager, String modelName,
                      IQuantifier quantifier) {
        try {
            PAQuantifier paQuantifier = (PAQuantifier) quantifier;
            classifierDataManager.write(storageManager, modelName
                    + classifierSuffix, paQuantifier.getClassifier());
            classifierDataManager.writeClassifierRuntimeConfiguration(
                    storageManager, modelName + classifierSuffix, paQuantifier
                            .getClassifier().getRuntimeCustomizer());
            ObjectOutputStream output = new ObjectOutputStream(
                    storageManager.getOutputStreamForResource(modelName
                            + parametersSuffix));
            if (paQuantifier.getClassificationMode() == ClassificationMode.PER_DOCUMENT) {
                output.writeBoolean(true);
            } else {
                output.writeBoolean(false);
            }

            output.writeObject(paQuantifier.getScalingFunction());

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
        try {
            ObjectInputStream reader = new ObjectInputStream(
                    storageManager.getInputStreamForResource(modelName
                            + parametersSuffix));
            boolean perDocument = reader.readBoolean();
            ClassificationMode classificationMode = ClassificationMode.PER_DOCUMENT;
            if (perDocument) {
                classificationMode = ClassificationMode.PER_DOCUMENT;
            } else {
                classificationMode = ClassificationMode.PER_DOCUMENT;
            }
            IScalingFunction scalingFunction;
            try {
                scalingFunction = (IScalingFunction) reader.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            reader.close();
            return new PAQuantifier(classifier, customizer, classificationMode,
                    scalingFunction);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
