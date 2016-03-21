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

package it.cnr.jatecs.indexes.DB.troveCompact;

import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IWeightingDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.io.*;

/**
 * Utility class which handles the data storage of Trove weighting DB.
 *
 * @author Tiziano Fagni
 */
public class TroveWeightingDBRW {

    protected IContentDB _contentDB;

    public TroveWeightingDBRW(IContentDB contentDB) {
        super();
        _contentDB = contentDB;
    }

    /**
     * Read specified weighting DB name from given storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The weighting db name.
     * @return The weighting DB read.
     * @throws NullPointerException     Raised if specified storage manager is 'null'.
     * @throws IllegalArgumentException Raised if specified db name is 'null', empty or invalid
     *                                  resource name.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public IWeightingDB read(IStorageManager storageManager, String dbName) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (dbName == null || dbName.isEmpty()
                || !storageManager.isResourceAvailable(dbName))
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is not available on this storage manager");

        InputStream is = storageManager.getInputStreamForResource(dbName);
        DataInputStream dis = new DataInputStream(is);
        TroveWeightingDBBuilder weightingDBBuilder = new TroveWeightingDBBuilder(
                _contentDB);

        try {
            weightingDBBuilder.getWeightingDB().setName(dis.readUTF());

            int count = dis.readInt();
            for (int i = 0; i < count; ++i) {
                int document = dis.readInt();
                int nonDefaultWeightsCount = dis.readInt();
                for (int j = 0; j < nonDefaultWeightsCount; ++j) {
                    int feature = dis.readInt();
                    double weight = dis.readDouble();
                    weightingDBBuilder.setDocumentFeatureWeight(document,
                            feature, weight);
                }
            }

            return weightingDBBuilder.getWeightingDB();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Reading weighting DB from storage manager", e);
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                throw new RuntimeException("Finally closing stream", e);
            }
        }
    }

    /**
     * Write the specified weighting DB on the given storage manager by using
     * the given db name. If the weighting DB already exists on the storage
     * manager and the parameter overwrite is true, the old db version will be
     * removed. In case the weighting DB already exists and the parameter
     * overwrite is false, the method does nothing.
     *
     * @param storageManager The storage manager.
     * @param weightingDB    The weighting DB to write.
     * @param dbName         The db name.
     * @param overwrite      True if an old db with the same name must be overwritten,
     *                       false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the index is
     *                                  'null'.
     * @throws IllegalArgumentException Raised if the db name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public void write(IStorageManager storageManager, IWeightingDB weightingDB,
                      String dbName, boolean overwrite) {

        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (weightingDB == null)
            throw new NullPointerException(
                    "The specified weighting DB is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (storageManager.isResourceAvailable(dbName) && !overwrite)
            return;

        if (storageManager.isResourceAvailable(dbName) && overwrite)
            storageManager.deleteResource(dbName);

        OutputStream os = storageManager.getOutputStreamForResource(dbName);
        DataOutputStream dos = new DataOutputStream(os);

        try {
            dos.writeUTF(weightingDB.getName());

            dos.writeInt(weightingDB.getContentDB().getDocumentDB()
                    .getDocumentsCount());

            IIntIterator docIt = weightingDB.getContentDB().getDocumentDB()
                    .getDocuments();
            while (docIt.hasNext()) {
                int document = docIt.next();
                dos.writeInt(document);

                int nonDefaultWeightsCount = 0;

                IIntIterator featIt = weightingDB.getContentDB()
                        .getDocumentFeatures(document);
                double defaultWeight = weightingDB.getDefaultWeight();
                while (featIt.hasNext()) {
                    if (weightingDB.getDocumentFeatureWeight(document,
                            featIt.next()) != defaultWeight)
                        ++nonDefaultWeightsCount;
                }
                dos.writeInt(nonDefaultWeightsCount);

                if (nonDefaultWeightsCount != 0) {
                    featIt.begin();
                    while (featIt.hasNext()) {
                        int feature = featIt.next();
                        if (weightingDB.getDocumentFeatureWeight(document,
                                feature) != defaultWeight) {
                            dos.writeInt(feature);
                            dos.writeDouble(weightingDB
                                    .getDocumentFeatureWeight(document, feature));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Writing weighting DB on storage manager", e);
        } finally {
            try {
                dos.close();
            } catch (IOException e) {
                throw new RuntimeException("Finally closing stream", e);
            }
        }
    }

}
