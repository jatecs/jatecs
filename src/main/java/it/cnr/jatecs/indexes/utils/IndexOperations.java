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

package it.cnr.jatecs.indexes.utils;

import java.util.HashSet;

import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDomainDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveFeatureDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class IndexOperations {

	public static IFeatureDB getUnionFeatureSpace(IIndex... indexes){
		HashSet<String> distinct=new HashSet<>();
		for(IIndex index:indexes){
			IIntIterator featit=index.getFeatureDB().getFeatures();
			while(featit.hasNext())
				distinct.add(index.getFeatureDB().getFeatureName(featit.next()));
		}
		
		TroveFeatureDBBuilder featureSpace=new TroveFeatureDBBuilder();
		for(String feat:distinct)
			featureSpace.addFeature(feat);
		
		return featureSpace.getFeatureDB();
	}
	
	//post-cond: might require re-weighting
	public static IIndex changeFeatureSpace(IIndex index, IFeatureDB newFeatureSpace){
		TroveContentDBBuilder contentDB = new TroveContentDBBuilder(index.getDocumentDB(), newFeatureSpace);
		TroveWeightingDBBuilder weightingDB = new TroveWeightingDBBuilder(contentDB.getContentDB());
		TroveDomainDB newDomain=new TroveDomainDB(index.getCategoryDB(), newFeatureSpace);
		
		IIntIterator docit = index.getDocumentDB().getDocuments();
		while(docit.hasNext()){
			int docid = docit.next();
			IIntIterator featit = index.getContentDB().getDocumentFeatures(docid);
			while(featit.hasNext()){
				int oldFeatID = featit.next();
				String featname = index.getFeatureDB().getFeatureName(oldFeatID);
				int newFeatID = newFeatureSpace.getFeature(featname);
				if(newFeatID!=-1){
					int count = index.getContentDB().getDocumentFeatureFrequency(docid, oldFeatID);					
					if(count > 0){
						contentDB.setDocumentFeatureFrequency(docid, newFeatID, count);
						double weight=index.getWeightingDB().getDocumentFeatureWeight(docid, oldFeatID);							
						weightingDB.setDocumentFeatureWeight(docid, newFeatID, weight);
					}
				}
			}
		}
		
		return new GenericIndex(newFeatureSpace, 
				index.getDocumentDB(), 
				index.getCategoryDB(), 
				newDomain, 
				contentDB.getContentDB(), 
				weightingDB.getWeightingDB(), 
				index.getClassificationDB());
	}
	
	public static IIndex changeClassificationSchema(IIndex index, IClassificationDB newClassification){
		return new GenericIndex(index.getFeatureDB(), 
				index.getDocumentDB(), 
				newClassification.getCategoryDB(), 
				new TroveDomainDB(newClassification.getCategoryDB(), index.getFeatureDB()),
				index.getContentDB(), 
				index.getWeightingDB(), 
				newClassification);
	}

}
