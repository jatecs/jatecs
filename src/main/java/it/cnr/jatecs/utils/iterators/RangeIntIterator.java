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

import java.util.Iterator;

import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class RangeIntIterator implements IIntIterator {

    private int _pos;
    private int _end;
    private int _start;
    public RangeIntIterator(int start, int end) {
        _start = start;
        _pos = start;
        _end = end;
    }

    public boolean hasNext() {
        return _pos < _end;
    }

    public Integer next() {
        int val = _pos;
        ++_pos;
        return val;
    }

    public void begin() {
        _pos = _start;
    }


	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove not allowed for this IInttIterator implementation.");		
	}

	@Override
	public Iterator<Integer> iterator() {
		return this;
	}

	
}
