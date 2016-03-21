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

package it.cnr.jatecs.satc.evaluation;

import gnu.trove.TIntHashSet;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Hashtable;

public class Reliability {

    protected double[] slopes;
    double reliability = 0.0;

    public Reliability(ClassificationScoreDB predConfidences,
                       IClassificationDB trueClassification, double[] probabilitySlopes,
                       TIntHashSet categoriesFilter) {

        this.slopes = probabilitySlopes;

        IShortIterator catsIter = trueClassification.getCategoryDB()
                .getCategories();
        IIntIterator docsIter = trueClassification.getDocumentDB()
                .getDocuments();

        double f1 = 0.0;
        // double TP = 0.0;
        // double FP = 0.0;
        // double FN = 0.0;
        // double TN = 0.0;

        while (catsIter.hasNext()) {
            short catId = catsIter.next();
            if (!categoriesFilter.contains(catId)) {
                continue;
            }
            double tp = 0.0;
            double fp = 0.0;
            double fn = 0.0;
            // double tn = 0.0;
            // double gpos = 0.0;
            // double score = 0.0;
            IIntIterator trueDocsIter = trueClassification
                    .getCategoryDocuments(catId);
            TIntHashSet trueDocs = new TIntHashSet();
            while (trueDocsIter.hasNext()) {
                trueDocs.add(trueDocsIter.next());
                // gpos++;
            }
            docsIter.begin();
            while (docsIter.hasNext()) {
                int docId = docsIter.next();
                Hashtable<Short, ClassifierRangeWithScore> catConfidences = predConfidences
                        .getDocumentScoresAsHashtable(docId);
                ClassifierRangeWithScore value = catConfidences.get(catId);
                // double predLabel = probability(Math.abs(value.score -
                // value.border), catId);
                double predLabel = scaleConfidence(value.score - value.border,
                        catId);
                // if ((value.score - value.border) <= 0.0) predLabel =
                // -predLabel;
                // System.out.println(value.score - value.border);
                if (trueDocs.contains(docId)) {
                    tp += predLabel;
                    fn += 1.0 - predLabel;
                    // //score += value.score - value.border;
                    // if ((value.score - value.border) > 0.0) {
                    // tp += predLabel;
                    // //System.out.println(docId + " " + catId + " tp " +
                    // predLabel);
                    //
                    // } else {
                    // fn += 1.0 - predLabel;
                    // //System.out.println(docId + " " + catId + " fn " +
                    // predLabel);
                    // }
                    // } else {
                    // if ((value.score - value.border) > 0.0) {
                    // fp += predLabel;
                    // //System.out.println(docId + " " + catId + " fp " +
                    // predLabel);
                    // } else {
                    // tn += 1.0 - predLabel;
                    // //System.out.println(docId + " " + catId + " tn " +
                    // predLabel);
                    // }
                } else {
                    fp += predLabel;
                }
            }
            // System.out.println(catId + " " + gpos + " " + score + " " + tp +
            // " " + fp + " " + fn)

            // to do precision and recall

            double den = (2.0 * tp) + fn + fp;
            // TP += tp;
            // FP += fp;
            // FN += fn;
            // TN += tn;
            if (den != 0) {
                f1 += (2 * tp) / den;
            } else {
                f1 += 1.0;
            }
        }
        // System.out.println(this.reliability);
        this.reliability = f1
                / trueClassification.getCategoryDB().getCategoriesCount();
        // System.out.println(this.reliability);
        // System.out.println(TP + " " + FP + " " + FN + " " + TN + " " + (new
        // F(1.0).get(TP, 0.0, FP, FN)));

    }

    public double scaleConfidence(double x, int catId) {
        double y = Math.exp(x / slopes[catId]);
        y = y / (y + 1.0);
        if (Double.isNaN(y)) {
            return x > 0 ? 1.0 : 0.0;
        } else {
            return y;
        }
    }

    // public Reliability(Classification predConfidences, IClassificationDB
    // trueClassification, double[] probabilitySlopes, TIntHashSet
    // categoriesFilter) {
    //
    // this.slopes = probabilitySlopes;
    //
    // IShortIterator catsIter =
    // trueClassification.getCategoriesDB().getCategories();
    // IIntIterator docsIter =
    // trueClassification.getDocumentsDB().getDocuments();
    //
    // double f1 = 0.0;
    // double TP= 0.0;
    // double FP = 0.0;
    // double FN = 0.0;
    // double TN = 0.0;
    //
    // while (catsIter.hasNext()) {
    // short catId = catsIter.next();
    // if (!categoriesFilter.contains(catId)) {
    // continue;
    // }
    //
    // IIntIterator trueDocsIter =
    // trueClassification.getCategoryDocuments(catId);
    // TIntHashSet trueDocs = new TIntHashSet();
    // while (trueDocsIter.hasNext()) {
    // trueDocs.add(trueDocsIter.next());
    // }
    // docsIter.begin();
    //
    // double catSumT = 0;
    // double catSumF = 0;
    //
    // double fp = 0;
    // double fn = 0;
    // double tp = 0;
    // double tn = 0;
    //
    // while (docsIter.hasNext()) {
    // int docId = docsIter.next();
    // Hashtable<Short, ClassifierRangeWithScore> catConfidences =
    // predConfidences.getEntriesHashtable(docId);
    // ClassifierRangeWithScore value = catConfidences.get(catId);
    // double predLabel = probability(Math.abs(value.score - value.border),
    // catId);
    // //double predLabel = scaleConfidence(value.score - value.border, catId);
    // //if ((value.score - value.border) <= 0.0) predLabel = -predLabel;
    // //System.out.println(value.score - value.border);
    // if (trueDocs.contains(docId)) {
    // // tp += predLabel;
    // // fn += 1.0 - predLabel;
    // // //score += value.score - value.border;
    // if ((value.score - value.border) > 0.0) {
    // catSumT += 0.5-predLabel;
    // tp++;
    // //System.out.println(docId + " " + catId + " tp " + predLabel);
    //
    // } else {
    // catSumF += predLabel;
    // fn++;
    // //System.out.println(docId + " " + catId + " fn " + predLabel);
    // }
    // } else {
    // if ((value.score - value.border) > 0.0) {
    // catSumF += predLabel;
    // fp++;
    // //System.out.println(docId + " " + catId + " fp " + predLabel);
    // } else {
    // catSumT += 0.5-predLabel;
    // tn++;
    // //System.out.println(docId + " " + catId + " tn " + predLabel);
    // }
    // // } else {
    // // fp += predLabel;
    // }
    // }
    //
    // this.reliability += (catSumF*2 + catSumT*2) / (tp+tn+fp+fn);
    // }
    // this.reliability /=
    // trueClassification.getCategoriesDB().getCategoriesCount();
    // //System.out.println(this.reliability);
    //
    //
    // }

    public double probability(double x, int catId) {
        x = Math.exp(x / slopes[catId]);
        x = 1.0 - (x / (x + 1.0));
        if (Double.isNaN(x)) {
            return 0.0;
        } else {
            return x;
        }
    }

    public double get() {
        return this.reliability;
    }

}
