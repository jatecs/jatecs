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

import it.cnr.jatecs.indexes.DB.generic.MultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IMultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentLanguageDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.io.IStorageManager;

import java.io.IOException;

public class TroveMultilingualReadWriteHelper extends TroveReadWriteHelper {

    private static final String langMapName = "langmap";

    public static void writeIndex(IStorageManager storageManager,
                                  IMultilingualIndex index, String name, boolean overwrite) {
        TroveReadWriteHelper.writeIndex(storageManager, index, name, overwrite);
        try {
            TroveDocumentLanguageDB.write(storageManager,
                    index.getDocumentLanguageDB(), name + langMapName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static IMultilingualIndex readIndex(IStorageManager storageManager,
                                                String indexname, TroveContentDBType contentDBType,
                                                TroveClassificationDBType classificationDBType) {

        IIndex index = TroveReadWriteHelper.readIndex(storageManager,
                indexname, contentDBType, classificationDBType);

        TroveDocumentLanguageDB documentLanguageDB = null;
        try {
            documentLanguageDB = TroveDocumentLanguageDB.read(storageManager,
                    indexname + langMapName, index.getDocumentDB());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MultilingualIndex clindex = new MultilingualIndex(
                index.getFeatureDB(), index.getDocumentDB(),
                index.getCategoryDB(), index.getDomainDB(),
                documentLanguageDB.getLanguageDB(), index.getContentDB(),
                index.getWeightingDB(), index.getClassificationDB(),
                (IDocumentLanguageDB) documentLanguageDB);

        return clindex;

    }

}
