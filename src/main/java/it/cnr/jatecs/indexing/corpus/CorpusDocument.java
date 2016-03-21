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
 * Created on 29-nov-2004
 */
package it.cnr.jatecs.indexing.corpus;

import java.util.List;
import java.util.Vector;

/**
 * This is a generic corpus document.
 *
 * @author Tiziano Fagni, Andrea Esuli
 */
public class CorpusDocument {

    /**
     * The name of the document.
     */
    private String _name;

    /**
     * The content of this document.
     */
    private String _content;

    /**
     * The categories which belong this document.
     */
    private List<CorpusCategory> _categories;

    /**
     * The type of document.
     */
    private DocumentType _docType;

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
     */
    public CorpusDocument(String name, DocumentType docType, String content,
                          List<String> categories) {
        _docType = docType;
        _name = name;

        _content = content;

        _categories = new Vector<CorpusCategory>(categories.size());
        for (int i = 0; i < categories.size(); i++) {
            CorpusCategory cc = new CorpusCategory();
            cc.name = categories.get(i);
            cc.primary = false;
            _categories.add(cc);
        }

    }

    public CorpusDocument(DocumentType documentType, String name,
                          String content, List<CorpusCategory> categories) {
        _docType = documentType;
        _name = name;
        _content = content;
        _categories = categories;
    }

    /**
     * Get the document name. Must be unique for the index.
     *
     * @return The document name.
     */
    public String name() {
        return _name;
    }

    /**
     * Get the type of this document.
     *
     * @return The type of this document.
     */
    public DocumentType documentType() {
        return _docType;
    }

    /**
     * Get the content of this document.
     *
     * @return
     */
    public String content() {
        return _content;
    }

    /**
     * Get the categories which the document belong to.
     *
     * @return The categories set.
     */
    public List<CorpusCategory> categories() {
        return _categories;
    }

}
