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

import it.cnr.jatecs.indexing.tsr.ITsrFunction;
import it.cnr.jatecs.indexing.tsr.InformationGain;

/**
 * Configuration parameters for the Distributional Correspondence Indexing method
 * @see DistributionalCorrespondeceIndexing
 * */
public class DCIcustomizer {
	/** Number of pivots */
	public int _num_pivots=100;
	
	/** Support, or smallest number of occurrences in the two domains any word must satisfy */
	public int _phi=30;
	
	/** Perform the cross correspondence strategy */
	public boolean _crosscorrespondence=true;
	
	/** Clean ill-formed features */
	public boolean _cleanfeats=true;
	
	/** Rescale the feature distributions to a normal-distribution */
	public boolean _rescale=true;
	
	/** Unify vector projections for common words in the two domains */
	public boolean _unification=true;
	
	/** The TSR function 
	 * @see ITsrFunction */
	public ITsrFunction _tsrFunction=new InformationGain();
	
	/** The label associated to the DCF */
	public DCFtype _dcf=DCFtype.cosine;
	
	/** The word-translator oracle (only for cross-lingual adaptation) */
	public IWordTranslationOracle _oracle=null;
	
	/** Indicates if the dictionary is to be constructed online or not */
	public boolean constructDictionary=false;
	
	/** Number of parallel threads to run the DCI algorithm */
	public int _nThreads=1;
}
