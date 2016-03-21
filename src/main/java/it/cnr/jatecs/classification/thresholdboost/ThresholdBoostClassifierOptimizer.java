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

package it.cnr.jatecs.classification.thresholdboost;

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.OptimalConfiguration;
import it.cnr.jatecs.classification.ThresholdOptimizerType;
import it.cnr.jatecs.classification.interfaces.*;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeSet;
import java.util.Vector;

public class ThresholdBoostClassifierOptimizer implements IClassifierOptimizer {

    protected ThresholdOptimizerType _optimizerType;
    protected int _decimalPrecision;
    private String _dirname;

    public ThresholdBoostClassifierOptimizer() {
        _optimizerType = ThresholdOptimizerType.F1;
        _decimalPrecision = 20;
    }

    public void writeDistributionsOnDirectory(String dirname) {
        _dirname = dirname;
    }

    public void setOptimizationType(ThresholdOptimizerType t) {
        _optimizerType = t;
    }

    public OptimalConfiguration optimizeFor(ILearner learner, IIndex training,
                                            IIndex validation, TShortArrayList catsValid) {
        ThresholdBoostClassifier classifier = (ThresholdBoostClassifier) learner
                .build(training);

		/*
         * // DEBUG AdaBoostClassifier cl = classifier._cl; TIntArrayList pivots
		 * = cl.getDistinctPivots((short)0); for (int i = 0; i < pivots.size();
		 * i++) { int pivot = pivots.get(i); IIntIterator docs =
		 * training.getContentDB().getFeatureDocuments(pivot);
		 * System.out.print("In training pivot "+pivot+" appears in docs:");
		 * while(docs.hasNext()) { int docID = docs.next();
		 * System.out.print(" "+docID); } System.out.println("");
		 *
		 * docs = validation.getContentDB().getFeatureDocuments(pivot);
		 * System.out.print("In test pivot "+pivot+" appears in docs:");
		 * while(docs.hasNext()) { int docID = docs.next();
		 * System.out.print(" "+docID); } System.out.println(""); }
		 */

        ThresholdBoostCustomizerOptimizer cust = new ThresholdBoostCustomizerOptimizer();

        // For each category in index, perform validation.
        IShortIterator cats = new TShortArrayListIterator(catsValid);
        while (cats.hasNext()) {
            JatecsLogger.status().println("Optimizing category...");

            short catID = cats.next();

            cust._scores.add(new Vector<Pair<Double, Boolean>>());
            Vector<Pair<Double, Boolean>> el = cust._scores.get(cust._scores
                    .size() - 1);

            // Classify validation documents using current configuration.
            ClassificationResult[] results = classifier._cl.classify(
                    validation, catID);

            for (int i = 0; i < results.length; i++) {
                ClassificationResult res = results[i];
                Pair<Double, Boolean> pair = new Pair<Double, Boolean>(
                        res.score.get(0), validation.getClassificationDB()
                        .hasDocumentCategory(res.documentID, catID));
                el.add(pair);
            }
        }

        OptimalConfiguration conf = new OptimalConfiguration();
        conf.learnerCustomizer = learner.getRuntimeCustomizer();
        conf.classifierCustomizer = cust;

        return conf;
    }

    public void writeDistributionForCategory(short catID,
                                             Vector<Pair<Double, Boolean>> all,
                                             Vector<Effectiveness> effectiveness) {
        File f = new File(_dirname);
        if (!f.exists())
            f.mkdirs();

        String path = _dirname + Os.pathSeparator() + catID + ".txt";
        BufferedWriter out;
        try {
            out = new BufferedWriter(new FileWriter(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            out.write("Score\tF1\tPrecision\tRecall\tAccuracy\tF1PosNeg\n");
        } catch (IOException e) {
            try {
                out.close();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
            throw new RuntimeException(e);
        }
        String msg = "";
        for (int i = 0; i < all.size(); i++) {
            Pair<Double, Boolean> v = all.get(i);
            Effectiveness ef = effectiveness.get(i);

            msg = "" + Os.generateDouble(v.getFirst(), 4) + "\t"
                    + Os.generateDouble(ef.f1, 4) + "\t"
                    + Os.generateDouble(ef.precision, 4) + "\t"
                    + Os.generateDouble(ef.recall, 4) + "\t"
                    + Os.generateDouble(ef.accuracy, 4) + "\t"
                    + Os.generateDouble(ef.f1PosNeg, 4) + "\n";
            try {
                out.write(msg);
            } catch (IOException e) {
                try {
                    out.close();
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
                throw new RuntimeException(e);
            }
        }

        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void _assignBestClassifierConfiguration(
            IClassifierRuntimeCustomizer target,
            TShortArrayList externalCategories,
            Vector<IClassifierRuntimeCustomizer> customizers,
            TShortArrayList internalCategories) {
        IShortIterator cats = new TShortArrayListIterator(internalCategories);

        IClassifierRuntimeCustomizer customizer = target;

        IThresholdClassifier cValid = (IThresholdClassifier) customizer;
        int count = 0;
        while (cats.hasNext()) {
            short catID = cats.next();
            short externalCatID = externalCategories.get(count++);

            double bestThreshold = Math.abs(Double.MAX_VALUE);
            double bestEffectiveness = -Double.MAX_VALUE;

            // Merge all together is a single vector.
            Vector<Pair<Double, Boolean>> all = new Vector<Pair<Double, Boolean>>();
            for (int i = 0; i < customizers.size(); i++) {
                IClassifierRuntimeCustomizer c = customizers.get(i);

                ThresholdBoostCustomizerOptimizer cust = (ThresholdBoostCustomizerOptimizer) c;
                Vector<Pair<Double, Boolean>> el = cust._scores.get(catID);

                for (int j = 0; j < el.size(); j++) {
                    Pair<Double, Boolean> p = el.get(j);
                    all.add(p);
                }
            }

            Pair<Double, Boolean> zero = new Pair<Double, Boolean>(0.0, true);
            all.add(zero);

            Vector<Effectiveness> effec = new Vector<Effectiveness>();
            TreeSet<ThresholdStructure> ts = new TreeSet<ThresholdStructure>();
            for (int i = 0; i < all.size(); i++) {
                Pair<Double, Boolean> ref = all.get(i);

                // Build a contingency table for this configuration.
                ContingencyTable ct = new ContingencyTable();
                for (int j = 0; j < all.size(); j++) {
                    Pair<Double, Boolean> p = all.get(j);
                    if (p.getFirst() >= ref.getFirst() && p.getSecond())
                        ct.setTP(ct.tp() + 1);
                    else if (p.getFirst() >= ref.getFirst() && !p.getSecond())
                        ct.setFP(ct.fp() + 1);
                    else if (p.getFirst() < ref.getFirst() && p.getSecond())
                        ct.setFN(ct.fn() + 1);
                    else
                        ct.setTN(ct.tn() + 1);
                }

                // Evaluate this configuration.
                double effectiveness = 0;

                if (_optimizerType == ThresholdOptimizerType.ACCURACY)
                    effectiveness = ct.accuracy();
                else if (_optimizerType == ThresholdOptimizerType.ERROR)
                    effectiveness = ct.error();
                else if (_optimizerType == ThresholdOptimizerType.F1)
                    effectiveness = ct.f1();
                else if (_optimizerType == ThresholdOptimizerType.PRECISION)
                    effectiveness = ct.precision();
                else if (_optimizerType == ThresholdOptimizerType.RECALL)
                    effectiveness = ct.recall();

                ThresholdStructure st = new ThresholdStructure(i);
                st.efficiency = effectiveness;
                st.threshold = ref.getFirst();
                st.table = ct;
                assert (ts.add(st));

                if (effectiveness > bestEffectiveness) {
                    bestThreshold = ref.getFirst();
                    bestEffectiveness = effectiveness;
                } else if (effectiveness == bestEffectiveness
                        && Math.abs(ref.getFirst()) < bestThreshold) {
                    bestThreshold = Math.abs(ref.getFirst());
                    bestEffectiveness = effectiveness;
                }

                Effectiveness e = new Effectiveness();
                e.f1 = ct.f1();
                e.accuracy = ct.accuracy();
                e.precision = ct.precision();
                e.recall = ct.recall();
                effec.add(e);
            }

            if (_dirname != null) {
                writeDistributionForCategory(externalCatID, all, effec);
            }

			/*
			 * Iterator<ThresholdStructure> it = ts.iterator(); int counter = 0;
			 * while(it.hasNext()) { ThresholdStructure s = it.next();
			 * System.out.println("Actual effectiveness: "+s.efficiency+"
			 * Threshold: "+s.threshold+" tp="+s.table.tp()+ ",
			 * fp="+s.table.fp()+", tn="+s.table.tn()+", fn="+s.table.fn());
			 * counter++; if (counter == 30000) break; }
			 */

            ClassifierRange range = new ClassifierRange();
            range.border = bestThreshold;
            range.minimum = -Double.MAX_VALUE;
            range.maximum = Double.MAX_VALUE;
            cValid.setClassifierRange(externalCatID, range);

            JatecsLogger.status().println(
                    "For category " + externalCatID + " the border is "
                            + range.border + ". The best effectiveness is "
                            + bestEffectiveness);
        }

    }

    public void assignBestClassifierConfiguration(
            IClassifierRuntimeCustomizer target,
            TShortArrayList externalCategories,
            Vector<IClassifierRuntimeCustomizer> customizers,
            TShortArrayList internalCategories) {
        IShortIterator cats = new TShortArrayListIterator(internalCategories);

        IClassifierRuntimeCustomizer customizer = target;

        IThresholdClassifier cValid = (IThresholdClassifier) customizer;
        int count = 0;
        while (cats.hasNext()) {
            short catID = cats.next();
            short externalCatID = externalCategories.get(count++);

            double bestThreshold = Math.abs(Double.MAX_VALUE);
            double bestEffectiveness = -Double.MAX_VALUE;

            // Merge all together is a single vector.
            Vector<Pair<Double, Boolean>> all = new Vector<Pair<Double, Boolean>>();
            for (int i = 0; i < customizers.size(); i++) {
                IClassifierRuntimeCustomizer c = customizers.get(i);

                ThresholdBoostCustomizerOptimizer cust = (ThresholdBoostCustomizerOptimizer) c;
                Vector<Pair<Double, Boolean>> el = cust._scores.get(catID);

                for (int j = 0; j < el.size(); j++) {
                    Pair<Double, Boolean> p = el.get(j);
                    all.add(p);
                }
            }

            Pair<Double, Boolean> zero = new Pair<Double, Boolean>(0.0, true);
            all.add(zero);

            Vector<Effectiveness> effec = new Vector<Effectiveness>();
            for (int i = 0; i < all.size(); i++) {
                Pair<Double, Boolean> ref = all.get(i);

                // Build a contingency table for this configuration.
                ContingencyTable ct = new ContingencyTable();
                for (int j = 0; j < all.size(); j++) {
                    Pair<Double, Boolean> p = all.get(j);
                    if (p.getFirst() >= ref.getFirst() && p.getSecond())
                        ct.setTP(ct.tp() + 1);
                    else if (p.getFirst() >= ref.getFirst() && !p.getSecond())
                        ct.setFP(ct.fp() + 1);
                    else if (p.getFirst() < ref.getFirst() && p.getSecond())
                        ct.setFN(ct.fn() + 1);
                    else
                        ct.setTN(ct.tn() + 1);
                }

                // Evaluate this configuration.
                double effectiveness = 0;

                // effectiveness = Math.abs(ct.precision()-ct.recall());
                effectiveness = (ct.f1Pos() + ct.f1Neg()) / 2;

                // if (effectiveness < bestEffectiveness)
                if (effectiveness > bestEffectiveness) {
                    bestThreshold = ref.getFirst();
                    bestEffectiveness = effectiveness;
                } else if (effectiveness == bestEffectiveness
                        && Math.abs(ref.getFirst()) < bestThreshold) {
                    bestThreshold = Math.abs(ref.getFirst());
                    bestEffectiveness = effectiveness;
                }

                Effectiveness e = new Effectiveness();
                e.f1 = ct.f1();
                e.accuracy = ct.accuracy();
                e.precision = ct.precision();
                e.recall = ct.recall();
                e.f1PosNeg = (ct.f1Pos() + ct.f1Neg()) / 2;
                effec.add(e);
            }

            if (_dirname != null) {
                writeDistributionForCategory(externalCatID, all, effec);
            }

			/*
			 * Iterator<ThresholdStructure> it = ts.iterator(); int counter = 0;
			 * while(it.hasNext()) { ThresholdStructure s = it.next();
			 * System.out.println("Actual effectiveness: "+s.efficiency+"
			 * Threshold: "+s.threshold+" tp="+s.table.tp()+ ",
			 * fp="+s.table.fp()+", tn="+s.table.tn()+", fn="+s.table.fn());
			 * counter++; if (counter == 30000) break; }
			 */

            ClassifierRange range = new ClassifierRange();
            range.border = bestThreshold;
            range.minimum = -Double.MAX_VALUE;
            range.maximum = Double.MAX_VALUE;
            cValid.setClassifierRange(externalCatID, range);

            JatecsLogger.status().println(
                    "For category " + externalCatID + " the border is "
                            + range.border + ". The best effectiveness is "
                            + bestEffectiveness);
        }

    }

    public void assignBestLearnerConfiguration(
            ILearnerRuntimeCustomizer target,
            TShortArrayList externalCategories,
            Vector<ILearnerRuntimeCustomizer> customizers,
            TShortArrayList internalCategories) {

    }

    class Effectiveness {
        public double f1;
        public double precision;
        public double recall;
        public double accuracy;
        public double f1PosNeg;
    }

}
