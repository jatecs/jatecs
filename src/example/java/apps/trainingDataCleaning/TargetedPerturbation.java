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

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TShortObjectHashMap;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.adaboost.AdaBoostDataManager;
import it.cnr.jatecs.classification.interfaces.IClassifier;
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

import java.io.*;
import java.util.Collections;
import java.util.Vector;

public class TargetedPerturbation {

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err
                    .println("Usage: TargetedPerturbation <indexDirectory> <amount> <classifierDirectory>");
            return;
        }

        File file = new File(args[0]);
        String indexPath = file.getParentFile().getPath();
        String indexName = file.getName();

        double amount = Double.parseDouble(args[1]);

        file = new File(args[2]);
        String classifierPath = file.getParentFile().getPath();
        String classifierName = file.getName();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                indexPath, false);
        storageManager.open();
        IIndex index = TroveReadWriteHelper.readIndex(storageManager,
                indexName, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        TroveClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                index.getDocumentDB(), index.getCategoryDB());

        AdaBoostDataManager dataManager = new AdaBoostDataManager();

        storageManager = new FileSystemStorageManager(classifierPath, false);
        storageManager.open();
        IClassifier classifier = dataManager.read(storageManager,
                classifierName);
        storageManager.close();

        IIntIterator docs = index.getDocumentDB().getDocuments();
        IShortIterator cats = index.getCategoryDB().getCategories();

        int total = index.getDocumentDB().getDocumentsCount();
        int toBeChanged = (int) (amount * total);

        TShortObjectHashMap<Serializable> sets = new TShortObjectHashMap<Serializable>();
        while (cats.hasNext()) {
            short cat = cats.next();
            sets.put(cat, new Vector<IdScorePair>());
        }

        while (docs.hasNext()) {
            int doc = docs.next();
            ClassificationResult cr = classifier.classify(index, doc);
            for (int i = 0; i < cr.categoryID.size(); ++i) {
                short cat = cr.categoryID.getQuick(i);
                double score = Math.abs(cr.score.getQuick(i));
                @SuppressWarnings("unchecked")
                Vector<IdScorePair> vector = (Vector<IdScorePair>) sets
                        .get(cat);
                vector.add(new IdScorePair(doc, score));
            }
        }

        cats.begin();
        while (cats.hasNext()) {
            short cat = cats.next();
            @SuppressWarnings("unchecked")
            Vector<IdScorePair> vector = (Vector<IdScorePair>) sets.get(cat);
            Collections.sort(vector);
            TIntDoubleHashMap set = new TIntDoubleHashMap();
            for (int i = 0; i < toBeChanged; ++i) {
                IdScorePair p = vector.get(i);
                set.put(p.Id(), p.Score());
            }
            sets.put(cat, set);
        }

        FileOutputStream fstream = new FileOutputStream(indexPath
                + Os.pathSeparator() + indexName + "_TP-" + classifierName
                + "-" + args[1] + ".txt");
        PrintStream out = new PrintStream(fstream);
        cats.begin();
        while (cats.hasNext()) {
            short cat = cats.next();
            TIntDoubleHashMap set = (TIntDoubleHashMap) sets.get(cat);
            docs.begin();
            while (docs.hasNext()) {
                int doc = docs.next();
                if (set.contains(doc)) {
                    out.println(doc + "\t" + cat + "\t" + set.get(doc));
                    if (!index.getClassificationDB().hasDocumentCategory(doc,
                            cat))
                        builder.setDocumentCategory(doc, cat);
                } else {
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

        storageManager = new FileSystemStorageManager(indexPath, false);
        storageManager.open();
        TroveReadWriteHelper.writeIndex(storageManager, index, indexName
                + "_TP-" + classifierName + "-" + args[1], true);
        storageManager.close();
    }
}
