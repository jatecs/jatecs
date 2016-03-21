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

package it.cnr.jatecs.indexes.DB.troveCompact;

import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.indexing.corpus.CorpusCategory;

public class TroveDependentIndexBuilder extends GenericIndex implements IIndexBuilder {

    protected TroveDocumentsDBBuilder _documentsDBBuilder;
    protected IContentDBBuilder _contentDBBuilder;
    protected IClassificationDBBuilder _classificationDBBuilder;
    public TroveDependentIndexBuilder(IDomainDB domainDB) {
        super(null, null, null, null, null, null, null);
        _domainDB = domainDB;
        _categoriesDB = domainDB.getCategoryDB();
        _featuresDB = domainDB.getFeatureDB();
        _documentsDBBuilder = new TroveDocumentsDBBuilder();
        _documentsDB = _documentsDBBuilder.getDocumentDB();
        _contentDBBuilder = new TroveContentDBBuilder(_documentsDB, _featuresDB);
        _contentDB = _contentDBBuilder.getContentDB();
        _weightingDB = new TroveWeightingDB(_contentDB);
        _classificationDBBuilder = new TroveClassificationDBBuilder(_documentsDB, _categoriesDB);
        _classificationDB = _classificationDBBuilder.getClassificationDB();
    }
    public TroveDependentIndexBuilder(IIndex index) {
        super(null, null, null, null, null, null, null);
        _domainDB = new TroveDomainDB((TroveDomainDB) index.getDomainDB());
        _categoriesDB = index.getCategoryDB();
        _featuresDB = index.getFeatureDB();
        _documentsDBBuilder = new TroveDocumentsDBBuilder((TroveDocumentsDB) index.getDocumentDB());
        _documentsDB = _documentsDBBuilder.getDocumentDB();
        if (index.getContentDB() instanceof TroveContentDB)
            _contentDBBuilder = new TroveContentDBBuilder((TroveContentDB) index.getContentDB());
        else if (index.getContentDB() instanceof TroveContentILDB)
            _contentDBBuilder = new TroveContentILDBBuilder((TroveContentILDB) index.getContentDB());
        else
            _contentDBBuilder = new TroveContentFullDBBuilder((TroveContentFullDB) index.getContentDB());
        _contentDB = _contentDBBuilder.getContentDB();
        _weightingDB = new TroveWeightingDB((TroveWeightingDB) index.getWeightingDB());
        if (index.getClassificationDB() instanceof TroveClassificationDB)
            _classificationDBBuilder = new TroveClassificationDBBuilder((TroveClassificationDB) index.getClassificationDB());
        else if (index.getClassificationDB() instanceof TroveClassificationILDB)
            _classificationDBBuilder = new TroveClassificationILDBBuilder((TroveClassificationILDB) index.getClassificationDB());
        else
            _classificationDBBuilder = new TroveClassificationFullDBBuilder((TroveClassificationFullDB) index.getClassificationDB());
        _classificationDB = _classificationDBBuilder.getClassificationDB();
    }

    public int addDocument(String documentName, String[] features,
                           String[] categories) {
        int document = _documentsDBBuilder.addDocument(documentName);
        for (int i = 0; i < categories.length; ++i) {
            short category = _categoriesDB.getCategory(categories[i]);
            _classificationDBBuilder.setDocumentCategory(document, category);
        }
        for (int i = 0; i < features.length; ++i) {
            int feature = _featuresDB.getFeature(features[i]);
            if (feature >= 0) {
                int freq = 0;
                try {
                    freq = _contentDB.getDocumentFeatureFrequency(document, feature);
                } catch (Exception e) {
                }
                _contentDBBuilder.setDocumentFeatureFrequency(document, feature, freq + 1);
            } else
                _contentDBBuilder.setDocumentFeatureFrequency(document, feature, 1);
        }

        if (features.length == 0) {
            // Force an empty document.
            _contentDBBuilder.setDocumentFeatureFrequency(document, -1, 0);
        }
        return document;
    }


    public int addDocument(String documentName, String[] features,
                           CorpusCategory[] categories) {
        int document = _documentsDBBuilder.addDocument(documentName);
        for (int i = 0; i < categories.length; ++i) {
            short category = _categoriesDB.getCategory(categories[i].name);
            _classificationDBBuilder.setDocumentCategory(document, category, categories[i].primary);
        }
        for (int i = 0; i < features.length; ++i) {
            int feature = _featuresDB.getFeature(features[i]);
            if (feature >= 0) {
                int freq = 0;
                try {
                    freq = _contentDB.getDocumentFeatureFrequency(document, feature);
                } catch (Exception e) {
                }
                _contentDBBuilder.setDocumentFeatureFrequency(document, feature, freq + 1);
            } else
                _contentDBBuilder.setDocumentFeatureFrequency(document, feature, 1);
        }

        if (features.length == 0) {
            // Force an empty document.
            _contentDBBuilder.setDocumentFeatureFrequency(document, -1, 0);
        }
        return document;
    }

    public IIndex getIndex() {
        return this;
    }


    @Override
    public void addDocumentFeatures(int docID, String[] featureNames,
                                    int[] occurrences) {
        if (!_documentsDBBuilder.getDocumentDB().isValidDocument(docID))
            throw new IllegalArgumentException("The specified document ID is invalid: " + docID);

        for (int i = 0; i < featureNames.length; ++i) {
            if (featureNames[i].isEmpty())
                continue;
            int feature = _featuresDB.getFeature(featureNames[i]);
            if (feature >= 0) {
                int freq = 0;
                try {
                    freq = _contentDB.getDocumentFeatureFrequency(docID, feature);
                } catch (Exception e) {
                }
                _contentDBBuilder.setDocumentFeatureFrequency(docID, feature, freq + occurrences[i]);
            }
        }

    }
}
