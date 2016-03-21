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

package it.cnr.jatecs.representation.randomprojections;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

/**
 * Interface for Random Mapping methods.
 * */
public interface IProjectionMethod {	
	/**
	 * Performs a projection of the indexed corpus to a different vector space.
	 * */
	public void project();
	
	/**
	 * @return a version of the training index, projected in the new vector space
	 * */
	public IIndex getLatentTrainindex();
	
	/**
	 * @param testindex the test index in the original vector space
	 * @return a version of test index, projected in the new vector space
	 * */
	public IIndex getLatentTestindex(IIndex testindex);
	
	/**
	 * clear memory allocated for internal resources
	 * */
	public void clearResources();
}
