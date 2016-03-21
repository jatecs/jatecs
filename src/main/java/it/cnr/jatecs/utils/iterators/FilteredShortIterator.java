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

import gnu.trove.TShortHashSet;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Arrays;
import java.util.Iterator;

public class FilteredShortIterator implements IShortIterator {

    private static short noCategory = -1;

    private IShortIterator _values;
    private IShortIterator _filter;
    private boolean _remove;
    private boolean _alreadyCheckedNext;
    private boolean _hasNext;
    private short _next;
    private short _nextFilter;

    public FilteredShortIterator(IShortIterator it, TShortHashSet list, boolean listDefinesRemoved) {
        _values = it;
        short[] filterValues = list.toArray();
        Arrays.sort(filterValues);
        _filter = new ShortArrayIterator(filterValues);
        _remove = listDefinesRemoved;
        begin();
    }

    public FilteredShortIterator(IShortIterator it, IShortIterator list, boolean listDefinesRemoved) {
        _values = it;
        _filter = list;
        _remove = listDefinesRemoved;
        begin();
    }

    public void begin() {
        _values.begin();
        _filter.begin();
        _alreadyCheckedNext = false;
        if (_filter.hasNext())
            _nextFilter = _filter.next();
        else
            _nextFilter = Short.MAX_VALUE;
        _next = noCategory;
    }

    public boolean hasNext() {
        if (_alreadyCheckedNext)
            return _hasNext;
        else {
            checkNext();
            _alreadyCheckedNext = true;
            return _hasNext;
        }
    }

    public Short next() {
        if (_alreadyCheckedNext) {
            _alreadyCheckedNext = false;
            return _next;
        } else {
            checkNext();
            return _next;
        }
    }

    private void checkNext() {
        if (_remove) {
            if (_values.hasNext())
                _next = _values.next();
            else {
                _hasNext = false;
                return;
            }
            while (true) {
                if (_next == _nextFilter) {
                    if (_values.hasNext())
                        _next = _values.next();
                    else {
                        _hasNext = false;
                        return;
                    }
                    if (_filter.hasNext())
                        _nextFilter = _filter.next();
                    else
                        _nextFilter = Short.MAX_VALUE;
                } else if (_next > _nextFilter) {
                    if (_filter.hasNext())
                        _nextFilter = _filter.next();
                    else
                        _nextFilter = Short.MAX_VALUE;
                } else {
                    _hasNext = true;
                    return;
                }
            }
        } else {
            while (true) {
                if (_next < _nextFilter) {
                    if (_values.hasNext())
                        _next = _values.next();
                    else {
                        _hasNext = false;
                        return;
                    }
                } else if (_next > _nextFilter) {
                    if (_filter.hasNext())
                        _nextFilter = _filter.next();
                    else {
                        _hasNext = false;
                        return;
                    }
                } else {
                    if (_filter.hasNext())
                        _nextFilter = _filter.next();
                    else
                        _nextFilter = Short.MAX_VALUE;
                    _hasNext = true;
                    return;
                }
            }
        }
    }
    
	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove not allowed for this IShortIterator implementation.");		
	}

	@Override
	public Iterator<Short> iterator() {
		return this;
	}
}
