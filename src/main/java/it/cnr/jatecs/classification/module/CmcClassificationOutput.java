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

package it.cnr.jatecs.classification.module;

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.*;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class CmcClassificationOutput extends JatecsModule {

    protected ClassificationScoreDB _classification;
    protected String _inputFile;
    protected String _outputFile;
    protected String _email;
    private TShortArrayList _validCategories;

    public CmcClassificationOutput(IIndex testIndex,
                                   ICategoryDB validCategories, ClassificationScoreDB classification,
                                   String inputFile, String outputFile) {
        super(testIndex, CmcClassificationOutput.class.getName());
        _classification = classification;
        _inputFile = inputFile;
        _outputFile = outputFile;
        _email = "tiziano.fagni@isti.cnr.it";

        _validCategories = new TShortArrayList();
        IShortIterator catIt = validCategories.getCategories();
        while (catIt.hasNext()) {
            short cat = catIt.next();
            String catName = validCategories.getCategoryName(cat);
            short indexCat = index().getCategoryDB().getCategory(catName);
            if (indexCat >= 0)
                _validCategories.add(indexCat);
        }
        _validCategories.sort();
    }

    @Override
    protected void processModule() {
        try {
            File f2 = new File(_outputFile);
            String dir = f2.getParent();
            File f3 = new File(dir);
            if (!f3.exists())
                f3.mkdirs();

            BufferedReader reader = new BufferedReader(new FileReader(
                    _inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    _outputFile));

            // Skip the first 2 lines
            String line = reader.readLine();
            writer.write(line + Os.newline());
            assert (line != null);
            line = reader.readLine();
            writer.write(line + Os.newline());

            String nextDoc = extractRawDocument(reader);
            while (nextDoc != null) {

                String doc = documentWithCodes(nextDoc);
                writer.write(doc + Os.newline());

                // Read next document.
                nextDoc = extractRawDocument(reader);
            }

            writer.write("</docs>" + Os.newline());

            reader.close();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String documentWithCodes(String doc) {
        String newDoc = new String(doc);

        // Extract doc name.
        String pattern = "<doc id=\"";
        int startIdx = doc.indexOf(pattern);
        assert (startIdx != -1);
        startIdx += pattern.length();
        int endIdx = doc.indexOf("\"", startIdx);
        assert (endIdx != -1);
        String docName = new String(doc.substring(startIdx, endIdx));

        pattern = "<codes>";
        startIdx = doc.indexOf(pattern, endIdx);
        assert (startIdx != -1);
        pattern = "</codes>";
        endIdx = doc.indexOf(pattern, startIdx);
        assert (endIdx != -1);

        String codes = "";

        int docID = index().getDocumentDB().getDocument(docName);
        assert (docID != -1);

        Set<Entry<Short, ClassifierRangeWithScore>> entries = _classification
                .getDocumentScoresAsSet(docID);
        Iterator<Entry<Short, ClassifierRangeWithScore>> it = entries
                .iterator();
        while (it.hasNext()) {
            Entry<Short, ClassifierRangeWithScore> en = it.next();

            if (!_validCategories.contains(en.getKey()))
                continue;

            if (en.getValue().score >= en.getValue().border) {
                String c = "      <code origin=\"" + _email
                        + "\" type=\"ICD-9-CM\">";
                String catName = index().getCategoryDB().getCategoryName(
                        en.getKey());
                c += catName;
                c += "</code>";
                codes += c + Os.newline();
            }
        }

        startIdx += "<codes>".length();
        newDoc = newDoc.substring(0, startIdx) + Os.newline() + codes + "    "
                + newDoc.substring(endIdx);

        return newDoc;
    }

    protected String extractRawDocument(BufferedReader reader)
            throws IOException {
        boolean done = false;
        String line = "";
        while (!done) {
            line = reader.readLine();
            assert (line != null);
            int idx = line.indexOf("</docs>");
            if (idx != -1)
                // End of documents.
                return null;

            idx = line.indexOf("<doc ");
            if (idx == -1)
                continue;
            else
                done = true;

        }

        StringBuilder builder = new StringBuilder();
        builder.append(line + Os.newline());
        done = false;
        while (!done) {
            line = reader.readLine();
            assert (line != null);
            if (line.indexOf("</doc>") != -1)
                builder.append(line);
            else
                builder.append(line + Os.newline());

            if (line.indexOf("</doc>") != -1)
                done = true;
        }

        return builder.toString();
    }

}
