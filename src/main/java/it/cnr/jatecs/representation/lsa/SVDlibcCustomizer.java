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

package it.cnr.jatecs.representation.lsa;


/**
 * Configuration for the SVDlibc wrapper.
 * See {@code https://tedlab.mit.edu/~dr/SVDLIBC/}
 * */
public class SVDlibcCustomizer {
	private String _SVDlibcPath = "";
	private int _k;
	private boolean _useFrequencies;
	private boolean _verbose;
	
	public SVDlibcCustomizer(String SVDlibcPath, int k){
		_SVDlibcPath=SVDlibcPath;
		_k=k;
		_useFrequencies=false;
		_verbose=true;
	}

	public String getSVDlibcPath() {
		return _SVDlibcPath;
	}

	public void set_SVDlibcPath(String _SVDlibcPath) {
		this._SVDlibcPath = _SVDlibcPath;
	}

	public int getK() {
		return _k;
	}

	public void setK(int _k) {
		this._k = _k;
	}

	public boolean isVerbose() {
		return _verbose;
	}

	public void setVerbose(boolean _verbose) {
		this._verbose = _verbose;
	}
	
	public void setUseFrequencies(){
		_useFrequencies=true;
	}
	
	public void setUseWeights(){
		_useFrequencies=false;
	}
	
	public boolean isUseFrequencies(){
		return _useFrequencies;
	}
	
	public boolean isUseWeights(){
		return !_useFrequencies;
	}

}
