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

package apps.chunkClassification;

import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.File;
import java.io.IOException;

/**
 * This app merges the classification of chunk-documents into a classification
 * of the whole documents (see also DocumentSplittingCorpusReader,
 * IndexDepSplittingCSV).
 */
public class MergeSplitClassification {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err
                    .println("Usage: MergeSplitClassification <predictedSplitClassificationDirectory> <trueClassificationDirectory>");
            return;
        }

        String predictionFilename = args[0];
        File predictionFile = new File(predictionFilename);
        predictionFilename = predictionFile.getName();
        String predictionPath = predictionFile.getParent();

        String trueValuesFilename = args[1];
        File trueValuesFile = new File(trueValuesFilename);
        trueValuesFilename = trueValuesFile.getName();
        String trueValuesPath = trueValuesFile.getParent();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                predictionPath, false);
        storageManager.open();

        IClassificationDB predictions = TroveReadWriteHelper
                .readClassification(storageManager, predictionFilename);

        storageManager.close();

        storageManager = new FileSystemStorageManager(trueValuesPath, false);
        storageManager.open();
        IClassificationDB trueValues = TroveReadWriteHelper.readClassification(
                storageManager, trueValuesFilename);
        storageManager.close();

        TroveClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                trueValues.getDocumentDB(), trueValues.getCategoryDB());

        IIntIterator docs = trueValues.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int docId = docs.next();
            String docName = trueValues.getDocumentDB().getDocumentName(docId);

            int i = 0;
            boolean found = true;
            while (found) {
                int splitDocId = predictions.getDocumentDB().getDocument(
                        docName + "_" + i);
                if (splitDocId < 0)
                    found = false;
                else {
                    IShortIterator cats = predictions
                            .getDocumentCategories(splitDocId);
                    while (cats.hasNext()) {
                        short cat = cats.next();
                        builder.setDocumentCategory(docId, cat);
                    }
                    ++i;
                }

            }
        }

        String classificationName = predictionFilename + ".mer";

        IClassificationDB mergedClassification = builder.getClassificationDB();
        mergedClassification.setName(classificationName);

        storageManager = new FileSystemStorageManager(predictionPath, false);
        storageManager.open();
        TroveReadWriteHelper.writeClassification(storageManager,
                mergedClassification, classificationName, true);
        storageManager.close();
    }
}
