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

package it.cnr.jatecs.crosslingual;

import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.iterators.IntArrayIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class LSAbasic {

	Matrix matrix;
	int _k = 2;
	int t = 8;
	int d = 5;

	@Before
	public void init() {

		double[][] m = new double[t][d];
		for (int i = 0; i < t; i++)
			for (int j = 0; j < d; j++)
				m[i][j] = 0;

		m[0][0] = 1;
		m[0][2] = 1;
		m[1][0] = 1;
		m[1][1] = 1;
		m[2][1] = 1;
		m[3][1] = 1;
		m[3][2] = 1;
		m[4][3] = 1;
		m[5][2] = 1;
		m[5][3] = 1;
		m[6][3] = 1;
		m[7][3] = 1;
		m[7][4] = 1;

		matrix = new Matrix(m);
	}

	@Test
	public void lsa() throws IOException {
		show("A:");
		show(matrix);
		SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
		Matrix U = svd.getU();
		Matrix S = svd.getS();
		Matrix Vt = svd.getV().transpose();

		U = submatrix(t, _k, U);
		S = submatrix(_k, _k, S);
		Vt = submatrix(_k, d, Vt);

		show("U:");
		show(U);
		show("S:");
		show(S);
		show("Vt:");
		show(Vt);

		Matrix aprox = U.times(S.times(Vt));
		show("Approximation");
		show(aprox);

		Matrix docs = S.times(Vt);
		show("Latent Documents");
		show(docs);

		Matrix T = new Matrix(t, 1);
		T.set(3, 0, 1);
		T.set(5, 0, 1);
		show("Test");
		show(T);

		Matrix latentT = T.transpose().times(U.times(S));
		show("Latent Test");
		show(latentT);

		FileSystemStorageManager storageManager = new FileSystemStorageManager(
				"D:/CrossLingualCorpora/CLTC/JRC-AcquisLittleIndexes/NonWeighted/Index_EN",
				false);
		storageManager.open();
		IIndex index = TroveReadWriteHelper.readIndex(storageManager,
				"Training", TroveContentDBType.Full,
				TroveClassificationDBType.Full);
		storageManager.close();
		int docsindex = index.getDocumentDB().getDocumentsCount();
		int ntoremove = docsindex - d;
		int[] toremove = new int[ntoremove];
		for (int i = 0; i < ntoremove; i++)
			toremove[i] = i;
		index.getDocumentDB().removeDocuments(new IntArrayIterator(toremove));
		IIndex latentIndex = buildIndexFromMatrix(docs, index);
		int doc = latentIndex.getDocumentDB().getDocumentsCount();
		int feat = latentIndex.getFeatureDB().getFeaturesCount();

		System.out.println("Weights");
		for (int i = 0; i < feat; i++) {
			for (int j = 0; j < doc; j++) {
				System.out.print(latentIndex.getWeightingDB()
						.getDocumentFeatureWeight(j, i) + " ");
			}
			System.out.println();
		}
		System.out.println("Content");
		for (int i = 0; i < feat; i++) {
			for (int j = 0; j < doc; j++) {
				System.out.print(latentIndex.getContentDB()
						.getDocumentFeatureFrequency(j, i) + " ");
			}
			System.out.println();
		}
		storageManager = new FileSystemStorageManager(
				"D:/CrossLingualCorpora/CLTC/JRC-AcquisLittleIndexes/NonWeighted/Index_EN",
				false);
		storageManager.open();
		TroveReadWriteHelper.writeIndex(storageManager, latentIndex, "Latent",
				true);
		latentIndex = TroveReadWriteHelper.readIndex(storageManager, "Latent",
				TroveContentDBType.Full, TroveClassificationDBType.Full);
		storageManager.close();
		Matrix read = transformIndex(latentIndex);
		show("Latent Matrix read");
		show(read);

	}

	public void show(Matrix m) {
		int f = m.getRowDimension();
		int c = m.getColumnDimension();
		for (int i = 0; i < f; i++) {
			for (int j = 0; j < c; j++) {
				System.out.print(s(m.get(i, j)) + " ");
			}
			System.out.println();
		}
		System.out.println();
	}

	public void show(String msg) {
		System.out.println(msg);
	}

	public String s(double val) {
		String m = "" + val;
		return m.substring(0, Math.min(m.length(), 6));
	}

	private Matrix submatrix(int f, int c, Matrix orig) {
		Matrix zero = new Matrix(f, c);
		zero.setMatrix(0, f - 1, 0, c - 1, orig);
		return zero;
	}

	private IIndex buildIndexFromMatrix(Matrix latent, IIndex origIndex) {

		IIndex latentIndex = origIndex.cloneIndex();

		// remove exceeding features
		int nf = latentIndex.getFeatureDB().getFeaturesCount();
		int ntoremove = nf - _k;
		int[] toRemove = new int[ntoremove];
		for (int i = 0; i < ntoremove; i++)
			toRemove[i] = i;
		latentIndex.getFeatureDB().removeFeatures(
				new IntArrayIterator(toRemove));

		// modify feature names to "lantent_i" pseudo-names
		// <to do>

		// set the weights of document-features, and feature frequencies to 1 in
		// the latent index
		TroveWeightingDBBuilder weighting = new TroveWeightingDBBuilder(
				latentIndex.getContentDB());
		TroveContentDBBuilder content = new TroveContentDBBuilder(
				latentIndex.getDocumentDB(), latentIndex.getFeatureDB());
		for (int l = 0; l < latent.getRowDimension(); l++) {// latent features
			for (int d = 0; d < latent.getColumnDimension(); d++) {// documents
				double weight = latent.get(l, d);
				weighting.setDocumentFeatureWeight(d, l, weight);
				content.setDocumentFeatureFrequency(d, l, 1);
			}
		}

		latentIndex = new GenericIndex("Lantent index",
				latentIndex.getFeatureDB(), latentIndex.getDocumentDB(),
				latentIndex.getCategoryDB(), latentIndex.getDomainDB(),
				content.getContentDB(), weighting.getWeightingDB(),
				latentIndex.getClassificationDB());

		return latentIndex;
	}

	private Matrix transformIndex(IIndex index) {
		int d = index.getDocumentDB().getDocumentsCount();
		int t = index.getFeatureDB().getFeaturesCount();

		// term x documents matrix
		double[][] matrix = new double[t][d];

		for (IIntIterator docit = index.getDocumentDB().getDocuments(); docit
				.hasNext();) {
			int doc = docit.next();
			for (IIntIterator featit = index.getFeatureDB().getFeatures(); featit
					.hasNext();) {
				int feat = featit.next();
				double w = index.getContentDB().getDocumentFeatureFrequency(
						doc, feat);
				matrix[feat][doc] = w;
			}
		}

		return new Matrix(matrix);
	}
}
