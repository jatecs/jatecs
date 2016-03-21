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

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class RoundRobinTSR implements ITsr {

	protected ITsrFunction _func;

	protected int _numBestFeatures;

	protected double _minThreshold;

	final protected HashSet<String> excludedFeaturesPrefixes;

	private TIntHashSet _bestFeatsSet;
	
	private int _rounds;
	
	
	public RoundRobinTSR(ITsrFunction func)	{
		_func = func;
		_numBestFeatures = 2000;
		_minThreshold = 0.0;
		excludedFeaturesPrefixes = new HashSet<String>();
	}

	/**
	 * Set the number of best features (the ones which have the best local TEF
	 * computed values) to select globally when computing the set of features to
	 * maintain in the features DB. The number of requested best features is computed on the set
	 * of features which does not include the features requested to be kept in the index (see {@link #addExcludedFeaturesPrefixes(List)}
	 * method).
	 * 
	 * @param k
	 *            The number of best features to consider.
	 */
	public void setNumberOfBestFeatures(int k) {
		if (k <= 0)
			k = 3;

		_numBestFeatures = k;
	}

	/**
	 * Set the list of features prefixes to exclude from TSR process.
	 * 
	 * @param prefixes
	 *            The set of prefixes to exclude in TSR process.
	 */
	public void addExcludedFeaturesPrefixes(List<String> prefixes) {
		if (prefixes == null)
			throw new NullPointerException("The list of prefixes is 'null'");
		excludedFeaturesPrefixes.clear();
		for (String prefix : prefixes) {
			excludedFeaturesPrefixes.add(prefix);
		}
	}

	/**
	 * Set the minimum threshold value used to decide if a feature will be
	 * erased. The value of threshold must range from 0 (excluded) and 100
	 * (excluded) and indicate the percentage of features that must be deleted
	 * from the original features contained in the initial index. The features
	 * are ordered according to their local TEF value: only the (100-threshold)%
	 * of the features with higher local TEF value, at least in one category,
	 * will be kept back in the resulting index.
	 * 
	 * @param threshold
	 *            The value of threshold.
	 */
	public void setMinimumThreshold(double threshold) {
		if (threshold <= 0)
			throw new RuntimeException(
					"The value of TSR threshold must be greater than 0.");

		if (threshold >= 100)
			throw new RuntimeException(
					"The value of TSR threshold must be lower than 100.");

		_minThreshold = 1.0 - (threshold / 100.0);
	}

	/**
	 * Get the number of best features considered by the module to compute which
	 * features must be removed from features DB.
	 * 
	 * @return The number of best features for category.
	 */
	public int numberOfBestFeaturesForCategory() {
		return _numBestFeatures;
	}
	
	public TreeSet<FeatureEntry> selectBestFeatures(IIndex index){
		JatecsLogger.status().println("Start computing Round Robin TSR using "+_func.getClass().getName());
		TextualProgressBar bar = new TextualProgressBar("Select best features for each category");

		int total = index.getCategoryDB().getCategoriesCount();
		int step = 0;
		
		if(_minThreshold!=0)
			_numBestFeatures = (int)Math.round((index.getFeatureDB().getFeaturesCount()*_minThreshold));		
		
		HashMap<Short, TreeSet<FeatureEntry>> bests = new HashMap<Short, TreeSet<FeatureEntry>>();

		// First compute which features to keep (excluded from TSR process).
		HashSet<Integer> featuresToKeep = new HashSet<Integer>();
		if (excludedFeaturesPrefixes.size() > 0) {
			IIntIterator features = index.getFeatureDB().getFeatures();
			while (features.hasNext()) {
				int featID = features.next();
				String featName = index.getFeatureDB().getFeatureName(featID);
				Iterator<String> prefixes = excludedFeaturesPrefixes.iterator();
				boolean toSkip = false;
				while (prefixes.hasNext() && !toSkip) {
					if (featName.startsWith(prefixes.next())) {
						toSkip = true;
						break;
					}
				}
				if (toSkip) {
					featuresToKeep.add(featID);
				}
			}
		}

		IShortIterator itCats = index.getCategoryDB().getCategories();

		while(itCats.hasNext()){
			short catID = itCats.next();
			if (index.getClassificationDB().getCategoryDocumentsCount(catID) == 0)
				continue;

			TreeSet<FeatureEntry> best = new TreeSet<FeatureEntry>();

			// For each valid feature in this category compute TEF.
			IIntIterator itFeats = index.getDomainDB().getCategoryFeatures(catID);
			while(itFeats.hasNext()){
				int featID = itFeats.next();

				// Check if this feature must be excluded from TSR process.
				if (featuresToKeep.contains(featID))
					continue;

				double tef = _func.compute(catID, featID, index);

				assert(!Double.isNaN(tef));				
				
				FeatureEntry fe = new FeatureEntry(featID, tef);
				best.add(fe);

				if (best.size() > _numBestFeatures){
					best.remove(best.first());
				}
				assert (best.size() <= _numBestFeatures);
			}

			bests.put(catID, best);			
			
			step++;
			bar.signal((step * 100) / total);
		}

		bar.signal(100);

		// Select the best _numBestFeatures features in a round robin manner.
		JatecsLogger.status().print("Selecting best features among categories using round robin...");
		TreeSet<FeatureEntry> bestFeatEntrySet=new TreeSet<FeatureEntry>();
		
		_rounds=0;

		boolean allEmpty = false;

		_bestFeatsSet = new TIntHashSet();
		while (_bestFeatsSet.size() < _numBestFeatures && !allEmpty){
			allEmpty = true;
			IShortIterator it = index.getCategoryDB().getCategories();

			while(it.hasNext() && _bestFeatsSet.size() < _numBestFeatures){
				short catID = it.next();
				if (index.getClassificationDB()
						.getCategoryDocumentsCount(catID) == 0)
					continue;

				TreeSet<FeatureEntry> f = bests.get(catID);

				if (f.size() == 0)
					continue;

				allEmpty = false;
				FeatureEntry fe  = f.pollLast();
				if (!_bestFeatsSet.contains(fe.featureID)){
					_bestFeatsSet.add(fe.featureID);
					bestFeatEntrySet.add(fe);
				}
			}
			
			_rounds++;
		}
		JatecsLogger.status().println("done.");
		
		// Add "as best features" also the features that need to be keep.
		Iterator<Integer> featsToKeepIt = featuresToKeep.iterator();
		while (featsToKeepIt.hasNext()) {
			int featID = featsToKeepIt.next();
			assert(!_bestFeatsSet.contains(featID));
			_bestFeatsSet.add(featID);
			bestFeatEntrySet.add(new FeatureEntry(featID, Double.MAX_VALUE));
		}
		
		return bestFeatEntrySet;
	}
	
	public void computeTSR(IIndex index){
		computeTSR(index, null);
	}
	
	public void computeTSR(IIndex index, IIndex testindex){
		selectBestFeatures(index);
		
		// Select the features to remove from DB.
		JatecsLogger
				.status()
				.print("Selecting worst features to remove from DB and removing it...");
		TIntArrayList toRemove = new TIntArrayList();
		
		IIntIterator it = index.getFeatureDB().getFeatures();
		while(it.hasNext()){
			int featID = it.next();
			if (!_bestFeatsSet.contains(featID)){
				toRemove.add(featID);
			}
		}
		
		TIntArrayListIterator toRemoveIt=new TIntArrayListIterator(toRemove);		

		// Remove the features from DB.
		index.removeFeatures(toRemoveIt);		
		if(testindex!=null){
			toRemoveIt.begin();
			testindex.removeFeatures(toRemoveIt);
		}
		
		JatecsLogger.status().println("done.");
		JatecsLogger.status().println("Round-robin TSR: "+_bestFeatsSet.size()+" features globally selected, "+_rounds+" features for each category");
	}
}
