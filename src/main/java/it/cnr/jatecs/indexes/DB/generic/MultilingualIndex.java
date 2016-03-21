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

package it.cnr.jatecs.indexes.DB.generic;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentLanguageDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDomainDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.interfaces.ILanguageDB;
import it.cnr.jatecs.indexes.DB.interfaces.IMultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IWeightingDB;
import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.utils.iterators.IntArrayIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of a Multilingual Index data structure, i.e., an indexed corpus
 * containing documents in various languages.
 * */
public class MultilingualIndex extends GenericIndex implements IMultilingualIndex {
	/**
	 * the language db
	 */
    protected ILanguageDB _languageDB;
    
    /**
     * the db storing all relations between documents and languages
     */ 
    protected IDocumentLanguageDB _documentLanguageDB;

    public MultilingualIndex(IFeatureDB featureDB, IDocumentDB documentDB,
                              ICategoryDB categoryDB, IDomainDB domainDB,
                              ILanguageDB languageDB, IContentDB contentDB,
                              IWeightingDB weightingDB, IClassificationDB classificationDB,
                              IDocumentLanguageDB documentLanguageDB) {

        super(featureDB, documentDB, categoryDB, domainDB, contentDB,
                weightingDB, classificationDB);

        _languageDB = languageDB;
        _documentLanguageDB = documentLanguageDB;
    }

    /**
     * Class constructor specifying the indexed corpus and the document-language information
     * */
    public MultilingualIndex(IIndex index, IDocumentLanguageDB documentLanguageDB) {
        super(index.getFeatureDB(), index.getDocumentDB(), index.getCategoryDB(), index.getDomainDB(),
                index.getContentDB(), index.getWeightingDB(), index.getClassificationDB());
        _languageDB = documentLanguageDB.getLanguageDB();
        _documentLanguageDB = documentLanguageDB;
    }

    private static boolean containsSomeLang(List<LanguageLabel> a,
                                            HashSet<LanguageLabel> b) {
        for (Iterator<LanguageLabel> i = a.iterator(); i.hasNext(); ) {
            if (b.contains(i.next()))
                return true;
        }
        return false;
    }

    /**
     * @return the language db of the multilingual index
     */
    @Override
    public ILanguageDB getLanguageDB() {
        return _languageDB;
    }

    /**
     * @return the document-language db of the multilingual index
     * */
    @Override
    public IDocumentLanguageDB getDocumentLanguageDB() {
        return _documentLanguageDB;
    }

    /**
     * Removes all documents not belonging to any of the specified languages
     * @param langs a list of the languages of which documents ought to be kept
     * */
    @Override
    public void selectDocumentsInLanguages(List<LanguageLabel> langs) {
        IDocumentDB documents = getDocumentDB();

        ArrayList<Integer> toRemoveList = new ArrayList<Integer>();
        IIntIterator docit = documents.getDocuments();
        while (docit.hasNext()) {
            int doc = docit.next();
            HashSet<LanguageLabel> doclangs = _documentLanguageDB.getDocumentLanguages(doc);
            if (!containsSomeLang(langs, doclangs)) 
                toRemoveList.add(doc);            
        }

        if (!toRemoveList.isEmpty()) {
            int[] toRemove = new int[toRemoveList.size()];
            int p = 0;
            for (int toRemoveEl : toRemoveList)
                toRemove[p++] = toRemoveEl;

            IntArrayIterator toRemoveIt = new IntArrayIterator(toRemove);
            this.removeDocuments(toRemoveIt, false);//false <- important, otherwise could cause mismatchs between training/test indexes
        }

        Iterator<LanguageLabel> langsit = _languageDB.getLanguages();
        while (langsit.hasNext()) {
            LanguageLabel langnext = langsit.next();
            if (!langs.contains(langnext)) {
                _languageDB.removeLanguage(langnext);
            }
        }
    }

    /**
     * remove some documents from the db, EVENTUALLY compacting the features ids.
     * ATTENTION: this method HAS effects on ALL the dbs,
     *
     * @param removedDocuments   an iterator on the documents to be removed
     * @param compressFeaturesId true to compact the features id, false to keep
     *                           them sparse
     */
    @Override
    public void removeDocuments(IIntIterator removedDocuments,
                                boolean compressFeaturesId) {
        super.removeDocuments(removedDocuments, compressFeaturesId);
        removedDocuments.begin();
        while (removedDocuments.hasNext()) {
            int doc = removedDocuments.next();
            this._documentLanguageDB.removeDocument(doc);
        }
    }

    /**
     * @return a copy of the multilingual index
     * */
    public IMultilingualIndex cloneCLIndex() {
        IIndex clone = super.cloneIndex();
        ILanguageDB langDB = this._languageDB.cloneDB();
        IDocumentLanguageDB docLangDB = this._documentLanguageDB.cloneDB(clone.getDocumentDB(), langDB);
        return new MultilingualIndex(clone, docLangDB);
    }
}
