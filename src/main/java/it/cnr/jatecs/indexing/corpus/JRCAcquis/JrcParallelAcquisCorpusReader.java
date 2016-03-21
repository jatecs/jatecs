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

package it.cnr.jatecs.indexing.corpus.JRCAcquis;

import gnu.trove.TIntArrayList;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.indexing.corpus.DocumentType;
import it.cnr.jatecs.utils.Os;

import java.io.*;
import java.util.ArrayList;

public class JrcParallelAcquisCorpusReader extends CorpusReader {

    private String _alignmentDir;

    /**
     * Is the language to be analyzed the first language in alignment record
     * pair?
     */
    private boolean _firstLanguage;

    /**
     * The name of input documents.
     */
    private String[] _files;

    /**
     * THe source input directory.
     */
    private String _inputDir;

    /**
     * The current filename;
     */
    private int _index;

    /**
     * Do we want the title included on the text content of the document?
     */
    private boolean _wantTitle;

    private TroveCategoryDBBuilder _builder;

    public JrcParallelAcquisCorpusReader(TroveCategoryDBBuilder builder) {
        super(builder.getCategoryDB());
        _builder = builder;
        _firstLanguage = true;
        _wantTitle = true;
    }

    public void setWantTile(boolean wantTitle) {
        _wantTitle = wantTitle;
    }

    public void setInputFile(String inputDir, String alignmentDir,
                             boolean firstLanguage) throws IOException {
        _alignmentDir = alignmentDir;
        _firstLanguage = firstLanguage;
        _inputDir = inputDir;

        File f = new File(_inputDir);
        _files = f.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                if (name.endsWith(".xml"))
                    return true;
                else
                    return false;
            }

        });
    }

    @Override
    public void begin() {
        _index = 0;
    }

    @Override
    public void close() {
    }

    protected String readFile(String fname) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(fname), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line + "\n");
            line = br.readLine();
        }
        br.close();
        return sb.toString();

    }

    @Override
    public CorpusDocument next() {
        if (_index >= _files.length)
            return null;

        CorpusDocument doc = null;
        boolean done = false;
        while (!done && _index < _files.length) {
            String name = new String(_files[_index]);
            name = name.replace('/', '_');
            name = name.replace('\\', '_');
            name = name.replace('(', '_');
            name = name.replace(")", "");
            name = name.replaceFirst("jrc", "");
            name = name.replaceFirst("-[a-z]{2}.xml", ".xml");
            name = _alignmentDir + Os.pathSeparator() + name;
            String alignmentDoc;
            try {
                alignmentDoc = readFile(name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (alignmentDoc == null) {
                System.out.println("Skip document...");
                _index++;
                continue;
            }

            TIntArrayList validParagraphs = getValidParagraphs(alignmentDoc,
                    _firstLanguage);

            String fname = _inputDir + Os.pathSeparator() + _files[_index];
            String xmlDoc;
            try {
                xmlDoc = readFile(fname);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            _index++;
            if (validParagraphs.size() == 0)
                continue;

            doc = generateDocument(xmlDoc, validParagraphs);
            if (doc != null || _index >= _files.length) {
                done = true;
                continue;
            }
        }

        return doc;
    }

    protected TIntArrayList getValidParagraphs(String doc, boolean firstlanguage) {
        TIntArrayList valids = new TIntArrayList();
        TIntArrayList others = new TIntArrayList();
        String pattern = "xtargets=\"";
        int from = 0;
        boolean done = false;
        while (!done) {
            int startIdx = doc.indexOf(pattern, from);
            if (startIdx == -1) {
                done = true;
                continue;
            }
            if (from == 0) {
                from = startIdx + 1;
                continue;
            }

            startIdx += pattern.length();
            int endIdx = doc.indexOf("\"", startIdx);
            assert (endIdx != -1);

            String val = doc.substring(startIdx, endIdx);
            String[] values = null;
            if (val.startsWith(";")) {
                values = val.split("[;]");
                values = new String[]{" ", values[0]};
            } else if (val.endsWith(";")) {
                values = val.split("[;]");
                values = new String[]{values[0], " "};
            } else
                values = val.split("[;]");

            String[] intLangVals = null;
            String[] intForeignVals = null;
            if (!(values.length == 2))
                System.out.println("Val: " + val);

            if (firstlanguage) {
                intLangVals = values[0].split("[ ]");
                intForeignVals = values[1].split("[ ]");
            } else {
                intLangVals = values[1].split("[ ]");
                intForeignVals = values[0].split("[ ]");
            }

            if (intForeignVals.length != 0 && intLangVals.length != 0) {

                for (int i = 0; i < intLangVals.length; i++) {
                    if (intLangVals[i].equals(""))
                        continue;
                    valids.add(Integer.parseInt(intLangVals[i]));
                }

                for (int i = 0; i < intForeignVals.length; i++) {
                    if (intForeignVals[i].equals(""))
                        continue;
                    others.add(Integer.parseInt(intForeignVals[i]));
                }
            }

            from = endIdx;
        }

        if (others.size() == 0)
            return new TIntArrayList();

        return valids;
    }

    protected CorpusDocument generateDocument(String xmlDoc,
                                              TIntArrayList validParagraphs) {
        StringBuilder sb = new StringBuilder();
        if (_wantTitle) {
            String pattern = "<head n=\"1\">";
            String pattern2 = "</head>";
            int startIdx = xmlDoc.indexOf(pattern);
            if (startIdx != -1) {
                startIdx += pattern.length();
                int endIdx = xmlDoc.indexOf(pattern2, startIdx);
                assert (endIdx != -1);
                String title = xmlDoc.substring(startIdx, endIdx);
                sb.append(title.trim() + "\n");
            }
        }

        for (int i = 0; i < validParagraphs.size(); i++) {
            int par = validParagraphs.get(i);
            String pattern = "<p n=\"" + par + "\">";
            String pattern2 = "</p>";
            int startIdx = xmlDoc.indexOf(pattern);
            startIdx += pattern.length();
            assert (startIdx != -1);
            int endIdx = xmlDoc.indexOf(pattern2, startIdx);
            String content = xmlDoc.substring(startIdx, endIdx);
            sb.append(content.trim() + "\n");
        }

        // Get the categories for the current document.
        ArrayList<String> cats = new ArrayList<String>();
        String pattern = "<classCode scheme=\"eurovoc\">";
        String pattern2 = "</classCode>";
        int from = 0;
        boolean done = false;
        while (!done) {
            int startIdx = xmlDoc.indexOf(pattern, from);
            if (startIdx == -1) {
                done = true;
                continue;
            }

            startIdx += pattern.length();
            int endIdx = xmlDoc.indexOf(pattern2, startIdx);
            assert (endIdx != -1);
            String code = xmlDoc.substring(startIdx, endIdx);
            int catID = _builder.getCategoryDB().getCategory(code);
            if (catID == -1) {
                _builder.addCategory(code);
            }
            cats.add(code);
            from = endIdx;
        }

        // Get document ID.
        pattern = "<TEI.2 id=\"";
        int startIdx = xmlDoc.indexOf(pattern, 0);
        assert (startIdx != -1);
        startIdx += pattern.length();
        int endIdx = xmlDoc.indexOf("\"", startIdx);
        String docID = xmlDoc.substring(startIdx, endIdx);

        CorpusDocument doc = new CorpusDocument(docID, DocumentType.TRAINING,
                sb.toString(), cats);
        return doc;
    }

}
