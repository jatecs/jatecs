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
import gnu.trove.TShortObjectHashMap;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CreateCleanIndex {

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err
                    .println("Usage: CreateCleanIndex <percent> <cleanIndex> <perturbedIndex> <rankFile> ");
            return;
        }

        int percent = Integer.parseInt(args[0]);
        File file = new File(args[1]);
        String cleanIndexPath = file.getParent();
        String cleanIndexName = file.getName();
        file = new File(args[2]);
        String pertIndexPath = file.getParent();
        String pertIndexName = file.getName();
        file = new File(args[3]);
        String rankPath = file.getParent();
        String rankName = file.getName();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                cleanIndexPath, false);
        storageManager.open();
        IIndex cleanIndex = TroveReadWriteHelper.readIndex(storageManager,
                cleanIndexName, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        storageManager = new FileSystemStorageManager(pertIndexPath, false);
        storageManager.open();
        IIndex pertIndex = TroveReadWriteHelper.readIndex(storageManager,
                pertIndexName, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        int k = (pertIndex.getDocumentDB().getDocumentsCount() * percent) / 100;
        System.out.println("Cleaning the first " + k + " document out of "
                + pertIndex.getDocumentDB().getDocumentsCount());

        TShortObjectHashMap<TIntHashSet> ranks = new TShortObjectHashMap<TIntHashSet>();

        FileReader freader = new FileReader(rankPath + Os.pathSeparator()
                + rankName + ".txt");
        BufferedReader in = new BufferedReader(freader);
        String line;
        while ((line = in.readLine()) != null) {
            String[] fields = line.split("\t");
            int doc = Integer.parseInt(fields[0]);
            short cat = Short.parseShort(fields[1]);

            TIntHashSet vect = (TIntHashSet) ranks.get(cat);

            if (vect == null) {
                vect = new TIntHashSet();
                ranks.put(cat, vect);
            }
            if (vect.size() < k)
                vect.add(doc);
        }
        in.close();

        TroveClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                cleanIndex.getDocumentDB(), cleanIndex.getCategoryDB());

        IIntIterator docs = cleanIndex.getDocumentDB().getDocuments();
        IShortIterator cats = cleanIndex.getCategoryDB().getCategories();

        while (cats.hasNext()) {
            short cat = cats.next();
            TIntHashSet vect = (TIntHashSet) ranks.get(cat);
            docs.begin();
            while (docs.hasNext()) {
                int doc = docs.next();
                if (vect.contains(doc)) {
                    if (cleanIndex.getClassificationDB().hasDocumentCategory(
                            doc, cat))
                        builder.setDocumentCategory(doc, cat);
                } else {
                    if (pertIndex.getClassificationDB().hasDocumentCategory(
                            doc, cat))
                        builder.setDocumentCategory(doc, cat);
                }
            }
        }

        IIndex newIndex = new GenericIndex(cleanIndex.getFeatureDB(),
                cleanIndex.getDocumentDB(), cleanIndex.getCategoryDB(),
                cleanIndex.getDomainDB(), cleanIndex.getContentDB(),
                cleanIndex.getWeightingDB(), builder.getClassificationDB());

        storageManager = new FileSystemStorageManager(rankPath, false);
        storageManager.open();
        TroveReadWriteHelper.writeIndex(storageManager, newIndex, rankName
                + "_K-" + args[0], true);
        storageManager.close();

    }
}
