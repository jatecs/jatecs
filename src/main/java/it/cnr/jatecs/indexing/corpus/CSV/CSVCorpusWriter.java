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

package it.cnr.jatecs.indexing.corpus.CSV;

import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.PrintStream;

public class CSVCorpusWriter {

    private String _fieldSeparator;
    private String _contentSeparator;
    private boolean _alsoParents;

    public CSVCorpusWriter() {
        super();
        _fieldSeparator = ",";
        _contentSeparator = " ";
        _alsoParents = true;
    }

    /**
     * @param fieldSeparator fields separator, default is ","
     */
    public void setFieldSeparator(String fieldSeparator) {
        _fieldSeparator = fieldSeparator;
    }

    public void writeOnlyLeafCategories(boolean onlyLeaves) {
        _alsoParents = !onlyLeaves;
    }

    /**
     * @param _contentSeparator content feature separator , default is " "
     */
    public void setContentSeparator(String contentSeparator) {
        _contentSeparator = contentSeparator;
    }

    public void writeCorpus(PrintStream stream, IIndex index) {
        IContentDB contdb = index.getContentDB();
        IFeatureDB featdb = contdb.getFeatureDB();
        IClassificationDB classdb = index.getClassificationDB();
        IDocumentDB docdb = classdb.getDocumentDB();
        ICategoryDB catdb = classdb.getCategoryDB();
        IIntIterator docit = docdb.getDocuments();
        while (docit.hasNext()) {
            int document = docit.next();
            stream.print(docdb.getDocumentName(document) + _fieldSeparator);
            IIntIterator featit = contdb.getDocumentFeatures(document);
            while (featit.hasNext()) {
                int feature = featit.next();
                stream.print(featdb.getFeatureName(feature) + _contentSeparator);
            }
            stream.print(_fieldSeparator);
            IShortIterator catit = classdb.getDocumentCategories(document);
            while (catit.hasNext()) {
                short category = catit.next();
                if (_alsoParents || !catdb.hasChildCategories(category))
                    stream.print(catdb.getCategoryName(category) + _fieldSeparator);
            }
            stream.println();
        }
    }

    public void writeClassification(PrintStream stream, IClassificationDB classification) {
        IDocumentDB docdb = classification.getDocumentDB();
        ICategoryDB catdb = classification.getCategoryDB();
        IIntIterator docit = docdb.getDocuments();
        while (docit.hasNext()) {
            int document = docit.next();
            stream.print(docdb.getDocumentName(document) + _fieldSeparator);
            IShortIterator catit = classification.getDocumentCategories(document);
            while (catit.hasNext()) {
                short category = catit.next();
                if (_alsoParents || !catdb.hasChildCategories(category))
                    stream.print(catdb.getCategoryName(category) + _fieldSeparator);
            }
            stream.println();
        }
    }

}
