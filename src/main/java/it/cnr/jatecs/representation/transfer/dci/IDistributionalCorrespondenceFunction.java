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

import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.representation.vector.DenseVector;

import java.util.List;


/**
 * Interface of a Distributional Correspondence Function of the
 * Distributional Correspondence Indexing model described in:
 * <i>{@code Moreo Fernandez, A., Esuli, A., and Sebastiani, F. (2016). 
 * Distributional Correspondence Indexing for Cross-Lingual 
 * and Cross-Domain Sentiment Classification. 
 * Journal of Artificial Intelligence Research, 131-163.}</i>
 * */
public interface IDistributionalCorrespondenceFunction {
	/**
	 * Computes the Distributional Correspondence Function for a given feature
	 * with respect to a given set of pivot features.
	 * 
	 * @param featID the id of the feature being projected
	 * @param pivotsID the set of pivots on which the projection is performed
	 * */
	public DenseVector distributionalCorrespondenceFunction(int featID, List<Integer> pivotsID);
	
	/**
	 * @return the feature space of the distributional model
	 * */
	public IFeatureDB getFeatureDB();
	
	/**
	 * @return the number of dimensions of the feature space
	 * */
	public int getFeaturesCount();
	
	/**
	 * @return the number of documents represented in the distributional model
	 * */
	public int getDocumentsCount();
	
	/**
	 * Gets the number of documents in the distributional model that contain a specific feature
	 * 
	 * @param featID the id of the feature
	 * @return the number of documents containing a specific feature
	 * */
	public int getFeatureDocumentsCount(int featID);
	
	/**
	 * Gets the proportion of documents in the distributional model that contain a specific feature
	 * 
	 * @param featID the id of the feature
	 * @return the proportion of documents containing a specific feature
	 * */
	public double getFeatProportion(int featID);
}
