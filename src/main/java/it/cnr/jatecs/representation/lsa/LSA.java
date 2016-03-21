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

package it.cnr.jatecs.representation.lsa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.representation.randomprojections.IProjectionMethod;
import it.cnr.jatecs.representation.vector.SparseMatrix;
import it.cnr.jatecs.representation.vector.SparseMatrix.XY;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.IntArrayIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import Jama.Matrix;

/**
 * Implementation of Latent Semantic Analysis, wrapping the SVDlibc software
 * (which might be obtained externally from {@code https://tedlab.mit.edu/~dr/SVDLIBC/}).
 * For an overview, see e.g., {@code Deerwester, S., Dumais, S. T., Furnas, G. W., Landauer, 
 * T. K., & Harshman, R. (1990). Indexing by latent semantic analysis. Journal of the American 
 * society for information science, 41(6), 391.}
 * */
public class LSA implements IProjectionMethod{

	private IIndex _index;
	private boolean _computed;
	private Matrix Ut;
	private Matrix S;
	private Matrix Vt;
	private IIndex _latentTraining;
	private SVDlibcCustomizer _customizer;
	private int _k;
	
	public LSA(IIndex index, SVDlibcCustomizer customizer) {
		_index = index;
		_computed = false;
		_customizer=customizer;
		_k=_customizer.getK();
		_latentTraining = null;
	}

	public void project() {
		JatecsLogger.status().println("Start Latent Semantic Analysis indexes generation");
		
		try {
			JatecsLogger.status().println("Start singular value decomposition");
			SVDlibc SVD = new SVDlibc(_index, _customizer);
			Ut=SVD.getU_t();
			S=SVD.getS();
			Vt=SVD.getVt();
			_k=SVD.getRank();
		} catch (IOException e) {
			JatecsLogger.execution().println("An error ocurred while runing svdlibc");
			e.printStackTrace();
		}
		
		JatecsLogger.status().println("Start projecting Training Index into Latent Space");
		Matrix docslatent = S.times(Vt);

		JatecsLogger.status().println("\tcreating Jatecs Index");
		_latentTraining = buildIndexFromMatrix(docslatent, _index);
		
		_computed = true;
	}
	

	@Override
	public IIndex getLatentTrainindex() {
		if (!_computed) {
			project();
		}		

		return _latentTraining;
	}

	@Override
	public IIndex getLatentTestindex(IIndex testindex) {
		if (!_computed) {
			project();
		}

		JatecsLogger.status().println("Start generating Latent Testing Index");
		SparseMatrix testlatent = transformIndex(testindex);
		Matrix testlatentDense = timesDenseSparse(Ut, testlatent);

		JatecsLogger.status().println("\tcreating Jatecs Index");
		IIndex latentTesting = buildIndexFromMatrix(testlatentDense, testindex);
		return latentTesting;
	}
	
	private Matrix timesDenseSparse(Matrix D, SparseMatrix S) {		
		if(D.getColumnDimension()!=S.getRowsDimension()){
			System.err.println("Dimensions must agree: exit"); System.exit(0);
		}
		int f=D.getRowDimension();
		int c=S.getColumnDimensions();
		
		List<Set<Integer>> colsNonZeroDims=new ArrayList<Set<Integer>>();
		for(int i = 0; i < c; i++)
			colsNonZeroDims.add(new HashSet<Integer>());
		for(XY xy:S.getNonZeroPositions()){
			int col=xy.y;
			int dim=xy.x;
			colsNonZeroDims.get(col).add(dim);
		}
		
		JatecsLogger.status().println("Creating a "+f+"x"+c+" matrix...");
		Matrix R=new Matrix(f, c);
		
		for(int i=0; i < f; i ++){
			for(int j = 0; j < c; j++){
				double dot=0;
				for(int k:colsNonZeroDims.get(j)){
					dot+=D.get(i, k)*S.get(k, j);
				}
				R.set(i, j, dot);
			}
		}
		
		return R;
	}

	private IIndex buildIndexFromMatrix(Matrix latent, IIndex origIndex) {
		
		IIndex latentIndex = origIndex.cloneIndex();

		// remove exceeding features
		int nf = latentIndex.getFeatureDB().getFeaturesCount();
		int ntoremove = nf - _k;
		int[] toRemove = new int[ntoremove];
		for (int i = 0; i < ntoremove; i++)
			toRemove[i] = i;
		latentIndex.getFeatureDB().removeFeatures(new IntArrayIterator(toRemove));

		// modify feature names to "lantent_i" pseudo-names

		// set the weights of document-features, and feature frequencies to 1 in the latent index
		TroveContentDBBuilder content = new TroveContentDBBuilder(latentIndex.getDocumentDB(), latentIndex.getFeatureDB());
		TroveWeightingDBBuilder weighting = new TroveWeightingDBBuilder(
				content.getContentDB());		
		for (int l = 0; l < latent.getRowDimension(); l++) {// latent features
			for (int d = 0; d < latent.getColumnDimension(); d++) {// documents
				double weight = latent.get(l, d);
				content.setDocumentFeatureFrequency(d, l, weight!=0? 1 : 0);
				weighting.setDocumentFeatureWeight(d, l, weight);				
			}
		}

		latentIndex = new GenericIndex("Lantent index",
				latentIndex.getFeatureDB(), latentIndex.getDocumentDB(),
				latentIndex.getCategoryDB(), latentIndex.getDomainDB(),
				content.getContentDB(), weighting.getWeightingDB(),
				latentIndex.getClassificationDB());

		return latentIndex;
	}

	public boolean isComputed() {
		return _computed;
	}
	
	private SparseMatrix transformIndex(IIndex index) {		
		int d = index.getDocumentDB().getDocumentsCount();
		int t = index.getFeatureDB().getFeaturesCount();

		// term x documents matrix
		SparseMatrix matrix=new SparseMatrix(t, d);		

		IIntIterator docit = index.getDocumentDB().getDocuments();
		for (int col=0; docit.hasNext();col++) {
			int doc = docit.next();

			IIntIterator featit = index.getFeatureDB().getFeatures();
			for (int fil=0; featit.hasNext(); fil++) {
				int feat = featit.next();
				double w = index.getWeightingDB().getDocumentFeatureWeight(doc, feat);
				matrix.set(fil, col, w);
			}
		}

		return matrix;
	}

	
	
	@Override
	public void clearResources() {
		_index=null;
		_computed=false;
		Ut=null;
		S=null;
		Vt=null;
		_latentTraining=null;
		Runtime.getRuntime().freeMemory();		
	}
	
	protected IIndex trainingIndex(){
		return _index;
	}
	protected void setTrainingIndex(IIndex index){
		_index = index;
	}


	
}
