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

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

public class InformationGainSmooth implements ITsrFunction {

    @Override
    public double compute(short catID, int featID, IIndex index) {
    	int D = index.getDocumentDB().getDocumentsCount();
        int numFeatAndCat = index.getFeatureCategoryDocumentsCount(featID, catID);
        int numFeatAndNotCat = index.getContentDB().getFeatureDocumentsCount(featID) - numFeatAndCat;
        int numNotFeatAndCat = index.getClassificationDB().getCategoryDocumentsCount(catID) - numFeatAndCat;
        int numNotFeatAndNotCat = D - (numFeatAndCat + numFeatAndNotCat + numNotFeatAndCat);
        return compute(numFeatAndCat, numFeatAndNotCat, numNotFeatAndCat, numNotFeatAndNotCat);
        
    }

	@Override
	public double compute(int numFeatAndCat, int numFeatAndNotCat, int numNotFeatAndCat, int numNotFeatAndNotCat) {
		//smooth
        numFeatAndCat++;
        numFeatAndNotCat++;
        numNotFeatAndCat++;
        numNotFeatAndNotCat++;
        int D = numFeatAndCat + numFeatAndNotCat + numNotFeatAndCat + numNotFeatAndNotCat + 4;

        double P_t_and_c = ((double) numFeatAndCat) / ((double) D);
        double P_not_t_and_c = ((double) numNotFeatAndCat) / ((double) D);
        double P_t_and_not_c = ((double) numFeatAndNotCat) / ((double) D);
        double P_not_t_and_not_c = ((double) numNotFeatAndNotCat) / ((double) D);

        double P_c = P_t_and_c + P_not_t_and_c;
        double P_not_c = 1.0-P_c;
        
        double P_t = P_t_and_c + P_t_and_not_c;
        double P_not_t = 1.0-P_t;
        

        assert(P_t > 0);
		assert(P_c > 0);
		assert(P_not_t > 0);
		assert(P_not_c > 0);
		
		double log_2 = Math.log(2);
		double tmp1, tmp2, tmp3, tmp4;
		tmp1 = tmp2 = tmp3 = tmp4 = 0;

		tmp1 = P_t_and_c * (Math.log( P_t_and_c / (P_t * P_c) ) / log_2);
		tmp2 = P_not_t_and_c * (Math.log( P_not_t_and_c/(P_not_t*P_c) ) / log_2);
		tmp3 = P_t_and_not_c * (Math.log( P_t_and_not_c/(P_t*P_not_c) ) / log_2);
		tmp4 = P_not_t_and_not_c * (Math.log( P_not_t_and_not_c/(P_not_t*P_not_c) ) /log_2);
		
		double v = tmp1+tmp2+tmp3+tmp4;
		
		
		return v;
	}
}
