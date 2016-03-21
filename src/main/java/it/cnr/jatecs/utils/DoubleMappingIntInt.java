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

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;

public class DoubleMappingIntInt {

    private TIntIntHashMap _map1;
    private TIntIntHashMap _map2;

    public DoubleMappingIntInt(int size) {
        _map1 = new TIntIntHashMap(size);
        _map2 = new TIntIntHashMap(size);
    }


    public void put(int key1, int key2) {
        _map1.put(key1, key2);
        _map2.put(key2, key1);

        assert (_map1.size() == _map2.size());
    }


    public int get1(int key) {
        return _map1.get(key);
    }


    public int get2(int key) {
        return _map2.get(key);
    }

    public void remove1(int key) {
        int key2 = _map1.get(key);
        _map1.remove(key);
        _map2.remove(key2);
    }

    public void remove2(int key) {
        int key1 = _map2.get(key);
        _map2.remove(key);
        _map1.remove(key1);
    }


    public int size() {
        return _map1.size();
    }


    public TIntIntIterator iterator1() {
        return _map1.iterator();
    }

    public TIntIntIterator iterator2() {
        return _map2.iterator();
    }
}
