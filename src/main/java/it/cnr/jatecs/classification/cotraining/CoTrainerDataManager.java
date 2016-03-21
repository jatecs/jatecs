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

package it.cnr.jatecs.classification.cotraining;

import gnu.trove.TDoubleArrayList;
import it.cnr.jatecs.utils.Os;

import java.io.*;

public class CoTrainerDataManager {
    public void write(String outDir, CotrainOutputData data) throws Exception {
        java.io.File f = new java.io.File(outDir);
        f.mkdirs();

        String fname = outDir + Os.pathSeparator() + "cotraining.db";
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(fname)));

        os.writeInt(data.catsThreshold.size());
        for (int i = 0; i < data.catsThreshold.size(); i++) {
            os.writeDouble(data.catsThreshold.get(i));
        }

        // Close the stream.
        os.close();
    }

    public void read(String inputDir, CotrainOutputData data) throws Exception {
        java.io.File f = new java.io.File(inputDir);
        if (!f.exists())
            throw new FileNotFoundException("The input directory " + inputDir
                    + " does not exist!");

        String fname = inputDir + Os.pathSeparator() + "cotraining.db";
        DataInputStream is = new DataInputStream(
                new java.io.BufferedInputStream(new FileInputStream(fname)));

        data.catsThreshold = new TDoubleArrayList();
        int numCats = is.readInt();
        for (int i = 0; i < numCats; i++) {
            double threshold = is.readDouble();
            data.catsThreshold.add(threshold);
        }

        is.close();
    }
}
