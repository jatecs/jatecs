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

public class TroveContentFullDB implements IContentDB {

    protected String _name;
    protected IFeatureDB _featuresDB;
    protected IDocumentDB _documentsDB;
    protected Vector<TIntArrayList> _documentsFeatures;
    protected Vector<TIntArrayList> _documentsFrequencies;
    protected Vector<TIntArrayList> _featuresDocuments;
    protected Vector<TIntArrayList> _featuresFrequencies;
    protected TIntIntHashMap _documentLenghts;

    public TroveContentFullDB(IDocumentDB documentsDB, IFeatureDB featuresDB) {
        super();
        _documentsDB = documentsDB;
        _featuresDB = featuresDB;

        int sizeDocs = documentsDB.getDocumentsCount();
        _documentsFeatures = new Vector<TIntArrayList>(sizeDocs);
        _documentsFrequencies = new Vector<TIntArrayList>(sizeDocs);
        for (int i = 0; i < sizeDocs; ++i) {
            _documentsFeatures.add(new TIntArrayList());
            _documentsFrequencies.add(new TIntArrayList());
        }

        int sizeFeats = featuresDB.getFeaturesCount();
        _featuresDocuments = new Vector<TIntArrayList>(sizeFeats);
        _featuresFrequencies = new Vector<TIntArrayList>(sizeFeats);
        for (int i = 0; i < sizeFeats; ++i) {
            _featuresDocuments.add(new TIntArrayList());
            _featuresFrequencies.add(new TIntArrayList());
        }

        _documentLenghts = new TIntIntHashMap();

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
            if (document < _documentsFrequencies.size()) {
                TIntArrayList frequencies = _documentsFrequencies.get(document);
                int length = 0;
                for (int i = 0; i < frequencies.size(); ++i)
                    length += frequencies.getQuick(i);
                _documentLenghts.put(document, length);
                return length;
            } else
                return 0;
        }
    }

    public int getDocumentFeaturesCount(int document) {
        if (document < _documentsFeatures.size())
            return _documentsFeatures.get(document).size();
        else
            return 0;
    }

    public IIntIterator getDocumentFeatures(int document) {
        if (document < _documentsFeatures.size())
            return new TIntArrayListIterator(_documentsFeatures.get(document));
        else
            return new EmptyIntIterator();
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

    public boolean hasDocumentFeature(int document, int feature) {
        return _documentsFeatures.get(document).binarySearch(feature) >= 0;
    }

    public int getDocumentFeatureFrequency(int document, int feature) {
        if (document < _documentsFeatures.size()) {
            int pos = _documentsFeatures.get(document).binarySearch(feature);
            if (pos >= 0)
                return _documentsFrequencies.get(document).getQuick(pos);
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

    public void removeDocuments(IIntIterator removedDocuments) {
        int shift = 0;
        while (removedDocuments.hasNext()) {
            int document = removedDocuments.next() - shift;
            _documentsFeatures.remove(document);
            _documentsFrequencies.remove(document);
            _documentLenghts.remove(document);
            ++shift;
        }
        _documentLenghts.clear();
        for (int i = 0; i < _featuresDocuments.size(); ++i) {
            TIntArrayList docs = _featuresDocuments.get(i);
            TIntArrayList freqs = _featuresFrequencies.get(i);
            int j = 0;
            shift = 0;
            int doc;
            int rem;

            removedDocuments.begin();

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
        }

        removedDocuments.begin();
        while (removedDocuments.hasNext()) {
            int document = removedDocuments.next();
            _documentLenghts.remove(document);
        }
    }

    public void removeFeatures(IIntIterator removedFeatures) {
        int shift = 0;
        while (removedFeatures.hasNext()) {
            int feature = removedFeatures.next() - shift;
            _featuresDocuments.remove(feature);
            _featuresFrequencies.remove(feature);
            ++shift;
        }
        removedFeatures.begin();
        for (int i = 0; i < _documentsFeatures.size(); ++i) {
            TIntArrayList feats = _documentsFeatures.get(i);
            TIntArrayList freqs = _documentsFrequencies.get(i);
            int j = 0;
            shift = 0;
            int feat;
            int rem;
            if (j < feats.size() && removedFeatures.hasNext()) {
                feat = feats.getQuick(j);
                rem = removedFeatures.next();

                while (true) {
                    if (feat == rem) {
                        feats.remove(j);
                        freqs.remove(j);
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
                ++shift;
            }
            while (j < feats.size()) {
                feats.setQuick(j, feats.getQuick(j) - shift);
                ++j;
            }
            removedFeatures.begin();
        }
        _documentLenghts.clear();
    }

    public IContentDB cloneDB(IDocumentDB docDB, IFeatureDB featDB) {
        TroveContentFullDB contentDB = new TroveContentFullDB(docDB, featDB);
        contentDB._name = new String(_name);

        contentDB._documentsFeatures = new Vector<TIntArrayList>(
                _documentsFeatures.size());
        for (int i = 0; i < _documentsFeatures.size(); ++i)
            contentDB._documentsFeatures.add((TIntArrayList) _documentsFeatures
                    .get(i).clone());

        contentDB._documentsFrequencies = new Vector<TIntArrayList>(
                _documentsFrequencies.size());
        for (int i = 0; i < _documentsFrequencies.size(); ++i)
            contentDB._documentsFrequencies
                    .add((TIntArrayList) _documentsFrequencies.get(i).clone());

        contentDB._documentLenghts = (TIntIntHashMap) _documentLenghts.clone();

        contentDB._featuresDocuments = new Vector<TIntArrayList>(
                _featuresDocuments.size());
        for (int i = 0; i < _featuresDocuments.size(); ++i)
            contentDB._featuresDocuments.add((TIntArrayList) _featuresDocuments
                    .get(i).clone());

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
