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

package apps.output;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexing.corpus.output.SvmLightOutput;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.File;

public class DumpSVMLight {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err
                    .println("Usage: DumpSVMLight <indexDirectory> [<outputFile>]");
            return;
        }

        File file = new File(args[0]);
        String indexName = file.getName();
        String dataPath = file.getParent();

        String outputName;
        if (args.length > 1) {
            outputName = args[1];
        } else {
            if (indexName.endsWith(".idx")) {
                outputName = indexName.substring(0, indexName.length() - 4)
                        + "_vectors";
            } else {
                outputName = indexName + "_vectors-";
            }
        }

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                dataPath, false);
        storageManager.open();
        IIndex index = TroveReadWriteHelper
                .readIndex(storageManager, indexName);
        storageManager.close();
        SvmLightOutput.Write(outputName, index);
    }
}
