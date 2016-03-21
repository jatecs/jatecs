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

import gnu.trove.TShortObjectHashMap;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.adaboost.AdaBoostDataManager;
import it.cnr.jatecs.classification.bagging.BaggingClassifier;
import it.cnr.jatecs.classification.bagging.BaggingDataManager;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
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
import java.util.Collections;
import java.util.Vector;

public class CleanByBagConf {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err
                    .println("Usage: CleanByBagConf <indexDirectory> <classifierDirectory>");
            return;
        }

        File file = new File(args[0]);
        String indexPath = file.getParentFile().getPath();
        String indexName = file.getName();

        file = new File(args[1]);
        String classifierPath = file.getParentFile().getPath();
        String classifierName = file.getName();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                indexPath, false);
        storageManager.open();
        IIndex index = TroveReadWriteHelper.readIndex(storageManager,
                indexName, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        AdaBoostDataManager internalDataManager = new AdaBoostDataManager();
        BaggingDataManager dataManager = new BaggingDataManager(
                internalDataManager);

        storageManager = new FileSystemStorageManager(classifierPath, false);
        storageManager.open();
        BaggingClassifier classifier = (BaggingClassifier) dataManager.read(
                storageManager, classifierName);
        storageManager.close();

        IIntIterator docs = index.getDocumentDB().getDocuments();
        IShortIterator cats = index.getCategoryDB().getCategories();

        TShortObjectHashMap<Vector<IdScorePair>> sets = new TShortObjectHashMap<Vector<IdScorePair>>();
        while (cats.hasNext()) {
            short cat = cats.next();
            sets.put(cat, new Vector<IdScorePair>());
        }

        while (docs.hasNext()) {
            int doc = docs.next();
            ClassificationResult crVariance = classifier.computeVariance(index,
                    doc);
            ClassificationResult crClassification = classifier.classify(index,
                    doc);
            for (int i = 0; i < crClassification.categoryID.size(); ++i) {
                short cat = crClassification.categoryID.getQuick(i);
                double scoreCorrection = 1.0 / (Math.sqrt(crVariance.score
                        .getQuick(i)) + Double.MIN_VALUE);
                double score = crClassification.score.getQuick(i);
                int flag = -1;
                if (index.getClassificationDB().hasDocumentCategory(doc, cat))
                    flag = 1;
                Vector<IdScorePair> vector = (Vector<IdScorePair>) sets
                        .get(cat);
                vector.add(new IdScorePair(doc, flag * score * scoreCorrection));
            }
        }

        FileOutputStream fstream = new FileOutputStream(indexPath
                + Os.pathSeparator() + indexName + "_CONF-" + classifierName
                + ".txt");
        PrintStream out = new PrintStream(fstream);
        cats.begin();
        while (cats.hasNext()) {
            short cat = cats.next();
            Vector<IdScorePair> vector = (Vector<IdScorePair>) sets.get(cat);
            Collections.sort(vector);
            for (int i = 0; i < vector.size(); ++i) {
                IdScorePair p = vector.get(i);
                out.println(p.Id() + "\t" + cat + "\t" + p.Score());
            }
        }
        out.close();
    }
}
