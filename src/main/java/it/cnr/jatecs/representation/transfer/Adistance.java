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

package it.cnr.jatecs.representation.transfer;

import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.classification.svm.SvmLearner;
import it.cnr.jatecs.classification.svm.SvmLearnerCustomizer;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDomainDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDocumentsDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDomainDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.indexes.utils.IndexOperations;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Adistance {
	private IIndex _trainA, _trainB, _testA, _testB;	
	private ICategoryDB _domainCategories;
	private static short _catA;
	
	public Adistance(IIndex trainA, IIndex testA, IIndex trainB, IIndex testB){
		_trainA=trainA;
		_trainB=trainB;
		_testA=testA;
		_testB=testB;
		
		initDomainCategoriesSchema();
	}
	
	public double compute() throws FileNotFoundException, IOException{
		IFeatureDB joinFeatures=IndexOperations.getUnionFeatureSpace(_trainA, _trainB, _testA, _testB);
		_trainA=IndexOperations.changeFeatureSpace(_trainA, joinFeatures);
		_trainB=IndexOperations.changeFeatureSpace(_trainB, joinFeatures);
		_testA=IndexOperations.changeFeatureSpace(_testA, joinFeatures);
		_testB=IndexOperations.changeFeatureSpace(_testB, joinFeatures);
		
		IDomainDB domain=new TroveDomainDB(_domainCategories, joinFeatures);
		IIndex train=joinIndexes(_trainA, _trainB, domain);
		IIndex test=joinIndexes(_testA, _testB, domain);
		
		IClassifier classifier = trainSVM(train);
		
		IClassificationDB predictions = testClassifier(test, classifier);
		IClassificationDB trueValues = test.getClassificationDB();
		
		ClassificationComparer flatComparer = new ClassificationComparer(predictions, trueValues);
		ContingencyTableSet tableSet = flatComparer.evaluate(false);
		
		double acc=tableSet.macroAccuracy();
		double error=1.0-acc;
		System.out.println("Acc " + acc);
		System.out.println("Err " + (1.0-acc));
		
		return 2*(1-2*error);
	}
	
	//simply returns a category db containing the category DomA.
	//the absence of label DomA will represent the case in which the document belonged to DomB.
	private void initDomainCategoriesSchema(){
		TroveCategoryDBBuilder cats=new TroveCategoryDBBuilder();
		cats.addCategory("DomA");
		_domainCategories=cats.getCategoryDB();
		_catA=_domainCategories.getCategory("DomA");
	}

	//relabel the documents as belonging to domain A (+1) or domain B (0)
	private IIndex joinIndexes(IIndex indexA, IIndex indexB, IDomainDB domain) {
		TroveDocumentsDBBuilder documentsDB=new TroveDocumentsDBBuilder();
		TroveContentDBBuilder contentDB=new TroveContentDBBuilder(documentsDB.getDocumentDB(), domain.getFeatureDB());
		TroveWeightingDBBuilder weightingDB=new TroveWeightingDBBuilder(contentDB.getContentDB());
		TroveClassificationDBBuilder classificationDB=new TroveClassificationDBBuilder(documentsDB.getDocumentDB(), _domainCategories);
		
		addDocuments(documentsDB, contentDB, weightingDB, classificationDB, indexA, true);
		addDocuments(documentsDB, contentDB, weightingDB, classificationDB, indexB, false);
		
		 return new GenericIndex(domain.getFeatureDB(), 
				documentsDB.getDocumentDB(), 
				_domainCategories, 
				domain, 
				contentDB.getContentDB(), 
				weightingDB.getWeightingDB(), 
				classificationDB.getClassificationDB());
	}

	private static void addDocuments(TroveDocumentsDBBuilder documentsDB, TroveContentDBBuilder contentDB, 
			TroveWeightingDBBuilder weightingDB, TroveClassificationDBBuilder classificationDB, IIndex index, boolean label){

		IIntIterator docs=index.getDocumentDB().getDocuments();
		while(docs.hasNext()){
			int docID=docs.next();
			String docName=index.getDocumentDB().getDocumentName(docID);
			int newdocID=documentsDB.addDocument((label?"DomA":"DomB")+"_"+docName);
			
			IIntIterator docfeats=index.getContentDB().getDocumentFeatures(docID);
			while(docfeats.hasNext()){
				int featID=docfeats.next();
				String featname=index.getFeatureDB().getFeatureName(featID);
				int newfeatID=contentDB.getContentDB().getFeatureDB().getFeature(featname);
				if(newfeatID!=-1){
				
					int content=index.getContentDB().getDocumentFeatureFrequency(docID, featID);
					double weight=index.getWeightingDB().getDocumentFeatureWeight(docID, featID);
				
					contentDB.setDocumentFeatureFrequency(newdocID, newfeatID, content);
					weightingDB.setDocumentFeatureWeight(newdocID, newfeatID, weight);
					if(label)
					classificationDB.setDocumentCategory(newdocID, _catA);
				}
				else{
					System.err.println("Critical Error: feature <"+featname+"> not found in joined index!");
					System.exit(0);					
				}
			}						
		}
	}
	
	private static IClassifier trainSVM(IIndex training) throws FileNotFoundException, IOException {
		SvmLearner learner = new SvmLearner();
	    SvmLearnerCustomizer customizer = new SvmLearnerCustomizer();
	    learner.setRuntimeCustomizer(customizer);
	    return learner.build(training);
    }
	
	public static IClassificationDB testClassifier(IIndex indexTesting,
			IClassifier classifier) throws IOException {
		Classifier classifierModule = new Classifier(indexTesting, classifier, false);
		classifierModule.setClassificationMode(ClassificationMode.PER_CATEGORY);
		classifierModule.exec();
		return classifierModule.getClassificationDB();
	}
	
}
