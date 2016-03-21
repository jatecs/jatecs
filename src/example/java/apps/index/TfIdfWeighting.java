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
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.indexing.weighting.TfNormalizedIdf;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.File;
import java.io.IOException;

public class TfIdfWeighting {

    public static void main(String[] args) throws IOException {
    	
        if (args.length != 4) {
            System.err.println("Usage: " + TfIdfWeighting.class.getName() + " <inputIndexDirectory> <trainIndexName> <testIndexName> <outputIndexDirectory>");
            return;
        }
        
        String indexPath = args[0];
        String trainIndexName = args[1];
        String testIndexName = args[2];
        String outputIndexDir = args[3];        

        // Load train a test indexes.
        FileSystemStorageManager storageManager = new FileSystemStorageManager(indexPath, false);
        storageManager.open();
        IIndex trainIndex = TroveReadWriteHelper.readIndex(storageManager, trainIndexName, TroveContentDBType.Full, TroveClassificationDBType.Full);
        IIndex testIndex = TroveReadWriteHelper.readIndex(storageManager, testIndexName, TroveContentDBType.Full, TroveClassificationDBType.Full);
        storageManager.close();

        // Compute features weights using TfIdf method.
        IWeighting weighting = new TfNormalizedIdf(trainIndex);//idf values will be taken from this collection
        IIndex weightedTrainingIndex = weighting.computeWeights(trainIndex);
        IIndex weightedTestIndex = weighting.computeWeights(testIndex);

        // Save TSR index.
        // Open a storage manager on output directory.
        storageManager = new FileSystemStorageManager(outputIndexDir, true);
        storageManager.open();
        TroveReadWriteHelper.writeIndex(storageManager, weightedTrainingIndex, trainIndexName+"Tfidf", true);
        TroveReadWriteHelper.writeIndex(storageManager, weightedTestIndex, testIndexName+"Tfidf", true);
        storageManager.close();
        
    }    
}
