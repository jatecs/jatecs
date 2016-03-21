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

import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.io.IStorageManager;

import java.io.*;

public class TroveIndexRW {

    protected IFeatureDB _featuresDB;
    protected IDocumentDB _documentsDB;
    protected ICategoryDB _categoriesDB;
    protected IDomainDB _domainDB;
    protected IContentDB _contentDB;
    protected IWeightingDB _weigthingDB;
    protected IClassificationDB _classificationDB;
    public TroveIndexRW(IFeatureDB featuresDB, IDocumentDB documentsDB,
                        ICategoryDB categoriesDB, IDomainDB domainDB, IContentDB contentDB,
                        IWeightingDB weigthingDB, IClassificationDB classificationDB) {
        super();
        _categoriesDB = categoriesDB;
        _classificationDB = classificationDB;
        _contentDB = contentDB;
        _weigthingDB = weigthingDB;
        _documentsDB = documentsDB;
        _domainDB = domainDB;
        _featuresDB = featuresDB;
    }

    /**
     * Read the specified index name from the given storage manager.
     *
     * @param storageManager The storage manager.
     * @param indexName      The index name to read.
     * @return The read index.
     * @throws NullPointerException     Raised if specified storage manager is 'null'.
     * @throws IllegalArgumentException Raised if specified index name is 'null', empty or invalid
     *                                  resource name.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public IIndex read(IStorageManager storageManager, String indexName) {

        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (indexName == null || indexName.isEmpty()
                || !storageManager.isResourceAvailable(indexName))
            throw new IllegalArgumentException("The index name <" + indexName
                    + "> is not available on this storage manager");

        InputStream is = storageManager.getInputStreamForResource(indexName);
        DataInputStream dis = new DataInputStream(is);
        try {
            String name = dis.readUTF();

            IIndex index = new GenericIndex(_featuresDB, _documentsDB,
                    _categoriesDB, _domainDB, _contentDB, _weigthingDB,
                    _classificationDB);
            index.setName(name);
            return index;
        } catch (Exception e) {
            throw new RuntimeException("Reading index from storage manager", e);
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                throw new RuntimeException("Finally closing stream", e);
            }
        }
    }

    /**
     * Write the specified index on the given storage manager by using the given
     * index name. If the index already exists on the storage manager and the
     * parameter overwrite is true, the old index version will be removed. In
     * case the index already exists and the parameter overwrite is false, the
     * method does nothing.
     *
     * @param storageManager The storage manager.
     * @param index          The index to write.
     * @param indexName      The index name.
     * @param overwrite      True if an old index with the same name must be overwritten,
     *                       false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the index is
     *                                  'null'.
     * @throws IllegalArgumentException Raised if the index name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public void write(IStorageManager storageManager, IIndex index,
                      String indexName, boolean overwrite) {

        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (index == null)
            throw new NullPointerException("The specified index is 'null'");
        if (indexName == null || indexName.isEmpty())
            throw new IllegalArgumentException("The index name <" + indexName
                    + "> is invalid");

        if (storageManager.isResourceAvailable(indexName) && !overwrite)
            return;

        if (storageManager.isResourceAvailable(indexName) && overwrite)
            storageManager.deleteResource(indexName);

        OutputStream os = storageManager.getOutputStreamForResource(indexName);
        DataOutputStream dos = new DataOutputStream(os);

        try {
            dos.writeUTF(index.getName());
        } catch (Exception e) {
            throw new RuntimeException("Writing index on storage manager", e);
        } finally {
            try {
                dos.close();
            } catch (IOException e) {
                throw new RuntimeException("Finally closing stream", e);
            }
        }
    }

}
