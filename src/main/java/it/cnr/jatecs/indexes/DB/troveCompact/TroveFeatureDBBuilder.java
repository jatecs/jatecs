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

import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDBBuilder;

public class TroveFeatureDBBuilder implements
        IFeatureDBBuilder {

    protected TroveFeaturesDB _featuresDB;


    public TroveFeatureDBBuilder() {
        super();
        _featuresDB = new TroveFeaturesDB();
    }


    public TroveFeatureDBBuilder(TroveFeaturesDB db) {
        super();
        _featuresDB = db;
    }

    public int addFeature(String featureName) {
        if (_featuresDB._featuresMap.containsKey(featureName))
            throw new RuntimeException("Duplicate feature: " + featureName);
        int feature = _featuresDB._featuresMap.size();
        _featuresDB._featuresMap.put(featureName, feature);
        _featuresDB._featuresRMap.add(featureName);
        return feature;
    }

    public IFeatureDB getFeatureDB() {
        return _featuresDB;
    }

}
