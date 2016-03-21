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

import gnu.trove.TIntDoubleHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IWeightingDB;
import it.cnr.jatecs.indexes.DB.interfaces.IWeightingDBBuilder;

public class TroveWeightingDBBuilder implements IWeightingDBBuilder {

    protected TroveWeightingDB _weightingDB;

    public TroveWeightingDBBuilder(IContentDB contentDB) {
        super();
        _weightingDB = new TroveWeightingDB(contentDB);
    }

    // This builder can be also used to modify an exiting weighting
    // by using this constructor and passing an already existing
    // TroveWeightingDB
    public TroveWeightingDBBuilder(TroveWeightingDB weightingDB) {
        _weightingDB = weightingDB;
    }

    public void setDocumentFeatureWeight(int document, int feature,
                                         double weight) {
        if (_weightingDB.getContentDB().hasDocumentFeature(document, feature)) {
            while (document >= _weightingDB._documentsWeights.size())
                _weightingDB._documentsWeights.add(new TIntDoubleHashMap());
            _weightingDB._documentsWeights.get(document).put(feature, weight);
        }
    }

    public IWeightingDB getWeightingDB() {
        return _weightingDB;
    }
}
