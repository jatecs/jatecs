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

package it.cnr.jatecs.indexing.corpus.WebisCLS1;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.indexing.corpus.DocumentType;
import it.cnr.jatecs.utils.Os;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class WebisCLS1CorpusReader extends CorpusReader {

    private static String labeltoken = "#label#:";
    /**
     * The name of input document
     */
    private String _file;
    /**
     * The source input directory.
     */
    private String _inputDir;
    private int _index = 0;
    private BufferedReader bufferReader;
    private String _docNamePrefix;
    private DocumentType _docType;

    private boolean _readSentimentAnalysisCategory = true;
    private String _prestablishedCategory = null;

    public WebisCLS1CorpusReader(ICategoryDB categoryDB) {
        super(categoryDB);
        _readSentimentAnalysisCategory = true;
    }

    private static String getProcessedContent(String line) {
        // format:
        // token:count token:count ...
        String[] content = line.substring(0, line.lastIndexOf(labeltoken))
                .split("\\s+");

        StringBuilder st = new StringBuilder();
        for (String wordFreq : content) {
            String[] parts = wordFreq.split(":");
            String word = parts[0];
            int count = Integer.parseInt(parts[1]);
            for (int i = 0; i < count; i++) {
                st.append(word).append(" ");
            }
        }
        return st.toString();
    }

    public void setSentimentAnalysisTask() {
        _readSentimentAnalysisCategory = true;
    }

    public void setCategoryClassificationTask(String setCategory) {
        _readSentimentAnalysisCategory = false;
        _prestablishedCategory = setCategory;
    }

    public void setInputFile(String inputDir, String file) throws IOException {
        _inputDir = inputDir;
        _file = file;
    }

    public void setDocNamePrefix(String prefix) {
        _docNamePrefix = prefix;
    }

    public void setDocumentType(DocumentType docType) {
        _docType = docType;
    }

    @Override
    public void begin() {
        _index = 0;

        String fullpath = _inputDir + Os.pathSeparator() + _file;
        try {
            bufferReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(fullpath), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            bufferReader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public CorpusDocument next() {
        CorpusDocument doc = null;

        String line = null;
        try {
            line = bufferReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (line != null) {
            String name = _docNamePrefix + "_" + _index++;
            String processedContent = getProcessedContent(line);
            List<String> cat = getCategory(line);
            if (cat != null)
                doc = new CorpusDocument(name, _docType, processedContent, cat);
        }

        return doc;
    }

    private List<String> getCategory(String line) {
        ArrayList<String> cats = new ArrayList<String>();
        String cat = null;
        if (_readSentimentAnalysisCategory) {
            int initpos = line.lastIndexOf(labeltoken);
            if (initpos < 0)
                return null;

            cat = line.substring(initpos + labeltoken.length());
        } else {
            cat = _prestablishedCategory;
        }
        // only one category in this dataset
        cats.add(cat);

        return cats;
    }
}
