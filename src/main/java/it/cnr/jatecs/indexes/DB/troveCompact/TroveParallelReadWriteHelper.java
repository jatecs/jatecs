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

import it.cnr.jatecs.indexes.DB.generic.ParallelMultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IMultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IParallelMultilingualIndex;
import it.cnr.jatecs.io.IStorageManager;

import java.io.IOException;

public class TroveParallelReadWriteHelper extends TroveMultilingualReadWriteHelper {

    private static final String parallelMapName = "parallelmap";

    public static void writeIndex(IParallelMultilingualIndex index,
                                  IStorageManager storageManager, String filename, boolean overwrite) {
        TroveMultilingualReadWriteHelper.writeIndex(storageManager, index, filename,
                overwrite);
        try {
            TroveParallelDB.write(index.getParallelDB(), storageManager,
                    filename + parallelMapName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static IParallelMultilingualIndex readIndex(IStorageManager storageManager,
                                           String indexname, TroveContentDBType contentDBType,
                                           TroveClassificationDBType classificationDBType) {

        IMultilingualIndex index = TroveMultilingualReadWriteHelper.readIndex(
                storageManager, indexname, contentDBType, classificationDBType);

        TroveParallelDB parallelDB = null;
        try {
            parallelDB = TroveParallelDB.read(storageManager, indexname
                    + parallelMapName, index.getDocumentDB());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ParallelMultilingualIndex clindex = new ParallelMultilingualIndex(index.getFeatureDB(),
                index.getDocumentDB(), index.getCategoryDB(),
                index.getDomainDB(), index.getLanguageDB(),
                index.getContentDB(), index.getWeightingDB(),
                index.getClassificationDB(), index.getDocumentLanguageDB(),
                parallelDB);

        return clindex;

    }
}
