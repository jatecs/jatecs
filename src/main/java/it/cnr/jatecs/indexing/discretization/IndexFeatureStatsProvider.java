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

package it.cnr.jatecs.indexing.discretization;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public abstract class IndexFeatureStatsProvider implements IStatsProvider {

    private IIndex index;
    private int featureID;

    public IndexFeatureStatsProvider(IIndex index) {
        if (index == null)
            throw new NullPointerException("The specified index is 'null'");

        this.index = index;
        this.featureID = 0;
    }

    public IIndex getIndex() {
        return index;
    }

    public void setIndex(IIndex index) {
        if (index == null)
            throw new NullPointerException("The specified index is 'null'");
        this.index = index;
    }

    public int getFeatureID() {
        return featureID;
    }

    public void setFeatureID(int featureID) {
        if (featureID >= index.getFeatureDB().getFeaturesCount())
            throw new IllegalArgumentException(
                    "The specified feature ID is invalid: " + featureID);

        this.featureID = featureID;
    }

    @Override
    public int getNumOfClasses() {
        return index.getCategoryDB().getCategoriesCount();
    }

    @Override
    public int getNumTotalExamples() {
        return index.getContentDB().getFeatureDocumentsCount(getFeatureID());
    }

    @Override
    public int getNumExamplesInIntervalClass(DiscreteBin discreteBin,
                                             int classIdx) {
        int numExamples = 0;
        IIntIterator docs = index.getContentDB().getFeatureDocuments(
                getFeatureID());
        while (docs.hasNext()) {
            int docID = docs.next();
            double valueToDiscretize = getValueToDiscretize(docID,
                    getFeatureID());
            if (index.getClassificationDB().hasDocumentCategory(docID,
                    (short) classIdx)
                    && valueToDiscretize >= discreteBin.getStartValue()
                    && valueToDiscretize < discreteBin.getEndValue()) {
                numExamples++;
            }
        }

        return numExamples;
    }

    /**
     * Get the value used in discretization process for the specified document
     * ID and feature ID.
     *
     * @param docID     The document ID.
     * @param featureID The feature ID.
     * @return The value used in discretization process for the specified
     * document ID and feature ID.
     */
    protected abstract double getValueToDiscretize(int docID, int featureID);

    @Override
    public int getNumExamplesInInterval(DiscreteBin discreteBin) {
        int numExamples = 0;
        IIntIterator docs = index.getContentDB().getFeatureDocuments(
                getFeatureID());
        while (docs.hasNext()) {
            int docID = docs.next();
            double valueToDiscretize = getValueToDiscretize(docID,
                    getFeatureID());
            if (valueToDiscretize >= discreteBin.getStartValue()
                    && valueToDiscretize < discreteBin.getEndValue()) {
                numExamples++;
            }
        }

        return numExamples;
    }

    @Override
    public int getNumExamplesInClass(int classIdx) {
        int numExamples = 0;
        IIntIterator docs = index.getContentDB().getFeatureDocuments(
                getFeatureID());
        while (docs.hasNext()) {
            int docID = docs.next();
            if (index.getClassificationDB().hasDocumentCategory(docID,
                    (short) classIdx)) {
                numExamples++;
            }
        }

        return numExamples;
    }

}
