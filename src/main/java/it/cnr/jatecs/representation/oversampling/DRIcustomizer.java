package it.cnr.jatecs.representation.oversampling;

public class DRIcustomizer {
	
	public int _nThreads=1;
	public int _trainReplicants=2000;
	public Integer _testReplicants=1;
	public double _undersampling_ratio=-1;
	public boolean _useSoftmax=false;
	
	//if specified, tries to load the probabilistic model from the file
	//if not exists, then saves the computed model in this path
	public String _loadSaveProbmodelPath;
	
	//controls the criterion whereby the latent space is densified
	public String _latentDensity;
		
}
