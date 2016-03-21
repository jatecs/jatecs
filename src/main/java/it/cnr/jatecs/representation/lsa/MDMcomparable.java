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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Jama.Matrix;
import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IMultilingualIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.representation.randomprojections.IProjectionMethod;
import it.cnr.jatecs.representation.vector.SparseMatrix;
import it.cnr.jatecs.representation.vector.SparseMatrix.XY;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.IntArrayIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

/**
 * Implementation of the Multilingual Domain Models (MDM) of Gliozzo and Strapparava in
 * {@code Gliozzo, A., & Strapparava, C. (2005, June). Cross language text categorization 
 * by acquiring multilingual domain models from comparable corpora. In Proceedings of the
 * ACL Workshop on Building and Using Parallel Texts (pp. 9-16). Association for 
 * Computational Linguistics.}
 * (Note that the kernel of the SVM-light might be tuned as described in the article).
 * */
public class MDMcomparable implements IProjectionMethod{

	private IMultilingualIndex _index;
	private boolean _computed;
	private Matrix D; //Multilingual Domain Model
	private Matrix IidfD;
	private double[] I_IDF; //diagonal matrix of idf weights for features
	private IIndex _latentTraining;
	private SVDlibcCustomizer _customizer;
	private boolean BoW_D=false;
	private boolean normalize=true;

	public MDMcomparable(IMultilingualIndex index, SVDlibcCustomizer customizer) {
		_index = index;
		_computed = false;
		_customizer=customizer;
		_latentTraining = null;
	}
	
	public void computeDasBOW(){
		BoW_D=true;
	}
	public void computeDasMDM(){
		BoW_D=false;
	}

	public void project() {
		I_IDF=generateIidf();
		
		if(BoW_D){
			D=null;
			IidfD = null;
		}
		else{
			D=generateMDM();
			IidfD = diagonalDenseMatrixTimes(I_IDF, D);
		}
		
		JatecsLogger.status().println("Start projecting the original term-by-documents matrix into the multilingual space");
		SparseMatrix termByDocuments = transformIndex(_index);
		Matrix projected = projectIntoMDMLatentSpace(termByDocuments, normalize);
		_latentTraining = buildIndexFromMatrix(projected, _index);		

		_computed = true;
	}
	
	private Matrix generateMDM(){
		JatecsLogger.status().println("Start Multilingual Domain Model (MDM) generation");		
		
		SVDlibc SVD=null;
		try {
			SVD = new SVDlibc(_index, _customizer);
		} catch (IOException e) {
			JatecsLogger.execution().println("Error calling svdlibc.");
			e.printStackTrace();
			System.exit(0);
		}
		Matrix U=SVD.getU();
		Matrix S=SVD.getS();	
		Matrix sqrtS = S.inverse();
		
		JatecsLogger.status().println("Start creating the D matrix");
		Matrix UsqrtS=U.times(sqrtS);
		double[] diagIN = composeIN(UsqrtS);
		return diagonalDenseMatrixTimes(diagIN, UsqrtS);
	}
	
	
	private void normalizeDocumentVectors(Matrix termByDocument){
		JatecsLogger.status().println("Normalizing document-vectors");
		int rows=termByDocument.getRowDimension();
		int cols=termByDocument.getColumnDimension();
		for(int c = 0; c < cols; c++){
			double norm=0.0;
			for(int f = 0; f < rows; f++){
				norm+=(termByDocument.get(f, c)*termByDocument.get(f, c));
			}
			norm = Math.sqrt(norm);
			for(int f = 0; f < rows; f++){
				double old = termByDocument.get(f, c);
				termByDocument.set(f, c, old / norm);
			}
		}
		JatecsLogger.status().println("[done].");
	}
	
	//multiply the diagonal matrix whose elements are in diagIN by Q
	private SparseMatrix diagonalSparseMatrixTimes(double [] diagIN, SparseMatrix Q){
		int rows=Q.getRowsDimension();
		int cols=Q.getColumnDimensions();
		SparseMatrix prod = new SparseMatrix(rows, cols);
		for(int i = 0; i < rows; i++){
			double INi=diagIN[i];
			for(int j = 0; j < cols; j++){
				double old = Q.get(i, j);
				prod.set(i, j, old*INi);
			}
		}
		return prod;
	}
	
	private Matrix diagonalDenseMatrixTimes(double [] diagIN, Matrix Q){
		int rows=Q.getRowDimension();
		int cols=Q.getColumnDimension();
		Matrix prod = new Matrix(rows, cols);
		for(int i = 0; i < rows; i++){
			double INi=diagIN[i];
			for(int j = 0; j < cols; j++){
				double old = Q.get(i, j);
				prod.set(i, j, old*INi);
			}
		}
		return prod;
	}

	private double[] generateIidf() {
		JatecsLogger.status().println("Generating Iidf matrix");
		//idf is generated locally to each corpus (i.e. each feature in its language-corpus), for this reason we
		//first calculate languages each feature appears in
		HashMap<Integer, HashSet<LanguageLabel>> feat_languages = new HashMap<Integer, HashSet<LanguageLabel>>();
		IIntIterator itFeats = _index.getFeatureDB().getFeatures();
		while (itFeats.hasNext()) {
			int featID = itFeats.next();
			feat_languages.put(featID, new HashSet<LanguageLabel>());
			
			IIntIterator itDocs = _index.getContentDB().getFeatureDocuments(featID);
			while(itDocs.hasNext()){
				int docID = itDocs.next();
				feat_languages.get(featID).addAll(_index.getDocumentLanguageDB().getDocumentLanguages(docID));
			}
		}
		
		int t = _index.getFeatureDB().getFeaturesCount();		
		double [] Iidf = new double[t];
		
		itFeats = _index.getFeatureDB().getFeatures();
		int i=0;
		while (itFeats.hasNext()) {
			int featID = itFeats.next();
			
			//N is not the total number of documents in the cross-lingual corpus, but the
			//total number of documents in local corpura the features appears in, e.g. the idf of an English
			//word does only take into account the total number of English documents (note a word could be
			//shared by more than one language)
			int N = 0;
			for(LanguageLabel differentCorpora: feat_languages.get(featID)){
				N+=_index.getDocumentLanguageDB().getDocumentsInLanguage(differentCorpora).size();
			}
			
			int docsWithFeat = _index.getContentDB().getFeatureDocumentsCount(featID);			
			double idf = 0;
			if(N>0 && docsWithFeat>0)
				idf = Math.log(N*1.0/docsWithFeat);		
			Iidf[i]=idf;
			i++;			
		}
		JatecsLogger.status().println("[done].");
		return Iidf;
	}
	
	private double[] composeIN(Matrix UsqrtS){		
		int n=UsqrtS.getRowDimension();
		//IN is a n x n matrix, and n is the number of features, so this matrix is size-prohivitive
		//However IN is diagonal, only terms in the diagonal are stored.
		double[] diagonalIN=new double[n];
		for(int i = 0; i < n; i++){
			diagonalIN[i]= 1.0/Math.sqrt(innerProd(UsqrtS, i, i));
		}
		return diagonalIN;		
	}
	
	//returns the inner product of rows row_i and row_j
	private double innerProd(Matrix M, int row_i, int row_j){
		double inner = 0.0;
		
		int cols = M.getColumnDimension();				
		for(int i = 0; i < cols; i++){
			inner += (M.get(row_i, i) * M.get(row_j, i));
		}
		
		return inner;
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
		SparseMatrix termByDocTest = transformIndex(testindex);		
		Matrix projected = projectIntoMDMLatentSpace(termByDocTest, normalize);
		return buildIndexFromMatrix(projected, testindex);
	}	
	
	private Matrix projectIntoMDMLatentSpace(SparseMatrix termByDocuments, boolean normalize){
		JatecsLogger.status().println("Projecting matrix into lantent space");
		Matrix proj = null;
		if(BoW_D){
			proj = toDense(diagonalSparseMatrixTimes(I_IDF, termByDocuments));
		}
		else{
			//proj = IidfD.transpose().times(termByDocuments);
			proj = timesDenseSparse(IidfD.transpose(), termByDocuments);
		}
		
		if(normalize)
			normalizeDocumentVectors(proj);
		
		JatecsLogger.status().println("[done].");
		return proj;		
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

	private Matrix toDense(SparseMatrix sparse) {
		int f= sparse.getRowsDimension();
		int c= sparse.getColumnDimensions();
		Matrix dense=new Matrix(f, c, 0);
		for(SparseMatrix.XY xy : sparse.getNonZeroPositions()){
			double v=sparse.get(xy.x, xy.y);
			dense.set(xy.x, xy.y, v);
		}
		return dense;
	}

	private IIndex buildIndexFromMatrix(Matrix latent, IIndex origIndex) {
		JatecsLogger.status().println("Building Jatecs Index from Matrix...");
		IIndex latentIndex = origIndex.cloneIndex();

		// remove exceeding features
		int nf = latentIndex.getFeatureDB().getFeaturesCount();
		int ntoremove = nf - _customizer.getK();
		int[] toRemove = new int[ntoremove];
		for (int i = 0; i < ntoremove; i++)
			toRemove[i] = i;
		latentIndex.getFeatureDB().removeFeatures(new IntArrayIterator(toRemove));

		// modify feature names to "lantent_i" pseudo-names
		// to do
		//TroveFeaturesDBBuilder featureBuilder = new TroveFeaturesDBBuilder();		

		// set the weights of document-features, and feature frequencies to 1 in the latent index
		TroveContentDBBuilder content = new TroveContentDBBuilder(latentIndex.getDocumentDB(), latentIndex.getFeatureDB());
		TroveWeightingDBBuilder weighting = new TroveWeightingDBBuilder(
				content.getContentDB());		
		for (int l = 0; l < latent.getRowDimension(); l++) {// latent features
			for (int d = 0; d < latent.getColumnDimension(); d++) {// documents
				double weight = latent.get(l, d);
				content.setDocumentFeatureFrequency(d, l, weight!=0 ? 1 : 0);
				weighting.setDocumentFeatureWeight(d, l, weight);
			}
		}

		latentIndex = new GenericIndex("Lantent index",
				latentIndex.getFeatureDB(), latentIndex.getDocumentDB(),
				latentIndex.getCategoryDB(), latentIndex.getDomainDB(),
				content.getContentDB(), weighting.getWeightingDB(),
				latentIndex.getClassificationDB());

		JatecsLogger.status().println("[done].");
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
				double w = index.getContentDB().getDocumentFeatureFrequency(doc, feat);
				//double w = index.getWeightingDB().getDocumentFeatureWeight(doc, feat);
				matrix.set(fil, col, w);
			}
		}

		return matrix;
	}	
	
	@Override
	public void clearResources() {
		_index=null;
		_computed=false;
		D=null;
		I_IDF=null;	
		IidfD=null;
		_latentTraining=null;
		Runtime.getRuntime().freeMemory();		
	}
	
	public static void showMatrix(Matrix m){
		int f = m.getRowDimension();
		int c = m.getColumnDimension();
		System.out.println(""+f+" x " + c);
		for(int i = 0; i < f; i++){
			for(int j = 0; j < c; j++){
				System.out.print(m.get(i,j)+"\t");
			}
			System.out.println();
		}
		System.out.println();
	}
	
}


