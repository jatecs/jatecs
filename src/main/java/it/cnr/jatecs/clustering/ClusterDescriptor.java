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

package it.cnr.jatecs.clustering;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;

public class ClusterDescriptor {

    /**
     * The description (if available) of this cluster.
     */
    public String description;


    /**
     * The list of generic documents contained in this cluster.
     */
    public TIntArrayList documents;


    /**
     * The distance of each document from the centroid of this cluster.
     */
    public TDoubleArrayList distance;


    /**
     * The centroid which describes this cluster.
     */
    public double[] centroid;


    public ClusterDescriptor() {
        description = "";
        documents = new TIntArrayList();
        distance = new TDoubleArrayList();
    }


}
