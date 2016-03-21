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

package it.cnr.jatecs.indexing.tsr.ranks;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Abstracts a ranking of the indexes of the features.
 * Synchronized.
 *
 * @author Alejandro Moreo Fernandez (using Jatecs)
 */
public class Rank {

    private ArrayList<Integer> rank;
    private HashMap<Integer, Integer> hash_rank;//each element is mapped to its rank, to allow fast access
    private String name;

    public Rank(String name, ArrayList<Integer> rank) {
        this.name = name;
        this.rank = rank;
    }

    public ArrayList<Integer> getRank() {
        return rank;
    }

    public synchronized HashMap<Integer, Integer> getHashRank() {
        //lazy call
        if (hash_rank == null) {
            computeHashRank();
        }
        return hash_rank;
    }

    public String getName() {
        return name;
    }

    private synchronized void computeHashRank() {
        hash_rank = new HashMap<Integer, Integer>();
        for (int i = 0; i < rank.size(); i++) {
            hash_rank.put(rank.get(i), i);
        }
    }

    int size() {
        return this.rank.size();
    }
}
