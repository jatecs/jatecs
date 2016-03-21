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

package apps.utils;

import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.File;

public class PrintClassification {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err
                    .println("Usage: PrintClassification <classificationDirectory>");
            return;
        }

        File file = new File(args[0]);
        String path = file.getParentFile().getPath();
        String indexname = file.getName();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                path, false);
        storageManager.open();
        IClassificationDB classification = TroveReadWriteHelper
                .readClassification(storageManager, indexname,
                        TroveClassificationDBType.Full);
        storageManager.close();

        IIntIterator docs = classification.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            IShortIterator cats = classification.getDocumentCategories(docs
                    .next());
            while (cats.hasNext()) {
                System.out.print(cats.next() + " ");
            }
            System.out.print("\n");
        }
    }
}
