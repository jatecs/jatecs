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

import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.representation.vector.AVector;
import it.cnr.jatecs.representation.vector.DenseMatrix;
import it.cnr.jatecs.representation.vector.IMatrix;
import it.cnr.jatecs.representation.vector.IMatrix.MATRIX_MODE;
import it.cnr.jatecs.representation.vector.SparseMatrix;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.IntArrayIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.HashMap;

public abstract class ARandomProjection implements IProjectionMethod{
	protected IIndex _index;
	protected boolean _computed;
	protected int _dim;// reduced rank
	protected IIndex _latentTraining;
	protected HashMap<Integer, AVector> _randomIndexes;
	protected IMatrix matrix;
	private MATRIX_MODE matrix_mode=MATRIX_MODE.DENSE_MATRIX;

	public ARandomProjection(IIndex index, int dim) {
		_index = index;
		_computed = false;
		_dim = (dim==-1? index.getFeatureDB().getFeaturesCount() : dim);
		_latentTraining = null;
		_randomIndexes = new HashMap<Integer, AVector>();
		matrix=null;
	}

	public void setMatrixMode(MATRIX_MODE newMode){
		matrix_mode=newMode;
	}
	
	public void project() {
		initIndexes();
		
		switch(matrix_mode){
			case DENSE_MATRIX:
				matrix = new DenseMatrix(_index.getDocumentDB().getDocumentsCount(), _dim); 
				break;
			case SPARSE_MATRIX:
			default:
				matrix = new SparseMatrix(_index.getDocumentDB().getDocumentsCount(), _dim); 
				break;
		}		
		
		generateLatentMatrix(_index, matrix);

		_computed = true;
	}

	protected abstract void initIndexes();	

	protected void generateLatentMatrix(IIndex index, IMatrix mat) {
		JatecsLogger.execution().println("Start generating latent matrix");
		IIntIterator docit = index.getDocumentDB().getDocuments();
		int rawpos = 0;
		while (docit.hasNext()) {
			int doc = docit.next();
			IIntIterator featdocit = index.getContentDB().getDocumentFeatures(doc);
			while (featdocit.hasNext()) {
				int feat = featdocit.next();
				AVector rand = _randomIndexes.get(feat);
				// int times = index.getContentDB().getDocumentFeatureFrequency(doc, feat);
				double weight = index.getWeightingDB().getDocumentFeatureWeight(doc, feat);
				addIndex(mat, rawpos, rand, weight);				
			}
			
			rawpos++;
		}
	}

	public boolean isComputed() {
		return _computed;
	}

	protected void addIndex(IMatrix mat, int doc, AVector vect, double times) {
		if (vect instanceof SparseVector) {
			SparseVector v = (SparseVector) vect;
			int[] nonZeroDimensions = v.getNonZeroDimensions();
			for (int nonZeroDim : nonZeroDimensions) {
				double old = mat.get(doc, nonZeroDim);
				mat.set(doc, nonZeroDim, old +  (v.get(nonZeroDim) * times));
			}
		}
		else{
			for(int i = 0; i < vect.size(); i++){
				double old = mat.get(doc, i);
				mat.set(doc, i, old +  (vect.get(i) * times));
			}
		}
	}

	public IIndex getLatentTrainindex() {
		if (!_computed) {
			project();
		}

		if (_latentTraining != null)
			return _latentTraining;

		JatecsLogger.status().println("Start generating Latent Training Index");
		_latentTraining = buildIndexFromMatrix(matrix, _index);
		matrix=null;
		
		return _latentTraining;
	}

	public IIndex getLatentTestindex(IIndex testindex) {
		if (!_computed) {
			project();
		}

		JatecsLogger.status().println("Start generating Latent Testing Index");
		IMatrix testmatrix=null;
		if(matrix_mode==MATRIX_MODE.DENSE_MATRIX){
			testmatrix = new DenseMatrix(testindex.getDocumentDB().getDocumentsCount(), _dim);
		}
		else{
			testmatrix = new SparseMatrix(testindex.getDocumentDB().getDocumentsCount(), _dim);
		}
		generateLatentMatrix(testindex, testmatrix);

		IIndex latentTesting = buildIndexFromMatrix(testmatrix, testindex);
		return latentTesting;
	}

	private IIndex buildIndexFromMatrix(IMatrix latent, IIndex origIndex) {

		IIndex latentIndex = origIndex.cloneIndex();

		// remove exceeding features
		int nf = latentIndex.getFeatureDB().getFeaturesCount();
		int ntoremove = nf - _dim;
		if(ntoremove>0){
			int[] toRemove = new int[ntoremove];
			for (int i = 0; i < ntoremove; i++)
				toRemove[i] = i;
			latentIndex.getFeatureDB().removeFeatures(new IntArrayIterator(toRemove));
		}

		// modify feature names to "lantent_i" pseudo-names
		// to do
		// TroveFeaturesDBBuilder featureBuilder = new TroveFeaturesDBBuilder();

		// set the weights of document-features, and feature frequencies to 1 in
		// the latent index
		TroveContentDBBuilder content = new TroveContentDBBuilder(
				latentIndex.getDocumentDB(), latentIndex.getFeatureDB());
		TroveWeightingDBBuilder weighting = new TroveWeightingDBBuilder(
				content.getContentDB());
		int documents = origIndex.getDocumentDB().getDocumentsCount();
		int lantentFeats = _dim;
		for (int l = 0; l < lantentFeats; l++) {// latent features
			for (int d = 0; d < documents; d++) {// documents
				//double weight = latent[d][l];
				double weight = latent.get(d, l);
				if(weight!=0){
					content.setDocumentFeatureFrequency(d, l, 1);
					weighting.setDocumentFeatureWeight(d, l, weight);
				}
			}
		}

		latentIndex = new GenericIndex("Lantent index",
				latentIndex.getFeatureDB(), latentIndex.getDocumentDB(),
				latentIndex.getCategoryDB(), latentIndex.getDomainDB(),
				content.getContentDB(), weighting.getWeightingDB(),
				latentIndex.getClassificationDB());

		return latentIndex;
	}

	public void clearResources() {
		_index = null;
		_computed = false;
		_latentTraining = null;
		_randomIndexes.clear();
		Runtime.getRuntime().freeMemory();
	}

	
}
