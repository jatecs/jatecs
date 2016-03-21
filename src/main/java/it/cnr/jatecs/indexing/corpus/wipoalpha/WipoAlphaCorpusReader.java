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

package it.cnr.jatecs.indexing.corpus.wipoalpha;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.indexing.corpus.DocumentType;
import it.cnr.jatecs.indexing.corpus.SetType;

import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class WipoAlphaCorpusReader extends CorpusReader {

    /**
     * The Wipo-Alpha input directory.
     */
    private String _inputDir = null;
    private Hashtable<String, File> _validPaths = null;
    private File _validDir = null;
    private File _nextFile = null;
    private Vector<File> _nextFiles = null;
    private int _docId;
    private boolean _wantTitle;
    private boolean _wantAbstract;
    private boolean _wantClaims;
    private boolean _wantDescription;
    private boolean _excludeTrainingDocWithoutCat;
    private int _numWordsInDescr;
    /**
     * Extract nly primary categories.
     */
    private boolean _extractOnlyPrimary;

    public WipoAlphaCorpusReader(ICategoryDB cats) {
        super(cats);

        _inputDir = "";
        _validPaths = new Hashtable<String, File>();
        _wantTitle = true;
        _wantAbstract = true;
        _wantClaims = false;
        _wantDescription = true;
        _numWordsInDescr = 200;
        _extractOnlyPrimary = false;
        _excludeTrainingDocWithoutCat = true;
    }

    public void indexTitle(boolean indexTitle) {
        _wantTitle = indexTitle;
    }

    public void indexAbstract(boolean indexAbstract) {
        _wantAbstract = indexAbstract;
    }

    public void indexClaims(boolean indexClaims) {
        _wantClaims = indexClaims;
    }

    public void indexDescription(boolean indexDescription) {
        _wantDescription = indexDescription;
    }

    /**
     * Set the input directory of Wipo-alpha corpus set.
     *
     * @param inputDir The directory containing the original RCV1 corpus set.
     */
    public void setInputDir(String inputDir) {
        _inputDir = inputDir;

        // Go to initial state.
        // begin();
    }

    protected Hashtable<String, File> extractValidPaths(File f) {
        Hashtable<String, File> dirs = new Hashtable<String, File>();

        File[] files = f.listFiles(new ValidDirectory());
        if (files.length != 0) {
            for (int i = 0; i < files.length; i++) {
                Hashtable<String, File> validDirs = extractValidPaths(files[i]);
                Iterator<File> it = validDirs.values().iterator();
                while (it.hasNext()) {
                    File fi = it.next();
                    if (!dirs.containsKey(fi.getAbsolutePath()))
                        dirs.put(fi.getAbsolutePath(), fi);
                }
            }
        } else {
            if (!dirs.containsKey(f.getAbsolutePath()))
                dirs.put(f.getAbsolutePath(), f);
        }

        return dirs;
    }

    @Override
    public void begin() {
        _nextFiles = new Vector<File>();

        File f = new File(_inputDir);
        _validPaths = extractValidPaths(f);
        System.out.println("Ci sono da leggere: " + _validPaths.size());

        // Compute the next valid directory.
        Iterator<File> it = _validPaths.values().iterator();
        _validDir = it.next();
        assert (_validDir != null);

        _validPaths.remove(_validDir.getAbsolutePath());
        // Compute the next valid file.
        File[] nextFiles = _validDir.listFiles();
        for (int i = 0; i < nextFiles.length; i++)
            _nextFiles.add(nextFiles[i]);
        assert (_nextFiles.get(0).isFile());
        _nextFile = _nextFiles.get(0);
        _nextFiles.remove(0);

        _docId = 0;
    }

    @Override
    public void close() {
    }

    @Override
    public CorpusDocument next() {
        File curFile = null;

        CorpusDocument doc = null;
        while (doc == null && _nextFile != null) {

            if (_nextFile == null)
                return null;

            curFile = _nextFile;

            // Compute next valid document file.
            _nextFile = computeNextDocument();

            BufferedReader input;
            try {
                input = new BufferedReader(new FileReader(curFile));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            String xmlDoc = readRawDocument(input);

            try {
                input.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            doc = processRawDocument(xmlDoc, curFile.getAbsolutePath());

        }

        return doc;
    }

    protected CorpusDocument processRawDocument(String xmlDoc, String filename) {

        CorpusDocument doc = null;

        int start = xmlDoc.indexOf("<ipcs");
        assert (start != -1);
        int end = xmlDoc.indexOf(">", start + 1);
        String tag = new String(xmlDoc.substring(start, end));

        // Get main category code.
        int startIdx = tag.indexOf("mc=\"");
        assert (startIdx != -1);
        startIdx += "mc=\"".length();
        int endIdx = tag.indexOf("\"", startIdx);
        String code = new String(tag.substring(startIdx, endIdx));

        // Get main codes.
        String classCode = new String(code.substring(0, 3));
        String subclassCode = new String(code.substring(0, 4));

        Hashtable<String, Short> catsDict = new Hashtable<String, Short>();
        extractCategories(classCode, subclassCode, catsDict, true);

        end = xmlDoc.indexOf("</ipcs>", start + "<ipcs".length());
        tag = new String(xmlDoc.substring(start, end + "</ipcs>".length()));
        boolean done = false;
        startIdx = 0;
        while (!done) {
            // Extract all secondary codes.
            startIdx = tag.indexOf("ic=\"", startIdx);
            if (startIdx == -1) {
                done = true;
                continue;
            }
            startIdx += "ic=\"".length();
            endIdx = tag.indexOf("\"", startIdx);
            code = new String(tag.substring(startIdx, endIdx));

            // Get main codes.
            classCode = new String(code.substring(0, 3));
            subclassCode = new String(code.substring(0, 4));

            extractCategories(classCode, subclassCode, catsDict, false);

            startIdx = endIdx + 1;
        }

        end += "</ipcs>".length();

        StringBuilder contentBuilder = new StringBuilder(xmlDoc.length());

        if (_wantTitle) {
            extractTitle(contentBuilder, xmlDoc, end);
        }

        if (_wantAbstract) {
            extractAbstract(contentBuilder, xmlDoc, end);
        }

        if (_wantDescription) {
            StringBuilder sb = new StringBuilder();
            extractDescription(sb, xmlDoc, end);

            if (_numWordsInDescr != Integer.MAX_VALUE) {
                String c = sb.toString();
                String[] words = c.split("\\p{Space}+");
                int toAdd = _numWordsInDescr;
                for (int i = 0; toAdd > 0 && i < words.length; i++) {
                    if (!words[i].equals("")) {
                        contentBuilder.append(" " + words[i]);
                        toAdd--;
                    }
                }
            } else {
                contentBuilder.append(" " + sb.toString());
            }
        }

        if (_wantClaims) {
            extractClaims(contentBuilder, xmlDoc, end);
        }

        // Construct document to return.
        Vector<String> categories = new Vector<String>(catsDict.keySet());
        String content = new String(contentBuilder.toString().trim());

        if (_excludeTrainingDocWithoutCat) {
            if (getDocumentSetType() == SetType.TRAINING) {
                if (categories.size() == 0) {
                    // This document contain no valid categories then skip
                    // it.
                    return null;
                }
            }
        }

        doc = new CorpusDocument(
                "" + _docId,
                getDocumentSetType() == SetType.TRAINING ? DocumentType.TRAINING
                        : DocumentType.TEST, content, categories);

        _docId++;

        return doc;
    }

    protected void extractCategories(String classCode, String subclassCode,
                                     Hashtable<String, Short> cats, boolean primary) {
        if (_extractOnlyPrimary && !primary)
            return;

        short catID = getCategoryDB().getCategory(classCode);
        if (catID != -1) {
            Short cat = cats.get(classCode);
            if (cat == null)
                cats.put(classCode, catID);
        }

        catID = getCategoryDB().getCategory(subclassCode);
        if (catID != -1) {
            Short cat = cats.get(subclassCode);
            if (cat == null)
                cats.put(subclassCode, catID);
        }

    }

    protected void extractClaims(StringBuilder builder, String xmlDoc, int start) {
        int startIdx = xmlDoc.indexOf("<cls>", start);
        assert (startIdx != -1);
        startIdx += "<cls>".length();
        startIdx = xmlDoc.indexOf("<cl", startIdx);
        startIdx = xmlDoc.indexOf(">", startIdx + 1);
        startIdx++;

        int endIdx = xmlDoc.indexOf("</cl>", startIdx);
        assert (endIdx != -1);

        String claims = new String(xmlDoc.substring(startIdx, endIdx));

        builder.append(" ");
        builder.append(claims);
    }

    protected void extractDescription(StringBuilder builder, String xmlDoc,
                                      int start) {
        int startIdx = xmlDoc.indexOf("<txts>", start);
        assert (startIdx != -1);
        startIdx += "<txts>".length();
        startIdx = xmlDoc.indexOf("<txt", startIdx);
        startIdx = xmlDoc.indexOf(">", startIdx + 1);
        startIdx++;

        int endIdx = xmlDoc.indexOf("</txt>", startIdx);
        if (endIdx == -1) {
            endIdx = xmlDoc.indexOf("</txts>", startIdx);
        }

        String desc = new String(xmlDoc.substring(startIdx, endIdx));

        builder.append(" ");
        builder.append(desc);
    }

    protected void extractTitle(StringBuilder builder, String xmlDoc, int start) {
        int startIdx = xmlDoc.indexOf("<tis>", start);
        assert (startIdx != -1);
        startIdx += "<tis>".length();
        startIdx = xmlDoc.indexOf("<ti", startIdx);
        startIdx = xmlDoc.indexOf(">", startIdx + 1);
        startIdx++;

        int endIdx = xmlDoc.indexOf("</ti>", startIdx);
        assert (endIdx != -1);

        String title = new String(xmlDoc.substring(startIdx, endIdx));
        builder.append(" ");
        builder.append(title);
    }

    protected void extractAbstract(StringBuilder builder, String xmlDoc,
                                   int start) {
        int startIdx = xmlDoc.indexOf("<abs>", start);
        assert (startIdx != -1);
        startIdx += "<abs>".length();
        startIdx = xmlDoc.indexOf("<ab", startIdx);
        startIdx = xmlDoc.indexOf(">", startIdx + 1);
        startIdx++;

        int endIdx = xmlDoc.indexOf("</ab>", startIdx);
        if (endIdx == -1) {
            endIdx = xmlDoc.indexOf("</abs>", startIdx);
            assert (endIdx != -1);
        }

        String abs = new String(xmlDoc.substring(startIdx, endIdx));
        builder.append(" ");
        builder.append(abs);
    }

    protected File computeNextDocument() {
        if (_nextFiles.size() != 0) {
            File toReturn = _nextFiles.get(0);
            _nextFiles.remove(0);

            assert (toReturn != null);

            return toReturn;
        }

        // Compute the next valid directory.
        if (_validPaths.size() == 0)
            return null;

        Iterator<File> it = _validPaths.values().iterator();
        _validDir = it.next();
        assert (_validDir != null);
        _validPaths.remove(_validDir.getAbsolutePath());

        // Compute the next valid file.
        File[] nextFiles = _validDir.listFiles();
        for (int i = 0; i < nextFiles.length; i++)
            _nextFiles.add(nextFiles[i]);
        assert (nextFiles.length != 0);
        assert (_nextFiles.get(0).isFile());
        _nextFile = _nextFiles.get(0);
        _nextFiles.remove(0);

        return _nextFile;
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

            if (line.startsWith("<?xml"))
                continue;
            if (line.startsWith("<!DOCTYPE"))
                continue;

            doc.append(line);

            if (line.startsWith("</record")) {
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                assert (line == null);
                readed = true;
            }
        }

        return doc.toString();
    }

    protected class ValidDirectory implements FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory())
                return true;
            else
                return false;
        }

    }
}
