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
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IContentDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;

public class TroveContentFullDBBuilder implements IContentDBBuilder {

    protected TroveContentFullDB _contentDB;

    public TroveContentFullDBBuilder(IDocumentDB documentsDB,
                                     IFeatureDB featuresDB) {
        super();
        _contentDB = new TroveContentFullDB(documentsDB, featuresDB);
    }

    public TroveContentFullDBBuilder(TroveContentFullDB contentDB) {
        super();
        _contentDB = contentDB;
    }

    public void setDocumentFeatureFrequency(int document, int feature,
                                            int frequency) {
        if (document >= 0) {
            int size = _contentDB._documentsFeatures.size();
            if (document >= size) {
                for (int i = size; i <= document; ++i) {
                    _contentDB._documentsFeatures.add(new TIntArrayList());
                    _contentDB._documentsFrequencies.add(new TIntArrayList());
                }
            }
            if (feature >= 0) {
                TIntArrayList feats = _contentDB._documentsFeatures
                        .get(document);
                TIntArrayList freqs = _contentDB._documentsFrequencies
                        .get(document);
                int pos = feats.binarySearch(feature);
                if (pos < 0 && frequency > 0) {
                    pos = -pos - 1;
                    feats.insert(pos, feature);
                    freqs.insert(pos, frequency);
                } else {
                    if (frequency > 0) {
                        freqs.setQuick(pos, frequency);
                    } else {
                        freqs.remove(pos);
                        feats.remove(pos);
                    }
                }
            }
        }
        if (feature >= 0) {
            int size = _contentDB._featuresDocuments.size();
            if (feature >= size) {
                for (int i = size; i <= feature; ++i) {
                    _contentDB._featuresDocuments.add(new TIntArrayList());
                    _contentDB._featuresFrequencies.add(new TIntArrayList());
                }
            }
            if (document >= 0) {
                TIntArrayList docs = _contentDB._featuresDocuments.get(feature);
                TIntArrayList freqs = _contentDB._featuresFrequencies
                        .get(feature);
                int pos = docs.binarySearch(document);
                if (pos < 0 && frequency > 0) {
                    pos = -pos - 1;
                    docs.insert(pos, document);
                    freqs.insert(pos, frequency);
                } else {
                    if (frequency > 0) {
                        freqs.setQuick(pos, frequency);
                    } else {
                        docs.remove(pos);
                        freqs.remove(pos);
                    }
                }
            }
        }
    }

    public IContentDB getContentDB() {
        return _contentDB;
    }
}
