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

package it.cnr.jatecs.indexing.corpus.EPO;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.indexing.corpus.DocumentType;
import it.cnr.jatecs.indexing.corpus.SetType;

import java.io.*;
import java.util.Vector;

public class EPOCorpusReader extends CorpusReader {

    protected String _inputDir;
    private boolean _excludeTrainingDocWithoutCat;
    private File _nextFile;
    private int _curPos;
    private String _content;
    private File[] _files;
    private int _whichFile;
    public EPOCorpusReader(ICategoryDB catsDB) {
        super(catsDB);

        _inputDir = "Noname";
        _excludeTrainingDocWithoutCat = false;
        _nextFile = null;
        _curPos = -1;
    }

    /**
     * Set the input directory of Wipo-alpha corpus set.
     *
     * @param inputDir The directory containing the original RCV1 corpus set.
     */
    public void setInputDir(String inputDir) {
        // Must init structures.
        _inputDir = inputDir;
        _curPos = 0;
    }

    protected File computeNextDocument() {
        if (_whichFile < _files.length)
            return _files[_whichFile++];
        else
            return null;
    }

    @Override
    public void begin() {
        File dir = new File(_inputDir);
        _files = dir.listFiles(new ValidFile());
        _whichFile = 0;

        _nextFile = computeNextDocument();
        if (_nextFile != null) {

            BufferedReader input;
            try {
                input = new BufferedReader(new FileReader(_nextFile));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            String xmlDoc = readRawDocument(input);

            try {
                input.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            _content = xmlDoc;
            _curPos = 0;
        } else {
            _content = null;
            _curPos = -1;
        }
    }

    @Override
    public void close() {
    }

    @Override
    public CorpusDocument next() {

        CorpusDocument doc = null;
        while (doc == null && _nextFile != null) {
            // Compute next valid document file.
            if (_curPos == -1) {
                _nextFile = computeNextDocument();
                if (_nextFile == null)
                    continue;

                BufferedReader input;
                try {
                    input = new BufferedReader(new FileReader(_nextFile));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }

                String xmlDoc = readRawDocument(input);

                try {
                    input.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                _content = xmlDoc;
                _curPos = 0;
            }

            // Extract current document from file.
            _curPos = _content.indexOf("<document", _curPos);
            if (_curPos == -1)
                continue;

            int pos = _content.indexOf("</document>", _curPos);
            String document = _content.substring(_curPos, pos + "</document>".length() + 1);

            doc = processRawDocument(document);
            _curPos = pos + "</document>".length() + 1;

        }

        return doc;
    }

    protected CorpusDocument processRawDocument(String doc) {
        int pos = 0;
        int pos2 = 0;
        String pattern = "doc-id=\"";
        String pattern2 = "<![CDATA[";
        String pattern3 = "]]>";
        String pattern4 = "cat-id=\"";

        // First obtain original document name.
        pos = doc.indexOf(pattern, pos);
        assert (pos != -1);
        pos += pattern.length();
        pos2 = doc.indexOf("\"", pos);
        assert (pos2 != -1);
        String docName = new String(doc.substring(pos, pos2));

        // Extract document content.
        pos = pos2 + 1;
        pos = doc.indexOf(pattern2, pos);
        assert (pos != -1);
        pos += pattern2.length();
        pos2 = doc.indexOf(pattern3, pos);
        assert (pos2 != -1);
        String content = new String(doc.substring(pos, pos2));

        // Extract categories.
        Vector<String> categories = new Vector<String>();
        pos = pos2 + pattern3.length();
        pos = doc.indexOf(pattern4, pos);
        while (pos != -1) {
            pos += pattern4.length();
            pos2 = doc.indexOf("\"", pos);
            assert (pos2 != -1);
            String cat = new String(doc.substring(pos, pos2));

            if (getCategoryDB().getCategory(cat) != -1)
                categories.add(cat);

            pos = pos2 + 1;
            pos = doc.indexOf(pattern4, pos);
        }

        if (_excludeTrainingDocWithoutCat) {
            if (getDocumentSetType() == SetType.TRAINING) {
                if (categories.size() == 0) {
                    // This document contain no valid categories then skip it.
                    return null;
                }
            }
        }


        CorpusDocument document = new CorpusDocument(docName, getDocumentSetType() == SetType.TRAINING ? DocumentType.TRAINING : DocumentType.TEST,
                content, categories);

        return document;
    }

    protected String readRawDocument(BufferedReader reader) {

        StringBuilder doc = new StringBuilder();

        boolean readed = false;
        while (!readed) {
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (line == null) {
                readed = true;
                continue;
            }


            doc.append(line);

        }

        return doc.toString();
    }

    protected class ValidFile implements FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory())
                return false;

            String filename = f.getName();
            if (filename.endsWith(".xml"))
                return true;
            else
                return false;
        }

    }

}
