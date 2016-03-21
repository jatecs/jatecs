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

package it.cnr.jatecs.classification;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TShortArrayList;

public class ClassificationResult {

    /**
     * The document ID.
     */
    public int documentID;

    /**
     * The list of category ID.
     */
    public TShortArrayList categoryID;

    /**
     * The score given by a classifier to the specified document ID.
     */
    public TDoubleArrayList score;

    public ClassificationResult() {
        categoryID = new TShortArrayList();
        score = new TDoubleArrayList();
    }

    public ClassificationResult(int size) {
        categoryID = new TShortArrayList();
        categoryID.ensureCapacity(size);
        score = new TDoubleArrayList();
        score.ensureCapacity(size);
    }
}
