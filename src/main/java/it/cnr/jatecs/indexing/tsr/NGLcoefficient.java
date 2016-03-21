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

public class NGLcoefficient extends GSS {

    @Override
    public double compute(short catID, int featID, IIndex index) {
        int D = index.getDocumentDB().getDocumentsCount();
        int numFeatAndCat = index.getFeatureCategoryDocumentsCount(featID, catID);
        int numFeatAndNotCat = index.getContentDB().getFeatureDocumentsCount(featID) - numFeatAndCat;
        int numNotFeatAndCat = index.getClassificationDB().getCategoryDocumentsCount(catID) - numFeatAndCat;

        //adaptation to Chi-Square
        double P_c = ((double) (numFeatAndCat + numNotFeatAndCat)) / ((double) D);
        double P_notC = 1.0 - P_c;
        double P_t = ((double) (numFeatAndCat + numFeatAndNotCat)) / ((double) D);
        double P_notT = 1.0 - P_t;

        double gss = super.compute(catID, featID, index);
        return (Math.sqrt(D) * gss) / Math.sqrt(P_t * P_notT * P_c * P_notC);
    }
}