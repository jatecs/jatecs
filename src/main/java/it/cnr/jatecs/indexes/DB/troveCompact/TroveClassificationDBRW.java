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

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.*;

/**
 * Utility class which handles the data storage of Trove classification DB.
 *
 * @author Tiziano Fagni
 */
public class TroveClassificationDBRW {

    protected ICategoryDB _categoriesDB;
    protected IDocumentDB _documentsDB;
    public TroveClassificationDBRW(IDocumentDB documentsDB,
                                   ICategoryDB categoriesDB) {
        super();
        _categoriesDB = categoriesDB;
        _documentsDB = documentsDB;
    }

    /**
     * Read specified classification DB name from given storage manager. The
     * classification DB is loaded using the specified db optimization type
     * "DBType".
     *
     * @param storageManager
     *            The storage manager.
     * @param dbName
     *            The classification db name.
     * @param DBType
     *            The type of optimization to use when creating the DB.
     * @return The classification DB read.
     * @throws NullPointerException
     *             Raised if specified storage manager is 'null' or DB type is
     *             'null'.
     * @throws IllegalArgumentException
     *             Raised if specified db name is 'null', empty or invalid
     *             resource name.
     * @throws IllegalStateException
     *             Raised if the storage manager is not open.
     */
    public IClassificationDB read(IStorageManager storageManager,
                                  String dbName, TroveClassificationDBType DBType) {
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

        IClassificationDBBuilder classificationDBBuilder;
        if (DBType == TroveClassificationDBType.Full)
            classificationDBBuilder = new TroveClassificationFullDBBuilder(
                    _documentsDB, _categoriesDB);
        else if (DBType == TroveClassificationDBType.IL)
            classificationDBBuilder = new TroveClassificationILDBBuilder(
                    _documentsDB, _categoriesDB);
        else
            classificationDBBuilder = new TroveClassificationDBBuilder(
                    _documentsDB, _categoriesDB);

        try {
            classificationDBBuilder.getClassificationDB()
                    .setName(dis.readUTF());

            int count = dis.readInt();
            for (int i = 0; i < count; ++i) {
                int document = dis.readInt();
                int catCount = dis.readInt();
                for (int j = 0; j < catCount; ++j) {
                    short cat = dis.readShort();
                    boolean primary = dis.readBoolean();
                    classificationDBBuilder.setDocumentCategory(document, cat,
                            primary);
                }
            }

            return classificationDBBuilder.getClassificationDB();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Reading classification DB from storage manager", e);
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                throw new RuntimeException("Finally closing stream", e);
            }
        }
    }

    /**
     * Write the specified classification DB on the given storage manager by using the given
     * db name. If the classification DB already exists on the storage manager and the
     * parameter overwrite is true, the old db version will be removed. In
     * case the classification DB already exists and the parameter overwrite is false, the
     * method does nothing.
     *
     * @param storageManager
     *            The storage manager.
     * @param classificationDB
     *            The classification DB to write.
     * @param dbName
     *            The db name.
     * @param overwrite
     *            True if an old db with the same name must be overwritten,
     *            false otherwise.
     * @throws NullPointerException Raised if the storage manager is 'null' or
     * the index is 'null'.
     * @throws IllegalArgumentException Raised if the db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public void write(IStorageManager storageManager,
                      IClassificationDB classificationDB, String dbName, boolean overwrite) {

        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (classificationDB == null)
            throw new NullPointerException(
                    "The specified classification DB is 'null'");
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
            dos.writeUTF(classificationDB.getName());

            dos.writeInt(classificationDB.getDocumentDB().getDocumentsCount());

            IIntIterator docIt = classificationDB.getDocumentDB()
                    .getDocuments();
            while (docIt.hasNext()) {
                int document = docIt.next();
                dos.writeInt(document);

                dos.writeInt(classificationDB
                        .getDocumentCategoriesCount(document));

                IShortIterator catIt = classificationDB
                        .getDocumentCategories(document);
                while (catIt.hasNext()) {
                    short cat = catIt.next();
                    dos.writeShort(cat);
                    dos.writeBoolean(classificationDB.isPrimaryCategory(
                            document, cat));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Writing classification DB on storage manager", e);
        } finally {
            try {
                dos.close();
            } catch (IOException e) {
                throw new RuntimeException("Finally closing stream", e);
            }
        }
    }

}
