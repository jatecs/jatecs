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
package it.cnr.jatecs.indexing.corpus.CSV;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.indexing.corpus.DocumentType;
import it.cnr.jatecs.indexing.corpus.SetType;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * @author Andrea Esuli
 */
public class CSVCorpusReader extends CorpusReader {

    private BufferedReader _file;
    private String _fieldSeparator;
    private Vector<String> _fileName;
    private Iterator<String> _fileIterator;
    /**
     * @param catsDB
     * @throws JatecsException
     */
    public CSVCorpusReader(ICategoryDB catsDB) {
        super(catsDB);

        _fieldSeparator = ",";
        _fileName = new Vector<String>();
        _file = null;
    }

    /**
     * @param file input file to get data from
     */
    public void setInputFile(String filename) {
        _fileName.clear();
        _fileName.add(filename);
    }

    /**
     * @param file input file to get data from
     */
    public void addInputFile(String filename) {
        _fileName.add(filename);
    }

    /**
     * @param fieldSeparator fields separator (regex), default is ","
     */
    public void setFieldSeparator(String fieldSeparator) {
        _fieldSeparator = fieldSeparator;
    }

    /**
     * * @see it.cnr.jatecs.indexing.corpus.CorpusReader#begin()
     */
    @Override
    public void begin() {
        if (_file != null) {
            try {
                _file.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        _fileIterator = null;
        nextFile();
    }

    /**
     * * @see it.cnr.jatecs.indexing.corpus.CorpusReader#close()
     */
    @Override

    public void close() {
        try {
            _file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see it.cnr.jatecs.indexing.corpus.CorpusReader#next()
     */
    @Override
    public CorpusDocument next() {
        String line = null;
        try {
            line = _file.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (line == null || line.equals(""))
            if (nextFile()) {
                return next();
            } else
                return null;

        String[] fields = line.split(_fieldSeparator);
        String id = fields[0];
        DocumentType dt;

        if (getDocumentSetType() == SetType.TEST)
            dt = it.cnr.jatecs.indexing.corpus.DocumentType.TEST;
        else if (getDocumentSetType() == SetType.VALIDATION)
            dt = it.cnr.jatecs.indexing.corpus.DocumentType.VALIDATION;
        else
            dt = it.cnr.jatecs.indexing.corpus.DocumentType.TRAINING;

        String content = fields[1];

        Vector<String> categories = new Vector<String>(fields.length - 2);
        for (int i = 2; i < fields.length; ++i)
            categories.add(fields[i]);


        CorpusDocument cd = new CorpusDocument(id, dt, content, categories);

        return cd;
    }

    private boolean nextFile() {
        try {
            if (_fileIterator == null)
                _fileIterator = _fileName.iterator();
            _file = new BufferedReader(new FileReader(_fileIterator.next()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

}
