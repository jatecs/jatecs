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

package it.cnr.jatecs.classification.svmlight;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class SvmLightMulticlassFormatGenerator {

    public static void generateClassificationData(String outFile, IIndex index) throws Exception {
        File dir = new File(outFile).getParentFile();
        if (!dir.exists())
            dir.mkdirs();

        FileOutputStream os = new FileOutputStream(outFile);
        @SuppressWarnings("resource")
        OutputStreamWriter out = new OutputStreamWriter(os);

        IIntIterator it = index.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int docID = it.next();
            StringBuilder b = new StringBuilder();
            int catsCount = index.getClassificationDB().getDocumentCategoriesCount(docID);
            IShortIterator cats = index.getClassificationDB().getDocumentCategories(docID);
            if (catsCount > 1)
                throw new Exception("Each document must have at most one label, document " + docID + " has " + catsCount);
            else if (catsCount == 1) {
                short cat = cats.next();
                b.append(cat);
            } else
                b.append("-1");

            IIntIterator itFeats = index.getContentDB().getDocumentFeatures(docID);
            while (itFeats.hasNext()) {
                int featID = itFeats.next();
                double score = index.getWeightingDB().getDocumentFeatureWeight(docID, featID);
                if (score == 0)
                    continue;

                b.append(" " + (featID + 1) + ":" + score);
            }

            b.append(" #" + Os.newline());

            out.write(b.toString());
        }

        out.close();
    }
}
