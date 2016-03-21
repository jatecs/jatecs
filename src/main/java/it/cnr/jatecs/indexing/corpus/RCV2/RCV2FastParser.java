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
package it.cnr.jatecs.indexing.corpus.RCV2;

import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.indexing.corpus.CorpusDocumentLang;
import it.cnr.jatecs.indexing.corpus.DocumentType;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RCV2FastParser {

    private String _xmlDoc;

    private RCV2CorpusReader _reader;

    private int _docIDcounter;

    public RCV2FastParser(RCV2CorpusReader reader) {
        _reader = reader;
        _docIDcounter = 0;
    }

    public static LanguageLabel showLangLabel(String filepath) {
        String rawdoc = RCV2CorpusReader.readRawDocument(filepath, "<title>");
        LanguageLabel langLabel = showLangLabelFromContent(rawdoc);
        return langLabel;
    }

    public static LanguageLabel showLangLabelFromContent(String rawdoc) {
        LanguageLabel langLabel = LanguageLabel.unknown;
        Pattern p = Pattern.compile("xml:lang=\"([a-z\\-]+)\"");
        Matcher m = p.matcher(rawdoc);
        if (m.find()) {
            String lang = m.group(1);
            if (lang.equals("zhcn"))
                lang = "zh_cn";
            if (lang.equals("zhtw"))
                lang = "zh_tw";
            if (lang.equals("jp"))
                lang = "ja";

            try {
                langLabel = LanguageLabel.valueOf(lang);
            } catch (java.lang.IllegalArgumentException e) {
                JatecsLogger.execution().warning(
                        "Unknown language identifier <" + lang + ">\n");
            }
        }
        return langLabel;
    }

    public void setXMLDocument(String xmlDoc) {
        _xmlDoc = xmlDoc;
    }

    public CorpusDocumentLang parse() {
        int idx = 0;

        // Get the id of the document.
        int startIdx = _xmlDoc.indexOf("itemid=\"");
        assert (startIdx != -1);
        startIdx += "itemid=\"".length();
        int endIdx = _xmlDoc.indexOf("\"", startIdx);
        assert (endIdx != -1);
        String id = new String(_xmlDoc.substring(startIdx, endIdx)) + "_"
                + (_docIDcounter++);

        CorpusDocumentLang doc = null;

        DocumentType dt = (_reader.getDocumentSetType() == SetType.TRAINING) ? DocumentType.TRAINING
                : DocumentType.TEST;

        idx = endIdx + 1;

        // Get the document title.
        startIdx = _xmlDoc.indexOf("<title>", idx);
        assert (startIdx != -1);
        startIdx += "<title>".length();
        endIdx = _xmlDoc.indexOf("</title>", startIdx);
        assert (endIdx != -1);
        String title = new String(_xmlDoc.substring(startIdx, endIdx));

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

        LanguageLabel langLabel = showLangLabelFromContent(_xmlDoc);
        doc = new CorpusDocumentLang(id, dt, title + "." + Os.newline() + text,
                categories, langLabel);

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
