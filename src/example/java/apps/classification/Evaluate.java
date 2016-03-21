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

package apps.classification;

import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTableDataManager;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.evaluation.HierarchicalClassificationComparer;
import it.cnr.jatecs.evaluation.util.EvaluationReport;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This app evaluates the outcome of a classification experiment, by comparing a
 * predicted classification to a true classification from a test set.
 *
 * @author Andrea Esuli
 */
public class Evaluate {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err
                    .println("Usage: Evaluate <predictedClassificationDirectory> <trueClassificationDirectory> [shortNames] [onlyLeaves]");
            return;
        }

        // Compute prediction and ground truth file names.
        String predictionFilename = args[0];
        File predictionFile = new File(predictionFilename);
        predictionFilename = predictionFile.getName();
        String predictionPath = predictionFile.getParent();
        String trueValuesFilename = args[1];
        File trueValuesFile = new File(trueValuesFilename);
        trueValuesFilename = trueValuesFile.getName();
        String trueValuesPath = trueValuesFile.getParent();

        boolean shortNames = false;
        boolean onlyLeaves = false;
        for (int i = 2; i < args.length; ++i) {
            if (args[i].equals("shortNames")) {
                System.out.println("Using short file names.");
                shortNames = true;
            }
            if (args[i].equals("onlyLeaves")) {
                System.out.println("Evaluating only leaf categories.");
                onlyLeaves = true;
            }
        }


        // Load predictions fro disk.
        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                predictionPath, false);
        storageManager.open();
        IClassificationDB predictions = TroveReadWriteHelper
                .readClassification(storageManager, predictionFilename);
        storageManager.close();

        // Load groud truth from disk.
        storageManager = new FileSystemStorageManager(trueValuesPath, false);
        storageManager.open();
        IClassificationDB trueValues = TroveReadWriteHelper.readClassification(
                storageManager, trueValuesFilename);
        storageManager.close();

        // Evaluate the obtained classifications results with ground truth. To be interested, we assume that the taxonomy in use
        // is hierarchical. The method evaluate() will return a set of contingency tables, one for each category.
        HierarchicalClassificationComparer comparer = new HierarchicalClassificationComparer(
                predictions, trueValues);
        ContingencyTableSet tableSet = comparer.evaluate(onlyLeaves);

        // Give a custom name to the contingency table set.
        tableSet.setName(predictions.getName());


        // Write contingency table set to disk.
        if (shortNames)
            ContingencyTableDataManager.writeContingencyTableSet(predictionPath
                    + Os.pathSeparator() + "hierEvalTable_"
                    + predictionFilename, tableSet);
        else
            ContingencyTableDataManager.writeContingencyTableSet(predictionPath
                    + Os.pathSeparator() + "hierEvalTable_"
                    + predictionFilename + "_" + trueValuesFilename, tableSet);

        // Get a report of the results we have obtained...
        String report = EvaluationReport.printReport(tableSet,
                trueValues.getCategoryDB());

        // and write it to disk.
        FileWriter writer;
        if (shortNames)
            writer = new FileWriter(predictionPath + Os.pathSeparator()
                    + "hierEvalResult_" + predictionFilename + ".txt");
        else
            writer = new FileWriter(predictionPath + Os.pathSeparator()
                    + "hierEvalResult_" + predictionFilename + "_"
                    + trueValuesFilename + ".txt");
        writer.write(report);
        writer.close();


        // Now evaluate the same results in a flat way, only considering the leaf codes.
        ClassificationComparer flatComparer = new ClassificationComparer(
                predictions, trueValues);
        tableSet = flatComparer.evaluate(onlyLeaves);

        tableSet.setName(predictions.getName());
        if (shortNames)
            ContingencyTableDataManager.writeContingencyTableSet(predictionPath
                    + Os.pathSeparator() + "flatEvalTable_"
                    + predictionFilename, tableSet);
        else
            ContingencyTableDataManager.writeContingencyTableSet(predictionPath
                    + Os.pathSeparator() + "flatEvalTable_"
                    + predictionFilename + "_" + trueValuesFilename, tableSet);

        report = EvaluationReport.printReport(tableSet,
                trueValues.getCategoryDB());
        if (shortNames)
            writer = new FileWriter(predictionPath + Os.pathSeparator()
                    + "flatEvalResult_" + predictionFilename + ".txt");
        else
            writer = new FileWriter(predictionPath + Os.pathSeparator()
                    + "flatEvalResult_" + predictionFilename + "_"
                    + trueValuesFilename + ".txt");
        writer.write(report);
        writer.close();
    }
}
