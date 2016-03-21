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

/*
 * Created on 11-gen-2005
 *
 */
package it.cnr.jatecs.indexing.corpus.Reuters21578;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.indexing.corpus.DocumentType;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Construct a new Reuters21578 corpus reader. The class can read both the
 * training documents as well as the test documents. The types of split
 * supported are the three main ("The modified Lewis split", "The modified APTE
 * split" and "The modified Hayes split") defined in the file README.txt
 * contained in the official original compressed Reuters distribution file.
 *
 * @author Tiziano Fagni, Andrea Esuli
 */
public class Reuters21578CorpusReader extends CorpusReader {

	/*
     * private class DT { DocumentType docType; }
	 */

    /**
     * The split type used by this object.
     */
    private Reuters21578SplitType _splitType;

    /**
     * The input directory where the original corpus sources are contained.
     */
    private String _inputDir;

    /**
     * The reader used to iterate throught the input files.
     */
    private BufferedReader _reader;

    /**
     * The ID for the file that is currently readed.
     */
    private int _idFile;

    /**
     * Indicate if we must exclude training document with no valid categories.
     */
    private boolean _excludeDocWithoutCat;

    /**
     * Construct a new Reuters21578 reader with the default split set to "Lewis
     * split" and the document types to training documents.
     *
     * @param catsDB The categories DB used to store and identify the categories
     *               specified in the documents.
     */
    public Reuters21578CorpusReader(ICategoryDB catsDB) {
        super(catsDB);

        setName("Reuters21578");

        String description = "The class can read both the"
                + " training documents as well as the test documents.\nThe types of split supported"
                + " are the three main ('The modified Lewis split', 'The modified APTE split' and"
                + " 'The modified Hayes split') defined in the file README.txt contained in the official"
                + " original compressed Reuters distribution file.";

        setDescription(description);

        // Reset the input reuters directory.
        _inputDir = "";

        _splitType = Reuters21578SplitType.APTE;
        this.setDocumentSetType(SetType.TRAINING);

        _excludeDocWithoutCat = false;
    }

    /**
     * Set the split type to use when reading the documents from original
     * Reuters21578 corpus set.
     *
     * @param splitType The type of split to use.
     */
    public void setSplitType(Reuters21578SplitType splitType) {
        _splitType = splitType;
    }

    /**
     * Get the type of set split currently used by this object.
     *
     * @return The type of split used.
     */
    public Reuters21578SplitType splitType() {
        return _splitType;
    }

    /**
     * Set the input directory of Reuters21578 corpus set.
     *
     * @param inputDir The directory containing the original Reuters21578 corpus set.
     */
    public void setInputDir(String inputDir) {
        _inputDir = inputDir;

        // Go to initial state.
        begin();
    }

    /**
     * @see it.cnr.jatecs.indexing.corpus.CorpusReader#begin()
     */
    public void begin() {

        String filename = _inputDir + Os.pathSeparator() + "reut2-000.sgm";

        try {
            _reader = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        _idFile = 0;
    }

    public void close() {
        try {
            _reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see it.cnr.jatecs.indexing.corpus.CorpusReader#next()
     */
    public CorpusDocument next() {

        CorpusDocument doc = null;

        while (doc == null) {

            String[] lines = readRawDocument();
            if (lines.length == 0) {
                // Reached the end of file, check if this file was the last in
                // the corpus dataset.
                if (this._idFile == 21) {
                    // Ok. We have seen all documents...
                    try {
                        _reader.close();
                    } catch (Exception e) {
                    }

                    return null;
                }

                // Change the file and read from the beginning.
                ++_idFile;
                String id = "";
                id = String.format("%03d", _idFile);
                String filename = _inputDir + Os.pathSeparator() + "reut2-"
                        + id + ".sgm";

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
                lines = readRawDocument();

            }

            // Process the raw document and possibly obtain a valid
            // CorpusDocument
            // object.
            doc = processRawDocument(lines);
        }

        return doc;
    }

    /**
     * Read a raw document from the current Reuters file.
     *
     * @return The set of lines containing the document.
     */
    protected String[] readRawDocument() {
        boolean docRead = false;
        LinkedList<String> lines = new LinkedList<String>();

        while (!docRead) {
            String line;
            try {
                line = _reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (line == null) {
                // Reach the end of file.
                String[] linesToReturn = new String[0];
                return linesToReturn;
            }

            if (line.equals("<!DOCTYPE lewis SYSTEM \"lewis.dtd\">")) {
                continue;
            }

            if (line.equals(""))
                continue;

            // Add the line.
            lines.add(line);

            // Check if we have reach the end of a document.
            if (line.equals("</REUTERS>"))
                docRead = true;
        }

        String[] linesToReturn = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            linesToReturn[i] = lines.get(i);
        }

        return linesToReturn;
    }

    public void excludeDocumentsWithoutValidCategories(boolean exclude) {
        _excludeDocWithoutCat = exclude;
    }

    /**
     * Get an high level representation of the passed raw document.
     *
     * @param lines The set of lines representing the raw document to process.
     * @return An high level representation of the document or "null" if current
     * document must be excluded from set of available documents.
     */
    protected CorpusDocument processRawDocument(String[] lines) {
        // Get the discriminating attributes of first line (tag REUTERS)
        String lewisSplit = getAttribute(lines[0], "LEWISSPLIT");
        String topics = getAttribute(lines[0], "TOPICS");
        String cgiSplit = getAttribute(lines[0], "CGISPLIT");

        DocumentType docType = DocumentType.TRAINING;
        Pair<Boolean, DocumentType> res = excludeDocument(lewisSplit, topics,
                cgiSplit, docType);
        if (res.getFirst())
            // The document is not contained in the set of currently
            // requested documents tipology.
            return null;

        docType = res.getSecond();

        // Get the ID of the document.
        String docName = new String(getAttribute(lines[0], "NEWID"));

        Vector<String> categories = new Vector<String>();
        String content = "";

        // Analyze all the lines of this document.
        DocIterator it = new DocIterator();
        it.row = 1;
        it.col = 0;
        while (it.row < lines.length - 1) {
            if (lines[it.row].startsWith("<DATE>")) {
                // Analyze the date of the document. SINGLE LINE.
                processDate(lines, it);
            } else if (lines[it.row].startsWith("<MKNOTE>")) {
                // Analyze the note contained in the document. MULTI LINE.
                processNotes(lines, it);
            } else if ((lines[it.row].startsWith("<TOPICS>"))
                    || (lines[it.row].startsWith("<PLACES>"))
                    || (lines[it.row].startsWith("<PEOPLE>"))
                    || (lines[it.row].startsWith("<ORGS>"))
                    || (lines[it.row].startsWith("<EXCHANGES>"))
                    || (lines[it.row].startsWith("<COMPANIES>"))) {
                // Analyze the TOPICS categories, if any, contained in the
                // document. SINGLE LINE.

                String[] cats = processCategories(lines, it);
                for (int i = 0; i < cats.length; ++i)
                    categories.add(new String(cats[i]));
            } else if (lines[it.row].startsWith("<UNKNOWN>")) {
                // Unknown characters sequence contained in the document. MULTI
                // LINE.
                processUnknowns(lines, it);
            } else if (lines[it.row].startsWith("<TEXT")) {
                // Textual material contained in this document. MULTI LINE.
                content += " . " + Os.newline() + processText(lines, it);
            }
        }

        assert (lines[lines.length - 1].startsWith("</REUTERS>"));

        if (_excludeDocWithoutCat) {
            if (categories.size() == 0) {
                // This document contain no valid categories then skip it.
                return null;
            }
        }

        return new CorpusDocument(docName, docType, content, categories);
    }

    protected Date processDate(String[] lines, DocIterator indexRow) {
        String start = "<DATE>";
        String end = "</DATE>";

        int startIdx = lines[indexRow.row].indexOf(start) + start.length();
        int endIdx = lines[indexRow.row].indexOf(end);

        String content = lines[indexRow.row].substring(startIdx, endIdx);

        indexRow.row++;
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        try {
            return dateFormat.parse(content);
        } catch (ParseException e) {
        }
        return null;
    }

    protected void processNotes(String[] lines, DocIterator it) {
        String line = lines[it.row];
        it.col = line.indexOf("<MKNOTE>");
        assert (it.col != -1);
        it.col += "<MKNOTE>".length();

        boolean analyzed = false;
        while (!analyzed) {
            int lastPos = line.length();

            if (line.indexOf("</MKNOTE>", it.col) != -1) {
                // Ok. This is the last line.
                lastPos = line.indexOf("</MKNOTE>", it.col);
                analyzed = true;
            }

            it.col = lastPos;

            if (analyzed)
                it.col += "</MKNOTE>".length();

            if (it.col == line.length()) {
                // Update row and column.
                it.row++;
                it.col = 0;

                if (!analyzed)
                    line = lines[it.row];
            }
        }
    }

    protected String[] processCategories(String[] lines, DocIterator indexRow) {

        String line = lines[indexRow.row];

        // Get the name of parent category.
        int startPos = line.indexOf("<");
        int endPos = line.indexOf(">");
        String nameParent = line.substring(startPos + 1, endPos);

        if ((nameParent.equals("EXCHANGES")) || (nameParent.equals("ORGS"))
                || (nameParent.equals("PEOPLE"))
                || (nameParent.equals("PLACES"))
                || (nameParent.equals("COMPANIES"))) {
            // I need to skip this categories...

            // Update the row
            indexRow.row++;

            return new String[]{};
        }

        Vector<String> cats = new Vector<String>();
        String bPattern = "<D>";
        String ePattern = "</D>";
        int index = endPos + 1;

        while (index < line.length()) {
            int pos = line.indexOf(bPattern, index);
            if (pos == -1) {
                // Ok. Reach the end of categories.
                index = line.length();
                continue;
            } else {
                // Get a category.
                int last = line.indexOf(ePattern, index);
                assert (last != -1);
                String category = line.substring(pos + bPattern.length(), last);

                // Update position
                index = last + ePattern.length();

                short catID = this.getCategoryDB().getCategory(category);
                if (catID != -1)
                    cats.add(category);
            }
        }
        // Update the row
        indexRow.row++;

        return cats.toArray(new String[cats.size()]);
    }

    protected void processUnknowns(String[] lines, DocIterator indexRow) {
        // Skip unknowns lines
        boolean processAll = false;
        while (!processAll) {
            String line = lines[indexRow.row];
            if (line.indexOf("</UNKNOWN>") != -1)
                processAll = true;
            indexRow.row++;
        }
    }

    protected String processText(String[] lines, DocIterator it) {
        String content = "";

        it.col = 0;

        assert (lines[it.row].indexOf("<TEXT") != -1);

        it.row++;

        String line = lines[it.row];

        boolean processAll = false;
        it.col = 0;
        while (!processAll) {
            if (line.indexOf("<AUTHOR>", it.col) != -1) {
                // Skip this data.
                processAuthor(lines, it);

                // String[] authfeat = processAuthor(lines, it);
                // for (int i = 0; i < authfeat.length; i++)
                // features.add(authfeat[i]);
            } else if (line.indexOf("<DATELINE>", it.col) != -1) {
                // Skip this data.
                processDateline(lines, it);
            } else if (line.indexOf("<TITLE>", it.col) != -1) {
                // Get the title features.
                content += " . " + Os.newline() + processTitle(lines, it);
            } else if (line.indexOf("<BODY>", it.col) != -1) {
                // Extract the features from data.
                content += " . " + Os.newline() + processBody(lines, it);

                // Skip irrelevant lines...
                it.col = 0;
                while (!processAll) {
                    if (lines[it.row].indexOf("</TEXT>") != -1)
                        processAll = true;
                    it.row++;
                }
            } else {
                // The article is of type "UNPROC".
                content += " . " + Os.newline() + processUnproc(lines, it);

                // Skip irrelevant lines...
                it.col = 0;
                processAll = true;
            }

            // Update the line.
            line = lines[it.row];
        }

        return content;
    }

    protected String processAuthor(String[] lines, DocIterator it) {
        String author = "";
        String line = lines[it.row];
        it.col = line.indexOf("<AUTHOR>");
        assert (it.col != -1);
        it.col += "<AUTHOR>".length();

        boolean analyzed = false;
        while (!analyzed) {
            int lastPos = line.length();

            if (line.indexOf("</AUTHOR>", it.col) != -1) {
                // Ok. This is the last line.
                lastPos = line.indexOf("</AUTHOR>", it.col);
                analyzed = true;
            }

            // Split the text.
            String lineToSplit = line.substring(it.col, lastPos);
            it.col = lastPos;

            // Get the features contained in this line.
            author += " . " + Os.newline() + lineToSplit;

            if (analyzed)
                it.col += "</AUTHOR>".length();

            if (it.col == line.length()) {
                // Update row and column.
                it.row++;
                it.col = 0;

                if (!analyzed)
                    line = lines[it.row];
            }
        }

        return author;
    }

    void processDateline(String[] lines, DocIterator it) {
        String line = lines[it.row];
        it.col = line.indexOf("<DATELINE>");
        assert (it.col != -1);
        it.col += "<DATELINE>".length();

        boolean analyzed = false;
        while (!analyzed) {
            int lastPos = line.length();

            if (line.indexOf("</DATELINE>", it.col) != -1) {
                // Ok. This is the last line.
                lastPos = line.indexOf("</DATELINE>", it.col);
                analyzed = true;
            }

            it.col = lastPos;
            if (analyzed)
                it.col += "</DATELINE>".length();

            if (it.col == line.length()) {
                // Update row and column.
                it.row++;
                it.col = 0;

                if (!analyzed)
                    line = lines[it.row];
            }
        }
    }

    protected String processTitle(String[] lines, DocIterator it) {
        String title = "";
        String line = lines[it.row];
        it.col = line.indexOf("<TITLE>");
        assert (it.col != -1);
        it.col += "<TITLE>".length();

        boolean analyzed = false;
        while (!analyzed) {
            int lastPos = line.length();

            if (line.indexOf("</TITLE>", it.col) != -1) {
                // Ok. This is the last line.
                lastPos = line.indexOf("</TITLE>", it.col);
                analyzed = true;
            }

            // Split the text.
            String lineToSplit = line.substring(it.col, lastPos);
            it.col = lastPos;

            // Extract the features from current line.
            title += " . " + Os.newline() + lineToSplit;

            if (analyzed)
                it.col += "</TITLE>".length();

            if (it.col == line.length()) {
                // Update row and column.
                it.row++;
                it.col = 0;

                if (!analyzed)
                    line = lines[it.row];
            }
        }

        return title;
    }

    protected String processBody(String[] lines, DocIterator it) {
        String body = "";
        String line = lines[it.row];
        it.col = line.indexOf("<BODY>");
        assert (it.col != -1);
        it.col += "<BODY>".length();

        boolean analyzed = false;
        while (!analyzed) {
            int lastPos = line.length();

            if (line.indexOf("</BODY>", it.col) != -1) {
                // Ok. This is the last line.
                lastPos = line.indexOf("</BODY>", it.col);
                analyzed = true;
            }

            // Split the text.
            String lineToSplit = line.substring(it.col, lastPos);
            it.col = lastPos;

            // Extract the features from current line.
            body += " . " + Os.newline() + lineToSplit;

            if (analyzed)
                it.col += "</BODY>".length();

            if (it.col == line.length()) {
                // Update row and column.
                it.row++;
                it.col = 0;

                if (!analyzed)
                    line = lines[it.row];
            }
        }

        return body;
    }

    protected String processUnproc(String[] lines, DocIterator it) {
        String unproc = "";
        String line = lines[it.row];
        it.col = 0;

        boolean analyzed = false;
        while (!analyzed) {
            int lastPos = line.length();

            if (line.indexOf("</TEXT>", it.col) != -1) {
                // Ok. This is the last line.
                lastPos = line.indexOf("</TEXT>", it.col);
                analyzed = true;
            }

            // Split the text.
            String lineToSplit = line.substring(it.col, lastPos);
            it.col = lastPos;

            // Extract the features from current line.
            unproc += " . " + Os.newline() + lineToSplit;

            if (analyzed)
                it.col += "</TEXT>".length();

            if (it.col == line.length()) {
                // Update row and column.
                it.row++;
                it.col = 0;

                if (!analyzed)
                    line = lines[it.row];
            }
        }

        return unproc;
    }

    /**
     * Get the value of an attribute from the specified input string. The
     * attribute must have the syntax ATTRIBUTE_NAME="VALUE" where
     * ATTRIBUTE_NAME is the name of attribute (parameter "attributeName") and
     * VALUE is the value that must be returned to caller.
     *
     * @param line          The string where to search the specified attribute.
     * @param attributeName The attribute name to search.
     * @return The value of specified attribute.
     */
    protected String getAttribute(String line, String attributeName) {
        String toSearch = attributeName + "=\"";

        // Find the start position.
        int pos = line.indexOf(toSearch);
        assert (pos != -1);
        int startPos = pos + toSearch.length();

        // Find the end position.
        int endPos = line.indexOf("\"", startPos);

        String attr = new String(line.substring(startPos, endPos));
        return attr;
    }

    protected Pair<Boolean, DocumentType> excludeDocument(String lewisSplit,
                                                          String topics, String cgiSplit, DocumentType docType) {
        if (splitType() == Reuters21578SplitType.LEWIS) {
            // Lewis split.

            return excludeLewisDocument(lewisSplit, topics, cgiSplit, docType);
        } else if (splitType() == Reuters21578SplitType.HAYES) {
            // Hayes split.

            return excludeHayesDocument(lewisSplit, topics, cgiSplit, docType);
        } else {
            // Apte split.
            return excludeApteDocument(lewisSplit, topics, cgiSplit, docType);
        }
    }

    protected Pair<Boolean, DocumentType> excludeLewisDocument(
            String lewisSplit, String topics, String cgiSplit,
            DocumentType docType) {

        if (getDocumentSetType() == SetType.TRAINING) {
            // I want training documents...
            if ((lewisSplit.equals("TRAIN")) && (!topics.equals("BYPASS"))) {
                docType = DocumentType.TRAINING;

                // Valid document.

                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        false, docType);
                return res;
            } else {
                // Invalid document.
                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        true, docType);
                return res;
            }
        } else if (getDocumentSetType() == SetType.TEST) {
            // I want test documents...

            if ((lewisSplit.equals("TEST")) && (!topics.equals("BYPASS"))) {
                // Valid document.
                docType = DocumentType.TEST;
                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        false, docType);
                return res;
            } else {
                // Invalid document.
                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        true, docType);
                return res;
            }
        } else if (getDocumentSetType() == SetType.ALL) {
            if ((lewisSplit.equals("TRAIN")) && (!topics.equals("BYPASS")))
                docType = DocumentType.TRAINING;
            else if ((lewisSplit.equals("TEST")) && (!topics.equals("BYPASS"))) {
                docType = DocumentType.TEST;
            } else
                docType = DocumentType.VALIDATION;

            // I want all documemt types.
            Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                    false, docType);
            return res;
        } else {
            // Skip the document.
            Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                    true, docType);
            return res;
        }
    }

    protected Pair<Boolean, DocumentType> excludeHayesDocument(
            String lewisSplit, String topics, String cgiSplit,
            DocumentType docType) {
        if (getDocumentSetType() == SetType.TRAINING) {
            // I want training documents...
            if (cgiSplit.equals("TRAINING-SET")) {
                // Valid document.
                docType = DocumentType.TRAINING;
                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        false, docType);
                return res;
            } else {
                // Invalid document.
                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        true, docType);
                return res;
            }
        } else if (getDocumentSetType() == SetType.TEST) {
            // I want test documents...

            if (cgiSplit.equals("PUBLISHED-TESTSET")) {
                // Valid document.
                docType = DocumentType.TEST;
                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        false, docType);
                return res;
            } else {
                // Invalid document.
                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        true, docType);
                return res;
            }
        } else if (getDocumentSetType() == SetType.ALL) {
            if (cgiSplit.equals("TRAINING-SET")) {
                // Valid document.
                docType = DocumentType.TRAINING;
            } else if (cgiSplit.equals("PUBLISHED-TESTSET"))
                docType = DocumentType.TEST;
            else
                docType = DocumentType.VALIDATION;

            // I want all documemt types.
            Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                    false, docType);
            return res;
        } else {
            Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                    true, docType);
            return res;
        }
    }

    protected Pair<Boolean, DocumentType> excludeApteDocument(
            String lewisSplit, String topics, String cgiSplit,
            DocumentType docType) {
        if (getDocumentSetType() == SetType.TRAINING) {
            // I want training documents...
            if ((lewisSplit.equals("TRAIN")) && (topics.equals("YES"))) {
                // Valid document.
                docType = DocumentType.TRAINING;
                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        false, docType);
                return res;
            } else {
                // Invalid document.
                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        true, docType);
                return res;
            }
        } else if (getDocumentSetType() == SetType.TEST) {
            // I want test documents...

            if ((lewisSplit.equals("TEST")) && (topics.equals("YES"))) {
                // Valid document.
                docType = DocumentType.TEST;
                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        false, docType);
                return res;
            } else {
                // Invalid document.
                Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                        true, docType);
                return res;
            }
        } else if (getDocumentSetType() == SetType.ALL) {
            if ((lewisSplit.equals("TRAIN")) && (topics.equals("YES"))) {
                // Valid document.
                docType = DocumentType.TRAINING;
            } else if ((lewisSplit.equals("TEST")) && (topics.equals("YES")))
                docType = DocumentType.TEST;
            else
                docType = DocumentType.VALIDATION;

            // I want all documemt types.
            Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                    false, docType);
            return res;
        } else {
            Pair<Boolean, DocumentType> res = new Pair<Boolean, DocumentType>(
                    true, docType);
            return res;
        }
    }

    /**
     * @see it.cnr.jatecs.indexing.corpus.CorpusReader#printConfiguration()
     */
    public String printConfiguration() {
        String msg = super.printConfiguration();

        msg += "Reuters21578 corpus input directory: " + _inputDir + ".\n";
        msg += "Reuters21578 split type: " + _splitType.toString() + ".\n";

        return msg;
    }

    protected enum ArticleType {
        NORMAL, BRIEF, UNPROC;
    }

    protected class DocIterator {
        public int row = 0;
        public int col = 0;
    }

}
