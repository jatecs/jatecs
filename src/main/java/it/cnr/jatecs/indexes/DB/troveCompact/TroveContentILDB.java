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
import gnu.trove.TIntIntHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.utils.iterators.EmptyIntIterator;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.Vector;

public class TroveContentILDB implements IContentDB {

    protected String _name;
    protected IFeatureDB _featuresDB;
    protected IDocumentDB _documentsDB;
    protected Vector<TIntArrayList> _featuresDocuments;
    protected Vector<TIntArrayList> _featuresFrequencies;
    protected TIntIntHashMap _documentLenghts;
    protected TIntIntHashMap _documentFeaturesCount;
    public TroveContentILDB(IDocumentDB documentsDB, IFeatureDB featuresDB) {
        super();
        _documentsDB = documentsDB;
        _featuresDB = featuresDB;

        int size = featuresDB.getFeaturesCount();
        _featuresDocuments = new Vector<TIntArrayList>(size);
        _featuresFrequencies = new Vector<TIntArrayList>(size);
        for (int i = 0; i < size; ++i) {
            _featuresDocuments.add(new TIntArrayList());
            _featuresFrequencies.add(new TIntArrayList());
        }

        _documentLenghts = new TIntIntHashMap();
        _documentFeaturesCount = new TIntIntHashMap();
        _name = "generic";
    }

    public IFeatureDB getFeatureDB() {
        return _featuresDB;
    }

    public IDocumentDB getDocumentDB() {
        return _documentsDB;
    }

    public int getDocumentLength(int document) {
        if (_documentLenghts.containsKey(document))
            return _documentLenghts.get(document);
        else {
            IIntIterator featIt = _featuresDB.getFeatures();
            int length = 0;
            while (featIt.hasNext()) {
                int feature = featIt.next();
                int pos = _featuresDocuments.get(feature)
                        .binarySearch(document);
                if (pos >= 0)
                    length += _featuresFrequencies.get(feature).getQuick(pos);
            }
            _documentLenghts.put(document, length);
            return length;
        }
    }

    public int getFeatureDocumentsCount(int feature) {
        if (feature < _featuresDocuments.size())
            return _featuresDocuments.get(feature).size();
        else
            return 0;
    }

    public IIntIterator getFeatureDocuments(int feature) {
        if (feature < _featuresDocuments.size())
            return new TIntArrayListIterator(_featuresDocuments.get(feature));
        else
            return new EmptyIntIterator();
    }

    public int getDocumentFeaturesCount(int document) {
        if (_documentFeaturesCount.containsKey(document))
            return _documentFeaturesCount.get(document);
        else {
            IIntIterator featIt = _featuresDB.getFeatures();
            int count = 0;
            while (featIt.hasNext()) {
                if (hasDocumentFeature(document, featIt.next()))
                    ++count;
            }
            _documentFeaturesCount.put(document, count);
            return count;
        }
    }

    public IIntIterator getDocumentFeatures(int document) {
        IIntIterator featIt = _featuresDB.getFeatures();
        int feature = 0;
        TIntArrayList features = new TIntArrayList();
        while (featIt.hasNext()) {
            feature = featIt.next();
            if (hasDocumentFeature(document, feature))
                features.add(feature);
        }
        return new TIntArrayListIterator(features);
    }

    public boolean hasDocumentFeature(int document, int feature) {
        if (feature < _featuresDocuments.size())
            return _featuresDocuments.get(feature).binarySearch(document) >= 0;
        else
            return false;
    }

    public int getDocumentFeatureFrequency(int document, int feature) {
        if (feature < _featuresDocuments.size()) {
            int pos = _featuresDocuments.get(feature).binarySearch(document);
            if (pos >= 0)
                return _featuresFrequencies.get(feature).getQuick(pos);
            else
                return 0;
        } else
            return 0;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public void removeFeatures(IIntIterator removedFeatures) {
        int shift = 0;
        while (removedFeatures.hasNext()) {
            int feature = removedFeatures.next() - shift;
            _featuresDocuments.remove(feature);
            _featuresFrequencies.remove(feature);
            ++shift;
        }
        _documentFeaturesCount.clear();
        _documentLenghts.clear();
    }

    public void removeDocuments(IIntIterator removedDocuments) {
        for (int i = 0; i < _featuresDocuments.size(); ++i) {
            TIntArrayList docs = _featuresDocuments.get(i);
            TIntArrayList freqs = _featuresFrequencies.get(i);
            int j = 0;
            int shift = 0;
            int doc;
            int rem;
            if (j < docs.size() && removedDocuments.hasNext()) {
                doc = docs.getQuick(j);
                rem = removedDocuments.next();

                while (true) {
                    if (doc == rem) {
                        docs.remove(j);
                        freqs.remove(j);
                        if (j < docs.size() && removedDocuments.hasNext()) {
                            doc = docs.getQuick(j);
                            rem = removedDocuments.next();
                            ++shift;
                        } else
                            break;
                    } else if (doc > rem) {
                        if (removedDocuments.hasNext()) {
                            rem = removedDocuments.next();
                            ++shift;
                        } else
                            break;
                    } else {
                        docs.setQuick(j, doc - shift);
                        ++j;
                        if (j < docs.size())
                            doc = docs.getQuick(j);
                        else
                            break;
                    }
                }
                ++shift;
            }
            while (j < docs.size()) {
                docs.setQuick(j, docs.getQuick(j) - shift);
                ++j;
            }
            removedDocuments.begin();
        }
        removedDocuments.begin();
        while (removedDocuments.hasNext()) {
            int document = removedDocuments.next();
            _documentFeaturesCount.remove(document);
            _documentLenghts.remove(document);
        }

    }

    public IContentDB cloneDB(IDocumentDB documentsDB, IFeatureDB featuresDB) {
        TroveContentILDB contentDB = new TroveContentILDB(documentsDB,
                featuresDB);
        contentDB._name = new String(_name);

        contentDB._featuresDocuments = new Vector<TIntArrayList>(
                _featuresDocuments.size());
        for (int i = 0; i < _featuresDocuments.size(); ++i)
            contentDB._featuresDocuments.add((TIntArrayList) _featuresDocuments
                    .get(i).clone());

        contentDB._documentFeaturesCount = (TIntIntHashMap) _documentFeaturesCount
                .clone();
        contentDB._documentLenghts = (TIntIntHashMap) _documentLenghts.clone();

        contentDB._featuresFrequencies = new Vector<TIntArrayList>(
                _featuresFrequencies.size());
        for (int i = 0; i < _featuresFrequencies.size(); ++i)
            contentDB._featuresFrequencies
                    .add((TIntArrayList) _featuresFrequencies.get(i).clone());

        return contentDB;
    }

    public IIntIterator getUnusedFeatures() {
        TIntArrayList zeroFeatures = new TIntArrayList();
        IIntIterator it = _featuresDB.getFeatures();
        while (it.hasNext()) {
            int feat = it.next();
            if (getFeatureDocumentsCount(feat) == 0)
                zeroFeatures.add(feat);
        }

        return new TIntArrayListIterator(zeroFeatures);
    }
}
