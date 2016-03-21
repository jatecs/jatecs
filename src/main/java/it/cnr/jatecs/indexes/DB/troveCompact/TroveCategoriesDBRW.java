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

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.*;
import java.util.Vector;

/**
 * Utility class which handles the data storage of Trove categories DB.
 *
 * @author Tiziano Fagni
 */
public class TroveCategoriesDBRW {

    public TroveCategoriesDBRW() {
        super();
    }

    /**
     * Read specified categories DB name from given storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The categories db name.
     * @return The categories DB read.
     * @throws NullPointerException     Raised if specified storage manager is 'null'.
     * @throws IllegalArgumentException Raised if specified db name is 'null', empty or invalid
     *                                  resource name.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public ICategoryDB read(IStorageManager storageManager, String dbName) {
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
        TroveCategoryDB categoriesDB = new TroveCategoryDB();

        try {
            categoriesDB._name = dis.readUTF();

            int count = dis.readInt();

            for (int i = 0; i < count; ++i) {
                short category = dis.readShort();
                String categoryName = dis.readUTF();
                categoriesDB._categoryMap.put(categoryName, category);
                if (category != i)
                    throw new RuntimeException("This is not a compact DB");
                categoriesDB._categoryRMap.add(categoryName);

                // Add information about parents.
                TShortArrayList par = new TShortArrayList();
                categoriesDB._categoryHierarchy.add(i, par);
                int numParents = dis.readInt();
                for (int j = 0; j < numParents; j++) {
                    short catID = dis.readShort();
                    par.add(catID);
                }
                par.sort();
            }

            return categoriesDB;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Reading categories DB on storage manager", e);
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                throw new RuntimeException("Finally closing stream", e);
            }
        }
    }

    /**
     * Write the specified categories DB on the given storage manager by using
     * the given db name. If the categories DB already exists on the storage
     * manager and the parameter overwrite is true, the old db version will be
     * removed. In case the categories DB already exists and the parameter
     * overwrite is false, the method does nothing.
     *
     * @param storageManager The storage manager.
     * @param catsDB         The categories DB to write.
     * @param dbName         The db name.
     * @param overwrite      True if an old db with the same name must be overwritten,
     *                       false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the index is
     *                                  'null'.
     * @throws IllegalArgumentException Raised if the db name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public void write(IStorageManager storageManager, ICategoryDB catsDB,
                      String dbName, boolean overwrite) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (catsDB == null)
            throw new NullPointerException("The specified cats DB is 'null'");
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
            dos.writeUTF(catsDB.getName());

            dos.writeInt(catsDB.getCategoriesCount());

            IShortIterator catIt = catsDB.getCategories();
            while (catIt.hasNext()) {
                short category = catIt.next();
                dos.writeShort(category);
                dos.writeUTF(catsDB.getCategoryName(category));

                // Write the parents of this category.
                Vector<Short> parents = new Vector<Short>();
                IShortIterator it = catsDB.getParentCategories(category);
                while (it.hasNext()) {
                    parents.add(it.next());
                }
                dos.writeInt(parents.size());
                for (int i = 0; i < parents.size(); i++)
                    dos.writeShort(parents.get(i));
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Writing categories DB on storage manager", e);
        } finally {
            try {
                dos.close();
            } catch (IOException e) {
                throw new RuntimeException("Finally closing stream", e);
            }
        }
    }

}
