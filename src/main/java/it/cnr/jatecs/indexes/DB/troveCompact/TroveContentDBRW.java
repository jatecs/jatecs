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
import it.cnr.jatecs.indexes.DB.interfaces.IContentDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.io.*;

/**
 * Utility class which handles the data storage of Trove content DB.
 *
 * @author Tiziano Fagni
 */
public class TroveContentDBRW {

    protected IFeatureDB _featuresDB;
    protected IDocumentDB _documentsDB;
    public TroveContentDBRW(IDocumentDB documentsDB, IFeatureDB featuresDB) {
        super();
        _featuresDB = featuresDB;
        _documentsDB = documentsDB;
    }

    /**
     * Read specified content DB name from given storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The content db name.
     * @return The content DB read.
     * @throws NullPointerException     Raised if specified storage manager is 'null'.
     * @throws IllegalArgumentException Raised if specified db name is 'null', empty or invalid
     *                                  resource name.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public IContentDB read(IStorageManager storageManager, String dbName) {
        return read(storageManager, dbName, TroveContentDBType.Default);
    }

    /**
     * Read specified content DB name from given storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The content db name.
     * @param DBType         The type of content DB to use.
     * @return The content DB read.
     * @throws NullPointerException     Raised if specified storage manager is 'null' or db type is
     *                                  'null'.
     * @throws IllegalArgumentException Raised if specified db name is 'null', empty or invalid
     *                                  resource name.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public IContentDB read(IStorageManager storageManager, String dbName,
                           TroveContentDBType DBType) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (dbName == null || dbName.isEmpty()
                || !storageManager.isResourceAvailable(dbName))
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is not available on this storage manager");
        if (DBType == null)
            throw new NullPointerException("The specified DB type is 'null'");

        InputStream is = storageManager.getInputStreamForResource(dbName);
        DataInputStream dis = new DataInputStream(is);

        IContentDBBuilder contentDBBuilder;
        if (DBType == TroveContentDBType.Full)
            contentDBBuilder = new TroveContentFullDBBuilder(_documentsDB,
                    _featuresDB);
        else if (DBType == TroveContentDBType.IL)
            contentDBBuilder = new TroveContentILDBBuilder(_documentsDB,
                    _featuresDB);
        else
            contentDBBuilder = new TroveContentDBBuilder(_documentsDB,
                    _featuresDB);

        try {
            contentDBBuilder.getContentDB().setName(dis.readUTF());

            int count = dis.readInt();
            for (int i = 0; i < count; ++i) {
                int document = dis.readInt();
                int featCount = dis.readInt();
                for (int j = 0; j < featCount; ++j) {
                    int features = dis.readInt();
                    int featFreq = dis.readInt();
                    contentDBBuilder.setDocumentFeatureFrequency(document,
                            features, featFreq);
                }
            }

            return contentDBBuilder.getContentDB();
        } catch (Exception e) {
            throw new RuntimeException("Reading content DB on storage manager",
                    e);
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                throw new RuntimeException("Finally closing stream", e);
            }
        }
    }

    /**
     * Write the specified content DB on the given storage manager by using the
     * given db name. If the content DB already exists on the storage manager
     * and the parameter overwrite is true, the old db version will be removed.
     * In case the content DB already exists and the parameter overwrite is
     * false, the method does nothing.
     *
     * @param storageManager The storage manager.
     * @param contentDB      The content DB to write.
     * @param dbName         The db name.
     * @param overwrite      True if an old db with the same name must be overwritten,
     *                       false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the index is
     *                                  'null'.
     * @throws IllegalArgumentException Raised if the db name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public void write(IStorageManager storageManager, IContentDB contentDB,
                      String dbName, boolean overwrite) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (contentDB == null)
            throw new NullPointerException("The specified content DB is 'null'");
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
            dos.writeUTF(contentDB.getName());

            dos.writeInt(contentDB.getDocumentDB().getDocumentsCount());

            IIntIterator docIt = contentDB.getDocumentDB().getDocuments();
            while (docIt.hasNext()) {
                int document = docIt.next();
                dos.writeInt(document);

                dos.writeInt(contentDB.getDocumentFeaturesCount(document));

                IIntIterator featIt = contentDB.getDocumentFeatures(document);
                while (featIt.hasNext()) {
                    int feature = featIt.next();
                    dos.writeInt(feature);
                    dos.writeInt(contentDB.getDocumentFeatureFrequency(
                            document, feature));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Writing content DB on storage manager",
                    e);
        } finally {
            try {
                dos.close();
            } catch (IOException e) {
                throw new RuntimeException("Finally closing stream", e);
            }
        }
    }

}
