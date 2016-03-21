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

import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.DocumentType;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.utils.Os;

import java.util.Vector;

/**
 * @author fagni
 */
public class RCV1FastParser {


    private String _xmlDoc;


    private RCV1CorpusReader _reader;

    private boolean _readAll;


    public RCV1FastParser(RCV1CorpusReader reader) {
        _reader = reader;
        _readAll = false;
    }


    public boolean readedAll() {
        return _readAll;
    }

    public void setXMLDocument(String xmlDoc) {
        _xmlDoc = xmlDoc;
    }


    public CorpusDocument parse() {
        int idx = 0;

        // Get the id of the document.
        int startIdx = _xmlDoc.indexOf("itemid=\"");
        assert (startIdx != -1);
        startIdx += "itemid=\"".length();
        int endIdx = _xmlDoc.indexOf("\"", startIdx);
        assert (endIdx != -1);
        String id = new String(_xmlDoc.substring(startIdx, endIdx));


        CorpusDocument doc = null;

        Long idLong = new Long(id);
        if ((_reader.getDocumentSetType() == SetType.TRAINING) && (idLong.longValue() > 26150)) {
            _readAll = true;
            return null;
        }

        if ((_reader.getDocumentSetType() == SetType.TEST) && (idLong.longValue() <= 26150))
            return null;

        DocumentType dt = DocumentType.TRAINING;

        if (idLong.longValue() <= 26150)
            dt = DocumentType.TRAINING;
        else
            dt = DocumentType.TEST;

        idx = endIdx + 1;

        // Get the document headline.
        startIdx = _xmlDoc.indexOf("<headline>", idx);
        assert (startIdx != -1);
        startIdx += "<headline>".length();
        endIdx = _xmlDoc.indexOf("</headline>", startIdx);
        assert (endIdx != -1);
        String headline = new String(_xmlDoc.substring(startIdx, endIdx));

        idx = endIdx + 1;

        // Get the text of document.
        startIdx = _xmlDoc.indexOf("<text>", idx);
        assert (startIdx != -1);
        startIdx += "<text>".length();
        endIdx = _xmlDoc.indexOf("</text>", startIdx);
        assert (endIdx != -1);
        String text = new String(_xmlDoc.substring(startIdx, endIdx));
        text = text.replaceAll("[\\<][pP][\\>]|[\\<]\\/[pP][\\>]", "");

        idx = endIdx + 1;

        // Get the topics categories.
        startIdx = _xmlDoc.indexOf("<codes class=\"bip:topics:1.0\">", idx);
        if (startIdx == -1)
            return null;

        Vector<String> categories = new Vector<String>();

        startIdx += "<codes class=\"bip:topics:1.0\">".length();
        endIdx = _xmlDoc.indexOf("</codes>", startIdx);
        assert (endIdx != -1);
        String topics = _xmlDoc.substring(startIdx, endIdx);
        int tmp = 0;
        boolean processAll = false;
        int numCode = 0;
        while (!processAll) {
            int sta = topics.indexOf("<code code=\"", tmp);
            if (sta == -1) {
                processAll = true;
                continue;
            }

            sta += "<code code=\"".length();
            int end = topics.indexOf("\">", sta);
            assert (end != -1);

            // Read a composed category name.
            assignCategories(categories, new String(topics.substring(sta, end)));

            tmp = end;
            numCode++;
        }

        if (numCode == 0)
            return null;

        startIdx = _xmlDoc.indexOf("<codes class=\"bip:countries:1.0\">", idx);
        if (startIdx == -1)
            return null;

        doc = new CorpusDocument(id, dt, headline + "." + Os.newline() + text, categories);

        return doc;
    }


    protected void assignCategories(Vector<String> categories, String catType) {
        String cat = catType;

        short catID = _reader.getCategoryDB().getCategory(cat);
        if (catID < 0)
            // Skip this category.
            return;

        // Assign this category and
        categories.add(cat);
    }
}
