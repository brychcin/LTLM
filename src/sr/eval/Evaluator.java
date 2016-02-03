package sr.eval;

import org.apache.log4j.Logger;

import sr.Sentence;
import sr.lm.Model;
import sr.utils.MemoryUtils;

public class Evaluator {
	
	static Logger logger = Logger.getLogger(Evaluator.class);
	
	private double logLikelihoodOOV = 0;
	private double logLikelihoodNotOOV = 0;
	private double logLikelihood = 0;
	private long tokens = 0;
	private long tokensOOV = 0;
	private long tokensNotOOV = 0;
	private long sentences = 0;
	private Model model;	
	private boolean joint = false;
	
	public Evaluator(Model model, boolean joint) {
		this.model = model;
		this.joint = joint;
	}
	
	public void newRound() {
		logLikelihood = 0;
		logLikelihoodOOV = 0;
		logLikelihoodNotOOV = 0;
		tokens = 0;
		sentences = 0;
		tokensOOV = 0;
		tokensNotOOV = 0;
	}
	
	public void processSentence(Sentence sentence) {
		for (int pos=0; pos<sentence.size(); pos++) {
			double log = Math.log(model.getProbabilityAtPosition(sentence, pos, joint));
			logLikelihood += log;
			tokens++;
			
			if (sentence.getTokens()[pos].getKey()==1) {
				logLikelihoodOOV += log;
				tokensOOV++;
			} else {
				logLikelihoodNotOOV += log;
				tokensNotOOV++;
			}			
		}

		sentences++;
	}
	
	public void print() {
		logger.debug(MemoryUtils.usedMemoryToString());
		double ppx = Math.exp(-logLikelihood/tokens);
		double ppxOOV = Math.exp(-logLikelihoodOOV/tokensOOV);
		double ppxNotOOV = Math.exp(-logLikelihoodNotOOV/tokensNotOOV);
		double OOVrate = ((double)tokensOOV / (double)tokens) * 100.0d;
		
		logger.info("sentences="+sentences+", tokens="+tokens+" OOV rate="+OOVrate+"%, ppx="+ppx+", ppxOOV="+ppxOOV+", ppxNotOOV="+ppxNotOOV);	
		logger.debug(model.toString());
	}
}
