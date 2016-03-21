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

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import it.cnr.jatecs.weighting.interfaces.IWeighting3D;
import it.cnr.jatecs.weighting.mmap.MemoryMappedWeighting3DBuilder;

import java.io.File;
import java.io.IOException;

public class CategoryDistributionGenerator {
    public IWeighting3D generateDistribution(IIndex index, String filename) {
        MemoryMappedWeighting3DBuilder builder = new MemoryMappedWeighting3DBuilder(index
                .getCategoryDB().getCategoriesCount(), index.getFeatureDB()
                .getFeaturesCount(), 1);
        File fname = new File(filename);
        fname.getParentFile().mkdirs();
        try {
            builder.open(fname.getParent(), fname.getName(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int numFeatures = index.getFeatureDB().getFeaturesCount();
        double w = 0;
        double normalization = 0;
        IShortIterator cats = index.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();
            IIntIterator docs = index.getClassificationDB()
                    .getCategoryDocuments(catID);

            normalization = 0;
            int totLenght = 0;
            while (docs.hasNext()) {
                int docID = docs.next();
                totLenght += index.getContentDB().getDocumentLength(docID);
            }

            for (int i = 0; i < numFeatures; i++) {
                int occ = 0;
                docs.begin();
                while (docs.hasNext()) {
                    int docID = docs.next();
                    occ += index.getContentDB().getDocumentFeatureFrequency(
                            docID, i);
                }

                w = (double) (1 + occ) / (double) (totLenght + numFeatures);
                normalization += w;
                builder.setWeight(w, catID, i, 0);
            }

            // Normalize the values.
            for (int i = 0; i < numFeatures; i++) {

                w = builder.getWeight(catID, i, 0);
                w /= normalization;
                assert (w != 0);
                builder.setWeight(w, catID, i, 0);
            }

        }

        try {
            builder.close();
            builder.open(fname.getParent(), fname.getName(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builder;

    }

    public IWeighting3D openDistribution(String filename) {
        MemoryMappedWeighting3DBuilder builder = new MemoryMappedWeighting3DBuilder();
        File fname = new File(filename);
        try {
            builder.open(fname.getParent(), fname.getName(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return builder;

    }

}
