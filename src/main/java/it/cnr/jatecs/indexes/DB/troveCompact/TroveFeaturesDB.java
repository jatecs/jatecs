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

import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.utils.iterators.RangeIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.Vector;

public class TroveFeaturesDB implements IFeatureDB {

    protected TObjectIntHashMap<String> _featuresMap;
    protected Vector<String> _featuresRMap;
    protected String _name;

    public TroveFeaturesDB() {
        super();
        _featuresMap = new TObjectIntHashMap<String>();
        _featuresRMap = new Vector<String>();
        _name = "generic";
    }

    public String getFeatureName(int feature) {
        return _featuresRMap.get(feature);
    }

    public int getFeature(String featureName) {
        if (_featuresMap.containsKey(featureName))
            return _featuresMap.get(featureName);
        else
            return -1;
    }

    public int getFeaturesCount() {
        return _featuresRMap.size();
    }

    public IIntIterator getFeatures() {
        return new RangeIntIterator(0, _featuresRMap.size());
    }

    public boolean isValidFeature(int feature) {
        return (feature >= 0) ? ((feature < _featuresRMap.size()) ? true
                : false) : false;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public void removeFeatures(IIntIterator removedFeatures) {
        int shift = 0;
        int lastGoodFeature = 0;
        int totalFeatures = _featuresMap.size();
        TIntIntHashMap featuresRemap = new TIntIntHashMap();
        while (removedFeatures.hasNext()) {
            int removedFeature = removedFeatures.next();
            while (lastGoodFeature < removedFeature) {
                featuresRemap.put(lastGoodFeature, lastGoodFeature - shift);
                ++lastGoodFeature;
            }
            lastGoodFeature = removedFeature + 1;
            int removedFeaturePosition = removedFeature - shift;
            _featuresMap.remove(_featuresRMap.get(removedFeaturePosition));
            _featuresRMap.remove(removedFeaturePosition);
            ++shift;
        }

        while (lastGoodFeature < totalFeatures) {
            featuresRemap.put(lastGoodFeature, lastGoodFeature - shift);
            ++lastGoodFeature;
        }

        TObjectIntIterator<String> mapIter = _featuresMap.iterator();
        while (mapIter.hasNext()) {
            mapIter.advance();
            int value = mapIter.value();
            int newvalue = featuresRemap.get(value);
            mapIter.setValue(newvalue);
        }
    }

    @SuppressWarnings("unchecked")
    public IFeatureDB cloneDB() {
        TroveFeaturesDB featuresDB = new TroveFeaturesDB();
        featuresDB._name = new String(_name);

        TObjectIntIterator<String> iterator = _featuresMap.iterator();
        while (iterator.hasNext()) {
            iterator.advance();
            featuresDB._featuresMap.put(iterator.key(), iterator.value());
        }

        featuresDB._featuresRMap = (Vector<String>) _featuresRMap.clone();

        return featuresDB;
    }
}
