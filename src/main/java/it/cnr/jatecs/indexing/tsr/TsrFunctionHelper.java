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

package it.cnr.jatecs.indexing.tsr;

import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

public class TsrFunctionHelper {
	protected ContingencyTable _cells;
	
	public TsrFunctionHelper(short catID, int featID, IIndex index) {
		int D = index.getDocumentDB().getDocumentsCount();
		int C = index.getClassificationDB().getCategoryDocumentsCount(catID);
		int T = index.getContentDB().getFeatureDocumentsCount(featID);
		int notC = D-C;
		int tp = index.getFeatureCategoryDocumentsCount(featID, catID);
		int fp = T - tp;
		int fn = C - tp;
		int tn = notC - fp;
		initContingencyTable(tp, fp, fn, tn);
	}

	public TsrFunctionHelper(int TP, int FP, int FN, int TN) {
		initContingencyTable(TP, FP, FN, TN);		
	}
	
	private void initContingencyTable(int TP, int FP, int FN, int TN){
		_cells=new ContingencyTable();
		_cells.setTP(TP);
		_cells.setFP(FP);
		_cells.setTN(TN);
		_cells.setFN(FN);
	}
	
	public double Pt(){
		return T()*1.0/total();
	}
	
	public double PnotT(){
		return 1.0-Pt();
	}
	
	public double Pc(){
		return C()*1.0/total();
	}
	
	public double PnotC(){
		return 1.0-Pc();
	}
	
	public double Ptc(){
		return _cells.tp()*1.0/total();
	}
	
	public double PnotTnotC(){
		return _cells.tn()*1.0/total();
	}
	
	public double PtnotC(){
		return _cells.fp()*1.0/total();
	}
	
	public double PnotTc(){
		return _cells.fn()*1.0/total();
	}
	
	public int T(){
		return _cells.tp()+_cells.fp();
	}
	
	public int C(){
		return _cells.tp()+ _cells.fn();
	}

	public int total() {
		return _cells.fn() + _cells.fp() + _cells.tn() + _cells.tp();
	}

	public int TP() {
		return _cells.tp();
	}	
	public int FP() {
		return _cells.fp();
	}
	public int TN() {
		return _cells.tn();
	}
	public int FN() {
		return _cells.fn();
	}

	public double f1() {
		return _cells.f1();
	}
	
	
}
