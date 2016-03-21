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

package it.cnr.jatecs.utils;


import java.util.LinkedHashMap;
import java.util.Map;

public class LRUMap<T> extends LinkedHashMap<String, T> {
    static final long serialVersionUID = 21432343254323L;
    Map.Entry<String, T> _eldest;
    private int _size;

    public LRUMap(int s) {
        super(s + 1, .75F, true);

        _size = s;
    }


    public boolean removeEldestEntry(Map.Entry<String, T> eldest) {

        if (size() > _size) {
            _eldest = eldest;
            return true;
        } else {
            _eldest = null;
            return false;
        }
    }


    public Map.Entry<String, T> getEldest() {
        return _eldest;
    }
}