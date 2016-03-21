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

/**
 *
 */
package it.cnr.jatecs.indexing.corpus.SVMLight;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.indexes.DB.troveCompact.*;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.*;

/**
 * @author Andrea Esuli
 */
public class SVMLightDataManager {

    public static IIndex readData(String inFile, IIndex mainIndex) {
        TroveDependentIndexBuilder indexBuilder = new TroveDependentIndexBuilder(
                mainIndex.getDomainDB());

        TroveWeightingDBBuilder weightingBuilder = new TroveWeightingDBBuilder(
                (TroveWeightingDB) indexBuilder.getWeightingDB());

        indexFile(inFile, indexBuilder, weightingBuilder,
                indexBuilder.getFeatureDB());

        return indexBuilder.getIndex();
    }

    public static IIndex readData(String inFile) {
        ICategoryDB categoriesBD = getCategoriesDB(inFile);

        TroveMainIndexBuilder indexBuilder = new TroveMainIndexBuilder(
                categoriesBD);

        TroveWeightingDBBuilder weightingBuilder = new TroveWeightingDBBuilder(
                (TroveWeightingDB) indexBuilder.getWeightingDB());

        indexFile(inFile, indexBuilder, weightingBuilder,
                indexBuilder.getFeatureDB());

        return indexBuilder.getIndex();
    }

    @SuppressWarnings("unused")
    private static void indexFile(String inFile, IIndexBuilder indexBuilder,
                                  IWeightingDBBuilder weightingBuilder, IFeatureDB featuresDB) {
        BufferedReader file = null;
        try {
            file = new BufferedReader(new FileReader(inFile));
        } catch (IOException e) {
            try {
                if (file != null) {
                    file.close();
                }
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
            throw new RuntimeException(e);
        }

        int count = 0;
        String line = null;
        while (true) {
            try {
                line = file.readLine();
            } catch (IOException e) {
                try {
                    file.close();
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
                throw new RuntimeException(e);
            }
            if (line == null)
                break;
            int commentPosition = line.indexOf('#');
            String documentName = null;
            if (commentPosition >= 0) {
                documentName = line.substring(commentPosition + 1);
                line = line.substring(0, commentPosition);
            } else
                documentName = "" + count;
            if (line.length() == 0)
                continue;
            TIntArrayList features = new TIntArrayList();
            TDoubleArrayList weights = new TDoubleArrayList();
            String[] fields = line.split(" ");
            String[] value = new String[1];
            value[0] = fields[0];
            for (int i = 1; i < fields.length; ++i) {
                String[] pair = fields[i].split(":");
                if (pair.length != 2)
                    continue;
                int feature;
                double weight;
                try {
                    feature = Integer.parseInt(pair[0]);
                    weight = Double.parseDouble(pair[1]);
                } catch (Exception e) {
                    documentName += " " + fields[i];
                    continue;
                }
                features.add(feature);
                weights.add(weight);
            }
            if (features.size() != 0) {
                String[] featureNames = new String[features.size()];
                int document = 0;
                for (int i = 0; i < features.size(); ++i) {
                    featureNames[i] = "" + features.getQuick(i);
                }
                document = indexBuilder.addDocument(documentName, featureNames,
                        value);
                for (int i = 0; i < features.size(); ++i) {
                    weightingBuilder.setDocumentFeatureWeight(document,
                            featuresDB.getFeature(featureNames[i]),
                            weights.getQuick(i));
                }
            }
            ++count;
        }
        try {
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    private static ICategoryDB getCategoriesDB(String inFile) {
        BufferedReader file = null;
        try {
            file = new BufferedReader(new FileReader(inFile));
        } catch (IOException e) {
            try {
                if (file != null) {
                    file.close();
                }
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
            throw new RuntimeException(e);
        }

        TroveCategoryDBBuilder categoriesBuilder = new TroveCategoryDBBuilder();

        TIntArrayList values = new TIntArrayList();
        String line = null;
        while (true) {
            try {
                line = file.readLine();
            } catch (IOException e) {
                try {
                    file.close();
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
                throw new RuntimeException(e);
            }
            if (line == null)
                break;
            int commentPosition = line.indexOf('#');
            if (commentPosition >= 0)
                line = line.substring(0, commentPosition);
            if (line.length() == 0)
                continue;
            String[] fields = line.split(" ");
            int value = Integer.parseInt(fields[0]);
            if (!values.contains(value))
                values.add(value);
        }
        values.sort();
        for (int i = 0; i < values.size(); ++i) {
            categoriesBuilder.addCategory("" + values.getQuick(i));
        }
        ICategoryDB categoriesBD = categoriesBuilder.getCategoryDB();
        try {
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return categoriesBD;
    }

    public static void writeData(String outFile, IIndex index) {
        File dir = new File(outFile).getParentFile();
        if (!dir.exists())
            dir.mkdirs();

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        OutputStreamWriter out = new OutputStreamWriter(os);

        StringBuilder b = new StringBuilder();

        IIntIterator docIt = index.getDocumentDB().getDocuments();
        while (docIt.hasNext()) {
            b.setLength(0);

            int document = docIt.next();

            if (index.getClassificationDB()
                    .getDocumentCategoriesCount(document) != 1) {
                try {
                    out.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                throw new RuntimeException("Document "
                        + index.getDocumentDB().getDocumentName(document)
                        + " (" + document
                        + ") has not a single label attached.");
            }
            IShortIterator catIt = index.getClassificationDB()
                    .getDocumentCategories(document);
            short category = catIt.next();
            b.append(index.getCategoryDB().getCategoryName(category));

            IIntIterator itFeats = index.getContentDB().getDocumentFeatures(
                    document);
            while (itFeats.hasNext()) {
                int feature = itFeats.next();
                double score = index.getWeightingDB().getDocumentFeatureWeight(
                        document, feature);
                if (score == 0)
                    continue;
                b.append(" " + index.getFeatureDB().getFeatureName(feature)
                        + ":" + score);
            }

            b.append(" # " + index.getDocumentDB().getDocumentName(document)
                    + " (" + document + ")" + Os.newline());

            try {
                out.write(b.toString());
            } catch (IOException e) {
                try {
                    out.close();
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
                throw new RuntimeException(e);
            }
        }

        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeValues(String outFile,
                                   IClassificationDB classification) {
        File dir = new File(outFile).getParentFile();
        if (!dir.exists())
            dir.mkdirs();

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        OutputStreamWriter out = new OutputStreamWriter(os);

        StringBuilder b = new StringBuilder();

        IIntIterator docIt = classification.getDocumentDB().getDocuments();
        while (docIt.hasNext()) {
            b.setLength(0);

            int document = docIt.next();

            if (classification.getDocumentCategoriesCount(document) != 1) {
                try {
                    out.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                throw new RuntimeException("Document "
                        + classification.getDocumentDB().getDocumentName(
                        document) + " (" + document
                        + ") has not a single label attached.");
            }
            IShortIterator catIt = classification
                    .getDocumentCategories(document);
            short category = catIt.next();
            try {
                out.write(classification.getCategoryDB().getCategoryName(
                        category)
                        + Os.newline());
            } catch (IOException e) {
                try {
                    out.close();
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
                throw new RuntimeException(e);
            }
        }

        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
