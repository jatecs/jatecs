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

package apps.output;

import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.File;
import java.io.FileWriter;

public class DumpClassification {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err
                    .println("Usage: DumpClassification <classificationDirectory> [onlyLeaves]");
            return;
        }

        String dataPath = args[0];

        boolean onlyLeaves = false;

        if (args.length > 1)
            onlyLeaves = true;

        File file = new File(dataPath);

        String indexName = file.getName();
        dataPath = file.getParent();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                dataPath, false);
        storageManager.open();
        IClassificationDB classification = TroveReadWriteHelper
                .readClassification(storageManager, indexName);
        storageManager.close();

        FileWriter writer = new FileWriter(dataPath + "/CSV_" + file.getName()
                + "_classification.txt");

        IIntIterator docs = classification.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int doc = docs.next();
            writer.write(classification.getDocumentDB().getDocumentName(doc));
            IShortIterator cats = classification.getDocumentCategories(doc);
            while (cats.hasNext()) {
                short cat = cats.next();
                String catName = classification.getCategoryDB()
                        .getCategoryName(cat);
                if (onlyLeaves) {
                    if (!classification.getCategoryDB().hasChildCategories(cat))
                        writer.write("\t" + catName);
                } else {
                    writer.write("\t" + catName);
                }
            }
            writer.write(Os.newline());
        }
        writer.close();

    }
}
