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

package it.cnr.jatecs.indexing.corpus.output;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ARFFOutput {

    public static void Write(String filename, IIndex index) {
        try {
            writeIndex(filename, index);

            writeDocumentMap(filename + "_docMap", index);
            writeCategoryMap(filename + "_catMap", index);
            writeFeatureMap(filename + "_featMap", index);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeDocumentMap(String filename, IIndex index)
            throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
                filename)));

        JatecsLogger.status().print("Writing Document<->ID map...");

        IIntIterator it = index.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int docID = it.next();
            writer.write("" + docID);
            writer.write(' ');
            writer.write(index.getDocumentDB().getDocumentName(docID));
            writer.write('\n');
        }

        JatecsLogger.status().println("done.");
        writer.close();
    }

    private static void writeCategoryMap(String filename, IIndex index)
            throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
                filename)));

        JatecsLogger.status().print("Writing Category<->ID map...");

        IShortIterator it = index.getCategoryDB().getCategories();
        while (it.hasNext()) {
            short catID = it.next();
            writer.write("" + catID);
            writer.write(' ');
            writer.write(index.getCategoryDB().getCategoryName(catID));
            writer.write('\n');
        }

        JatecsLogger.status().println("done.");
        writer.close();
    }

    private static void writeFeatureMap(String filename, IIndex index)
            throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
                filename)));

        JatecsLogger.status().print("Writing Feature<->ID map...");

        IIntIterator it = index.getFeatureDB().getFeatures();
        while (it.hasNext()) {
            int featID = it.next();
            writer.write("" + featID);
            writer.write(' ');
            writer.write(index.getFeatureDB().getFeatureName(featID));
            writer.write('\n');
        }

        JatecsLogger.status().println("done.");
        writer.close();
    }

    private static void writeIndex(String filename, IIndex index)
            throws Exception {

        TextualProgressBar bar = new TextualProgressBar(
                "Writing index in ARFF format");

        int numItems = index.getDocumentDB().getDocumentsCount()
                * index.getCategoryDB().getCategoriesCount();
        int numAnalyzed = 0;

        IShortIterator catit = index.getCategoryDB().getCategories();
        int featureCount = index.getFeatureDB().getFeaturesCount();
        while (catit.hasNext()) {
            short catId = catit.next();

            BufferedWriter writer = new BufferedWriter(new FileWriter(filename
                    + catId));

            writeHeader(index, catId, writer);

            writer.write("@DATA" + Os.newline());
            IIntIterator it = index.getDocumentDB().getDocuments();
            while (it.hasNext()) {
                int docID = it.next();
                StringBuilder b = new StringBuilder();

                writer.write("{");
                IIntIterator itFeats = index.getContentDB()
                        .getDocumentFeatures(docID);
                while (itFeats.hasNext()) {
                    int featID = itFeats.next();
                    double score = index.getWeightingDB()
                            .getDocumentFeatureWeight(docID, featID);
                    if (score == 0)
                        continue;

                    b.append(" " + featID + " " + score + ",");
                }

                if (index.getClassificationDB().hasDocumentCategory(docID,
                        catId))
                    b.append(featureCount + " \"yes\"}" + Os.newline());
                else
                    b.append(featureCount + " \"no\"}" + Os.newline());

                writer.write(b.toString());

                bar.signal((numAnalyzed * 100) / numItems);
            }

            writer.close();
        }

        bar.signal(100);
    }

    private static void writeHeader(IIndex index, short catId,
                                    BufferedWriter writer) throws IOException {
        writer.write("@RELATION " + catId + Os.newline() + Os.newline());
        IIntIterator features = index.getFeatureDB().getFeatures();

        while (features.hasNext())
            writer.write("@ATTRIBUTE f" + features.next() + " NUMERIC"
                    + Os.newline());

        writer.write("@ATTRIBUTE class {yes,no}" + Os.newline() + Os.newline());
    }
}
