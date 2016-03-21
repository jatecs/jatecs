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
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class TreeBoostOutput {

    public static void Write(String filename, IIndex index) {
        try {
            writeIndex(filename, index);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeIndex(String filename, IIndex index)
            throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

        TextualProgressBar bar = new TextualProgressBar(
                "Writing index in TreeBoost format");

        int numItems = index.getDocumentDB().getDocumentsCount();
        int numAnalyzed = 0;

        IIntIterator it = index.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int docID = it.next();

            writer.write(index.getDocumentDB().getDocumentName(docID)
                    .replace(" ", "_").replace("|", "_"));

            IIntIterator features = index.getContentDB().getDocumentFeatures(
                    docID);

            while (features.hasNext())
                writer.write(" "
                        + index.getFeatureDB().getFeatureName(features.next())
                        .replace(" ", "_").replace("|", "_"));

            IShortIterator cats = index.getClassificationDB()
                    .getDocumentCategories(docID);

            if (cats.hasNext())
                writer.write(" | "
                        + index.getCategoryDB().getCategoryName(cats.next())
                        .replace(" ", "_").replace("|", "_"));

            while (cats.hasNext())
                writer.write(" "
                        + index.getCategoryDB().getCategoryName(cats.next())
                        .replace(" ", "_").replace("|", "_"));

            writer.write("\n");

            bar.signal((numAnalyzed * 100) / numItems);
        }

        bar.signal(100);

        writer.close();
    }
}
