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
 * Created on 24-nov-2004
 *
 */
package it.cnr.jatecs.indexing.module;

import it.cnr.jatecs.indexes.DB.interfaces.IIndexBuilder;
import it.cnr.jatecs.indexing.corpus.*;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;

/**
 * @author Tiziano Fagni, Andrea Esuli
 */
public class FullIndexConstructor extends JatecsModule {

    /**
     * The corpus reader form which the corpus documents will be read.
     */
    private CorpusReader _reader;

    /**
     * The features extractor object.
     */
    private IFeatureExtractor _extr;

    private boolean _cleanCategories = false;
    private IIndexBuilder _indexBuilder;

    /**
     * Construct a new IndexReader object from a subclass class with no stopword
     * and stemming removal.
     *
     * @param reader The corpus reader object.
     * @param index  The index on which the the corpus will be written.
     * @throws Exception
     */
    public FullIndexConstructor(CorpusReader reader, IIndexBuilder indexBuilder) {
        super(indexBuilder.getIndex(), "FullIndexConstructor");

        assert (reader != null);
        assert (indexBuilder != null);

        setDescription("Construct a full index from documents coming from specified corpus reader.");

        _reader = reader;
        _extr = new BagOfWordsFeatureExtractor();
        _indexBuilder = indexBuilder;
    }

    /**
     * Construct a new IndexReader object from a subclass class with no stopword
     * and stemming removal.
     *
     * @param reader          The corpus reader object.
     * @param index           The index on which the the corpus will be written.
     * @param cleanCategories Remove categories from the categoryDB that don not have
     *                        positive examples in the dataset
     * @throws Exception
     */
    public FullIndexConstructor(CorpusReader reader,
                                IIndexBuilder indexBuilder, boolean cleanCategories) {
        this(reader, indexBuilder);
        _cleanCategories = cleanCategories;
    }

    /**
     * Get the feature extractor used by the indexer.
     *
     * @return The feature extractor used by the indexer.
     */
    public IFeatureExtractor featureExtractor() {
        return _extr;
    }

    /**
     * Set the feature extractor object.
     *
     * @param extractor The feature extractor object to use.
     */
    public void setFeatureExtractor(IFeatureExtractor extractor) {
        assert (extractor != null);

        _extr = extractor;
    }

    /**
     * Index all documents coming from specified (in constructor) corpus reader
     * object.
     *
     * @throws IndexWriterException Raised if some error occcurs during the operation.
     */
    protected void indexDocuments() {
        String msg = "";

        msg += "\n++ Begin preparing the complete index:";
        JatecsLogger.status().println(msg);

        // Read all documents from the given reader.
        CorpusDocument doc = null;

        _reader.begin();
        doc = _reader.next();

        int count = 0;

        while (doc != null) {
            ProcessedCorpusDocument predoc = new ProcessedCorpusDocument(
                    doc.name(), doc.documentType(), _extr.extractFeatures(doc
                    .content()), doc.categories());
            // Add the document to index
            indexDocument(predoc);

            // Read next document
            doc = _reader.next();

            count++;

            // Counter for std-out purpose.
            if ((count % 25) == 0)
                JatecsLogger.status().print("" + count);
            else
                JatecsLogger.status().print(".");

            if ((count % 50) == 0)
                JatecsLogger.status().print("\n");
        }
        if (_cleanCategories) {
            _indexBuilder.getIndex().cleanCategories();
        }
        _reader.close();
        JatecsLogger.status().println(
                "Done. Indexed "
                        + _indexBuilder.getIndex().getDocumentDB()
                        .getDocumentsCount()
                        + " documents, "
                        + _indexBuilder.getIndex().getCategoryDB()
                        .getCategoriesCount()
                        + " categories and "
                        + _indexBuilder.getIndex().getFeatureDB()
                        .getFeaturesCount() + " features.");

    }

    /**
     * Index the document by adding the document datas to index. The method is
     * thread-safe.
     *
     * @param doc The document to index.
     */
    protected void indexDocument(ProcessedCorpusDocument doc) {

        _indexBuilder.addDocument(doc.name(),
                doc.features().toArray(new String[0]), doc.categories()
                        .toArray(new CorpusCategory[0]));

    }

    /**
     * @see it.cnr.jatecs.module.JatecsModule#processModule()
     */
    protected void processModule() {

        indexDocuments();

        JatecsLogger.status().println("Done.");
    }

    /**
     * Always return the same signature.
     *
     * @see it.cnr.jatecs.module.JatecsModule#signature()
     */
    public String signature() {
        return "ok";
    }

    public boolean mustProcessModule() {
        return true;
    }
}
