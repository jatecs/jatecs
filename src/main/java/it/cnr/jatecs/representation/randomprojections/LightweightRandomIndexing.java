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

/**
 * Implementation of the Lightweight Random Indexing method. See e.g., 
 * {@code Moreo Fernández, A., Esuli, A., & Sebastiani, F. (2016). 
 * Lightweight Random Indexing for Polylingual Text Classification. 
 * Journal of Artificial Intelligence Research, 57, 151-185.}
 * */
package it.cnr.jatecs.representation.randomprojections;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import it.cnr.jatecs.indexes.DB.interfaces.IMultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentLanguageDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.representation.vector.IMatrix.MATRIX_MODE;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;


public class LightweightRandomIndexing extends ARandomProjection{	
	private Random rand;
	private static final int _nonZeros=2;// number of non zeros to be added
	private IDocumentLanguageDB _docLangDB;

	public LightweightRandomIndexing(IMultilingualIndex index, int dim) {
		super(index,dim);
		rand=new Random();
		this.setMatrixMode(MATRIX_MODE.SPARSE_MATRIX);
		_docLangDB=index.getDocumentLanguageDB();
	}
	
	public void setRandomSeed(long seed){
		rand=new Random(seed);
	}

	@Override
	protected void initIndexes() {
		JatecsLogger.status().println("Start Random Indexing generation");
		if (_randomIndexes.isEmpty()) {
			
			HashSet<Integer> common = getCommonTerms(_docLangDB);
			JatecsLogger.status().println("Common terms = " + common.size());
			
			// generate random indexes
			int basedim=0;
			IIntIterator featit = _index.getFeatureDB().getFeatures();
			while (featit.hasNext()) {
				int feat = featit.next();
				if(common.contains(feat)){
					_randomIndexes.put(feat, generateCommonIndex(_dim, 1.0, basedim));
				}
				else{
					_randomIndexes.put(feat, generateRandomIndex(_dim, _nonZeros, basedim));
				}
				basedim = (basedim+1)%_dim;
			}
		}
	}
	
	

	private HashSet<Integer> getFeaturesInLang(IIndex index, LanguageLabel lang, IDocumentLanguageDB docLangDB){
		HashSet<Integer> featsInLang = new HashSet<Integer>();
		HashSet<Integer> docsInLang = docLangDB.getDocumentsInLanguage(lang);
		for(int docID : docsInLang){
			IIntIterator feats = index.getContentDB().getDocumentFeatures(docID);
			while(feats.hasNext())
				featsInLang.add(feats.next());
		}
		return featsInLang;
	}
	
	private HashSet<Integer> getCommonTerms(IDocumentLanguageDB docLangDB){
		HashSet<Integer> intersection = new HashSet<Integer>();
		IIntIterator feats = _index.getFeatureDB().getFeatures();
		while(feats.hasNext()){
			intersection.add(feats.next());
		}		
		
		Iterator<LanguageLabel> langs = docLangDB.getLanguageDB().getLanguages();
		while(langs.hasNext()){
			LanguageLabel lang = langs.next();
			HashSet<Integer> featsInLang = getFeaturesInLang(_index, lang, docLangDB);
			intersection.retainAll(featsInLang);
		}
		
		return intersection;
	}
	
	public SparseVector generateRandomIndex(int n, int basedim) {
		return generateRandomIndex(n, 1, basedim);
	}

	public SparseVector generateRandomIndexNorm(int n, int nonZeros, int basedim) {
		return generateRandomIndex(n, 1.0/Math.sqrt(nonZeros), basedim);
	}
	
	private SparseVector generateRandomIndex(int n, double nonZeroValue, int basedim) {
		SparseVector index = new SparseVector(n);

		index.set(basedim, rand.nextDouble() < 0.5 ? nonZeroValue : -nonZeroValue);
		int changes = 1;		
		while (changes < _nonZeros) {
			int dim = rand.nextInt(n);
			if (index.get(dim) == 0) {
				index.set(dim, rand.nextDouble() < 0.5 ? nonZeroValue : -nonZeroValue);
				changes++;
			}
		}
		return index;
	}
	
	private static SparseVector generateCommonIndex(int n, double nonZeroValue, int basedim) {
		SparseVector index = new SparseVector(n);		
		index.set(basedim, nonZeroValue*_nonZeros);
		return index;
	}

}





