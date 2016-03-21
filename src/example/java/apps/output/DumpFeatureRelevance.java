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
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexing.tsr.InformationGain;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Comparator;

public class DumpFeatureRelevance {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: DumpFeatureRelevance <indexDirectory>");
            return;
        }

        File file = new File(args[0]);

        String indexName = file.getName();
        String dataPath = file.getParent();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                dataPath, false);
        storageManager.open();
        IIndex index = TroveReadWriteHelper.readIndex(storageManager,
                indexName, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        InformationGain ig = new InformationGain();

        if (indexName.endsWith(".idx")) {
            indexName = indexName.substring(0, indexName.length() - 4);
        }

        IShortIterator cats = index.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short cat = cats.next();
            String catName = index.getCategoryDB().getCategoryName(cat);
            Object[][] objects = new Object[index.getFeatureDB()
                    .getFeaturesCount()][3];
            IIntIterator feats = index.getFeatureDB().getFeatures();
            int counter = 0;
            while (feats.hasNext()) {
                int feat = feats.next();
                String featName = index.getFeatureDB().getFeatureName(feat);
                double value = ig.compute(cat, feat, index);
                objects[counter][0] = value;
                objects[counter][1] = feat;
                objects[counter][2] = featName;
                ++counter;
            }

            Arrays.sort(objects, new Comparator<Object[]>() {
                @SuppressWarnings({"rawtypes", "unchecked"})
                public int compare(Object[] o1, Object[] o2) {
                    return ((Comparable) o2[0]).compareTo(o1[0]);
                }
            });

            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(dataPath + Os.pathSeparator() + indexName
                            + "_rank-" + catName, true));
            for (Object[] tuple : objects) {
                writer.write(tuple[0] + "\t" + tuple[1] + "\t" + tuple[2]
                        + "\n");
            }
            writer.close();
        }
    }
}
