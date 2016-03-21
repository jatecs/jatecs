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

package apps.trainingDataCleaning;

import gnu.trove.TIntHashSet;
import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

public class RandomPerturbation {

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err
                    .println("Usage: RandomPerturbation <indexDirectory> <amount> <seed>");
            return;
        }

        File file = new File(args[0]);
        String path = file.getParentFile().getPath();
        String indexname = file.getName();

        double amount = Double.parseDouble(args[1]);
        int seed = Integer.parseInt(args[2]);

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                path, false);
        storageManager.open();
        IIndex index = TroveReadWriteHelper.readIndex(storageManager,
                indexname, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        TroveClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                index.getDocumentDB(), index.getCategoryDB());

        IIntIterator docs = index.getDocumentDB().getDocuments();
        IShortIterator cats = index.getCategoryDB().getCategories();

        int total = index.getDocumentDB().getDocumentsCount();
        int toBeChanged = (int) (amount * total);

        Random random = new Random(seed);

        TIntHashSet set = new TIntHashSet();

        while (set.size() < toBeChanged) {
            int id = random.nextInt(total);
            set.add(id);
        }

        FileOutputStream fstream = new FileOutputStream(path
                + Os.pathSeparator() + indexname + "_RP-" + args[1] + ".txt");
        PrintStream out = new PrintStream(fstream);
        while (docs.hasNext()) {
            int doc = docs.next();
            cats.begin();
            if (set.contains(doc)) {
                while (cats.hasNext()) {
                    short cat = cats.next();
                    out.println(doc + "\t" + cat);
                    if (!index.getClassificationDB().hasDocumentCategory(doc,
                            cat))
                        builder.setDocumentCategory(doc, cat);
                }
            } else {
                while (cats.hasNext()) {
                    short cat = cats.next();
                    if (index.getClassificationDB().hasDocumentCategory(doc,
                            cat))
                        builder.setDocumentCategory(doc, cat);
                }
            }
        }
        out.close();

        index = new GenericIndex(index.getFeatureDB(), index.getDocumentDB(),
                index.getCategoryDB(), index.getDomainDB(),
                index.getContentDB(), index.getWeightingDB(),
                builder.getClassificationDB());

        storageManager = new FileSystemStorageManager(path, false);
        storageManager.open();
        TroveReadWriteHelper.writeIndex(storageManager, index, indexname
                + "_RP-" + args[1], true);
        storageManager.close();
    }
}
