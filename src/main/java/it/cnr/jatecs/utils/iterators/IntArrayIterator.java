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

package it.cnr.jatecs.utils.iterators;

import java.util.Collection;
import java.util.Iterator;

import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class IntArrayIterator implements IIntIterator {

    private int[] _list;
    private int _pos;
    public IntArrayIterator(int[] list) {
        _list = list;
        _pos = 0;
    }

    public boolean hasNext() {
        return _pos < _list.length;
    }

	public void begin() {
		_pos = 0;
	}
	
	public static IntArrayIterator List2IntArrayIterator(Collection<Integer> list){
		int[] array = new int[list.size()];
		int pos=0;
		for(int el:list)
			array[pos++]=el;		
		return new IntArrayIterator(array);
	}

    public Integer next() {
        int val = _list[_pos];
        ++_pos;
        return val;
    }

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove not allowed for this IIntIterator implementation.");		
	}

	@Override
	public Iterator<Integer> iterator() {
		return this;
	}

}
