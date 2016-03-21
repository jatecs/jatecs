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

import java.util.Random;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

/**
 * Implementation of the Random Indexing method. See e.g., 
 * {@code Sahlgren, M. (2005, August). An introduction to 
 * random indexing. In Methods and applications of semantic 
 * indexing workshop at the 7th international conference on 
 * terminology and knowledge engineering, TKE (Vol. 5).}
 * */
public class RandomIndexing extends ARandomProjection{	
	private int _nonZeros;// number of non zeros to be added

	/**
	 * Class constructor for a Random Indexing method
	 * @param index the index to be projected
	 * @param dim the latent space to which the index is being projected
	 * @param nonZeros the number of non-zero values each random index vector is assumed to count with
	 * */
	public RandomIndexing(IIndex index, int dim, int nonZeros) {
		super(index,dim);
		_nonZeros=nonZeros;
	}

	@Override
	protected void initIndexes() {
		JatecsLogger.status().println("Start Random Indexing generation");
		if (_randomIndexes.isEmpty()) {
			// generate random indexes
			IIntIterator featit = _index.getFeatureDB().getFeatures();
			while (featit.hasNext()) {
				int feat = featit.next();
				_randomIndexes.put(feat, generateRandomIndex(_dim, _nonZeros));
			}
		}
	}
	
	private static SparseVector generateRandomIndex(int k, int nonZeros) {
		return generateRandomIndex(k, nonZeros, 1);
	}

	private static SparseVector generateRandomIndex(int k, int nonZeros, double nonZeroValue) {
		SparseVector index = new SparseVector(k);
		int changes = 0;
		Random R = new Random();
		while (changes < nonZeros) {
			int dim = R.nextInt(k);
			if (index.get(dim) == 0) {
				index.set(dim, R.nextDouble() < 0.5 ? nonZeroValue : -nonZeroValue);
				changes++;
			}
		}
		return index;
	}

}





