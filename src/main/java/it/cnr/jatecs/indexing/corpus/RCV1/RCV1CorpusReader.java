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

/**
 *
 */
package it.cnr.jatecs.indexing.corpus.RCV1;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

/**
 * @author fagni
 */
public class RCV1CorpusReader extends CorpusReader {

    /**
     * The RCV1 input directory.
     */
    private String _inputDir;

    /**
     * The files contained in the input directory.
     */
    private String[] _files;

    /**
     * The reader used to iterate throught the input files.
     */
    private BufferedReader _reader;

    /**
     * The ID for the file that is currently readed.
     */
    private int _idFile;

    /**
     * The categories type
     */
    private CategoryType _catType;

    /**
     * The filename which define the valid categories.
     */
    private String _validCategoriesFilename;

    private int _docSkipped;

    /*
     * Indicate if we must exclude document with no valid categories.
     */
    private boolean _excludeDocWithoutCat;

    /**
     * The XML parser used.
     */
    // private RCV1XmlParser _parser;
    private RCV1FastParser _parser;

    public RCV1CorpusReader(ICategoryDB catsDB) {
        super(catsDB);

        setName("RCV1");

        String description = "The class read the RCV1 corpus collection.";

        setDescription(description);

        // Reset the input reuters directory.
        _inputDir = "";

        _catType = CategoryType.TOPICS;

        // _parser = new RCV1XmlParser(this);
        _parser = new RCV1FastParser(this);

        _docSkipped = 0;

        _excludeDocWithoutCat = false;
    }

    public void excludeDocumentsWithoutValidCategories(boolean exclude) {
        _excludeDocWithoutCat = exclude;
    }

    /**
     * Get the filename of the file defining the categories set used.
     *
     * @return The filename.
     */
    public String validCategoriesFilename() {
        return _validCategoriesFilename;
    }

    /**
     * @see it.cnr.jatecs.indexing.corpus.CorpusReader#begin()
     */
    @Override
    public void begin() {

        _docSkipped = 0;

        File f = new File(_inputDir);
        _files = f.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                if (name.startsWith("199"))
                    return true;
                else
                    return false;
            }

        });

        assert (_files != null);
        assert (_files.length > 0);

        Arrays.sort(_files, new Comparator<String>() {

            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }

        });

        String filename = _inputDir + Os.pathSeparator() + _files[0];

        try {
            _reader = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        _idFile = 0;

    }

    @Override
    public void close() {
        try {
            _reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the input directory of RCV1 corpus set. The format of files in the
     * directory must be the following: - each file in the directory represents
     * a day and contains all the documents of that specific day coming from
     * original corpus set. Each file is created by catting each XML document
     * without any preprocessing. The number of files in the directory must be
     * 365.
     *
     * @param inputDir The directory containing the original Reuters21578 corpus set.
     */
    public void setInputDir(String inputDir) {
        _inputDir = inputDir;

        // Go to initial state.
        begin();
    }

    /**
     * @see it.cnr.jatecs.indexing.corpus.CorpusReader#next()
     */
    @Override
    public CorpusDocument next() {

        CorpusDocument doc = null;

        while (doc == null) {

            String xmlDoc = readRawDocument();
            if (xmlDoc == null) {
                // Reached the end of file, check if this file was the last in
                // the corpus dataset.
                if (this._idFile == (_files.length - 1)) {
                    // Ok. We have seen all documents...
                    try {
                        _reader.close();
                    } catch (Exception e) {
                    }

                    JatecsLogger.execution().warning(
                            "Skipped " + _docSkipped + " documents!");

                    return null;
                }

                // Change the file and read from the beginning.
                _idFile++;
                String filename = _inputDir + Os.pathSeparator()
                        + _files[_idFile];

                try {
                    _reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    _reader = new BufferedReader(new FileReader(filename));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                xmlDoc = readRawDocument();

            }

            // Process the raw document and possibly obtain a valid
            // CorpusDocument
            // object.
            doc = processXmlDocument(xmlDoc);
            if ((doc == null) && (_parser.readedAll())) {
                // Ok. We have seen all documents...
                try {
                    _reader.close();
                } catch (Exception e) {
                }

                JatecsLogger.execution().warning(
                        "Skipped " + _docSkipped + " documents!");

                return null;
            }

            if (doc != null) {
                if (_excludeDocWithoutCat) {
                    if (doc.categories().size() == 0) {
                        // Skip document.
                        doc = null;
                        continue;
                    }
                }
            }
        }

        return doc;
    }

    protected String readRawDocument() {
        boolean docRead = false;

        StringBuilder docBuilder = new StringBuilder(4096);
        String doc = null;

        while (!docRead) {
            String line;
            try {
                line = _reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (line == null) {
                doc = null;
                return doc;
            }

            if (line.equals("")) {
                doc = null;
                return doc;
            }

            if (line.startsWith("</newsitem>")) {
                docRead = true;
            }

            docBuilder.append(line + Os.newline());
        }

        doc = docBuilder.toString();
        return doc;
    }

    protected CorpusDocument processXmlDocument(String xmlDoc) {
        CorpusDocument doc = null;

        _parser.setXMLDocument(xmlDoc);

        doc = _parser.parse();

        if (doc == null)
            _docSkipped++;

        return doc;
    }

    public CategoryType categoriesType() {
        return _catType;
    }

    public void setCategoriesType(CategoryType catType) {
        _catType = catType;
    }
}
