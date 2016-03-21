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

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TShortArrayList;
import gnu.trove.TShortHashSet;
import gnu.trove.TShortIntHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.utils.iterators.EmptyIntIterator;
import it.cnr.jatecs.utils.iterators.FilteredIntIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class GenericIndex implements IIndex {

    protected IFeatureDB _featuresDB;
    protected IDocumentDB _documentsDB;
    protected ICategoryDB _categoriesDB;
    protected IDomainDB _domainDB;
    protected IContentDB _contentDB;
    protected IWeightingDB _weightingDB;
    protected IClassificationDB _classificationDB;
    protected TIntObjectHashMap<TShortIntHashMap> _featCatDocCountMap;
    protected String _name;
    private boolean _cachingEnabled;
    public GenericIndex(String name, IFeatureDB featuresDB,
                        IDocumentDB documentsDB, ICategoryDB categoriesDB,
                        IDomainDB domainDB, IContentDB contentDB, IWeightingDB weightingDB,
                        IClassificationDB classificationDB) {
        this(featuresDB, documentsDB, categoriesDB, domainDB, contentDB,
                weightingDB, classificationDB);
        _name = name;
    }

    public GenericIndex(IFeatureDB featuresDB, IDocumentDB documentsDB,
                        ICategoryDB categoriesDB, IDomainDB domainDB, IContentDB contentDB,
                        IWeightingDB weightingDB, IClassificationDB classificationDB) {
        super();
        _name = "generic";
        _categoriesDB = categoriesDB;
        _classificationDB = classificationDB;
        _contentDB = contentDB;
        _weightingDB = weightingDB;
        _documentsDB = documentsDB;
        _domainDB = domainDB;
        _featuresDB = featuresDB;
        _featCatDocCountMap = new TIntObjectHashMap<TShortIntHashMap>();
        _cachingEnabled = true;
    }

    public void enableCaching() {
        _cachingEnabled = true;
    }

    public void disableCaching() {
        _cachingEnabled = false;
    }

    public boolean isCachingEnabled() {
        return _cachingEnabled;
    }

    public IFeatureDB getFeatureDB() {
        return _featuresDB;
    }

    public IDocumentDB getDocumentDB() {
        return _documentsDB;
    }

    public ICategoryDB getCategoryDB() {
        return _categoriesDB;
    }

    public IDomainDB getDomainDB() {
        return _domainDB;
    }

    public IContentDB getContentDB() {
        return _contentDB;
    }

    public IWeightingDB getWeightingDB() {
        return _weightingDB;
    }

    public IClassificationDB getClassificationDB() {
        return _classificationDB;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public int getDocumentLength(int document, short category) {
        if (_domainDB.hasLocalRepresentation()) {
            IIntIterator featIt = _contentDB.getDocumentFeatures(document);
            int length = 0;
            while (featIt.hasNext()) {
                int feature = featIt.next();
                if (_domainDB.hasCategoryFeature(category, feature))
                    length += _contentDB.getDocumentFeatureFrequency(document,
                            feature);
            }
            return length;
        } else
            return _contentDB.getDocumentLength(document);
    }

    public int getDocumentFeaturesCount(int document, short category) {
        if (_domainDB.hasLocalRepresentation()) {
            IIntIterator featIt = _contentDB.getDocumentFeatures(document);
            int count = 0;
            while (featIt.hasNext()) {
                if (_domainDB.hasCategoryFeature(category, featIt.next()))
                    ++count;
            }
            return count;
        } else
            return _contentDB.getDocumentFeaturesCount(document);
    }

    public IIntIterator getDocumentFeatures(int document, short category) {
        if (_domainDB.hasLocalRepresentation())
            return new FilteredIntIterator(
                    _contentDB.getDocumentFeatures(document),
                    _domainDB.getCategoryFeatures(category), false);
        else
            return _contentDB.getDocumentFeatures(document);
    }

    public boolean hasDocumentFeature(int document, int feature, short category) {
        return _domainDB.hasCategoryFeature(category, feature)
                && _contentDB.hasDocumentFeature(document, feature);
    }

    public int getDocumentFeatureFrequency(int document, int feature,
                                           short category) {
        if (_domainDB.hasCategoryFeature(category, feature))
            return _contentDB.getDocumentFeatureFrequency(document, feature);
        else
            return 0;
    }

    public int getFeatureDocumentsCount(int feature, short category) {
        if (_domainDB.hasCategoryFeature(category, feature))
            return _contentDB.getFeatureDocumentsCount(feature);
        else
            return 0;
    }

    public IIntIterator getFeatureDocuments(int feature, short category) {
        if (_domainDB.hasCategoryFeature(category, feature))
            return _contentDB.getFeatureDocuments(feature);
        else
            return new EmptyIntIterator();
    }

    public double getDocumentFeatureWeight(int document, int feature,
                                           short category) {
        if (_domainDB.hasCategoryFeature(category, feature))
            return _weightingDB.getDocumentFeatureWeight(document, feature);
        else
            return 0.0;
    }

    public int getFeatureCategoryDocumentsCount(int featureID, short categoryID) {
        if (!_featCatDocCountMap.containsKey(featureID) && _cachingEnabled) {
            _featCatDocCountMap.put(featureID, new TShortIntHashMap());
        }

        TShortIntHashMap catDocCountMap = (TShortIntHashMap) _featCatDocCountMap
                .get(featureID);

        if (catDocCountMap == null || !catDocCountMap.containsKey(categoryID)) {
            IIntIterator it = getFeatureCategoryDocuments(featureID, categoryID);

            int count = 0;
            while (it.hasNext()) {
                it.next();
                count++;
            }
            if (catDocCountMap != null && _cachingEnabled) {
                catDocCountMap.put(categoryID, count);
            }
            return count;
        }

        return catDocCountMap.get(categoryID);
    }

    public IIntIterator getFeatureCategoryDocuments(int featureID,
                                                    short categoryID) {
        IIntIterator itCats = getClassificationDB().getCategoryDocuments(
                categoryID);
        IIntIterator itFeats = getContentDB().getFeatureDocuments(featureID);

        FilteredIntIterator it = new FilteredIntIterator(itCats, itFeats, false);

        return it;
    }

    public void removeFeatures(IIntIterator removedFeatures) {
        if (!removedFeatures.hasNext())
            return;

        _featCatDocCountMap.clear();

        removedFeatures.begin();
        _weightingDB.removeFeatures(removedFeatures);
        removedFeatures.begin();
        _contentDB.removeFeatures(removedFeatures);
        removedFeatures.begin();
        _domainDB.removeFeatures(removedFeatures);
        removedFeatures.begin();
        _featuresDB.removeFeatures(removedFeatures);
    }

    public void removeCategories(IShortIterator removedCategories) {
        if (!removedCategories.hasNext())
            return;

        _featCatDocCountMap.clear();

        removedCategories.begin();
        _classificationDB.removeCategories(removedCategories);
        removedCategories.begin();
        _domainDB.removeCategories(removedCategories);
        removedCategories.begin();
        _categoriesDB.removeCategories(removedCategories);
    }

    public void removeDocuments(IIntIterator removedDocuments,
                                boolean removedUnusedFeatures) {
        if (!removedDocuments.hasNext())
            return;

        _featCatDocCountMap.clear();

        removedDocuments.begin();
        _weightingDB.removeDocuments(removedDocuments);
        removedDocuments.begin();
        _contentDB.removeDocuments(removedDocuments);
        removedDocuments.begin();
        _classificationDB.removeDocuments(removedDocuments);
        removedDocuments.begin();
        _documentsDB.removeDocuments(removedDocuments);

        if (removedUnusedFeatures) {
            IIntIterator unusedFeatures = _contentDB.getUnusedFeatures();
            _domainDB.removeFeatures(unusedFeatures);
            unusedFeatures.begin();
            _contentDB.removeFeatures(unusedFeatures);
            unusedFeatures.begin();
            _featuresDB.removeFeatures(unusedFeatures);
        }
    }

    public IIndex cloneIndex() {
        ICategoryDB categoriesDB = _categoriesDB.cloneDB();
        IDocumentDB documentsDB = _documentsDB.cloneDB();
        IFeatureDB featuresDB = _featuresDB.cloneDB();
        IDomainDB domainDB = _domainDB.cloneDB(categoriesDB, featuresDB);
        IContentDB contentDB = _contentDB.cloneDB(documentsDB, featuresDB);
        IWeightingDB weightingDB = _weightingDB.cloneDB(contentDB);
        IClassificationDB classificationDB = _classificationDB.cloneDB(
                categoriesDB, documentsDB);
        String name = new String(_name);
        GenericIndex index = new GenericIndex(name, featuresDB, documentsDB,
                categoriesDB, domainDB, contentDB, weightingDB,
                classificationDB);
        return index;
    }

    @Override
    public TShortHashSet cleanCategories() {
        TShortHashSet negativeCategories = new TShortHashSet(
                _categoriesDB.getCategoriesCount());
        for (IShortIterator catIt = _categoriesDB.getCategories(); catIt
                .hasNext(); ) {
            negativeCategories.add(catIt.next());
        }
        for (IIntIterator docIt = _documentsDB.getDocuments(); docIt.hasNext(); ) {
            for (IShortIterator catIt = _classificationDB
                    .getDocumentCategories(docIt.next()); catIt.hasNext(); ) {
                negativeCategories.remove(catIt.next());
            }
        }
        TShortArrayList categoriesArray = new TShortArrayList(
                negativeCategories.toArray());
        categoriesArray.sort();
        removeCategories(new TShortArrayListIterator(categoriesArray));
        return negativeCategories;
    }

}
