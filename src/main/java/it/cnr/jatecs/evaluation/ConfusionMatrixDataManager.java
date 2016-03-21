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

package it.cnr.jatecs.evaluation;

import it.cnr.jatecs.utils.Os;

import java.io.*;

public class ConfusionMatrixDataManager {
    /**
     * Writes the given confusion matrix on specified filename.
     *
     * @param outDir The directory where the CM data will be written.
     * @param ct     The confusion matrix to write.
     * @throws IOException .
     */
    public static void writeConfusionMatrix(String outDir, ConfusionMatrix cm) throws IOException {
        File fparent = new File(outDir);
        fparent.mkdirs();
        File f = new File(outDir + Os.pathSeparator() + "cm");

        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        os.writeInt(cm.getNumCategories());
        for (short i = 0; i < cm.getNumCategories(); i++) {
            for (short j = 0; j < cm.getNumCategories(); j++) {
                os.writeInt(cm.getError(i, j));
            }
        }
        os.close();
    }

    /**
     * Reads the given directory and returns the corresponding confusion matrix object.
     *
     * @param inputDir The input directory to read.
     * @return The corresponding confusion matrix.
     * @throws JatecsException Raised if some error occurs during the methoed execution.
     */
    public static ConfusionMatrix readConfusionMatrix(String inputDir) throws IOException {
        File f = new File(inputDir + Os.pathSeparator() + "cm");

        DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
        int numCategories = is.readInt();
        ConfusionMatrix cm = new ConfusionMatrix(numCategories);
        for (short i = 0; i < numCategories; i++) {
            for (short j = 0; j < numCategories; j++) {
                int numErrors = is.readInt();
                cm.setError(i, j, numErrors);
            }
        }
        is.close();

        return cm;
    }
}
