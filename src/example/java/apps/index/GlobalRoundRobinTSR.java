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

package apps.index;


import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexing.tsr.InformationGain;
import it.cnr.jatecs.indexing.tsr.RoundRobinTSR;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.File;
import java.io.IOException;

public class GlobalRoundRobinTSR {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: " + GlobalRoundRobinTSR.class.getName() + " <inputIndexDirectory> <outputTSRIndexDirectory>");
            return;
        }

        String inputIndexDir = args[0];
        String outputIndexDir = args[1];

        File file = new File(inputIndexDir);
        String indexName = file.getName();
        String indexPath = file.getParent();

        // Load input index.
        FileSystemStorageManager storageManager = new FileSystemStorageManager(indexPath, false);
        storageManager.open();// Open a storage manager on output directory.
        IIndex inputIndex = TroveReadWriteHelper.readIndex(storageManager,
                indexName, TroveContentDBType.Full, TroveClassificationDBType.Full);
        storageManager.close();

        // Example using round-robin global TSR with InformationGain (many other TSR functions and policies
        // are also implemented in Jatecs).
        IIndex tsrIndex = inputIndex.cloneIndex(); // Create a copy of original input index.
        RoundRobinTSR tsr = new RoundRobinTSR(new InformationGain());

        // Retain 5000 best features (globally) from the index.
        tsr.setNumberOfBestFeatures(5000);
        // Or use a percentage of the total features available in the input index (e.g. 20%).
        //tsr.setNumberOfBestFeaturesInPercentage(20);

        tsr.computeTSR(tsrIndex); // Apply TSR and update specified index.

        // Save TSR index.        
        storageManager = new FileSystemStorageManager(outputIndexDir, false);// Open a storage manager on output directory.
        storageManager.open();
        TroveReadWriteHelper.writeIndex(storageManager, tsrIndex, "roundRobin", true);
        storageManager.close();

    }
}
