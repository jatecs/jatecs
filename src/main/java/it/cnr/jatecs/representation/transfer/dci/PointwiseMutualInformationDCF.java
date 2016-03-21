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

/**
 * Implements a probabilistic-based Pointwise Mutual Information (PMI) Distributional Correspondence Function.
 * @see IDistributionalCorrespondenceFunction
 * */
public class PointwiseMutualInformationDCF extends ADiscreteDCF{
	
	private static final double log2 = Math.log(2.0);

	public PointwiseMutualInformationDCF(IIndex index) {
		super(index);
	}
	
	private double pmi(int TP, int FP, int FN, int TN){		
		int D=TP+FP+FN+TN;
		double Pxy=TP*1.0/D;
		double Pxny=FP*1.0/D;
		double Pnxy=FN*1.0/D;
		double Px=Pxy+Pxny;
		double Py=Pxy+Pnxy;

		if (Px == 0 || Py == 0 || Pxy == 0)
			return 0;

		return Math.log(Pxy/(Px*Py))/log2;
	}

	@Override
	protected double distributionalCorrespondence(DocSet Fd, DocSet Pd) {
		int TP = Fd.intersectionSize(Pd);
		int FP = Fd.size() - TP;
		int FN = Pd.size() - TP;
		int TN = _nD - (TP+FP+FN);
		
		return pmi(TP, FP, FN, TN);
	}	

}
