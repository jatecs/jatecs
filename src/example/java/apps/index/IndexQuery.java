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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;


public class IndexQuery {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: "+IndexQuery.class.getName()+" <indexPath>");
            return;
        }

        // Extract index coordinates.
        String indexFile = args[0];
        
        File file = new File(indexFile);
        String indexName = file.getName();
        String indexPath = file.getParent();

        // Load index on memory.
        try {
            FileSystemStorageManager storageManager = new FileSystemStorageManager(indexPath, false);
            storageManager.open();
            IIndex index = TroveReadWriteHelper.readIndex(storageManager, indexName,
                    TroveContentDBType.Full, TroveClassificationDBType.Full);
            storageManager.close();

            // Iterate over all documents.
            for(int docID : index.getDocumentDB().getDocuments()) {
                String documentName = index.getDocumentDB().getDocumentName(docID);

                // Select only document's root categories inside the used taxonomy.
                ArrayList<String> rootCats = new ArrayList<>();
                for (short catID : index.getClassificationDB().getDocumentCategories(docID)) {
                    IShortIterator parents = index.getCategoryDB().getParentCategories(catID);
                    if (parents.hasNext())
                        // Skip category, it has one or more parents.
                        continue;
                    
                    String categoryName = index.getCategoryDB().getCategoryName(catID);
                    rootCats.add(categoryName);
                }

                // Select only the features which have number of occurrences >= 5 or weight > 0.15.
                ArrayList<String> wantedFeatures = new ArrayList<>();
                for (int featID : index.getContentDB().getDocumentFeatures(docID)) {
                	int frequency = index.getContentDB().getDocumentFeatureFrequency(docID, featID);
                	double weight = index.getWeightingDB().getDocumentFeatureWeight(docID, featID);
                	
                    if (frequency >= 5 || weight > 0.15)
                        wantedFeatures.add(index.getFeatureDB().getFeatureName(featID));
                }

                // Print information about the document.
                System.out.println("****************************");
                System.out.println("Document name: "+documentName);
                System.out.println("Main categories assigned: "+ Arrays.toString(rootCats.toArray()));
                System.out.println("Most important features: "+Arrays.toString(wantedFeatures.toArray()));
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
