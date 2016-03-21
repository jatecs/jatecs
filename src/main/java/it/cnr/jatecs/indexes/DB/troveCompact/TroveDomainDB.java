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

import gnu.trove.TIntArrayList;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDomainDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.utils.iterators.FilteredIntIterator;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class TroveDomainDB implements IDomainDB {

    protected String _name;
    protected boolean _hasLocalRepresentation;
    protected Vector<TIntArrayList> _categoriesFeatures;
    protected ICategoryDB _categoriesDB;
    protected IFeatureDB _featuresDB;

    public TroveDomainDB(ICategoryDB categoriesDB, IFeatureDB featuresDB) {
        super();
        _hasLocalRepresentation = false;
        int size = categoriesDB.getCategoriesCount();
        _categoriesFeatures = new Vector<TIntArrayList>(size);
        for (int i = 0; i < size; ++i)
            _categoriesFeatures.add(new TIntArrayList());
        _name = "generic";
        _categoriesDB = categoriesDB;
        _featuresDB = featuresDB;
    }
    public TroveDomainDB(TroveDomainDB domainDB) {
        _hasLocalRepresentation = domainDB._hasLocalRepresentation;
        _categoriesFeatures = domainDB._categoriesFeatures;
        _featuresDB = domainDB._featuresDB;
        _categoriesDB = domainDB._categoriesDB;
        _name = domainDB._name;
    }

    public ICategoryDB getCategoryDB() {
        return _categoriesDB;
    }

    public IFeatureDB getFeatureDB() {
        return _featuresDB;
    }

    public boolean hasLocalRepresentation() {
        return _hasLocalRepresentation;
    }

    public int getCategoryFeaturesCount(short category) {
        if (_hasLocalRepresentation)
            return _featuresDB.getFeaturesCount()
                    - _categoriesFeatures.get(category).size();
        else
            return _featuresDB.getFeaturesCount();
    }

    public IIntIterator getCategoryFeatures(short category) {
        if (_hasLocalRepresentation)
            return new FilteredIntIterator(
                    _featuresDB.getFeatures(),
                    new TIntArrayListIterator(_categoriesFeatures.get(category)),
                    true);
        else
            return _featuresDB.getFeatures();
    }

    public boolean hasCategoryFeature(short category, int feature) {
        if (_hasLocalRepresentation)
            return _categoriesFeatures.get(category).binarySearch(feature) < 0;
        else
            return true;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public void removeFeatures(IIntIterator removedFeatures) {
        for (int i = 0; i < _categoriesFeatures.size(); ++i) {
            TIntArrayList feats = _categoriesFeatures.get(i);
            int j = 0;
            int shift = 0;
            int feat;
            int rem;
            if (j < feats.size() && removedFeatures.hasNext()) {
                feat = feats.getQuick(j);
                rem = removedFeatures.next();

                while (true) {
                    if (feat == rem) {
                        feats.remove(j);
                        if (j < feats.size() && removedFeatures.hasNext()) {
                            feat = feats.getQuick(j);
                            rem = removedFeatures.next();
                            ++shift;
                        } else
                            break;
                    } else if (feat > rem) {
                        if (removedFeatures.hasNext()) {
                            rem = removedFeatures.next();
                            ++shift;
                        } else
                            break;
                    } else {
                        feats.setQuick(j, feat - shift);
                        ++j;
                        if (j < feats.size())
                            feat = feats.getQuick(j);
                        else
                            break;
                    }
                }
            }
            while (j < feats.size()) {
                feats.setQuick(j, feats.getQuick(j) - shift);
                ++j;
            }
            removedFeatures.begin();
        }
    }

    public void removeCategoryFeatures(short category,
                                       IIntIterator removedFeatures) {
        TIntArrayList feats = _categoriesFeatures.get(category);
        while (removedFeatures.hasNext()) {
            int feature = removedFeatures.next();
            if (feats.binarySearch(feature) < 0)
                feats.add(feature);
        }
        feats.sort();
        _hasLocalRepresentation = _hasLocalRepresentation || feats.size() > 0;
    }

    public void removeCategories(IShortIterator removedCategories) {
        int shift = 0;
        while (removedCategories.hasNext()) {
            _categoriesFeatures.remove(removedCategories.next() - shift);
            ++shift;
        }
        _hasLocalRepresentation = false;
        for (int i = 0; i < _categoriesFeatures.size(); ++i) {
            if (_categoriesFeatures.get(i).size() != 0) {
                _hasLocalRepresentation = true;
                break;
            }
        }
    }

    public IDomainDB cloneDB(ICategoryDB categoriesDB, IFeatureDB featuresDB) {
        TroveDomainDB domainDB = new TroveDomainDB(categoriesDB, featuresDB);
        domainDB._name = new String(_name);
        domainDB._hasLocalRepresentation = _hasLocalRepresentation;

        domainDB._categoriesFeatures = new Vector<TIntArrayList>(
                _categoriesFeatures.size());
        for (int i = 0; i < _categoriesFeatures.size(); ++i)
            domainDB._categoriesFeatures
                    .add((TIntArrayList) _categoriesFeatures.get(i).clone());

        return domainDB;
    }

}
