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
import it.cnr.jatecs.indexing.tsr.ITsrFunction;
import it.cnr.jatecs.indexing.tsr.InformationGain;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import it.cnr.jatecs.weighting.interfaces.IWeighting3D;
import it.cnr.jatecs.weighting.mmap.MemoryMappedWeighting3DBuilder;

import java.io.File;
import java.io.IOException;

public class FeatureDistributionGenerator {
    private ITsrFunction _func;

    public FeatureDistributionGenerator() {
        _func = new InformationGain();
    }

    public IWeighting3D generateDistribution(IIndex index, String filename) {
        MemoryMappedWeighting3DBuilder builder = new MemoryMappedWeighting3DBuilder(index
                .getFeatureDB().getFeaturesCount(), index.getCategoryDB()
                .getCategoriesCount(), 1);
        File fname = new File(filename);
        fname.getParentFile().mkdirs();
        try {
            builder.open(fname.getParent(), fname.getName(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        double epsilon = 1.0 / index.getFeatureDB().getFeaturesCount();
        double normalization = 0;

        IShortIterator cats = index.getCategoryDB().getCategories();
        IIntIterator feats = index.getFeatureDB().getFeatures();
        while (feats.hasNext()) {
            int featureID = feats.next();
            cats.begin();
            normalization = 0;
            while (cats.hasNext()) {
                short catID = cats.next();

                double score = _func.compute(catID, featureID, index);

                // Smooth value.
                score += epsilon;

                builder.setWeight(score, featureID, catID, 0);
                normalization += score;
            }

            assert (normalization != 0);
            assert (!Double.isNaN(normalization));

            cats.begin();
            while (cats.hasNext()) {
                short catID = cats.next();
                double score = builder.getWeight(featureID, catID, 0);

                score /= normalization;
                assert (!Double.isNaN(score));
                builder.setWeight(score, featureID, catID, 0);
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
