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
 * Implements a kernel-based Gaussian, a.k.a. Radial Basis Function (RBF), Distributional Correspondence Function.
 * @see IDistributionalCorrespondenceFunction
 * */
public class GaussianDCF extends AVectorDCF{
	
	private double _gamma;
	
	public GaussianDCF(IIndex index, double sigma){
		super(index, true, true);
		_gamma=1.0/(2*sigma*sigma);
	}

	@Override
	protected double distributionalCorrespondence(IVector featDist, IVector pivotDist) {
		double euclideanNorm = featDist.quadraticEuclideanDistance(pivotDist);		
		double rbf = Gauss(euclideanNorm); 
		return rbf;
	}

	@Override
	protected double distributionalRandomCorrespondence(int feat1, int feat2) {
		double pf=getFeatProportion(feat1);
		double pp=getFeatProportion(feat2);
		double edist = 2*(1.0-Math.sqrt(pf*pp));
		double rbf=Gauss(edist*edist);
		return rbf;
	}
	
	private double Gauss(double quadratic_distance){
		return Math.exp(-_gamma*quadratic_distance);
	}

}
