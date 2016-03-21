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

package it.cnr.jatecs.indexes.DB.interfaces;

import it.cnr.jatecs.indexes.DB.troveCompact.TroveParallelCorpusType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface IParallelDB {
    public IDocumentDB getDocumentDB();

    public boolean areParallelDocuments(int doc1, int doc2);

    public List<Integer> getParallelDocuments(int doc);

    public boolean hasParallelVersion(int doc);

    public void removeDocument(int doc);

    public Iterator<Integer> getAllParallelDocuments();

    public TroveParallelCorpusType getParallelCorpusType();

    public void setParallelCorpusType(TroveParallelCorpusType corpusType);

    public ArrayList<ArrayList<Integer>> getViewsClusters();
}
