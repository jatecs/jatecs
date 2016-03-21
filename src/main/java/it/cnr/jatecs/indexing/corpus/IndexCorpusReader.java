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

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.ArrayList;


/**
 * A corpus reader for Jatecs indexes.
 *
 * @author Tiziano Fagni (tiziano.fagni@isti.cnr.it)
 */
public class IndexCorpusReader extends CorpusReader {

    private IIndex indexSrc;
    private IIntIterator itDocs;
    private DocumentType docType;

    public IndexCorpusReader(IIndex indexSrc, ICategoryDB catsDB) {
        super(catsDB);

        this.indexSrc = indexSrc;
        begin();
    }

    @Override
    public void begin() {
        this.itDocs = indexSrc.getDocumentDB().getDocuments();
    }

    @Override
    public void close() {

    }

    @Override
    public CorpusDocument next() {
        if (!itDocs.hasNext())
            return null;

        int docID = itDocs.next();
        return extractDocument(docID);
    }


    void setDocumentType(DocumentType docType) {
        this.docType = docType;
    }


    private CorpusDocument extractDocument(int docID) {
        String docName = indexSrc.getDocumentDB().getDocumentName(docID);

        StringBuilder sb = new StringBuilder();
        ArrayList<String> cats = new ArrayList<String>();

        IIntIterator feats = indexSrc.getContentDB().getDocumentFeatures(docID);
        while (feats.hasNext()) {
            int featID = feats.next();
            String featName = indexSrc.getFeatureDB().getFeatureName(featID);
            int numOccurrences = indexSrc.getContentDB().getDocumentFeatureFrequency(docID, featID);
            for (int i = 0; i < numOccurrences; i++) {
                sb.append(" " + featName);
            }
        }

        IShortIterator catsIt = indexSrc.getClassificationDB().getDocumentCategories(docID);
        while (catsIt.hasNext()) {
            short catID = catsIt.next();
            String catName = indexSrc.getCategoryDB().getCategoryName(catID);
            if (getCategoryDB().getCategory(catName) != -1)
                cats.add(catName);
        }

        CorpusDocument cd = new CorpusDocument(docName, docType, sb.toString(), cats);
        return cd;
    }

}
