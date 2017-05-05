## JaTeCS (Java Text Categorization System)
JaTeCS is an open source Java library focused on Automatic Text Categorization (ATC).
It covers all the steps of an experimental activity, from reading the corpus to the evaluation of the experimental results. 
JaTeCS focuses on text as the central input, and its code is optimized for this type of data. As with many other machine learning (ML) frameworks, it provides data readers for many formats and well-known corpora, NLP tools, feature selection and weighting methods, the implementation of many ML algorithms as well as wrappers for well-known external software (e.g., libSVM, SVM_light).
JaTeCS also provides the implementation of methods related to ATC that are rarely, if never, provided by other ML framework (e.g., active learning, quantification, transfer learning).

The software is released under the terms of [GPL license](http://www.gnu.org/licenses/gpl-3.0.en.html).

## Software installation
To use the latest release of JaTeCS in your Maven projects, add the following on your project POM:
```
<repositories>

    <repository>
        <id>jatecs-mvn-repo</id>
        <url>https://raw.github.com/jatecs/jatecs/mvn-repo/</url>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>

</repositories>
```
then in the dependencies list add
```
<dependency>
    <groupId>hlt.isti.cnr.it</groupId>
    <artifactId>jatecs-gpl</artifactId>
    <version>1.0.0</version>
</dependency>
```

## How to develop your apps with the software
### Data representation through IIndex data structure
In JaTeCS, the raw textual data is manipulated through the use of an indexed structure named *IIndex*.
This data structure handles all relations among documents, features, and categories (which could be defined in a taxonomy).
The IIndex can be used to manipulate or query data.

The following snippet shows a very simple example printing the number of terms that appear more than 5 times in each document:

<pre><code>
	for(int docID : index.getDocumentDB().getDocuments()) {
		String documentName = index.getDocumentDB().getDocumentName(docID);                
		int frequentTerms = 0;
		for (int featID : index.getContentDB().getDocumentFeatures(docID)) {
			if (index.getContentDB().getDocumentFeatureFrequency(docID, featID) > 5)
				frequentTerms++;
		}
		System.out.println("Document "+documentName+" contains " + frequentTerms + " frequent terms");
	}	
</code></pre>

A richer example on the use of the *IIndex* structure can be found in [IndexQuery.java](src/example/java/apps/index/IndexQuery.java).

The class [TroveMainIndexBuilder.java](src/main/java/it/cnr/jatecs/indexes/DB/troveCompact/TroveMainIndexBuilder.java) is meant to create the *IIndex*, which might be used independently, or in combination with [CorpusReader.java](src/main/java/it/cnr/jatecs/indexing/corpus/CorpusReader.java) and [FullIndexConstructor.java](src/main/java/it/cnr/jatecs/indexing/module/FullIndexConstructor.java) to construct an index from a raw corpus of documents (several examples of this latter could be consulted in directory [dataset](src/example/java/apps/dataset), including, e.g., the [Reuters21578](http://www.daviddlewis.com/resources/testcollections/reuters21578/) collection -file [IndexReuters21578.java](src/example/java/apps/dataset/IndexReuters21578.java))-, the [RCV1-v2](http://www.daviddlewis.com/resources/testcollections/rcv1/) collection -file [IndexRCV1.java](src/example/java/apps/dataset/IndexRCV1.java)-, among many others).
JaTeCS provides common feature extractors to represent features from raw textual data, like the [BoW (bag-of-words)](src/main/java/it/cnr/jatecs/indexing/corpus/BagOfWordsFeatureExtractor.java) extractor, or the [characters n-grams](src/main/java/it/cnr/jatecs/indexing/corpus/CharsNGramFeatureExtractor.java) extractor.
The [dataset](src/example/java/apps/dataset) directory contains many examples of corpus indexing.
Both extractors are subclasses of the generic class [FeatureExtractor.java](src/main/java/it/cnr/jatecs/indexing/corpus/FeatureExtractor.java) which provides additional capabilities like stemming, stopword removal, etc.

### Preparing data for experiments: feature selection and feature weighting
Once the index has been created and instantiated, a common practice often followed in the experimentation pipeline consists of selecting most informative features (and discarding the rest). JaTeCS provides several implementations of global (see [GlobalTSR.java](src/main/java/it/cnr/jatecs/indexing/tsr/GlobalTSR.java)) or local (see [LocalTSR.java](src/main/java/it/cnr/jatecs/indexing/tsr/LocalTSR.java)) Term Selection Reduction (TSR) [methods](http://nmis.isti.cnr.it/sebastiani/Publications/ACMCS02.pdf). JaTeCS also provides many implementations of popular TSR functions, including [InformationGain](src/main/java/it/cnr/jatecs/indexing/tsr/InformationGain.java), [ChiSquare](src/main/java/it/cnr/jatecs/indexing/tsr/ChiSquare.java), [GainRatio](src/main/java/it/cnr/jatecs/indexing/tsr/GainRatio.java), among many others. Additionally, the GlobalTSR can be set with different policies, such as *sum*, *average*, or *max* (subclasses of [IGlobalTSRPolicy.java](src/main/java/it/cnr/jatecs/indexing/tsr/IGlobalTSRPolicy.java)). JaTeCS also implements the [RoundRobinTSR](src/main/java/it/cnr/jatecs/indexing/tsr/RoundRobinTSR.java) method, which selects the most important features to each category in a round robin manner. The following snippet illustrates how round robin feature selection with information gain is carried out in JaTeCS (see the full example [here](src/example/java/apps/index/GlobalRoundRobinTSR.java)):

<pre><code>
	RoundRobinTSR tsr = new RoundRobinTSR(new InformationGain());
	tsr.setNumberOfBestFeatures(5000);
	tsr.computeTSR(index);	
</code></pre>

The last step in data preparation consists of weighting the features so as to bring bear to the "relative importance" of terms in the documents. JaTeCS offers two such popular methods, including the well-known [TfIdf](src/main/java/it/cnr/jatecs/indexing/weighting/TfNormalizedIdf.java) and [BM25](src/main/java/it/cnr/jatecs/indexing/weighting/BM25.java). Generally, the weighting function (here exemplified by TfIdf) is to be applied as follows (see the complete example [here](src/example/java/apps/index/TfIdfWeighting.java)):

<pre><code>
	IWeighting weighting = new TfNormalizedIdf(trainIndex);
	IIndex weightedTrainingIndex = weighting.computeWeights(trainIndex);
	IIndex weightedTestIndex = weighting.computeWeights(testIndex);	
</code></pre>

### Building the classifier
Building a classifier typically involves a two-step process, including (i) model learning ([ILearner](src/main/java/it/cnr/jatecs/classification/interfaces/ILearner.java)), and (ii) document classification [IClassifier](src/main/java/it/cnr/jatecs/classification/interfaces/IClassifier.java).
JaTeCS implements several machine learning algorithms, including: [AdaBoost-MH](src/main/java/it/cnr/jatecs/classification/adaboost), [MP-Boost](src/main/java/it/cnr/jatecs/classification/mpboost), [KNN](src/main/java/it/cnr/jatecs/classification/knn), [logistic_regression](src/main/java/it/cnr/jatecs/classification/logistic_regression), [naive bayes](src/main/java/it/cnr/jatecs/classification/naivebayes), [SVM](src/main/java/it/cnr/jatecs/classification/svm), among many others (placed in the source directory [classification](src/main/java/it/cnr/jatecs/classification)).

The following code shows how SVMlib could be trained in JaTeCS (check [LearnSVMlib.java](src/example/java/apps/classification/LearnSVMlib.java) for the full example, and the source directory [classification](src/example/java/apps/classification) for examples involving other learning algorithms):

<pre><code>
		SvmLearner svmLearner = new SvmLearner();
		IClassifier svmClassifier = svmLearner.build(trainIndex);
</code></pre>

Once trained, the model could be used to classify unseen documents. This is carried out in JaTeCS by running a [classifier](src/main/java/it/cnr/jatecs/classification/interfaces/IClassifier.java), instantiated with the previous model parameters and receiving as argument an index containing all test documents to be classified (a full example is available in [ClassifySVMlib.java](src/example/java/apps/classification/ClassifySVMlib.java)), i.e.,:

<pre><code>
		Classifier classifier = new Classifier(testIndex, svmClassifier);
		classifier.exec();
</code></pre>

JaTeCS also brings support to evaluation of results by means of the following classes: [ClassificationComparer.java](src/main/java/it/cnr/jatecs/evaluation/ClassificationComparer.java) (simple flat evaluation) and [HierarchicalClassificationComparer.java](src/main/java/it/cnr/jatecs/evaluation/HierarchicalClassificationComparer.java) (evaluation for hierarchical taxonomies of codes); a full example involving both evaluation procedures could be found [here](src/example/java/apps/classification/Evaluate.java). Evaluation is easily performed in JaTeCS in just few lines of code, e.g., :

<pre><code>
		ClassificationComparer flatComparer = new ClassificationComparer(classifier.getClassificationDB(), testIndex.getClassificationDB());
		ContingencyTableSet tableSet = flatComparer.evaluate();
</code></pre>


### Applications of Text Classification
JaTeCS includes many ready-to-use applications that could be useful for users which are mainly interested in running experiments on their own data quickly, but also for the practitioners, that might rather be interested in developing their own algorithms and applications; those might found on the JaTeCS apps implementations a perfect starting point where to start familiarizing with the framework through examples. In what follows, we show some selected examples, while many others could be found [here](src/example/java).

#### Text Quantification
Text Quantification is the problem of estimating the distribution of labels in a collection of unlabeled documents, when the distribution in the training set may substantially differ.
Though quantification processes a dataset as a single entity, the classification of single documents is the building block on which many quantification methods are built.
JaTeCS implements a number of [classification-based quantification methods](src/main/java/it/cnr/jatecs/quantification):

- [Classify and Count](http://nmis.isti.cnr.it/sebastiani/SlidesQuantification.pdf)
- Adjusted Classify and Count
- Probabilistic Classify and Count
- Probabilistic Adjusted Classify and Count

 All of their implementations are independent from the underlying classification method that acts as a plug-in component, as shown in the following code from the library (see the [complete examples](src/example/java/apps/quantification)):

<pre><code>
	int folds = 50;
	IScalingFunction scaling = new LogisticFunction();
	// any other learner can be plugged in
	ILearner classificationLearner = new SvmLightLearner();

	// learns six different quantifiers on training data (train is an IIndex object)
	QuantificationLearner quantificationLearner = new QuantificationLearner(folds, classificationLearner, scaling);
	QuantifierPool pool = quantificationLearner.learn(train);

	// quantifies on test returning the six predictions (test is an IIndex object)
	Quantification[] quantifications = pool.quantify(test);
	// evaluates predictions against true quantifications
	QuantificationEvaluation.Report(quantifications,test);
</code></pre>

#### Transfer Learning
Transfer Learning concerns with leveraging the supervised information available for a *source domain* of knowledge in order to deploy a model that behaves well on a *target domain* (to which few, or none, labelled information exists), thus reducing, or completely avoiding, the need for human labelling effort in the target domain. In the context of ATC two scenarios are possible (i) *cross-domain TC*, where the source and target documents deal with different topics (e.g., book reviews vs music reviews), and (ii) *cross-lingual TC*, in which the source and target documents are written in different languages (e.g., English vs German book reviews). JaTeCS includes an implementation of the [Distributional Correspondence Indexing (DCI)](src/main/java/it/cnr/jatecs/representation/transfer/dci/DistributionalCorrespondeceIndexing.java), a feature-representation-transfer method for cross-domain and cross-lingual classification, described [here](http://dx.doi.org/10.1613/jair.4762). The class [DCImain.java](src/example/java/apps/transferLearning/DCImain.java) offers a complete implementation of the method, from the reading of source and target collections to the evaluation of results.

#### Active Learning, Training Data Cleaning, and Semi Automated Text Classification
JaTeCS provides implementations of a rich number of methods proposed for three classes problems that are interrelated: [Active Learning (AL)](src/example/java/apps/satc), where the learning algorithm is prompted to select which documents to add to the training set at each step, with the aim of minimizing the amount of human labeling needed to obtain high accuracy; [Training Data Cleaning (TDC)](src/example/java/apps/trainingDataCleaning), that consists of using learning algorithms to discover labeling errors in an already existing training set; and [Semi-Automated Text Classification (SATC)](src/example/java/apps/satc), which aims at reducing the amount of effort a human should invest while inspecting, and eventually repairing, the outcomes produced by a classifier in order to guarantee a required accuracy level.

#### Distributional Semantic Models
ATC often relies on a BoW model to represent a document collection, according to which each document could be though as a row in a matrix where each column informs about the frequency (see [IContentDB](src/main/java/it/cnr/jatecs/indexes/DB/interfaces/IContentDB.java)), or the relative importance (see [IWeightingDB](src/main/java/it/cnr/jatecs/indexes/DB/interfaces/IWeightingDB.java)) of each distinct feature (usually terms or n-grams) to that document, disregarding word order. Other representation mechanisms have been proposed as alternatives, including the *distributional semantic models* (DSM), which typically project the original BoW model into a reduced space, where semantics between terms is somehow modelled. JaTeCS implements a number of DSM, covering some [Random Projections](src/main/java/it/cnr/jatecs/representation/randomprojections) methods (such as [Random Indexing](src/main/java/it/cnr/jatecs/representation/randomprojections/RandomIndexing.java), or the [Achlioptas](src/main/java/it/cnr/jatecs/representation/randomprojections/AchlioptasIndexing.java) mapping), and [Latent Semantic Analysis](/src/main/java/it/cnr/jatecs/representation/lsa/LSA.java) (by wrapping the popular [SVDLIBC](https://tedlab.mit.edu/~dr/SVDLIBC/) implementation). Those methods are available as part of [full applications](src/example/java/apps/distributionalsemanticmodels), from reading the indexes to the evaluation of results. 

### Imbalanced Text Classification
The accuracy of many classification algorithms is known to suffer when the data are imbalanced (i.e., when the distribution of the examples across the classes is severely skewed). Many applications of binary text classification are of this type, with the positive examples of the class of interest far outnumbered by the negative examples. Oversampling (i.e., generating synthetic training examples of the minority class) is an often used strategy to counter this problem. JaTeCS provides a number of [SMOTE-based](http://www.jair.org/papers/paper953.html) implementations, including the original [SMOTE](src/main/java/it/cnr/jatecs/representation/oversampling/SMOTE.java) approach, [BorderSMOTE](src/main/java/it/cnr/jatecs/representation/oversampling/BorderSMOTE.java), and [SMOTE-ENN](src/main/java/it/cnr/jatecs/representation/oversampling/SMOTE_ENN.java). JaTeCS also provides an [implementation](src/main/java/it/cnr/jatecs/representation/oversampling/DistributionalRandomOversampling.java) of the recently proposed [Distributional Random Oversampling](http://dl.acm.org/citation.cfm?id=2914722) (DRO) , an oversampling method specifically designed for classifying data (such as text) for which the distributional hypothesis holds. Full [applications](jatecs/src/example/java/apps/oversampling) using these methods are also provided in JaTeCS. 

