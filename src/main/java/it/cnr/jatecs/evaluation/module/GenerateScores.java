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

package it.cnr.jatecs.evaluation.module;

import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

public class GenerateScores extends JatecsModule {

    protected ClassificationScoreDB _cl;
    protected String _dirname;
    public GenerateScores(IIndex testIndex, ClassificationScoreDB classification,
                          String outputDir) {
        super(testIndex, GenerateScores.class.getName());
        _cl = classification;
        _dirname = outputDir;
    }

    @Override
    protected void processModule() {

        Vector<Pair<Double, Boolean>> all = new Vector<Pair<Double, Boolean>>(
                index().getDocumentDB().getDocumentsCount());

        IShortIterator cats = index().getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();
            all.clear();

            JatecsLogger.status().print(
                    "Generating data for category " + catID + "...");

            IIntIterator docs = index().getDocumentDB().getDocuments();
            while (docs.hasNext()) {
                int docID = docs.next();

                Set<Entry<Short, ClassifierRangeWithScore>> en = _cl
                        .getDocumentScoresAsSet(docID);
                Iterator<Entry<Short, ClassifierRangeWithScore>> it = en
                        .iterator();
                Entry<Short, ClassifierRangeWithScore> entry = null;
                while (it.hasNext()) {
                    entry = it.next();
                    if (entry.getKey() == catID) {
                        Pair<Double, Boolean> p = new Pair<Double, Boolean>(
                                entry.getValue().score, index()
                                .getClassificationDB()
                                .hasDocumentCategory(docID, catID));
                        all.add(p);
                        break;
                    }
                }
            }

            Vector<Effectiveness> effec = new Vector<Effectiveness>(index()
                    .getDocumentDB().getDocumentsCount());
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

                Effectiveness e = new Effectiveness();
                e.f1 = ct.f1();
                e.accuracy = ct.accuracy();
                e.precision = ct.precision();
                e.recall = ct.recall();
                e.f1PosNeg = (ct.f1Pos() + ct.f1Neg()) / 2;
                effec.add(e);
            }

            try {
                writeDistributionForCategory(catID, all, effec);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            JatecsLogger.status().println("done.");

        }
    }

    public void writeDistributionForCategory(short catID,
                                             Vector<Pair<Double, Boolean>> all,
                                             Vector<Effectiveness> effectiveness) throws IOException {
        File f = new File(_dirname);
        if (!f.exists())
            f.mkdirs();

        String path = _dirname + Os.pathSeparator() + catID + ".txt";
        BufferedWriter out = new BufferedWriter(new FileWriter(path));

        out.write("Score\tF1\tPrecision\tRecall\tAccuracy\tF1PosNeg\n");
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
            out.write(msg);
        }

        out.close();
    }

    class Effectiveness {
        public double f1;
        public double precision;
        public double recall;
        public double accuracy;
        public double f1PosNeg;
    }
}
