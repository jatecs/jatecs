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

import gnu.trove.TIntArrayList;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDomainDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.*;

/**
 * Utility class which handles the data storage of Trove domain DB.
 *
 * @author Tiziano Fagni
 */
public class TroveDomainDBRW {

    protected IFeatureDB _featuresDB;
    protected ICategoryDB _categoriesDB;
    public TroveDomainDBRW(ICategoryDB categoriesDB, IFeatureDB featuresDB) {
        super();
        _featuresDB = featuresDB;
        _categoriesDB = categoriesDB;
    }

    /**
     * Read specified domain DB name from given storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The domain db name.
     * @return The domain DB read.
     * @throws NullPointerException     Raised if specified storage manager is 'null'.
     * @throws IllegalArgumentException Raised if specified db name is 'null', empty or invalid
     *                                  resource name.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public IDomainDB read(IStorageManager storageManager, String dbName) {
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
        TroveDomainDB domainDB = new TroveDomainDB(_categoriesDB, _featuresDB);

        try {
            String name = dis.readUTF();
            domainDB.setName(name);
            boolean localRepresentation = dis.readBoolean();

            if (localRepresentation) {
                int count = dis.readInt();
                for (int i = 0; i < count; ++i) {
                    short category = dis.readShort();
                    TIntArrayList feats = domainDB._categoriesFeatures
                            .get(category);
                    int featCount = dis.readInt();
                    for (int j = 0; j < featCount; ++j) {
                        feats.add(dis.readInt());
                    }
                    feats.sort();
                    domainDB._hasLocalRepresentation = domainDB._hasLocalRepresentation
                            || feats.size() > 0;
                }
            }

            return domainDB;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Reading domain DB from storage manager", e);
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                throw new RuntimeException("Finally closing stream", e);
            }
        }
    }

    /**
     * Write the specified domain DB on the given storage manager by using the
     * given db name. If the domain DB already exists on the storage manager and
     * the parameter overwrite is true, the old db version will be removed. In
     * case the domain DB already exists and the parameter overwrite is false,
     * the method does nothing.
     *
     * @param storageManager The storage manager.
     * @param domainDB       The domain DB to write.
     * @param dbName         The db name.
     * @param overwrite      True if an old db with the same name must be overwritten,
     *                       false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the index is
     *                                  'null'.
     * @throws IllegalArgumentException Raised if the db name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public void write(IStorageManager storageManager, IDomainDB domainDB,
                      String dbName, boolean overwrite) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (domainDB == null)
            throw new NullPointerException("The specified domain DB is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (storageManager.isResourceAvailable(dbName) && !overwrite)
            return;

        if (storageManager.isResourceAvailable(dbName) && overwrite)
            storageManager.deleteResource(dbName);

        TroveDomainDB troveDomainDB = (TroveDomainDB) domainDB;
        OutputStream os = storageManager.getOutputStreamForResource(dbName);
        DataOutputStream dos = new DataOutputStream(os);

        try {
            dos.writeUTF(domainDB.getName());

            if (domainDB.hasLocalRepresentation()) {
                dos.writeBoolean(true);
                dos.writeInt(domainDB.getCategoryDB().getCategoriesCount());

                IShortIterator catIt = domainDB.getCategoryDB().getCategories();
                while (catIt.hasNext()) {
                    short category = catIt.next();
                    dos.writeShort(category);
                    TIntArrayList featList = troveDomainDB._categoriesFeatures
                            .get(category);
                    dos.writeInt(featList.size());
                    IIntIterator featIt = new TIntArrayListIterator(featList);
                    while (featIt.hasNext())
                        dos.writeInt(featIt.next());
                }
            } else
                dos.writeBoolean(false);

        } catch (Exception e) {
            throw new RuntimeException("Writing domain DB on storage manager",
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
