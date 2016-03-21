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

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

public interface ITsrFunction {

	/**
	 * Compute the importance for the specified feature ID in the given category ID.
	 * 
	 * @param catID The category ID.
	 * @param featID The feature ID.
	 * @param index The index used to compute statistics for the specified category and feature.
	 * @return The score given to the feature ID in the specified category ID.
	 */
	public double compute(short catID, int featID, IIndex index);
	
	
	/**
	 * Compute the importance for a given 4-cell contingency table
	 * 
	 * @param TP true positives
	 * @param FP false positives
	 * @param FN false negatives
	 * @param TN true negatives
	 * @return The score given to the feature ID in the specified category ID.
	 */
	public double compute(int TP, int FP, int FN, int TN);
	
}
