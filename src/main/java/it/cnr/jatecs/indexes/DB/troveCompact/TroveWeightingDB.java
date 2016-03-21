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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntDoubleIterator;
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IWeightingDB;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.Vector;

public class TroveWeightingDB implements IWeightingDB {

    protected String _name;
    protected IContentDB _contentDB;
    protected Vector<TIntDoubleHashMap> _documentsWeights;

    public TroveWeightingDB(IContentDB contentDB) {
        super();
        _contentDB = contentDB;
        int size = contentDB.getDocumentDB().getDocumentsCount();
        _documentsWeights = new Vector<TIntDoubleHashMap>(size);
        for (int i = 0; i < size; ++i) {
            _documentsWeights.add(new TIntDoubleHashMap());
        }

        _name = "generic";
    }

    public TroveWeightingDB(TroveWeightingDB weightingDB) {
        _contentDB = weightingDB._contentDB;
        _documentsWeights = weightingDB._documentsWeights;
        _name = weightingDB._name;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public IContentDB getContentDB() {
        return _contentDB;
    }

    public double getDefaultWeight() {
        return 1.0;
    }

    public double getNoWeight() {
        return 0.0;
    }

    public double getDocumentFeatureWeight(int document, int feature) {
        if (_contentDB.hasDocumentFeature(document, feature)) {
            if (document < _documentsWeights.size()) {
                TIntDoubleHashMap weights = _documentsWeights.get(document);
                if (weights.containsKey(feature))
                    return weights.get(feature);
                else
                    return 1.0;
            } else
                return 1.0;
        } else
            return 0.0;
    }

    public void removeDocuments(IIntIterator removedDocuments) {
        if (_documentsWeights.size() == 0)
            return;

        int shift = 0;
        while (removedDocuments.hasNext()) {

            _documentsWeights.remove(removedDocuments.next() - shift);
            ++shift;
        }
    }

    public void removeFeatures(IIntIterator removedFeatures) {
        for (int i = 0; i < _documentsWeights.size(); ++i) {
            TIntDoubleHashMap weigs = _documentsWeights.get(i);
            TIntArrayList feats = new TIntArrayList(weigs.size());
            TDoubleArrayList weigths = new TDoubleArrayList(weigs.size());
            TIntDoubleIterator wit = weigs.iterator();
            while (wit.hasNext()) {
                wit.advance();
                feats.add(wit.key());
                weigths.add(wit.value());
            }
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
                        weigths.remove(j);
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

            weigs.clear();
            for (j = 0; j < feats.size(); ++j)
                weigs.put(feats.getQuick(j), weigths.getQuick(j));

            removedFeatures.begin();
        }
    }

    public IWeightingDB cloneDB(IContentDB contentDB) {
        TroveWeightingDB weightingDB = new TroveWeightingDB(contentDB);
        weightingDB._name = new String(_name);

        weightingDB._documentsWeights = new Vector<TIntDoubleHashMap>(
                _documentsWeights.size());
        for (int i = 0; i < _documentsWeights.size(); ++i)
            weightingDB._documentsWeights
                    .add((TIntDoubleHashMap) _documentsWeights.get(i).clone());

        return weightingDB;
    }

}
