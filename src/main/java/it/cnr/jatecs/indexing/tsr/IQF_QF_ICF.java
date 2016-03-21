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

/**
 * From:
 * Quan, X., Wenyin, L., & Qiu, B. (2011). Term weighting schemes for question
 * categorization. Pattern Analysis and Machine Intelligence, IEEE Transactions
 * on, 33(5), 1009-1021.
 */
public class IQF_QF_ICF extends QF_ICF {
    @Override
    public double compute(short catID, int featID, IIndex index) {
        int D = index.getDocumentDB().getDocumentsCount();
        int C = index.getClassificationDB().getCategoryDocumentsCount(catID);
        int tp = index.getFeatureCategoryDocumentsCount(featID, catID);
        int fp = index.getContentDB().getFeatureDocumentsCount(featID) - tp;

        //question frequency
        double qf = QF(tp);

        //category frequency (#cats containing feat)
        double icf = ICF(index, featID, C);

        //ATTENTION, there is a mismatch between the original reference notation and ours.
        //they have interchange fp and fn
        double iqf = Math.log(D * 1.0 / (tp + fp));

        return iqf * qf * icf;
    }
}
