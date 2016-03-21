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

package apps.trainingDataCleaning;

import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class EvaluatePerturbation {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err
                    .println("Usage: EvaluatePerturbation <perturbationFile_withoutExtension> <indexDirectory>");
            return;
        }

        File file = new File(args[0]);
        String pertPath = file.getParentFile().getPath();
        String pertName = file.getName();

        file = new File(args[1]);
        String indexPath = file.getParent();
        String indexName = file.getName();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                indexPath, false);
        storageManager.open();
        IClassificationDB index = TroveReadWriteHelper.readClassification(
                storageManager, indexName, TroveClassificationDBType.Full);
        storageManager.close();

        int catCount = index.getCategoryDB().getCategoriesCount();
        int[] perts = new int[catCount];
        int[] totals = new int[catCount];

        for (int i = 0; i < catCount; ++i) {
            perts[i] = 0;
            totals[i] = 0;
        }

        FileReader freader = new FileReader(pertPath + Os.pathSeparator()
                + pertName + ".txt");
        BufferedReader in = new BufferedReader(freader);
        String line;
        while ((line = in.readLine()) != null) {
            String[] fields = line.split("\t");
            int doc = Integer.parseInt(fields[0]);
            short cat = Short.parseShort(fields[1]);
            if (index.hasDocumentCategory(doc, cat))
                ++perts[cat];
            ++totals[cat];
        }
        in.close();

        double avg = 0.0;
        for (int i = 0; i < catCount; ++i) {
            double val = ((double) perts[i]) / totals[i];
            System.out.println(i + "\t" + val);
            avg += val;
        }
        System.out.println("avg\t" + (avg / catCount));
    }
}
