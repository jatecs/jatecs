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

package it.cnr.jatecs.classification.treeboost;

import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;

import java.util.HashMap;
import java.util.Iterator;

public class TreeBoostLearnerCustomizer implements ILearnerRuntimeCustomizer {

    public final static short ALL_LEVELS = -5;

    public final static short ALL_CATEGORIES = -5;

    /**
     * The customizer to use for internal used classifier.
     */
    protected HashMap<Short, HashMap<Short, ILearnerRuntimeCustomizer>> _internalCustomizer;

    public TreeBoostLearnerCustomizer(
            ILearnerRuntimeCustomizer internalCustomizer) {
        _internalCustomizer = new HashMap<Short, HashMap<Short, ILearnerRuntimeCustomizer>>();

        _internalCustomizer.put(ALL_LEVELS,
                new HashMap<Short, ILearnerRuntimeCustomizer>());
        _internalCustomizer.get(ALL_LEVELS).put(ALL_CATEGORIES,
                internalCustomizer);
    }

    public TreeBoostLearnerCustomizer() {
        _internalCustomizer = new HashMap<Short, HashMap<Short, ILearnerRuntimeCustomizer>>();
    }

    /**
     * Get the internal customizer to use at learning runtime.
     *
     * @return
     */
    public ILearnerRuntimeCustomizer getInternalCustomizer(short parentLevel,
                                                           short category) {
        if (_internalCustomizer.get(parentLevel) == null) {
            if (_internalCustomizer.get(ALL_LEVELS) == null)
                return null;
            else {
                return _internalCustomizer.get(ALL_LEVELS).get(ALL_CATEGORIES);
            }
        } else {
            if (_internalCustomizer.get(parentLevel).containsKey(category))
                return _internalCustomizer.get(parentLevel).get(category);
            else {
                return _internalCustomizer.get(parentLevel).get(ALL_CATEGORIES);
            }
        }
    }

    public void setInternalCustomizer(short parentLevel,
                                      ILearnerRuntimeCustomizer customizer) {
        _internalCustomizer.put(ALL_LEVELS,
                new HashMap<Short, ILearnerRuntimeCustomizer>());
        _internalCustomizer.get(ALL_LEVELS).put(ALL_CATEGORIES, customizer);
    }

    public void setInternalCustomizer(short parentLevel, short catID,
                                      ILearnerRuntimeCustomizer customizer) {
        if (!_internalCustomizer.containsKey(parentLevel)) {
            _internalCustomizer.put(parentLevel,
                    new HashMap<Short, ILearnerRuntimeCustomizer>());
        }

        _internalCustomizer.get(parentLevel).put(catID, customizer);
    }

    public ILearnerRuntimeCustomizer cloneObject() {

        TreeBoostLearnerCustomizer newc = new TreeBoostLearnerCustomizer();
        Iterator<Short> it = _internalCustomizer.keySet().iterator();
        while (it.hasNext()) {
            short catID = it.next();
            newc._internalCustomizer.put(catID,
                    new HashMap<Short, ILearnerRuntimeCustomizer>());
            Iterator<Short> it2 = _internalCustomizer.get(catID).keySet()
                    .iterator();
            while (it2.hasNext()) {
                short intCatID = it2.next();
                ILearnerRuntimeCustomizer cInternal = this._internalCustomizer
                        .get(catID).get(intCatID).cloneObject();
                newc._internalCustomizer.get(catID).put(intCatID, cInternal);
            }

        }

        return newc;
    }


    public HashMap<Short, HashMap<Short, ILearnerRuntimeCustomizer>> getInternalCustomizerHashMap() {
        return _internalCustomizer;
    }
}


