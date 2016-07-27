package it.cnr.jatecs.representation.oversampling;

import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.representation.vector.IVector;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public interface IRandomDistributionModel {
	IVector getFeatureVector(int feat); //column of this distributional model
	IIntIterator getFeatureSpace(); //there is one for each original feat
	IFeatureDB getLatentFeatureSpace();	//one for each column
	int getLatentFeatureSpaceSize();
	int getFeatureSpaceSize();
}
