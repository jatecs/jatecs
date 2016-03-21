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

import gnu.trove.TShortHashSet;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class NewFilteredShortIterator implements IShortIterator {

    private IShortIterator _values;
    private IShortIterator _filter;
    private boolean _remove;
    private short _next;
    private short _nextFilter;

    public NewFilteredShortIterator(IShortIterator it, TShortHashSet list, boolean listDefinesRemoved) {
        _values = it;
        _filter = new TShortIteratorIterator(list);
        _remove = listDefinesRemoved;
        begin();
    }

    public NewFilteredShortIterator(IShortIterator it, IShortIterator list, boolean listDefinesRemoved) {
        _values = it;
        _filter = list;
        _remove = listDefinesRemoved;
        begin();
    }


    public boolean hasNext() {
        return _next != -1;
    }

	
	/*public boolean hasNext() {
		if(_remove) {
			if(_values.hasNext())
				_next = _values.next();
			else
				return false;
			while(true) {
				if(_next==_nextFilter) {
					if(_values.hasNext())
						_next = _values.next();
					else
						return false;
					if(_filter.hasNext())
						_nextFilter=_filter.next();
					else
						_nextFilter=Short.MAX_VALUE;
				}
				else if(_next>_nextFilter) {
					if(_filter.hasNext())
						_nextFilter=_filter.next();
					else
						_nextFilter=Short.MAX_VALUE;
				}
				else
					return true;
			}
		}
		else {
			while(true) {
				if(_next<_nextFilter) {
					if(_values.hasNext())
						_next = _values.next();
					else
						return false;
				}
				else if(_next>_nextFilter) {
					if(_filter.hasNext())
						_nextFilter=_filter.next();
					else
						return false;
				}
				else {
					if(_values.hasNext())
						_next = _values.next();
					else
						_next = -1;
					if(_filter.hasNext())
						_nextFilter=_filter.next();
					else
						_nextFilter = Short.MAX_VALUE;
					return true;
				}
			}
		}
	}*/

    public Short next() {
        short ret = _next;

        if (_remove) {
            boolean done = false;
            while (!done) {
                if (_next < _nextFilter) {
                    if (_values.hasNext()) {
                        _next = _values.next();
                        if (_next != _nextFilter)
                            done = true;
                    } else {
                        _next = -1;
                        _nextFilter = Short.MAX_VALUE;
                        done = true;
                    }
                } else if (_next > _nextFilter) {
                    if (_filter.hasNext()) {
                        _nextFilter = _filter.next();
                        if (_next != _nextFilter)
                            done = true;
                    } else {
                        _nextFilter = Short.MAX_VALUE;
                    }
                } else {
                    if (_filter.hasNext()) {
                        _nextFilter = _filter.next();
                    } else {
                        _nextFilter = Short.MAX_VALUE;
                    }

                    if (_values.hasNext())
                        _next = _values.next();
                    else {
                        _next = -1;
                        _nextFilter = Short.MAX_VALUE;
                        done = true;
                    }


                    if (_next != _nextFilter)
                        done = true;
                }
            }
        } else {
            boolean done = false;
            while (!done) {
                if (_next < _nextFilter) {
                    if (_values.hasNext()) {
                        _next = _values.next();
                        if (_next == _nextFilter)
                            done = true;
                    } else {
                        _next = -1;
                        _nextFilter = Short.MAX_VALUE;
                        done = true;
                    }
                } else if (_next > _nextFilter) {
                    if (_filter.hasNext()) {
                        _nextFilter = _filter.next();
                        if (_next == _nextFilter)
                            done = true;
                    } else {
                        _nextFilter = Short.MAX_VALUE;
                    }
                } else {
                    if (_values.hasNext())
                        _next = _values.next();
                    else {
                        _next = -1;
                        _nextFilter = Short.MAX_VALUE;
                        done = true;
                    }

                    if (_filter.hasNext()) {
                        _nextFilter = _filter.next();
                    } else {
                        _nextFilter = Short.MAX_VALUE;
                    }
                    if (_next == _nextFilter)
                        done = true;
                }
            }
        }

        return ret;
    }

    public void begin() {
        _values.begin();
        _filter.begin();
        _nextFilter = Short.MAX_VALUE;
        if (_filter.hasNext())
            _nextFilter = _filter.next();

        _next = -1;
        if (_values.hasNext())
            _next = _values.next();


        if (_remove) {
            while (_next == _nextFilter) {
                if (_values.hasNext())
                    _next = _values.next();
                else
                    _next = -1;

                if (_filter.hasNext())
                    _nextFilter = _filter.next();
                else
                    _nextFilter = Short.MAX_VALUE;
            }
        } else {
            while (_next != _nextFilter) {
                if (_next < _nextFilter) {
                    if (_values.hasNext())
                        _next = _values.next();
                    else {
                        _nextFilter = Short.MAX_VALUE;
                        _next = -1;
                        break;
                    }
                } else if (_next > _nextFilter) {
                    if (_filter.hasNext())
                        _nextFilter = _filter.next();
                    else {
                        _nextFilter = Short.MAX_VALUE;
                        _next = -1;
                        break;
                    }
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
