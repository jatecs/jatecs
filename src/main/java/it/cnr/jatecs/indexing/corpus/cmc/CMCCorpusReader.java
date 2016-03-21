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

package it.cnr.jatecs.indexing.corpus.cmc;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexing.corpus.*;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Corpus reader for the Computational Medicine Center 2007 challenge dataset
 *
 * @author tiziano fagni
 */
public class CMCCorpusReader extends CorpusReader {

    private String _filename;

    private BufferedReader _reader;

    private CategoryType _cType;

    public CMCCorpusReader(ICategoryDB catsDB, String filename) {
        super(catsDB);
        _filename = filename;
        _cType = CategoryType.MAJORITY;
    }

    public void setCategoryType(CategoryType t) {
        _cType = t;
    }

    @Override
    public void begin() {
        try {
            _reader = new BufferedReader(new FileReader(_filename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Skip the first 3 lines
        String line;
        try {
            line = _reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assert (line != null);
        try {
            line = _reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            _reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CorpusDocument next() {
        CorpusDocument doc = null;
        while (doc == null) {
            String nextRawDoc = extractRawDocument();
            if (nextRawDoc == null)
                return null;

            doc = processXmlDocument(nextRawDoc);
            if (doc == null) {
                System.out.println("doc skipped:\n" + nextRawDoc);
            }
        }

        return doc;
    }

    protected String extractRawDocument() {
        boolean done = false;
        String line = "";
        while (!done) {
            try {
                line = _reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
        builder.append(line);
        done = false;
        while (!done) {
            try {
                line = _reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assert (line != null);
            builder.append(line);
            if (line.indexOf("</doc>") != -1)
                done = true;
        }

        return builder.toString();
    }

    protected Pair<String, Integer> extractAttribute(String text,
                                                     String attrName, int index) {
        String pattern = attrName + "=\"";
        int start = text.indexOf(pattern, index);
        assert (start != -1);
        start += pattern.length();
        int end = text.indexOf("\"", start);
        assert (end != -1);
        String origin = new String(text.substring(start, end));
        end++;

        Pair<String, Integer> p = new Pair<String, Integer>(origin, end);

        return p;
    }

    protected CorpusDocument processXmlDocument(String xmlDoc) {
        CMCDocument doc = null;

        // Extract doc ID.
        String pattern = "<doc id=\"";
        int startIdx = xmlDoc.indexOf(pattern);
        assert (startIdx != -1);
        startIdx += pattern.length();
        int endIdx = xmlDoc.indexOf("\"", startIdx);
        assert (endIdx != -1);
        String docID = new String(xmlDoc.substring(startIdx, endIdx));

        DocumentType dt = DocumentType.TRAINING;
        if (this.getDocumentSetType() == SetType.TEST)
            dt = DocumentType.TEST;
        doc = new CMCDocument(docID, dt);

        pattern = "<codes>";
        startIdx = xmlDoc.indexOf(pattern, endIdx);
        assert (startIdx != -1);
        pattern = "</codes>";
        endIdx = xmlDoc.indexOf(pattern, startIdx);
        assert (endIdx != -1);

        String codes = xmlDoc.substring(startIdx, endIdx + pattern.length());
        pattern = "<code";
        int start = 0;
        int end = 0;
        int offset = 0;
        start = codes.indexOf(pattern, offset);
        boolean primaryA, primaryB, primaryC;
        primaryA = primaryB = primaryC = true;
        while (start != -1) {
            if (dt == DocumentType.TEST)
                break;

            // Extract origin attribute.
            Pair<String, Integer> origin = extractAttribute(codes, "origin",
                    start);

            // Extract type attribute.
            start = origin.getSecond();
            Pair<String, Integer> type = extractAttribute(codes, "type", start);

            // Extract code.
            pattern = ">";
            start = type.getSecond();
            start = codes.indexOf(pattern, start);
            assert (start != -1);
            start += pattern.length();
            end = codes.indexOf("<", start);
            assert (end != -1);
            String code = codes.substring(start, end);

            short catID = getCategoryDB().getCategory(code);
            if (catID == -1)
                JatecsLogger.status().println(
                        "Category not valid. Code = " + code);
            else {
                if (origin.getFirst().equals("CMC_MAJORITY")) {
                    CorpusCategory cc = new CorpusCategory();
                    cc.name = code;
                    cc.primary = true;
                    doc.categories().add(cc);
                }
                if (origin.getFirst().equals("COMPANY1")) {
                    CorpusCategory cc = new CorpusCategory();
                    cc.name = code;
                    cc.primary = primaryA;
                    doc.getCodesA().add(cc);
                    primaryA = false;
                }
                if (origin.getFirst().equals("COMPANY2")) {
                    CorpusCategory cc = new CorpusCategory();
                    cc.name = code;
                    cc.primary = primaryB;
                    doc.getCodesB().add(cc);
                    primaryB = false;
                }
                if (origin.getFirst().equals("COMPANY3")) {
                    CorpusCategory cc = new CorpusCategory();
                    cc.name = code;
                    cc.primary = primaryC;
                    doc.getCodesC().add(cc);
                    primaryC = false;
                }
            }

            // Move to next code.
            pattern = "<code";
            start = end;
            start = codes.indexOf(pattern, start);
        }

        if (dt == DocumentType.TRAINING) {

            // Check consistency of the document.
            /*
			 * if (!isDocumentConsistent(doc)) { _inconsistents++;
			 * System.out.println("Document "+doc.name()+" is inconsistent!"); }
			 */

            if (_cType == CategoryType.CODER_A) {
                doc.categories().clear();
                doc.categories().addAll(doc.getCodesA());
            }
            if (_cType == CategoryType.CODER_B) {
                doc.categories().clear();
                doc.categories().addAll(doc.getCodesB());
            }
            if (_cType == CategoryType.CODER_C) {
                doc.categories().clear();
                doc.categories().addAll(doc.getCodesC());
            }

            if (doc.categories().size() == 0) {
                System.out.println("Skip document " + doc.name());
                return null;
            }
        }

        // Extract text "impression".
        startIdx = endIdx;
        Pair<String, Integer> origin = extractAttribute(xmlDoc, "origin",
                startIdx);

        startIdx = origin.getSecond();
        Pair<String, Integer> type = extractAttribute(xmlDoc, "type", startIdx);

        pattern = ">";
        start = type.getSecond();
        start = xmlDoc.indexOf(pattern, start);
        assert (start != -1);
        start += pattern.length();
        end = xmlDoc.indexOf("</text", start);
        assert (end != -1);
        String impression = xmlDoc.substring(start, end);

        // Extract text "clinical_history".
        startIdx = end;
        Pair<String, Integer> origin2 = extractAttribute(xmlDoc, "origin",
                startIdx);

        startIdx = origin2.getSecond();
        Pair<String, Integer> type2 = extractAttribute(xmlDoc, "type", startIdx);

        pattern = ">";
        start = type2.getSecond();
        start = xmlDoc.indexOf(pattern, start);
        assert (start != -1);
        start += pattern.length();
        end = xmlDoc.indexOf("</text", start);
        assert (end != -1);
        String clinicalHistory = xmlDoc.substring(start, end);

        doc.setImpressionContent(impression);
        doc.setClinicalHistoryContent(clinicalHistory);

        return doc;
    }

	/*
	 * protected boolean isDocumentConsistent(CMCDocument doc) { List<CorpusCategory>
	 * cats = doc.categories(); for (int i = 0; i < cats.size(); i++) { String
	 * cat = cats.get(i).name; int numCoders = 0; if
	 * (doc.getCodesA().contains(cat)) numCoders++; if
	 * (doc.getCodesB().contains(cat)) numCoders++; if
	 * (doc.getCodesC().contains(cat)) numCoders++;
	 * 
	 * if (numCoders < 2) return false; }
	 * 
	 * Vector< List<String> > cases = new Vector< List<String> >();
	 * cases.add(doc.getCodesA()); cases.add(doc.getCodesB());
	 * cases.add(doc.getCodesC());
	 * 
	 * for (int i = 0; i < cases.size(); i++) { List<String> coder =
	 * cases.get(i); List<String> c1 = cases.get((i+1)%3); List<String> c2 =
	 * cases.get((i+2)%3);
	 * 
	 * for (int k = 0; k < coder.size(); k++) { String cat = coder.get(k); int
	 * numCoders = 0; if (c1.contains(cat)) numCoders++; if (c2.contains(cat))
	 * numCoders++;
	 * 
	 * if ((numCoders > 0) && (!doc.categories().contains(cat))) return false; } }
	 * 
	 * return true; }
	 */

}
