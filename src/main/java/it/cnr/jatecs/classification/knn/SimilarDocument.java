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

package it.cnr.jatecs.classification.knn;

public class SimilarDocument implements Comparable<SimilarDocument> {
    /**
     * The document ID of the similar object.
     */
    public int docID;


    /**
     * The score assigned to the similar object.
     */
    public double score;


    public int compareTo(SimilarDocument o) {
        if (score > o.score)
            return 1;
        else if (score < o.score)
            return -1;
        else {
            if (docID > o.docID)
                return 1;
            else if (docID < o.docID)
                return -1;
            else
                return 0;
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SimilarDocument))
            return false;

        SimilarDocument d = (SimilarDocument) obj;

        return (compareTo(d) == 0);
    }


}
