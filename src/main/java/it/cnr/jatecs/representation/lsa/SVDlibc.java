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


import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.StreamRedirect;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import Jama.Matrix;

/**
 * Wrapper of the SVDlibc software (publicly available in {@code https://tedlab.mit.edu/~dr/SVDLIBC/}.
 * Computes the matrix factorization according to Singular Value Decomposition.
 * */
public class SVDlibc {
	
	private int _k;
	private Matrix Ut;
	private Matrix S;
	private Matrix Vt;
	private IIndex _index;
	private SVDlibcCustomizer _customizer;
	
	public SVDlibc(IIndex index, SVDlibcCustomizer customizer) throws IOException{
		_index = index;
		_customizer=customizer;
		_k = _customizer.getK();
		Ut=null;
		S=null;
		Vt=null;
		SVD();
	}
	
	public Matrix getU(){
		return Ut.transpose();
	}
	public Matrix getU_t(){
		return Ut;
	}
	
	public Matrix getS(){
		return S;		
	}
	
	public Matrix getVt(){
		return Vt;
	}
	
	public int getRank(){
		return _k;
	}
	
	private void SVD() throws IOException{
		sout("Start processing Singular Value Decomposition");
		sout("Creating data matrices");
		//set the input file path		
		File inputFile = File.createTempFile("svd_in"+System.currentTimeMillis(), null);
		String dataMatrixContent = createSparseTextInputFileContent(_index, _customizer.isUseFrequencies());
		writeFile(inputFile.getAbsolutePath(), dataMatrixContent);
		
		sout("Creating process");
		ProcessBuilder builder = new ProcessBuilder();
		ArrayList<String> cmdList = new ArrayList<String>();

		cmdList.add(_customizer.getSVDlibcPath());
		//input data format (sparse text) 
		cmdList.add("-r");
		cmdList.add("st");
		
		//max dimensions to compute
		cmdList.add("-d");
		cmdList.add(""+_k);
		
		//set output path
		cmdList.add("-o");
		Path outDir = Files.createTempDirectory("svd"+System.currentTimeMillis());
		String outDirAbsPath=outDir.toAbsolutePath().toString();
		cmdList.add(outDirAbsPath + Os.pathSeparator());
		cmdList.add(inputFile.getAbsolutePath());
					
		String[] cmd = cmdList.toArray(new String[0]);

		if(_customizer.isVerbose()){
			JatecsLogger.status().print("Executing: ");
			for (String string : cmdList) {
				JatecsLogger.status().print(string);
				JatecsLogger.status().print(" ");
			}
			JatecsLogger.status().println("");
		}

		builder.command(cmd);
		Process learnP;
		try {
			learnP = builder.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (_customizer.isVerbose()) {
			// any error message?
			StreamRedirect errorRedirect = new StreamRedirect(
					learnP.getErrorStream(), "svdlibc ERR");

			// any output?
			StreamRedirect outputRedirect = new StreamRedirect(
					learnP.getInputStream(), "svdlibc OUT");

			// kick them off
			errorRedirect.start();
			outputRedirect.start();
		}

		// any error???
		int exitVal;
		try {
			exitVal = learnP.waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		sout("svdlibc exit value: " + exitVal);

		sout("svdlibc getting out matrixes...");
		File matrixUt = new File(outDirAbsPath+Os.pathSeparator()+"-Ut");
		File matrixS = new File(outDirAbsPath+Os.pathSeparator()+"-S");		
		File matrixVt = new File(outDirAbsPath+Os.pathSeparator()+"-Vt");
		S = loadS(matrixS.getAbsolutePath());
		_k=S.getRowDimension();
		Ut = loadDenseMatrixFromText(matrixUt.getAbsolutePath(), _k);		
		Vt = loadDenseMatrixFromText(matrixVt.getAbsolutePath(), _k);
		
		
		
		sout("svdlibc removing termporal files...");
		File toRemoveOutDir=new File(outDirAbsPath);
		toRemoveOutDir.delete();
		inputFile.delete();
		
		sout("[done.]");
	}
	
	public static void writeFile(String path, String content) throws IOException{
		FileWriter writer = new FileWriter(path);
		writer.write(content);
		writer.close();
	}		
	
	private static String createSparseTextInputFileContent(IIndex index, boolean useFrequencies){
		int d = index.getDocumentDB().getDocumentsCount();
		int t = index.getFeatureDB().getFeaturesCount();
		
		int nonZeros = 0;
		
		StringBuilder st = new StringBuilder();
		IIntIterator docit = index.getDocumentDB().getDocuments();//columns
		while(docit.hasNext()) {
			int doc = docit.next();
			int nonZeroForDoc = 0;//index.getContentDB().getDocumentFeaturesCount(doc);			
			
			StringBuilder column = new StringBuilder();			
			double w=-1;
			IIntIterator featit = index.getFeatureDB().getFeatures();
			for (int fil=0; featit.hasNext(); fil++) {
				int feat = featit.next();
				if(index.getContentDB().hasDocumentFeature(doc, feat)){
					if(useFrequencies){
						w = index.getContentDB().getDocumentFeatureFrequency(doc, feat);
					}
					else{
						w = index.getWeightingDB().getDocumentFeatureWeight(doc, feat);
					}
					column.append(fil + " " + w + "\n");
					nonZeroForDoc++;
				}				
			}			
			st.append(nonZeroForDoc+"\n"+column);
			nonZeros+=nonZeroForDoc;
		}	
		
		StringBuilder head_st = new StringBuilder();
		head_st.append(t+" "+d+" "+nonZeros+"\n");
		head_st.append(st);		
		return head_st.toString();
	}
	
	private static Matrix loadDenseMatrixFromText(String path, int k) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(path));
		String [] matrixInfo = br.readLine().trim().split("\\s+");
		
		int filas = Math.min(Integer.parseInt(matrixInfo[0]), k);
		int cols = Integer.parseInt(matrixInfo[1]);
		double[][]m=new double[filas][cols];
		
		String rawdata;
		int f=0;
		while (f<filas && (rawdata = br.readLine()) != null) {
		   String[] vals = rawdata.split("\\s+");
		   assert(vals.length==cols);
		   for(int c = 0; c < cols; c++){
			   double val = Double.parseDouble(vals[c]);
			   m[f][c]=val;
		   }
		   f++;
		}
		
		br.close();
		JatecsLogger.status().println("svdlibc matrix dimensions: " + filas + " x " + cols);
		return new Matrix(m);
	}
	
	private static Matrix loadS(String path) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(path));
		String matrixInfo = br.readLine().trim();
		int rank = Integer.parseInt(matrixInfo);
		Matrix s = new Matrix(rank, rank, 0.0);	
		for(int i = 0; i < rank; i++){
			double eigval = Double.parseDouble(br.readLine());
			s.set(i, i, eigval);
		}
		br.close();		
		JatecsLogger.status().println("svdlibc matrix S dimensions: " + rank + " x " + rank);
		return s;
	}
	
	private void sout(String msg){
		if(_customizer.isVerbose()){
			JatecsLogger.status().println(msg);
		}
	}
}
