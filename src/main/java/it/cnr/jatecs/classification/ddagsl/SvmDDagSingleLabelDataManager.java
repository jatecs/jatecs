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

package it.cnr.jatecs.classification.ddagsl;

import it.cnr.jatecs.classification.ddagsl.SvmDDagSingleLabelClassifier.LocalClassifier;
import it.cnr.jatecs.classification.ddagsl.SvmDDagSingleLabelLearner.WeightingType;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.classification.svm.SvmDataManager;
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.IStorageManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Iterator;

public class SvmDDagSingleLabelDataManager implements IDataManager {

    public SvmDDagSingleLabelDataManager() {
        super();
    }

    @Override
    public void write(IStorageManager storageManager, String modelName,
                      IClassifier learningData) {
        try {
            if (storageManager == null)
                throw new NullPointerException("The storage manager is 'null'");
            if (!storageManager.isOpen())
                throw new IllegalStateException(
                        "The storage manager is not open");
            if (modelName == null || modelName.isEmpty())
                throw new IllegalArgumentException(
                        "The model name is 'null' or empty");
            if (learningData == null)
                throw new NullPointerException("The classifier is 'null'");
            if (!(learningData instanceof SvmDDagSingleLabelClassifier))
                throw new IllegalArgumentException(
                        "The classifier is not of type "
                                + SvmDDagSingleLabelClassifier.class.getName());

            SvmDDagSingleLabelClassifier c = (SvmDDagSingleLabelClassifier) learningData;
            Iterator<String> keys = c.getLocalClassifiers().keySet().iterator();

            DataOutputStream os = new DataOutputStream(
                    new BufferedOutputStream(
                            storageManager.getOutputStreamForResource(modelName
                                    + "_index_keys")));
            os.writeInt(c.getLocalClassifiers().size());

            try {
                while (keys.hasNext()) {
                    String key = keys.next();
                    LocalClassifier lc = c.getLocalClassifiers().get(key);

                    os.writeShort(lc.catIDPositive);
                    os.writeShort(lc.catIDNegative);
                    os.writeUTF(lc.weightingType.name());

                    SvmDataManager svmDataManager = new SvmDataManager();
                    svmDataManager.write(storageManager, modelName + "_" + key
                            + "_svm", lc.localClassifier);

                    TroveReadWriteHelper.writeFeatures(storageManager,
                            lc.localContentDB.getFeatureDB(), modelName + "_"
                                    + key + "_contentDB", true);
                    TroveReadWriteHelper.writeDocuments(storageManager,
                            lc.localContentDB.getDocumentDB(), modelName + "_"
                                    + key + "_contentDB", true);
                    TroveReadWriteHelper.writeContent(storageManager,
                            lc.localContentDB, modelName + "_" + key
                                    + "_contentDB", true);
                }
            } finally {
                os.close();
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Writing SvmDDagSingleLabelClassifier model", e);
        }
    }

    @Override
    public IClassifier read(IStorageManager storageManager, String modelName) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException(
                    "The model name is 'null' or empty");

        try {
            SvmDDagSingleLabelClassifier c = new SvmDDagSingleLabelClassifier();
            DataInputStream is = new DataInputStream(new BufferedInputStream(
                    storageManager.getInputStreamForResource(modelName
                            + "_index_keys")));
            try {
                int numClassifiers = is.readInt();
                for (int i = 0; i < numClassifiers; i++) {
                    short catPositive = is.readShort();
                    short catNegative = is.readShort();
                    String wtStr = is.readUTF();
                    WeightingType wt = WeightingType.valueOf(wtStr);
                    String key = LocalClassifier.buildKey(catPositive,
                            catNegative);
                    SvmDataManager svmDataManager = new SvmDataManager();
                    IClassifier localClassifier = svmDataManager.read(
                            storageManager, modelName + "_" + key + "_svm");

                    IContentDB contentDB = TroveReadWriteHelper.readContent(
                            storageManager, modelName + "_" + key
                                    + "_contentDB", TroveContentDBType.Full);

                    LocalClassifier lc = new LocalClassifier();
                    lc.catIDNegative = catNegative;
                    lc.catIDPositive = catPositive;
                    lc.localClassifier = localClassifier;
                    lc.weightingType = wt;
                    lc.localContentDB = contentDB;
                    c.getLocalClassifiers().put(key, lc);
                }

                return c;
            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Reading SvmDDagSingleLabelClassifier model", e);
        }
    }

    @Override
    public void writeLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            ILearnerRuntimeCustomizer customizer) {
    }

    @Override
    public ILearnerRuntimeCustomizer readLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName) {
        return null;
    }

    @Override
    public void writeClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            IClassifierRuntimeCustomizer customizer) {
    }

    @Override
    public IClassifierRuntimeCustomizer readClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName) {
        return null;
    }

}
