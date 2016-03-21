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
import it.cnr.jatecs.representation.vector.IVector;

/**
 * Implements a kernel-based Polynomial Distributional Correspondence Function.
 * @see IDistributionalCorrespondenceFunction
 * */
public class PolynomialDCF extends AVectorDCF{
	
	private double _offset;
	private double _exp;
	
	public PolynomialDCF(IIndex index, double offset, double exp){
		super(index, true, true);
		_offset=offset;
		_exp=exp;
	}

	@Override
	protected double distributionalCorrespondence(IVector featDist, IVector pivotDist) {
		double dot = featDist.dotProduct(pivotDist);
		return polynomial(dot);
	}

	@Override
	protected double distributionalRandomCorrespondence(int feat1, int feat2) {
		double prevf=getFeatProportion(feat1);
		double prevp=getFeatProportion(feat2);
		double dot=Math.sqrt(prevf*prevp);
		return polynomial(dot);
	}
	
	private double polynomial(double dot){
		return Math.pow(_offset+dot, _exp);
	}

}
