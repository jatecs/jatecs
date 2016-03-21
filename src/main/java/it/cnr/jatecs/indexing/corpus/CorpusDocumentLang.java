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

package it.cnr.jatecs.indexing.corpus;

import it.cnr.jatecs.indexes.utils.LanguageLabel;

import java.util.List;

public class CorpusDocumentLang extends CorpusDocument {

    private LanguageLabel _langLabel;

    /**
     * Construct a new corpus document with the specified
     *
     * @param name       the name of the document
     * @param docType    the type of the document (training or test)
     * @param content    the contet of the document
     * @param categories a list of the categories to which this document belongs to
     *                   <p>
     *                   Attention the categories list is NOT cloned! Do not modify the
     *                   list after creating a CorpusDocument and while it is used by
     *                   indexing functions.
     * @param lang       the language label in which the document is written in
     */
    public CorpusDocumentLang(String name, DocumentType docType,
                              String content, List<String> categories, LanguageLabel lang) {
        super(name, docType, content, categories);
        setLanguageLabel(lang);
    }

    public CorpusDocumentLang(DocumentType documentType, String name,
                              String content, List<CorpusCategory> categories, LanguageLabel lang) {
        super(documentType, name, content, categories);
        setLanguageLabel(lang);
    }

    public void setLanguageLabel(LanguageLabel lang) {
        _langLabel = lang;
    }

    public LanguageLabel lang() {
        return _langLabel;
    }
}
