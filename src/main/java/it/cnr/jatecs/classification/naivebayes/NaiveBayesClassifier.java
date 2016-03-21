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

package it.cnr.jatecs.classification.naivebayes;

import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.RangeShortIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import it.cnr.jatecs.weighting.mmap.MemomyMappedWeighting3D;

import java.io.File;
import java.io.IOException;

public class NaiveBayesClassifier extends BaseClassifier {

    MemomyMappedWeighting3D _weights;
    private String _modelDir;
    public NaiveBayesClassifier() {
        super();
        _customizer = new NaiveBayesClassifierCustomizer();
        _modelDir = null;
        _weights = null;
    }

    protected void read(String modelDir) throws IOException {
        if (_weights != null)
            _weights.close();
        _weights = new MemomyMappedWeighting3D();
        _weights.open(modelDir, "naivebayes", false);
        _modelDir = modelDir;
    }

    public void write(String modelDir) throws IOException {
        if (_weights.isOpen())
            _weights.close();
        Os.copy(new File(_modelDir + Os.pathSeparator() + "naivebayes"),
                new File(modelDir + Os.pathSeparator() + "naivebayes"));
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        ClassificationResult cr = new ClassificationResult();
        cr.documentID = docID;
        IShortIterator cats = new RangeShortIterator((short) 0,
                (short) _weights.getSecondDimensionSize());
        if (((NaiveBayesClassifierCustomizer) _customizer)._trueProbabilities) {
            while (cats.hasNext()) {
                short cat = cats.next();
                double score = 1.0;
                IIntIterator feats = testIndex.getContentDB()
                        .getDocumentFeatures(docID);
                while (feats.hasNext()) {
                    int feat = feats.next();
                    score *= _weights.getWeight(feat, cat, 0);
                }
                double catRatio = _weights.getWeight(_weights
                        .getFirstDimensionSize() - 1, cat, 0);
                score *= catRatio;
                score = score / (score + catRatio);
                cr.score.add(score);
                cr.categoryID.add(cat);
            }
        } else {
            while (cats.hasNext()) {
                short cat = cats.next();
                double score = 1.0;
                IIntIterator feats = testIndex.getContentDB()
                        .getDocumentFeatures(docID);
                while (feats.hasNext()) {
                    int feat = feats.next();
                    score *= _weights.getWeight(feat, cat, 0);
                }
                score *= _weights.getWeight(
                        _weights.getFirstDimensionSize() - 1, cat, 0);
                cr.score.add(score);
                cr.categoryID.add(cat);
            }
        }
        return cr;
    }

    public ClassifierRange getClassifierRange(short catID) {
        ClassifierRange cr = new ClassifierRange();
        if (((NaiveBayesClassifierCustomizer) _customizer)._trueProbabilities) {
            cr.minimum = 0.0;
            cr.maximum = 1.0;
            cr.border = 0.5;
        } else {
            cr.minimum = 0.0;
            cr.maximum = Double.MAX_VALUE;
            cr.border = 1.0;
        }
        return cr;
    }

    @Override
    public int getCategoryCount() {
        return _weights.getSecondDimensionSize();
    }

    @Override
    public IShortIterator getCategories() {
        return new RangeShortIterator((short) 0, (short) _weights
                .getSecondDimensionSize());
    }

    @Override
    public void destroy() {
        if (_weights != null && _modelDir != null) {
            if (_weights.isOpen()) {
                try {
                    _weights.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            _weights = null;
            Os.delete(new File(_modelDir + Os.pathSeparator() + "naivebayes"));
            Os.deleteDirectory(new File(_modelDir));
            _modelDir = null;
        }
    }

}
