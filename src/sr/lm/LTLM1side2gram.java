package sr.lm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import sr.Sentence;
import sr.Token;
import sr.Vocabulary;
import sr.prob.DirichletHyperparametersOptimizer;
import sr.utils.Cloner;


public class LTLM1side2gram extends Model {

	private static final long serialVersionUID = -435763286904872974L;
	static transient Logger logger = Logger.getLogger(LTLM1side2gram.class);
	
	private Vocabulary vocabulary;
	private final int roles;
	private final int words;
	//private final int MAX_DISTANCE = 10;
	
	private int[][] wordByRoleCounts;
	private int[] wordRoleCounts;
	
	private int[][] roleByRoleCounts;
	private int[] roleCounts;
	
	private double[] alpha;
	private double alphaSum;

	private double beta;
	private double betaSum;
	
	private DirichletHyperparametersOptimizer optWordByRole;
	private DirichletHyperparametersOptimizer optRoleByRole;
	
	
	public LTLM1side2gram(int roles, Vocabulary vocabulary) {
		this.roles = roles;
		this.vocabulary = vocabulary;
		this.words = vocabulary.size();
		
		this.alphaSum = 0.1d * roles;
		this.alpha = new double[roles];
		Arrays.fill(this.alpha, 0.1d);
		
		this.beta = 0.01d;
		this.betaSum = beta * words;
		
		this.optWordByRole = new DirichletHyperparametersOptimizer(words, roles);
		this.optRoleByRole = new DirichletHyperparametersOptimizer(roles, roles);
		
		initialize();
	}

	public void initialize() {	
		this.wordRoleCounts = new int[roles];
		this.wordByRoleCounts = new int[words][roles];
		this.roleCounts = new int[roles];
		this.roleByRoleCounts = new int[roles][roles];
	}
	
	@Override
	public void learnCountsFromInferedData(List<Sentence> sentences) {
		initialize();
		for (Sentence sentence : sentences) {
			learnCountsFromInferedData(sentence);
		}
	}
	
	@Override
	public void learnCountsFromInferedData(Sentence sentence) {
		for (Token token : sentence.getTokens()) {
			int key = token.getKey();
			int roleKey = token.getRole().getKey();
			int parent = token.getRole().getParent().getKey();
				
			wordByRoleCounts[key][roleKey]++;
			wordRoleCounts[roleKey]++;
						
			roleByRoleCounts[roleKey][parent]++;
			roleCounts[parent]++;	
		}
	}

	@Override
	public double getProbabilityAtPosition(Sentence sentence, int position, boolean joint) {
		Token token = sentence.getTokens()[position];
		int key = token.getKey();
		int parentKey = token.getRole().getParent().getKey();
		
		if (joint) {
			int role = token.getRole().getKey();
			return ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCounts[role][parentKey] + alpha[role]) / (roleCounts[parentKey] + alphaSum));
		} else {
			double sum = 0;
			for (int role=0; role<roles; role++) {
				sum += ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCounts[role][parentKey] + alpha[role]) / (roleCounts[parentKey] + alphaSum));
			}
			
			return sum;
		}
	}
	
	@Override
	public double getProbabilityAtPosition(Sentence sentence, int position, int key, boolean joint) {
		Token token = sentence.getTokens()[position];
		int parentKey = token.getRole().getParent().getKey();
		
		if (joint) {
			int role = token.getRole().getKey();
			return ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCounts[role][parentKey] + alpha[role]) / (roleCounts[parentKey] + alphaSum));
		} else {	
			double sum = 0;
			for (int role=0; role<roles; role++) {
				sum += ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCounts[role][parentKey] + alpha[role]) / (roleCounts[parentKey] + alphaSum));
			}
			
			return sum;
		}
	}

	/*
	@Override
	public double getLogLikelihood(Sentence sentence) {
		double log = 0d;

		for (Token token : sentence.getTokens()) {
			int key = token.getKey();
			
			int role = token.getRole().getKey();
			int parentKey = token.getRole().getParent().getKey();
			
			double sum = ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum));
			sum *= ((roleByRoleCounts[role][parentKey] + alpha[role]) / (roleCounts[parentKey] + alphaSum));
			
			log += Math.log(sum);
		}
		
		return log;
	}
	*/
	
	public boolean test() {
		boolean test1 = true;
		for (int role=0; role<roles; role++) {
			double sum = 0;
			for (int word=0; word<words; word++) {
				sum += (wordByRoleCounts[word][role] + beta) / (wordRoleCounts[role] + betaSum);
			}
			if (sum < 0.99d || sum > 1.01d) {
				logger.info(sum);
				test1 = false;
				break;
			}
		}
		if (test1) {
			logger.info("WORD BY ROLE TEST je OK");
		} else logger.info("WORD BY ROLE TEST je spatne");
		
		boolean test2 = true;
		for (int parent=0; parent<roles; parent++) {
			double sum = 0;
			for (int role=0; role<roles; role++) {
				sum += (roleByRoleCounts[role][parent] + alpha[role]) / (roleCounts[parent] + alphaSum);
			}
			if (sum < 0.99d || sum > 1.01d) {
				logger.info(sum);
				test2 = false;
				break;
			}
		}
		if (test2) {
			logger.info("ROLE BY ROLE LEFT TEST je OK");
		} else logger.info("ROLE BY ROLE LEFT TEST je spatne");
		

		
		if (test1 && test2)	{
			return true;
		} else {
			logger.error("PRAVDEPODOBNOSTNI ROZDELENI JSOU SPATNE");
			System.exit(1);
		}
		return false;
	}
	
	public void optimizeHyperParameters() {
		alpha = this.optRoleByRole.optimize(alpha, this.roleByRoleCounts, this.roleCounts);
		alphaSum = 0;
		for (double a : alpha) alphaSum += a;

		betaSum = this.optWordByRole.optimize(betaSum, this.wordByRoleCounts, this.wordRoleCounts);
		beta = betaSum / words;
		
	}
	
	public String toString() {
		String s = "MODEL SETTING\n";

		s += "alphaSum="+alphaSum+" alpha="+Arrays.toString(alpha)+"\n";
		s += "betaSum="+betaSum+" beta="+beta+"\n";
		
		return s;
	}
	

	@Override
	public Vocabulary getVocabulary() {
		return vocabulary;
	}
	
	public void print() {
		final int maxWordsToPrint = 30;
		
		logger.info("===WORDS IN ROLES===");
		for (int role=0; role<roles; role++) {
			Map<String, Double> wordsInRole = new HashMap<String, Double>();
			
			for (int wordKey=0; wordKey<vocabulary.size(); wordKey++) {
				double prob = wordByRoleCounts[wordKey][role];
				wordsInRole.put(vocabulary.getWordByKey(wordKey), prob);
			}
			
			List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>> ();
			list.addAll(wordsInRole.entrySet());
			Collections.sort(list, new EntryComparator());
			

			String s = "Role "+role+":\t";
			for (int i=0; i<maxWordsToPrint; i++) {
				Map.Entry<String, Double> entry = list.get(i);
				s = s+""+entry.getKey()+", ";
			}
			logger.info(s);
		}		
	}
	
	private class EntryComparator implements Comparator<Entry<String, Double>> {
		@Override
		public int compare(Entry<String, Double> a, Entry<String, Double> b) {
			return Double.compare(b.getValue(), a.getValue());
		}
	}

	public int getRoles() {
		return roles;
	}


	public int getWords() {
		return words;
	}


	public int[][] getWordByRoleCounts() {
		return wordByRoleCounts;
	}


	public int[] getWordRoleCounts() {
		return wordRoleCounts;
	}


	public int[][] getRoleByRoleCounts() {
		return roleByRoleCounts;
	}


	public int[] getRoleCounts() {
		return roleCounts;
	}

	public double[] getAlpha() {
		return alpha;
	}


	public double getAlphaSum() {
		return alphaSum;
	}

	public double getBeta() {
		return beta;
	}


	public double getBetaSum() {
		return betaSum;
	}

	@Override
	public Model createCopy() {
		LTLM1side2gram copy = new LTLM1side2gram(roles, vocabulary);
		
		//relative copies
		copy.vocabulary = vocabulary;
		copy.alpha = alpha;
		copy.alphaSum = alphaSum;
		copy.beta = beta;
		copy.betaSum = betaSum;
		copy.optRoleByRole = optRoleByRole;
		copy.optWordByRole = optWordByRole;
		
		//absolute copies
		copy.wordByRoleCounts = Cloner.copyArray(wordByRoleCounts);
		copy.wordRoleCounts = Cloner.copyArray(wordRoleCounts);
		copy.roleByRoleCounts = Cloner.copyArray(roleByRoleCounts);
		copy.roleCounts = Cloner.copyArray(roleCounts);
				
		return copy;
	}


	
}
