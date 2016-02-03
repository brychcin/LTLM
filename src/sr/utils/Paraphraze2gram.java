package sr.utils;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.PropertyConfigurator;

import sr.Role;
import sr.Sentence;
import sr.Token;
import sr.Vocabulary;
import sr.infer.ExactInferencer2gram;
import sr.infer.Inferencer;
import sr.lm.LTLM2sides2gram;

public class Paraphraze2gram {

	private Vocabulary vocabulary;
	private final int roles;
	private final int words;
	
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
	
	private final int ALTERNATIVES;
	
	public Paraphraze2gram(LTLM2sides2gram ltlm, int ALTERNATIVES) {
		this.vocabulary = ltlm.getVocabulary();
		this.roles = ltlm.getRoles();
		this.words = ltlm.getWords();
		this.wordByRoleCounts = ltlm.getWordByRoleCounts();
		this.wordRoleCounts = ltlm.getWordRoleCounts();
		this.roleByRoleCountsLeft = ltlm.getRoleByRoleCountsLeft();
		this.roleByRoleCountsRight = ltlm.getRoleByRoleCountsRight();
		this.roleCountsLeft = ltlm.getRoleCountsLeft();
		this.roleCountsRight = ltlm.getRoleCountsRight();
		this.alphaLeft = ltlm.getAlphaLeft();
		this.alphaRight = ltlm.getAlphaRight();
		this.beta = ltlm.getBeta();
		this.betaSum = ltlm.getBetaSum();
		this.ALTERNATIVES = ALTERNATIVES;
	}
	
	public void process(Sentence sentence) {
		Object[][] table = new Object[ALTERNATIVES+1][sentence.size()*2];
		
		int index = 0;
		for (Token token : sentence.getTokens()) {	
			Role role = token.getRole();
			Role parent = role.getParent();
			int parentKey = parent.getKey();
			
			double[] scores = new double[roles];
			if (role.isOnLeft()) {
				for (int roleKey=1; roleKey<roles; roleKey++) {
					double prob = ((roleByRoleCountsLeft[roleKey][parentKey] + alphaLeft[roleKey]) / (roleCountsLeft[parentKey] + alphaSumLeft));
					scores[roleKey] = prob;
				}
			} else {
				for (int roleKey=1; roleKey<roles; roleKey++) {
					double prob = ((roleByRoleCountsRight[roleKey][parentKey] + alphaRight[roleKey]) / (roleCountsRight[parentKey] + alphaSumRight));
					scores[roleKey] = prob;
				}
			}
			
			for (Role child : role.getChildrens()) {
				int childKey = child.getKey();
				
				if (child.isOnLeft()) {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double prob = ((roleByRoleCountsLeft[childKey][roleKey] + alphaLeft[childKey]) / (roleCountsLeft[roleKey] + alphaSumLeft));
						scores[roleKey] *= prob;
					}
				} else {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double prob = ((roleByRoleCountsRight[childKey][roleKey] + alphaRight[childKey]) / (roleCountsRight[roleKey] + alphaSumRight));
						scores[roleKey] *= prob;
					}
				}
			}
			
			double sum = 0;
			for (int roleKey=1; roleKey<roles; roleKey++) sum += scores[roleKey];
			for (int roleKey=1; roleKey<roles; roleKey++) scores[roleKey] /= sum;
			
			
			
			int original = role.getToken().getKey();
			Map.Entry<String, Double> origPossibility = null;
			
			List<Map.Entry<String, Double>> possibilities = new ArrayList<Map.Entry<String, Double>>();
			for (int wordKey=1; wordKey<vocabulary.size(); wordKey++) {
				double prob = 0;
				for (int roleKey=1; roleKey<roles; roleKey++) {
					prob += (wordByRoleCounts[wordKey][roleKey] + beta) / (wordRoleCounts[roleKey] + betaSum) * scores[roleKey];
				}
				
				String word = vocabulary.getWordByKey(wordKey);
				possibilities.add(new AbstractMap.SimpleEntry<String, Double>(word, prob));
				
				if (original == wordKey) origPossibility = new AbstractMap.SimpleEntry<String, Double>(word, prob);
			}
			
			Collections.sort(possibilities, new PossiblitiesComparator());
			
			//System.out.println(origPossibility.getKey()+" "+origPossibility.getValue());
			table[0][index] = origPossibility.getKey();
			table[0][index+1] = origPossibility.getValue();
			
			for (int i=0; i<ALTERNATIVES; i++) {
				Map.Entry<String, Double> possibility = possibilities.get(i);
				table[i+1][index] = possibility.getKey();
				table[i+1][index+1] = possibility.getValue();
				
				//System.out.println(possibility.getKey()+" "+possibility.getValue());
			}
			//System.out.println("---------------------------");
			
			index+=2;
		}	
		
		String format = "";
		for (int i=0; i<sentence.size(); i++) {
			format = format+" %15s %10f |";
		}
		format = format+"\n";
		
		for (int i=0; i<ALTERNATIVES+1; i++) {
			System.out.format(format, table[i]);
		}
		
	}
	
	class PossiblitiesComparator implements Comparator<Map.Entry<String, Double>> {
		public int compare(Entry<String, Double> a, Entry<String, Double> b) {
			return Double.compare(b.getValue(), a.getValue());
		}
	}
	
	
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		
		PropertyConfigurator.configure("log4j.properties");
		LTLM2sides2gram ltlm = IOUtils.load2Gram("models/2gram_en_LTLM_1000roles.bin");
		Inferencer infer = new ExactInferencer2gram(ltlm, 30);
		
		//Never interrupt your enemy when he is making a mistake.
		//He does n't know what is this .
		//Everything has beauty, but not everyone sees it.
		
		String[] words = "Everything has beauty , but not everyone sees it .".toLowerCase().split(" ");
		int[] keys = ltlm.getVocabulary().getWordKey(words);
		Sentence sentence = new Sentence(keys);
		infer.infer(sentence, false, false);
		
		GraphViz.save(sentence, ltlm.getVocabulary(), "example.dot", "example.png");
		
		Paraphraze2gram par = new Paraphraze2gram(ltlm, 10);
		par.process(sentence);

		
		
	}

}
