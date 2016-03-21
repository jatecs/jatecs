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

package it.cnr.jatecs.representation.transfer.dci;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.utils.DocSet;
import it.cnr.jatecs.indexing.tsr.InformationGain;

/**
 * Implements a probability-based Mutual Information (MI) Distributional Correspondence Function.
 * @see IDistributionalCorrespondenceFunction
 * */
public class MutualInformationDCF extends ADiscreteDCF{
	
	private InformationGain ig;
	
	public MutualInformationDCF(IIndex index){
		super(index);
		ig=new InformationGain();
	}

	@Override
	protected double distributionalCorrespondence(DocSet Fd, DocSet Pd) {
		int TP = Fd.intersectionSize(Pd);
		int FP = Fd.size() - TP;
		int FN = Pd.size() - TP;
		int TN = _nD - (TP+FP+FN);
		
		double mi = ig.compute(TP, FP, FN, TN);
		
		double tpr=TP*1.0/(TP+FN);
		double tnr=TN*1.0/(TN+FP);
				
		if(tpr+tnr < 1)
			mi *= -1;
		
		return mi;
	}
	
}
