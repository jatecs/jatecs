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

import gnu.trove.TShortObjectHashMap;
import gnu.trove.TShortObjectIterator;

import java.util.Hashtable;
import java.util.Iterator;

public class DoubleMappingShortObject<T> {

    private TShortObjectHashMap<T> _map1;
    private Hashtable<T, Short> _map2;

    public DoubleMappingShortObject(int initialSize) {
        _map1 = new TShortObjectHashMap<T>(initialSize);
        _map2 = new Hashtable<T, Short>(initialSize);
    }

    public DoubleMappingShortObject() {
        _map1 = new TShortObjectHashMap<T>();
        _map2 = new Hashtable<T, Short>();
    }


    public void put(short key1, T key2) {
        _map1.put(key1, key2);
        _map2.put(key2, key1);

        assert (_map1.size() == _map2.size());
    }


    public T get1(short key) {
        return (T) _map1.get(key);
    }


    public short get2(T key) {
        if (!_map2.containsKey(key)) {
            JatecsLogger.execution().warning("Non contengo chiave");
            return -1;
        }

        return _map2.get(key);
    }

    public void remove1(short key) {
        T key2 = (T) _map1.get(key);
        _map1.remove(key);
        _map2.remove(key2);
    }

    public void remove2(T key) {
        short key1 = _map2.get(key);
        _map2.remove(key);
        _map1.remove(key1);
    }


    public int size() {
        return _map1.size();
    }


    public TShortObjectIterator<T> iterator1() {
        return _map1.iterator();
    }

    public Iterator<T> iterator2() {
        return _map2.keySet().iterator();
    }
}
