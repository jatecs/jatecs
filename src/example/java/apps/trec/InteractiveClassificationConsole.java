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

package apps.trec;

import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.classification.naivebayes.NaiveBayesLearner;
import it.cnr.jatecs.classification.naivebayes.NaiveBayesLearnerCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.*;
import it.cnr.jatecs.indexing.corpus.BagOfWordsFeatureExtractor;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

public class InteractiveClassificationConsole {

    static final String CLEAR = "CLEAR";
    static final String EXCEPTION = "EXCEPTION";
    static final String CLASSIFY = "CLASSIFY";
    static final String LEARN = "LEARN";
    static final String INDEX = "INDEX";
    static final String INIT = "INIT";
    static final String EXIT = "EXIT";
    static final String SEPARATOR = " ";
    static final String TRAIN = "TRAIN";
    static final String TEST = "TEST";
    static final String[] STRING_ARRAY = new String[0];
    static final String LABEL_SEPARATOR = "|";
    static final String NAIVE_BAYES = "NB";
    static final String OK = "OK";

    public static void main(String[] args) throws IOException {

        JatecsLogger.status().removeAllHandlers();
        JatecsLogger.execution().removeAllHandlers();

        ICategoryDB categoryDB = null;

        TroveMainIndexBuilder mainIndexBuilder = null;

        BagOfWordsFeatureExtractor featureExtractor = new BagOfWordsFeatureExtractor();

        IIndex training = null;

        IClassifier classifier = null;

        TroveDependentIndexBuilder testIndexBuilder = null;

        Exception lastException = null;

        String line;
        InputStreamReader inStream = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(inStream);
        while (true) {
            line = reader.readLine();
            if (line == null) {
                continue;
            }
            line = line.trim();
            try {
                if (line.startsWith(EXIT)) {
                    // EXIT

                    writeLine(OK);
                    return;

                } else if (line.startsWith(INIT)) {
                    // INIT label label label
                    // Example:
                    // INIT +
                    int currSep = line.indexOf(SEPARATOR) + 1;
                    String[] labels = line.substring(currSep).split(SEPARATOR);

                    TroveCategoryDBBuilder categoryDBBuilder = new TroveCategoryDBBuilder();
                    for (String label : labels)
                        categoryDBBuilder.addCategory(label);
                    categoryDB = categoryDBBuilder.getCategoryDB();

                    mainIndexBuilder = null;
                    training = null;
                    classifier = null;
                    testIndexBuilder = null;

                    writeLine(OK);

                } else if (line.startsWith(INDEX)) {
                    // INDEX TRAIN/TEST ID label label label | text text text
                    // Example:
                    // INDEX TRAIN doc0 + | test text for a positive training
                    // document
                    // INDEX TRAIN doc1 | test text for a negative training
                    // document
                    // [TRAIN]
                    // INDEX TEST doc0 + | test text for a positive test
                    // document
                    // INDEX TEST doc1 | test text for a negative test document
                    if (categoryDB == null) {
                        writeLine("ERROR REQUIRES INIT");
                        continue;
                    }

                    int currSep = line.indexOf(SEPARATOR) + 1;
                    int nextSep = line.indexOf(SEPARATOR, currSep);
                    String type = line.substring(currSep, nextSep);
                    currSep = nextSep + 1;
                    nextSep = line.indexOf(SEPARATOR, currSep);
                    String docId = line.substring(currSep, nextSep);

                    List<String> labels = new Vector<String>();
                    while (true) {
                        currSep = nextSep + 1;
                        nextSep = line.indexOf(SEPARATOR, currSep);
                        String label;
                        if (nextSep < 0)
                            label = line.substring(currSep);
                        else
                            label = line.substring(currSep, nextSep);
                        if (label.equals(LABEL_SEPARATOR))
                            break;
                        labels.add(label);
                    }
                    currSep = nextSep + 1;
                    String text = line.substring(currSep);
                    List<String> features = featureExtractor
                            .extractFeatures(text);

                    if (type.equals(TRAIN)) {
                        if (mainIndexBuilder == null)
                            mainIndexBuilder = new TroveMainIndexBuilder(
                                    categoryDB);

                        mainIndexBuilder.addDocument(docId,
                                features.toArray(STRING_ARRAY),
                                labels.toArray(STRING_ARRAY));

                        training = null;
                        classifier = null;
                        testIndexBuilder = null;
                        writeLine(OK);

                    } else if (type.equals(TEST)) {
                        if (classifier == null) {
                            writeLine("ERROR REQUIRES TRAIN");
                            continue;
                        }

                        if (testIndexBuilder == null)
                            testIndexBuilder = new TroveDependentIndexBuilder(
                                    training.getDomainDB());

                        testIndexBuilder.addDocument(docId,
                                features.toArray(STRING_ARRAY),
                                labels.toArray(STRING_ARRAY));

                        writeLine(OK);
                    } else {
                        writeLine("ERROR UNKNOWN INDEX TYPE");
                    }

                } else if (line.startsWith(LEARN)) {
                    // LEARN method
                    // Example:
                    // LEARN NB
                    if (categoryDB == null) {
                        writeLine("ERROR REQUIRES INIT");
                        continue;
                    }

                    if (mainIndexBuilder == null) {
                        writeLine("ERROR REQUIRES INDEX TRAIN");
                        continue;
                    }

                    int currSep = line.indexOf(SEPARATOR) + 1;
                    String method = line.substring(currSep);

                    if (method.equals(NAIVE_BAYES)) {
                        NaiveBayesLearner learner = new NaiveBayesLearner();
                        NaiveBayesLearnerCustomizer customizer = new NaiveBayesLearnerCustomizer();
                        double smooth = 1.0;
                        customizer.setSmoothingFactor(smooth);
                        customizer.useMultinomialModel();
                        learner.setRuntimeCustomizer(customizer);

                        TroveClassificationDBType classificationDBType = TroveClassificationDBType.IL;
                        TroveContentDBType contentDBType = TroveContentDBType.IL;
                        training = TroveReadWriteHelper.generateIndex(
                                new FileSystemStorageManager(Os
                                        .getTemporaryDirectory(), false),
                                mainIndexBuilder.getIndex(), contentDBType,
                                classificationDBType);

                        classifier = learner.build(training);

                        writeLine(OK);
                    } else {
                        writeLine("ERROR UNSUPPORTED LEARNER");
                        continue;
                    }

                } else if (line.startsWith(CLASSIFY)) {
                    // CLASSIFY
                    if (categoryDB == null) {
                        writeLine("ERROR REQUIRES INIT");
                        continue;
                    }

                    if (mainIndexBuilder == null) {
                        writeLine("ERROR REQUIRES INDEX TRAIN");
                        continue;
                    }

                    if (classifier == null) {
                        writeLine("ERROR REQUIRES LEARN");
                        continue;
                    }

                    if (testIndexBuilder == null) {
                        writeLine("ERROR REQUIRES INDEX TEST");
                        continue;
                    }

                    IIndex testIndex = testIndexBuilder.getIndex();
                    Classifier classifierModule = new Classifier(testIndex,
                            classifier, true);
                    classifierModule.exec();

                    ClassificationScoreDB confidences = classifierModule
                            .getConfidences();
                    int numDoc = testIndex.getDocumentDB().getDocumentsCount();
                    writeLine(numDoc + " " + categoryDB.getCategoriesCount());
                    for (int docID = 0; docID < numDoc; ++docID) {
                        String docName = testIndex.getDocumentDB()
                                .getDocumentName(docID);
                        Set<Entry<Short, ClassifierRangeWithScore>> entries = confidences
                                .getDocumentScoresAsSet(docID);
                        Iterator<Entry<Short, ClassifierRangeWithScore>> iterator = entries
                                .iterator();
                        while (iterator.hasNext()) {
                            Entry<Short, ClassifierRangeWithScore> next = iterator
                                    .next();
                            ClassifierRangeWithScore value = next.getValue();
                            writeLine(docName + " "
                                    + categoryDB.getCategoryName(next.getKey())
                                    + " " + value.score + " " + value.border);
                        }
                    }

                    writeLine(OK);
                } else if (line.startsWith(CLEAR)) {
                    // CLEAR method
                    // Example:
                    // CLEAR TEST

                    int currSep = line.indexOf(SEPARATOR) + 1;
                    String method = line.substring(currSep);

                    if (method.equals(TEST)) {
                        testIndexBuilder = null;
                        writeLine(OK);
                    } else {
                        writeLine("ERROR UNSUPPORTED CLEAR");
                        continue;
                    }
                } else if (line.startsWith(EXCEPTION)) {
                    if (lastException == null) {
                        writeLine("ERROR NO EXCEPTION");
                        continue;
                    } else {
                        writeLine(lastException.getMessage());
                        writeLine(OK);
                    }
                } else {
                    writeLine("ERROR UNKNOWN COMMAND");
                }
            } catch (Exception e) {
                writeLine("ERROR EXCEPTION");
                lastException = e;
            }
        }
    }

    private static void writeLine(String line) {
        System.out.println(line);
    }
}
