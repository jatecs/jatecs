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

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.weighting.interfaces.IWeighting3D;
import it.cnr.jatecs.weighting.mmap.MemoryMappedWeighting3DBuilder;

import java.io.IOException;

public abstract class KnnDocumentWeightProcessor {

    protected static final String FNAME = "matrix.db";

    protected IKnnSearcher _searcher;

    protected IIndex _index;

    protected MemoryMappedWeighting3DBuilder _matrix;

    protected int _k;

    protected String _outputDir;


    public KnnDocumentWeightProcessor(IIndex index, IKnnSearcher searcher, String outputDir) {
        _searcher = searcher;
        _index = index;
        MemoryMappedWeighting3DBuilder builder = new MemoryMappedWeighting3DBuilder(index.getCategoryDB().getCategoriesCount(), index.getDocumentDB().getDocumentsCount(), 1);
        try {
            builder.open(outputDir, FNAME, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        _matrix = builder;
    }


    public abstract void computeWeights();


    public IWeighting3D getMatrixWeights() {
        return _matrix.getWeighting();
    }


    public void close() throws IOException {
        _matrix.close();
    }
}
