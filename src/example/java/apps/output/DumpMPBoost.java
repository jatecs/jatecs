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
import it.cnr.jatecs.indexing.corpus.output.MpBoostOutput;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.File;

public class DumpMPBoost {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err
                    .println("Dumps an index in MPBoost++ format, see http://www.esuli.it/mpboost/");
            System.err
                    .println("Usage: DumpMPBoost <indexDirectory> <outputFile>");
            return;
        }

        File file = new File(args[0]);

        String outputName = args[1];

        String indexName = file.getName();
        String dataPath = file.getParent();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                dataPath, false);
        storageManager.open();
        IIndex index = TroveReadWriteHelper
                .readIndex(storageManager, indexName);
        storageManager.close();

        MpBoostOutput.Write(outputName, index);
    }
}
