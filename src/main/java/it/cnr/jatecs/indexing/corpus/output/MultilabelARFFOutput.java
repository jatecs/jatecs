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
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MultilabelARFFOutput {

    public static void Write(String filename, IIndex index) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write("% This multilabel format uses a sparse ARFF representation (see http://weka.wikispaces.com/ARFF)."
                    + Os.newline());
            writer.write("% Features have the 'feat_ prefix." + Os.newline());
            writer.write("% Classes ('class_') are represented as numeric attributes, 1 means the label is assigned, 0 means not assigned."
                    + Os.newline() + Os.newline());

            File file = new File(filename);

            writer.write("@RELATION \"" + file.getName() + "\"" + Os.newline()
                    + Os.newline());

            writeHeader(index, writer);

            writeIndex(index, writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeHeader(IIndex index, BufferedWriter writer)
            throws IOException {


        writer.write("@ATTRIBUTE 'docId'" + " STRING"
                + Os.newline());

        IIntIterator features = index.getFeatureDB().getFeatures();

        while (features.hasNext()) {
            writer.write("@ATTRIBUTE 'feat_" + features.next() + "' NUMERIC"
                    + Os.newline());
        }

        IShortIterator catIt = index.getCategoryDB().getCategories();
        while (catIt.hasNext()) {
            writer.write("@ATTRIBUTE 'class_"
                    + index.getCategoryDB().getCategoryName(catIt.next())
                    + "' NUMERIC" + Os.newline());
        }

        writer.write(Os.newline());
    }

    private static void writeIndex(IIndex index, BufferedWriter writer)
            throws Exception {

        int featureCount = index.getFeatureDB().getFeaturesCount();

        writer.write("@DATA" + Os.newline());
        IIntIterator docIt = index.getDocumentDB().getDocuments();
        while (docIt.hasNext()) {
            int docID = docIt.next();

            StringBuilder b = new StringBuilder();
            b.append("{");

            b.append("0 '" + index.getDocumentDB().getDocumentName(docID)
                    + "',");

            IIntIterator itFeats = index.getContentDB().getDocumentFeatures(
                    docID);
            while (itFeats.hasNext()) {
                int featID = itFeats.next();
                double score = index.getWeightingDB().getDocumentFeatureWeight(
                        docID, featID);
                if (score == 0) {
                    continue;
                }

                b.append(" " + (featID + 1) + " " + score + ",");
            }

            IShortIterator catIt = index.getCategoryDB().getCategories();
            int shift = 1;
            while (catIt.hasNext()) {
                if (index.getClassificationDB().hasDocumentCategory(docID,
                        catIt.next())) {
                    b.append(" " + (featureCount + shift) + " 1,");
                }
                ++shift;
            }
            b.append("}" + Os.newline());
            writer.write(b.toString());

        }

        writer.close();
    }
}
