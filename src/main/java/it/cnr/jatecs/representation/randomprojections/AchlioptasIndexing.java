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
import it.cnr.jatecs.representation.vector.DenseVector;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.Random;

/**
 * Implementation of the Achlioptas random mapping, with ternary distribution.
 * As described in {@code Achlioptas, D. (2001, May). Database-friendly 
 * random projections. In Proceedings of the twentieth ACM SIGMOD-SIGACT-SIGART 
 * symposium on Principles of database systems (pp. 274-281). ACM.}
 * */
public class AchlioptasIndexing extends ARandomProjection { 
	
	private static final double sqrt_3 = Math.sqrt(3.0);
	
	public AchlioptasIndexing(IIndex index, int k) {
		super(index, k);		
	}

	protected void initIndexes() {
		JatecsLogger.status().println("Start Achlioptas Indexing generation");
		if (_randomIndexes.isEmpty()) {
			// generate random indexes
			Random r=new Random();
			IIntIterator featit = _index.getFeatureDB().getFeatures();			
			while (featit.hasNext()) {
				int feat = featit.next();
				DenseVector newindex = new DenseVector(_dim);
				for(int i = 0; i < _dim; i++){
					double rand = r.nextDouble();
					// Sqrt(3) times ...
					// +1 with prob 1/6
					// 0 with prob 2/3
					// -1 with prob 1/6
					if(rand < 1.0/6.0)
						newindex.set(i, sqrt_3);
					else if(rand > 5.0/6.0)
						newindex.set(i, -sqrt_3);
					else 
						newindex.set(i, 0);
				}				
				_randomIndexes.put(feat, newindex);
			}
		}
	}

}
