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

package apps.utils;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.File;

/**
 * This app prints some statistics about an index
 *
 * @author Andrea Esuli
 */
public class IndexStatistics {

    private static String details = "-d";
    private static String equality = "-e";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: IndexStatistics <indexDirectory> ["
                    + details + "] [" + equality + "]");
            return;
        }

        String dataPath = args[0];

        boolean showDetails = false;
        boolean checkEquivalentFeatures = false;

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals(details))
                showDetails = true;
            else if (args[i].equals(equality))
                checkEquivalentFeatures = true;
        }

        File file = new File(dataPath);

        String indexName = file.getName();
        dataPath = file.getParent();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                dataPath, false);
        storageManager.open();
        IIndex training = TroveReadWriteHelper.readIndex(storageManager,
                indexName, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        it.cnr.jatecs.indexes.utils.IndexStatistics.printStatistics(training,
                checkEquivalentFeatures, showDetails);
    }
}
