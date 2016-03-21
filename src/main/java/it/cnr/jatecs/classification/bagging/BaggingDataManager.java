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

package it.cnr.jatecs.classification.bagging;

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.io.IStorageManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class BaggingDataManager implements IDataManager {

    protected IDataManager _manager;

    public BaggingDataManager(IDataManager manager) {
        assert (manager != null);
        _manager = manager;
    }

    @Override
    public void write(IStorageManager storageManager, String modelName,
                      IClassifier classifier) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid");
        if (classifier == null)
            throw new NullPointerException("The classifier is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        BaggingClassifier bc = (BaggingClassifier) classifier;
        int bagCount = bc._classifiers.length;

        String f = modelName + storageManager.getPathSeparator() + "bagCount";
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(f)));

        try {
            try {

                os.writeInt(bagCount);

                for (int i = 0; i < bagCount; ++i)
                    _manager.write(storageManager,
                            modelName + storageManager.getPathSeparator() + i,
                            bc._classifiers[i]);

            } finally {
                // Close the stream.
                os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing bagging data", e);
        }

    }

    @Override
    public IClassifier read(IStorageManager storageManager, String modelName) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        String f = modelName + storageManager.getPathSeparator() + "bagCount";
        DataInputStream os = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(f)));

        try {
            try {
                int bagCount = os.readInt();

                BaggingClassifier bc = new BaggingClassifier(bagCount);
                for (int i = 0; i < bagCount; ++i)
                    bc._classifiers[i] = _manager.read(storageManager,
                            modelName + storageManager.getPathSeparator() + i);

                return bc;

            } finally {
                os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Reading bagging classifier", e);
        }
    }

    @Override
    public void writeLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            ILearnerRuntimeCustomizer cust) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid: "
                    + modelName);
        if (cust == null)
            throw new NullPointerException("The runtime customizer is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        BaggingLearnerCustomizer customizer = (BaggingLearnerCustomizer) cust;

        String fname = modelName + storageManager.getPathSeparator()
                + "bagCount";

        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(fname)));
        try {
            try {
                os.writeInt(customizer._bagCount);

                _manager.writeLearnerRuntimeConfiguration(storageManager,
                        modelName + storageManager.getPathSeparator()
                                + "internalCustomizer",
                        customizer.getInternalCustomizer());

            } finally {
                os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Writing learning runtime configuration", e);
        }

    }

    @Override
    public ILearnerRuntimeCustomizer readLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid: "
                    + modelName);

        String fname = modelName + storageManager.getPathSeparator()
                + "bagCount";
        if (!storageManager.isResourceAvailable(fname))
            throw new IllegalArgumentException(
                    "The specified model name is not available on this storage manager: "
                            + fname);

        BaggingLearnerCustomizer c = new BaggingLearnerCustomizer();
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(fname)));
        try {
            try {
                int bagCount = is.readInt();
                c.setBagCount(bagCount);
                ILearnerRuntimeCustomizer cust = _manager
                        .readLearnerRuntimeConfiguration(storageManager,
                                modelName + storageManager.getPathSeparator()
                                        + "internalCustomizer");
                c.setInternalCustomizer(cust);

            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Reading learner runtime configuration",
                    e);
        }

        return c;
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
