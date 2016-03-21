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

package it.cnr.jatecs.indexing.corpus.OHSUMED;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.indexing.corpus.DocumentType;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.utils.Os;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OHSUMEDCorpusReader extends CorpusReader {

    public static String[] trainFiles = {"ohsumed.87", "ohsumed.88", "ohsumed.89", "ohsumed.90"};
    public static String[] testFiles = {"ohsumed.91"};

    FileReader _fr;
    BufferedReader _reader;
    String _dataDir;
    int _currentFile;

    String name;

    boolean _excludeDocWithoutCat;

    public OHSUMEDCorpusReader(ICategoryDB catsDB, String dataDir) {
        super(catsDB);
        _dataDir = dataDir;
        _excludeDocWithoutCat = false;
    }

    public void excludeDocumentsWithoutValidCategories(boolean exclude) {
        _excludeDocWithoutCat = exclude;
    }

    @Override
    public void begin() {
        _currentFile = -1;
        try {
            nextFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public CorpusDocument next() {
        if (_reader == null)
            return null;

        String content = "";

        boolean gotAbstract = false;

        DocumentType docType;
        if (getDocumentSetType() == SetType.TRAINING)
            docType = DocumentType.TRAINING;
        else
            docType = DocumentType.TEST;

        List<String> categories = new ArrayList<String>();

        String line = null;
        while (true) {
            try {
                line = _reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (line == null) {
                try {
                    if (!nextFile())
                        break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    line = _reader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (line.startsWith(".I")) {
                if (name != null && gotAbstract) {
                    if (categories.size() == 0 && _excludeDocWithoutCat) {
                        gotAbstract = false;
                        content = "";
                        name = line.substring(line.indexOf(" ") + 1);
                    } else {
                        CorpusDocument cd = new CorpusDocument(name, docType, content, categories);
                        name = line.substring(line.indexOf(" ") + 1);
                        gotAbstract = false;
                        content = "";
                        categories.clear();
                        return cd;
                    }
                }
                content = "";
                categories.clear();
                name = line.substring(line.indexOf(" ") + 1);
            } else if (line.startsWith(".T")) {
                try {
                    content += _reader.readLine() + Os.newline();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (line.startsWith(".W")) {
                try {
                    content += _reader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                gotAbstract = true;
            } else if (line.startsWith(".M")) {
                String[] cats;
                try {
                    cats = _reader.readLine().split("/|;");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                for (int i = 0; i < cats.length; ++i) {
                    String cat = cats[i];
                    if (cat.length() > 0) {
                        if (cat.startsWith(" "))
                            cat = cat.substring(1);
                        if (!_excludeDocWithoutCat || getCategoryDB().getCategory(cat) >= 0)
                            categories.add(cat);
                    }
                }
            }
        }

        if (name != null && (categories.size() > 0 || !_excludeDocWithoutCat))
            return new CorpusDocument(name, docType, content, categories);

        return null;
    }

    private boolean nextFile() throws IOException {
        if (_reader != null) {
            _reader.close();
            _fr.close();
            _reader = null;
            _fr = null;
        }
        name = null;

        if (getDocumentSetType() == SetType.TRAINING) {
            if (_currentFile == trainFiles.length - 1)
                return false;
            else {
                ++_currentFile;
                _fr = new FileReader(_dataDir + Os.pathSeparator() + trainFiles[_currentFile]);
                _reader = new BufferedReader(_fr);
                return true;
            }
        } else if (getDocumentSetType() == SetType.TEST) {
            if (_currentFile == testFiles.length - 1)
                return false;
            else {
                ++_currentFile;
                _fr = new FileReader(_dataDir + Os.pathSeparator() + testFiles[_currentFile]);
                _reader = new BufferedReader(_fr);
                return true;
            }
        }
        return false;
    }

}
