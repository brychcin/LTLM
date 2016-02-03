package sr.lm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import sr.Sentence;
import sr.Vocabulary;
import sr.data.BasicDataProvider;
import sr.data.DataProvider;
import sr.eval.TestDataEvaluator;
import sr.infer.PseudoInferencer3gram;
import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;
import sr.utils.IOUtils;


public class LinearInterpolation extends Model {

	static Logger logger = Logger.getLogger(LinearInterpolation.class);
	private Model[] models;
	private double[] weights;
	private double EPSILON = 0.0001d;
	
	public LinearInterpolation(Model[] models) {
		if (models.length == 0) throw new IllegalArgumentException("you must give at least one model as a parameter");
		this.models = models;
		this.weights = new double[models.length];
		Arrays.fill(weights, 1.0d / models.length);
	}
	
	public LinearInterpolation(Model[] models, double[] weights) {
		if (models.length == 0) throw new IllegalArgumentException("you must give at least one model as a parameter");
		if (models.length != weights.length) throw new IllegalArgumentException("strange number of weights");
		this.models = models;
		this.weights = weights;
	}
	/*
	@Override
	public double getLogLikelihood(Sentence sentence) {
		double log = 0;
		for (int i=0; i<sentence.size(); i++) {
			log += Math.log(getProbabilityAtPosition(sentence, i));
		}
		
		return log;
	}*/

	@Override
	public Vocabulary getVocabulary() {
		return models[0].getVocabulary();
	}

	@Override
	public double getProbabilityAtPosition(Sentence sentence, int position, boolean joint) {
		double prob = 0;
		for (int i=0; i<models.length; i++) {
			prob += weights[i] * models[i].getProbabilityAtPosition(sentence, position, joint);
		}
		return prob;
	}
	
	@Override
	public double getProbabilityAtPosition(Sentence sentence, int position, int key, boolean joint) {
		double prob = 0;
		for (int i=0; i<models.length; i++) {
			prob += weights[i] * models[i].getProbabilityAtPosition(sentence, position, key, joint);
		}
		return prob;
	}

	@Override
	public void learnCountsFromInferedData(List<Sentence> sentences) {
		for (Model model : models) model.learnCountsFromInferedData(sentences);
	}
	
	@Override
	public void learnCountsFromInferedData(Sentence sentence) {
		for (Model model : models) model.learnCountsFromInferedData(sentence);
	}

	@Override
	public boolean test() {
		return true;
	}

	@Override
	public void optimizeHyperParameters() {}
	
	public void optimizeWeights(DataProvider provider) throws IOException {
		logger.info("===WEIGHTS OPTIMIZATION===");
		
		List<Sentence> heldout = new ArrayList<Sentence>();
		//int counter = 0;
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {
			//if (sentence.size() > MAX_SENTENCE_LENGTH) continue;
			//logger.info(counter+" sentences processed");
			heldout.add(sentence);
			//counter++;
		}
		provider.close();
		
		
		double difference = Double.MAX_VALUE;
		while (difference > EPSILON) {
			
			double[] expectedCountsOfLambdas = new double[models.length];
			double[] nextLambdas = new double[models.length];
			
			for (Sentence s : heldout) {
				for (int pos=0; pos<s.size(); pos++) {
					double prob = getProbabilityAtPosition(s, pos, false);
					
					for (int i=0; i<models.length; i++) {
						double pr = models[i].getProbabilityAtPosition(s, pos, false);
						expectedCountsOfLambdas[i] += (weights[i] * pr / prob);
					}
				}
			}
			
			// sum for next lambdas
			double sum = 0.0;
			for (int k = 0; k < this.weights.length; k++) {
				sum += expectedCountsOfLambdas[k];
			}

			// next lambdas
			for (int j = 0; j < this.weights.length; j++) {
				nextLambdas[j] = expectedCountsOfLambdas[j] / sum;
			}

			// updating difference
			double maxDifference = 0.0;
			for (int i = 0; i < this.weights.length; i++) {
				double d = Math.abs(nextLambdas[i] - weights[i]);

				if (d > maxDifference) {
					maxDifference = d;
				}
			}

			difference = maxDifference;
			weights = nextLambdas;
			logger.info(Arrays.toString(weights)+" diff="+difference);
		}

	}
	
	@Deprecated
	@Override
	public Model createCopy() {
		return null;
	}
	
	@Override
	public void initialize() {}
	

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		PropertyConfigurator.configure("log4j.properties");
		
		int MAX_SENTENCE_LENGTH = 30;
		LTLM2sides3gram LTLM3gram = IOUtils.load3Gram("models/LTLM100czengQuarter50000.bin");
		PseudoInferencer3gram treeBuilder = new PseudoInferencer3gram(LTLM3gram, MAX_SENTENCE_LENGTH);		
		
		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
		DataProvider provider = new BasicDataProvider("D:/korpusy/czeng/trainQuarter.txt", preprocessing, LTLM3gram.getVocabulary(), 3, MAX_SENTENCE_LENGTH);
		
		ModifiedKneserNeyInterpolation mkn = new ModifiedKneserNeyInterpolation(LTLM3gram.getVocabulary(), 4);
		
		int counter = 0;
		List<Sentence> sentences = new ArrayList<Sentence>();
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {
			mkn.processSentence(sentence);
			sentences.add(sentence);
			
			if (counter % 10000 == 0) logger.info(counter+" sentences processed");
			counter++;
		}
		provider.close();
		mkn.process();
		
		LinearInterpolation li = new LinearInterpolation(new Model[]{mkn, LTLM3gram}, new double[]{0.7d, 0.3d});
		//li.optimizeWeights(treeBuilder, MAX_SENTENCE_LENGTH, new BasicDataProvider("data/heldout-small.txt", preprocessing, LTLM3gram.getVocabulary(), 3, MAX_SENTENCE_LENGTH));
		
		//ProbabilityEvaluator eval = new ProbabilityEvaluator(treeBuilder, LTLM3gram, MAX_SENTENCE_LENGTH);
		TestDataEvaluator eval = new TestDataEvaluator(treeBuilder, li, MAX_SENTENCE_LENGTH);
		eval.process(new BasicDataProvider("D:/korpusy/czeng/etest.txt", preprocessing, LTLM3gram.getVocabulary(), 3, MAX_SENTENCE_LENGTH));
		
		
	}






	
}
