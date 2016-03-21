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

package it.cnr.jatecs.indexes.DB.generic;

import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDependentIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveParallelCorpusType;
import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.*;


public class ParallelMultilingualIndex extends MultilingualIndex implements IParallelMultilingualIndex {

	/**
	 * The database storing all relations of parallelism among documents
	 * */
    protected IParallelDB _parallelDB;

    /**
     * Class constructor specifying all different databases of the index
     * including the language db, the document-language db, and the parallel db
     * */
    public ParallelMultilingualIndex(IFeatureDB featuresDB, IDocumentDB documentsDB,
                         ICategoryDB categoriesDB, IDomainDB domainDB,
                         ILanguageDB languageDB, IContentDB contentDB,
                         IWeightingDB weightingDB, IClassificationDB classificationDB,
                         IDocumentLanguageDB documentLanguageDB, IParallelDB parallelDB) {

        super(featuresDB, documentsDB, categoriesDB, domainDB, languageDB,
                contentDB, weightingDB, classificationDB, documentLanguageDB);

        _parallelDB = parallelDB;
    }

    /**
     * Class constructor specifying an index plus the language db, the 
     * document-language db, and the parallel db
     * */
    public ParallelMultilingualIndex(MultilingualIndex index, IParallelDB parallelDB) {

        super(index.getFeatureDB(), index.getDocumentDB(), index
                        .getCategoryDB(), index.getDomainDB(), index.getLanguageDB(),
                index.getContentDB(), index.getWeightingDB(), index
                        .getClassificationDB(), index.getDocumentLanguageDB());

        _parallelDB = parallelDB;
    }

    @Override
    public void removeDocuments(IIntIterator removedDocuments,
                                boolean compressFeaturesId) {
        super.removeDocuments(removedDocuments, compressFeaturesId);
        removedDocuments.begin();
        while (removedDocuments.hasNext()) {
            int doc = removedDocuments.next();
            _parallelDB.removeDocument(doc);
        }
    }

    @Override
    public TroveParallelCorpusType getIndexType() {
        return _parallelDB.getParallelCorpusType();
    }

    @Override
    public void setIndexType(TroveParallelCorpusType corpusType) {
        _parallelDB.setParallelCorpusType(corpusType);
    }

    @Override
    public IParallelDB getParallelDB() {
        return _parallelDB;
    }

    @Override
    public IIndex compactParallelDocuments() {
        TroveDependentIndexBuilder indexBuilder = new TroveDependentIndexBuilder(
                getDomainDB());
        HashSet<Integer> addedDocs = new HashSet<Integer>();
        IIntIterator docIt = getDocumentDB().getDocuments();
        while (docIt.hasNext()) {
            int docID = docIt.next();
            if (!addedDocs.contains(docID)) {
                String name = getDocumentDB().getDocumentName(docID);
                List<Integer> parallelDocs = new ArrayList<Integer>();
                if (getParallelDB().hasParallelVersion(docID))
                    parallelDocs.addAll(getParallelDB().getParallelDocuments(
                            docID));
                else
                    parallelDocs.add(docID);

                indexBuilder.addDocument(name,
                        getDocumentFeaturesFromDocuments(parallelDocs),
                        getDocumentCategoriesFromDocuments(parallelDocs));

                addedDocs.addAll(parallelDocs);
            }

        }
        // indexBuilder.setName("Compacted-"+this.getName());
        return indexBuilder.getIndex();
    }

    private String[] getDocumentFeaturesFromDocuments(List<Integer> docIDs) {
        ArrayList<String> featAccum = new ArrayList<String>();
        for (int docID : docIDs) 
            featAccum.addAll(getDocumentFeatures(docID));
        
        return featAccum.toArray(new String[0]);
    }

    private String[] getDocumentCategoriesFromDocuments(List<Integer> docIDs) {
        ArrayList<String> catAccum = new ArrayList<String>();
        for (int docID : docIDs) 
            catAccum.addAll(getDocumentCategories(docID));
        
        removeDuplicates(catAccum);
        return catAccum.toArray(new String[0]);
    }

    private void removeDuplicates(ArrayList<String> v) {
        for (int i = 0; i < v.size() - 1; i++) {
            for (int j = i + 1; j < v.size(); ) {
                if (v.get(i).equals(v.get(j))) 
                    v.remove(j);
                else 
                    j++;                
            }
        }
    }

    private List<String> getDocumentFeatures(int docID) {
        ArrayList<String> feats = new ArrayList<String>();

        IIntIterator featIT = getContentDB().getDocumentFeatures(docID);
        while (featIT.hasNext()) {
            int feat = featIT.next();
            int count = getContentDB().getDocumentFeatureFrequency(docID, feat);
            String featname = getFeatureDB().getFeatureName(feat);
            for (int i = 0; i < count; i++)
                feats.add(featname);
        }

        return feats;
    }

    private List<String> getDocumentCategories(int docID) {
        ArrayList<String> cats = new ArrayList<String>();

        IShortIterator catIT = getClassificationDB().getDocumentCategories(docID);
        while (catIT.hasNext()) {
            short cat = catIT.next();
            String catname = getCategoryDB().getCategoryName(cat);
            cats.add(catname);
        }

        return cats;
    }

    @Override
    public HashMap<LanguageLabel, IIndex> unfoldTrainingViews(
            boolean onlyFullViews) {
        HashMap<LanguageLabel, IIndexBuilder> indexBuilders = new HashMap<LanguageLabel, IIndexBuilder>();
        Iterator<LanguageLabel> langIt = getLanguageDB().getLanguages();
        while (langIt.hasNext()) {
            indexBuilders.put(langIt.next(), new TroveMainIndexBuilder(
                    getCategoryDB()));
        }

        return unfoldViews(onlyFullViews, indexBuilders);
    }

    @Override
    public HashMap<LanguageLabel, IIndex> unfoldTestingDependantViews(
            boolean onlyFullViews,
            HashMap<LanguageLabel, IIndex> trainingIndexes) {
        HashMap<LanguageLabel, IIndexBuilder> indexBuilders = new HashMap<LanguageLabel, IIndexBuilder>();
        Iterator<LanguageLabel> langIt = getLanguageDB().getLanguages();
        while (langIt.hasNext()) {
            LanguageLabel lang = langIt.next();
            IIndex trainingIndex = trainingIndexes.get(lang);
            indexBuilders.put(lang, new TroveDependentIndexBuilder(
                    trainingIndex.getDomainDB()));
        }

        return unfoldViews(onlyFullViews, indexBuilders);
    }

    private HashMap<LanguageLabel, IIndex> unfoldViews(boolean onlyFullViews, HashMap<LanguageLabel, IIndexBuilder> indexBuilders) {
        HashMap<LanguageLabel, IIndex> lang_view = new HashMap<LanguageLabel, IIndex>();

        ArrayList<ArrayList<Integer>> clusters = getParallelDB()
                .getViewsClusters();

        int numViews = getLanguageDB().getLanguagesCount();
        int skipedDocuments = 0;
        for (ArrayList<Integer> cluster : clusters) {
            if (cluster.size() == numViews || !onlyFullViews) {
                for (Integer docID : cluster) {
                    LanguageLabel lang = getDocumentLanguageDB()
                            .getDocumentLanguage(docID);
                    String docName = getDocumentDB().getDocumentName(docID);
                    String[] docFeatures = getDocumentFeatures(docID).toArray(
                            new String[0]);
                    String[] docCategories = getDocumentCategories(docID)
                            .toArray(new String[0]);
                    indexBuilders.get(lang).addDocument(docName, docFeatures,
                            docCategories);
                }
            } else {
                skipedDocuments += cluster.size();
            }
        }

        Iterator<LanguageLabel> langIt = getLanguageDB().getLanguages();
        langIt = getLanguageDB().getLanguages();
        while (langIt.hasNext()) {
            LanguageLabel lang = langIt.next();
            IIndex view = indexBuilders.get(lang).getIndex();
            lang_view.put(lang, view);
        }

        if (skipedDocuments > 0)
            JatecsLogger.execution().warning(
                    "Skiped " + skipedDocuments + " documents: some views were not available.\n");

        return lang_view;
    }

}
