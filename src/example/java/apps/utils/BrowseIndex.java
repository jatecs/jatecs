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
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class BrowseIndex {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: BrowseIndex <indexDirectory>");
            return;
        }

        File file = new File(args[0]);
        String path = file.getParentFile().getPath();
        String indexname = file.getName();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                path, false);
        storageManager.open();
        IIndex index = TroveReadWriteHelper.readIndex(storageManager,
                indexname, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String line;
        while (true) {
            System.out.print(">");
            line = br.readLine();
            if (line.equals("x"))
                return;
            try {
                String[] fields = line.split(" ");
                if (fields[0].equals("d") || fields[0].equals("D")) {
                    if (fields.length == 1) {
                        IIntIterator docs = index.getDocumentDB()
                                .getDocuments();
                        if (fields[0].equals("d")) {
                            while (docs.hasNext()) {
                                System.out.print(docs.next() + "\t");
                            }
                            System.out.println();
                        } else {
                            while (docs.hasNext()) {
                                System.out.print(index.getDocumentDB()
                                        .getDocumentName(docs.next()) + "\t");
                            }
                            System.out.println();
                        }
                    } else {
                        int document;
                        if (fields[0].equals("d"))
                            document = Integer.parseInt(fields[1]);
                        else {
                            for (int i = 2; i < fields.length - 1; ++i) {
                                fields[1] += " " + fields[i];
                            }
                            fields[2] = fields[fields.length - 1];
                            document = index.getDocumentDB().getDocument(
                                    fields[1]);
                        }
                        if (fields[2].equals("c")) {
                            IShortIterator cats = index.getClassificationDB()
                                    .getDocumentCategories(document);
                            while (cats.hasNext()) {
                                System.out.print(cats.next() + "\t");
                            }
                            System.out.println();
                        } else if (fields[2].equals("C")) {
                            IShortIterator cats = index.getClassificationDB()
                                    .getDocumentCategories(document);
                            while (cats.hasNext()) {
                                String catName = index.getCategoryDB()
                                        .getCategoryName(cats.next());
                                System.out.print(catName + "\t");
                            }
                            System.out.println();
                        } else if (fields[2].startsWith("f")) {
                            if (fields[2].equals("f")) {
                                IIntIterator feats = index.getContentDB()
                                        .getDocumentFeatures(document);
                                while (feats.hasNext()) {
                                    System.out.print(feats.next() + "\t");
                                }
                                System.out.println();
                            } else {
                                IIntIterator feats = index.getContentDB()
                                        .getDocumentFeatures(document);
                                while (feats.hasNext()) {
                                    int feat = feats.next();
                                    System.out
                                            .print(feat
                                                    + ":("
                                                    + index.getWeightingDB()
                                                    .getDocumentFeatureWeight(
                                                            document,
                                                            feat)
                                                    + ", "
                                                    + index.getContentDB()
                                                    .getDocumentFeatureFrequency(
                                                            document,
                                                            feat)
                                                    + ")\t");
                                }
                                System.out.println();
                            }
                        } else if (fields[2].equals("F")) {
                            IIntIterator feats = index.getContentDB()
                                    .getDocumentFeatures(document);
                            while (feats.hasNext()) {
                                String featName = index.getFeatureDB()
                                        .getFeatureName(feats.next());
                                System.out.print(featName + "\t");
                            }
                            System.out.println();
                        } else if (fields[2].equals("D")) {
                            System.out.println(index.getDocumentDB()
                                    .getDocumentName(document));
                        } else if (fields[2].equals("d")) {
                            System.out.println(document);
                        }
                    }
                }
                if (fields[0].equals("f") || fields[0].equals("F")) {
                    if (fields.length == 1) {
                        IIntIterator feats = index.getFeatureDB().getFeatures();
                        if (fields[0].equals("f")) {
                            while (feats.hasNext()) {
                                System.out.print(feats.next() + "\t");
                            }
                            System.out.println();
                        } else {
                            while (feats.hasNext()) {
                                System.out.print(index.getFeatureDB()
                                        .getFeatureName(feats.next()) + "\t");
                            }
                            System.out.println();
                        }
                    } else {
                        int feature;
                        if (fields[0].equals("f"))
                            feature = Integer.parseInt(fields[1]);
                        else {
                            for (int i = 2; i < fields.length - 1; ++i) {
                                fields[1] += " " + fields[i];
                            }
                            fields[2] = fields[fields.length - 1];
                            feature = index.getFeatureDB()
                                    .getFeature(fields[1]);
                        }
                        if (fields[2].equals("d")) {
                            IIntIterator docs = index.getContentDB()
                                    .getFeatureDocuments(feature);
                            while (docs.hasNext()) {
                                System.out.print(docs.next() + "\t");
                            }
                            System.out.println();
                        } else if (fields[2].equals("D")) {
                            IIntIterator docs = index.getContentDB()
                                    .getFeatureDocuments(feature);
                            while (docs.hasNext()) {
                                String docName = index.getDocumentDB()
                                        .getDocumentName(docs.next());
                                System.out.print(docName + "\t");
                            }
                            System.out.println();
                        } else if (fields[2].equals("F")) {
                            System.out.println(index.getFeatureDB()
                                    .getFeatureName(feature));
                        } else if (fields[2].equals("f")) {
                            System.out.println(feature);
                        }
                    }
                }
                if (fields[0].equals("c") || fields[0].equals("C")) {
                    if (fields.length == 1) {
                        IShortIterator cats = index.getCategoryDB()
                                .getCategories();
                        if (fields[0].equals("c")) {
                            while (cats.hasNext()) {
                                System.out.print(cats.next() + "\t");
                            }
                            System.out.println();
                        } else {
                            while (cats.hasNext()) {
                                System.out.print(index.getCategoryDB()
                                        .getCategoryName(cats.next()) + "\t");
                            }
                            System.out.println();
                        }
                    } else {
                        short category;
                        if (fields[0].equals("c"))
                            category = Short.parseShort(fields[1]);
                        else {
                            for (int i = 2; i < fields.length - 1; ++i) {
                                fields[1] += " " + fields[i];
                            }
                            fields[2] = fields[fields.length - 1];
                            category = index.getCategoryDB().getCategory(
                                    fields[1]);
                        }
                        if (fields[2].equals("d")) {
                            IIntIterator docs = index.getClassificationDB()
                                    .getCategoryDocuments(category);
                            while (docs.hasNext()) {
                                System.out.print(docs.next() + "\t");
                            }
                            System.out.println();
                        } else if (fields[2].equals("D")) {
                            IIntIterator docs = index.getClassificationDB()
                                    .getCategoryDocuments(category);
                            while (docs.hasNext()) {
                                String docName = index.getDocumentDB()
                                        .getDocumentName(docs.next());
                                System.out.print(docName + "\t");
                            }
                            System.out.println();
                        } else if (fields[2].equals("f")) {
                            IIntIterator feats = index.getDomainDB()
                                    .getCategoryFeatures(category);
                            while (feats.hasNext()) {
                                System.out.print(feats.next() + "\t");
                            }
                            System.out.println();
                        } else if (fields[2].equals("F")) {
                            IIntIterator feats = index.getDomainDB()
                                    .getCategoryFeatures(category);
                            while (feats.hasNext()) {
                                String featName = index.getFeatureDB()
                                        .getFeatureName(feats.next());
                                System.out.print(featName + "\t");
                            }
                            System.out.println();
                        } else if (fields[2].equals("C")) {
                            System.out.println(index.getCategoryDB()
                                    .getCategoryName(category));
                        } else if (fields[2].equals("c")) {
                            System.out.println(category);
                        } else if (fields[2].equals("P")) {
                            IShortIterator cats = index.getCategoryDB()
                                    .getParentCategories(category);
                            while (cats.hasNext()) {
                                String catName = index.getCategoryDB()
                                        .getCategoryName(cats.next());
                                System.out.print(catName + "\t");
                            }
                            System.out.println();
                        } else if (fields[2].equals("p")) {
                            IShortIterator cats = index.getCategoryDB()
                                    .getParentCategories(category);
                            while (cats.hasNext())
                                System.out.print(cats.next() + "\t");
                            System.out.println();
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }
}
