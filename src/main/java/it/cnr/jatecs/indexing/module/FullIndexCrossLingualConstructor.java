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

package it.cnr.jatecs.indexing.module;

import it.cnr.jatecs.indexes.DB.generic.MultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IMultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDocumentLanguageDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.indexing.corpus.*;
import it.cnr.jatecs.indexing.preprocessing.*;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class FullIndexCrossLingualConstructor extends JatecsModule {
    protected IIndexBuilder _indexBuilder;
    /**
     * The corpus reader form which the corpus documents will be read.
     */
    protected CorpusReader _reader;
    protected boolean _cleanCategories = false;
    /**
     * The features extractor object.
     */
    private HashMap<LanguageLabel, IFeatureExtractor> _extr;
    private TroveDocumentLanguageDB _langMap;
    private boolean _useLanguageDependantStemmers = true;
    private boolean _useLanguageDependantStopWords = true;

    /**
     * Construct a new IndexReader object from a subclass class with no stopword
     * and stemming removal.
     *
     * @param reader The corpus reader object.
     * @param index  The index on which the the corpus will be written.
     * @throws Exception
     */
    public FullIndexCrossLingualConstructor(CorpusReader reader,
                                            IIndexBuilder indexBuilder) {
        super(indexBuilder.getIndex(), "FullCrossLingualIndexConstructor");

        assert (reader != null);
        assert (indexBuilder != null);

        setDescription("Construct a full cross-lingual index from documents coming from specified corpus reader.");

        _reader = reader;
        _indexBuilder = indexBuilder;

        _extr = new HashMap<LanguageLabel, IFeatureExtractor>();
        _langMap = new TroveDocumentLanguageDB();
    }

    public FullIndexCrossLingualConstructor(CorpusReader reader,
                                            TroveMainIndexBuilder indexBuilder, boolean cleanCategories) {
        this(reader, indexBuilder);
        _cleanCategories = cleanCategories;
    }

    /**
     * Get the feature extractor used by the indexer.
     *
     * @return The feature extractor used by the indexer.
     */
    public IFeatureExtractor featureExtractor(LanguageLabel l) {
        return _extr.get(l);
    }

    /**
     * Set the feature extractor object.
     *
     * @param extractor The feature extractor object to use.
     */
    public void setFeatureExtractor(IFeatureExtractor extractor,
                                    LanguageLabel lang) {
        assert (extractor != null);

        _extr.put(lang, extractor);
    }

    public HashMap<LanguageLabel, IFeatureExtractor> getFeatureExtractorMap() {
        return this._extr;
    }

    public void setFeatureExtractorMap(
            HashMap<LanguageLabel, IFeatureExtractor> langmap) {
        this._extr = langmap;
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
            LanguageLabel lang = ((CorpusDocumentLang) doc).lang();
            ProcessedCorpusDocument predoc = new ProcessedCorpusDocument(
                    doc.name(), doc.documentType(), _extr.get(lang)
                    .extractFeatures(doc.content()), doc.categories());

            // Add the document to index
            int docid = indexDocument(predoc);

            // Add the document language label
            indexLanguage(docid, lang);

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
    protected int indexDocument(ProcessedCorpusDocument doc) {

        int docid = _indexBuilder.addDocument(doc.name(), doc.features()
                        .toArray(new String[0]),
                doc.categories().toArray(new CorpusCategory[0]));

        // System.out.println("Feats " +
        // _indexBuilder.getContentDB().getFeaturesDB().getFeaturesCount(true));
        return docid;

    }

    private void indexLanguage(int docid, LanguageLabel lang) {
        this._langMap.indexDocLang(docid, lang);
    }

    public TroveDocumentLanguageDB getDocumentLanguageDB() {
        return this._langMap;
    }

    public IMultilingualIndex index() {
        _langMap.setDocumentDB(_index.getDocumentDB());
        return new MultilingualIndex(_index, _langMap);
    }

    /**
     * @see it.cnr.jatecs.module.JatecsModule#processModule()
     */
    @Override
    protected void processModule() {

        indexDocuments();

        JatecsLogger.status().println("Done.");
    }

    public void setLanguageDependantFeatureExtractors(
            Set<LanguageLabel> languages) throws IOException {
        for (LanguageLabel lang : languages) {
            CommonStemming stemming = null;
            InputStreamStopword stopwords = null;

            if (_useLanguageDependantStemmers) {
                switch (lang) {
                    case en:
                        stemming = new EnglishPorterStemming();
                        break;
                    case it:
                        stemming = new ItalianStemmer();
                        break;
                    case es:
                        stemming = new SpanishStemmer();
                        break;
                    case de:
                        stemming = new GermanStemmer();
                        break;
                    case fr:
                        stemming = new FrenchStemmer();
                        break;
                    default:
                        System.err
                                .println("\nError. Unsupported Stemmer for language <"
                                        + lang.toString() + "> found.\nExit.\n");
                        System.exit(0);
                        break;
                }
            }

            if (_useLanguageDependantStopWords) {
                switch (lang) {
                    case en:
                        stopwords = new EnglishStopword();
                        break;
                    case it:
                        stopwords = new ItalianStopword();
                        break;
                    case es:
                        stopwords = new SpanishStopword();
                        break;
                    case de:
                        stopwords = new GermanStopword();
                        break;
                    case fr:
                        stopwords = new FrenchStopword();
                        break;
                    default:
                        System.err
                                .println("\nError. Unsupported stopwords removal for language <"
                                        + lang.toString() + "> found.\nExit.\n");
                        System.exit(0);
                        break;
                }
            }
            setupLanguageFeatureExtractor(stemming, stopwords, lang);
        }

    }

    public void setupLanguageFeatureExtractor(CommonStemming stemming,
                                              InputStreamStopword stopwords, LanguageLabel lang)
            throws IOException {

        BagOfWordsFeatureExtractor extractor = new BagOfWordsFeatureExtractor();

        extractor.disableEntitiesSubstitution();
        extractor.disableSpecialTermsSubstitution();
        extractor.disableSpellChecking();

        if (stemming == null)
            extractor.disableStemming();
        else
            extractor.enableStemming(stemming);

        if (stopwords == null)
            extractor.disableStopwordRemoval();
        else
            extractor.enableStopwordRemoval(stopwords);

        extractor.addCircularRule("^[\\-–—]+");
        extractor.addCircularRule("(\\-|–|—)+$");
        extractor.addCircularRule("[_…“”„`″]");
        extractor.addCircularRule("^‘|’$");
        extractor.addCircularRule("%(quot|gt|lt|nbsp|amp)%");

        setFeatureExtractor(extractor, lang);
    }

    public void setLanguageDependantStemmers(boolean b) {
        _useLanguageDependantStemmers = b;
    }

    public void setLanguageDependantStopWordsRemoval(boolean b) {
        _useLanguageDependantStopWords = b;
    }

}
