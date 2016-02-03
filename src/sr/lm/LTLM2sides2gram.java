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


public class LTLM2sides2gram extends Model {

	private static final long serialVersionUID = -435763286904872974L;
	static transient Logger logger = Logger.getLogger(LTLM2sides2gram.class);
	
	private Vocabulary vocabulary;
	private final int roles;
	private final int words;
	//private final int MAX_DISTANCE = 10;
	
	private int[][] wordByRoleCounts;
	private int[] wordRoleCounts;
	
	private int[][] roleByRoleCountsLeft;
	private int[] roleCountsLeft;
	private int[][] roleByRoleCountsRight;
	private int[] roleCountsRight;
	
	private double[] alphaLeft;
	private double alphaSumLeft;
	private double[] alphaRight;
	private double alphaSumRight;

	private double beta;
	private double betaSum;
	
	private DirichletHyperparametersOptimizer optWordByRole;
	private DirichletHyperparametersOptimizer optRoleByRole;
	
	//private Distance distanceEstimator;
	//private boolean useDistance = false;
	
	public LTLM2sides2gram(int roles, Vocabulary vocabulary) {
		this.roles = roles;
		this.vocabulary = vocabulary;
		this.words = vocabulary.size();
		
		this.alphaSumLeft = 0.1d * roles;
		this.alphaSumRight = 0.1d * roles;
		this.alphaLeft = new double[roles];
		Arrays.fill(this.alphaLeft, 0.1d);
		this.alphaRight = new double[roles];
		Arrays.fill(this.alphaRight, 0.1d);
		
		this.beta = 0.01d;
		this.betaSum = beta * words;
		
		this.optWordByRole = new DirichletHyperparametersOptimizer(words, roles);
		this.optRoleByRole = new DirichletHyperparametersOptimizer(roles, roles);
		
		initialize();
	}

	public void initialize() {
		this.wordRoleCounts = new int[roles];
		this.wordByRoleCounts = new int[words][roles];
		
		this.roleCountsLeft = new int[roles];
		this.roleByRoleCountsLeft = new int[roles][roles];
		this.roleCountsRight = new int[roles];
		this.roleByRoleCountsRight = new int[roles][roles];
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
			int roleKey = token.getRole().getKey();
			int key = token.getKey();
			int parent = token.getRole().getParent().getKey();
					
			wordByRoleCounts[key][roleKey]++;
			wordRoleCounts[roleKey]++;
						
			if (token.getRole().isOnLeft()) {
				roleByRoleCountsLeft[roleKey][parent]++;
				roleCountsLeft[parent]++;
			} else {
				roleByRoleCountsRight[roleKey][parent]++;
				roleCountsRight[parent]++;
			}
		}
	}

	@Override
	public double getProbabilityAtPosition(Sentence sentence, int position, boolean joint) {
		Token token = sentence.getTokens()[position];
		int key = token.getKey();
		int parentKey = token.getRole().getParent().getKey();
		
		double sum = 0;
		if (token.getRole().isOnLeft()) {
			if (joint) {
				int role = token.getRole().getKey();
				return ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCountsLeft[role][parentKey] + alphaLeft[role]) / (roleCountsLeft[parentKey] + alphaSumLeft));
			} else {
				for (int role=0; role<roles; role++) {
					sum += ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCountsLeft[role][parentKey] + alphaLeft[role]) / (roleCountsLeft[parentKey] + alphaSumLeft));
				}
			}
		} else {
			if (joint) {
				int role = token.getRole().getKey();
				return ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCountsRight[role][parentKey] + alphaRight[role]) / (roleCountsRight[parentKey] + alphaSumRight));
			} else {
				for (int role=0; role<roles; role++) {
					sum += ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCountsRight[role][parentKey] + alphaRight[role]) / (roleCountsRight[parentKey] + alphaSumRight));
				}
			}
		}
		
		return sum;
	}
	
	@Override
	public double getProbabilityAtPosition(Sentence sentence, int position, int key, boolean joint) {
		Token token = sentence.getTokens()[position];
		int parentKey = token.getRole().getParent().getKey();
		
		double sum = 0;
		if (token.getRole().isOnLeft()) {
			if (joint) {
				int role = token.getRole().getKey();
				return ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCountsLeft[role][parentKey] + alphaLeft[role]) / (roleCountsLeft[parentKey] + alphaSumLeft));
			} else {
				for (int role=0; role<roles; role++) {
					sum += ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCountsLeft[role][parentKey] + alphaLeft[role]) / (roleCountsLeft[parentKey] + alphaSumLeft));
				}
			}
		} else {
			if (joint) {
				int role = token.getRole().getKey();
				return ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCountsRight[role][parentKey] + alphaRight[role]) / (roleCountsRight[parentKey] + alphaSumRight));
			} else {
				for (int role=0; role<roles; role++) {
					sum += ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleCountsRight[role][parentKey] + alphaRight[role]) / (roleCountsRight[parentKey] + alphaSumRight));
				}
			}
		}
		
		return sum;
	}

	
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
				sum += (roleByRoleCountsLeft[role][parent] + alphaLeft[role]) / (roleCountsLeft[parent] + alphaSumLeft);
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
		
		
		boolean test3 = true;
		for (int parent=0; parent<roles; parent++) {
			double sum = 0;
			for (int role=0; role<roles; role++) {
				sum += (roleByRoleCountsRight[role][parent] + alphaRight[role]) / (roleCountsRight[parent] + alphaSumRight);
			}
			if (sum < 0.99d || sum > 1.01d) {
				logger.info(sum);
				test3 = false;
				break;
			}
		}
		if (test3) {
			logger.info("ROLE BY ROLE RIGHT TEST je OK");
		} else logger.info("ROLE BY ROLE RIGHT TEST je spatne");
		
		
		if (test1 && test2 && test3)	{
			return true;
		} else {
			logger.error("PRAVDEPODOBNOSTNI ROZDELENI JSOU SPATNE");
			System.exit(1);
		}
		return false;
	}
	
	public void optimizeHyperParameters() {
		alphaLeft = this.optRoleByRole.optimize(alphaLeft, this.roleByRoleCountsLeft, this.roleCountsLeft);
		alphaSumLeft = 0;
		for (double a : alphaLeft) alphaSumLeft += a;
		
		alphaRight = this.optRoleByRole.optimize(alphaRight, this.roleByRoleCountsRight, this.roleCountsRight);
		alphaSumRight = 0;
		for (double a : alphaRight) alphaSumRight += a;
		
		betaSum = this.optWordByRole.optimize(betaSum, this.wordByRoleCounts, this.wordRoleCounts);
		beta = betaSum / words;
		
	}
	
	public String toString() {
		String s = "MODEL SETTING\n";

		s += "alphaSumLeft="+alphaSumLeft+" alpha="+Arrays.toString(alphaLeft)+"\n";
		s += "alphaSumRight="+alphaSumRight+" alpha="+Arrays.toString(alphaRight)+"\n";
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
			
			for (int wordKey=1; wordKey<vocabulary.size(); wordKey++) {
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


	public int[][] getRoleByRoleCountsLeft() {
		return roleByRoleCountsLeft;
	}


	public int[] getRoleCountsLeft() {
		return roleCountsLeft;
	}


	public int[][] getRoleByRoleCountsRight() {
		return roleByRoleCountsRight;
	}


	public int[] getRoleCountsRight() {
		return roleCountsRight;
	}


	public double[] getAlphaLeft() {
		return alphaLeft;
	}


	public double getAlphaSumLeft() {
		return alphaSumLeft;
	}


	public double[] getAlphaRight() {
		return alphaRight;
	}


	public double getAlphaSumRight() {
		return alphaSumRight;
	}


	public double getBeta() {
		return beta;
	}


	public double getBetaSum() {
		return betaSum;
	}

	@Override
	public Model createCopy() {
		LTLM2sides2gram copy = new LTLM2sides2gram(roles, vocabulary);
		
		//relative copies
		copy.vocabulary = vocabulary;
		copy.alphaLeft = alphaLeft;
		copy.alphaRight = alphaRight;
		copy.alphaSumLeft = alphaSumLeft;
		copy.alphaSumRight = alphaSumRight;
		copy.beta = beta;
		copy.betaSum = betaSum;
		copy.optRoleByRole = optRoleByRole;
		copy.optWordByRole = optWordByRole;
			
		//absolute copies
		copy.wordByRoleCounts = Cloner.copyArray(wordByRoleCounts);
		copy.wordRoleCounts = Cloner.copyArray(wordRoleCounts);
		copy.roleByRoleCountsLeft = Cloner.copyArray(roleByRoleCountsLeft);
		copy.roleByRoleCountsRight = Cloner.copyArray(roleByRoleCountsRight);
		copy.roleCountsLeft = Cloner.copyArray(roleCountsLeft);
		copy.roleCountsRight = Cloner.copyArray(roleCountsRight);
		
		return copy;
	}


	
}
