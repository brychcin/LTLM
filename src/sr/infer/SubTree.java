package sr.infer;

import sr.Role;

public class SubTree extends Role {
	
	private double logLikelihood;
	//private double probability;

	public SubTree(short key, short position) {
		super(key, null, position);
		this.logLikelihood = Double.NEGATIVE_INFINITY;
		//this.probability = Double.NEGATIVE_INFINITY;
	}

	public double getLogLikelihood() {
		return logLikelihood;
	}

	public void setLogLikelihood(double logLikelihood) {
		this.logLikelihood = logLikelihood;
	}
/*
	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}
*/

	
	
}
