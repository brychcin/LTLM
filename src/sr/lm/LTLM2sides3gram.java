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


/**
 * udelat inferenci na heldout datech - !!neupravovat pravdepodobnostni rozdeleni!! - jenom nasamplovat stromy
 * a podle toho optimalizovat vahu mezi trigramy a bigramy
 * @author Brych
 *
 */
public class LTLM2sides3gram extends Model {

	private static final long serialVersionUID = -435763286904872974L;
	static transient Logger logger = Logger.getLogger(LTLM2sides3gram.class);
	
	private Vocabulary vocabulary;
	private final int roles;
	private final int words;
	//private final int MAX_DISTANCE = 10;
	
	private int[][] wordByRoleCounts;
	private int[] wordRoleCounts;
	
	//bigramy vlevo - node x parent
	//private int[][] roleByRoleCountsLeft2;
	//private int[] roleCountsLeft2;
	//bigramy vpravo - node x parent
	//private int[][] roleByRoleCountsRight2;
	//private int[] roleCountsRight2;
	
	//trigramy vlevo - node x parent x parent-of-parent
	private int[][][] roleByRoleByRoleCountsLeft3;
	private int[][] roleByRoleCountsLeft3;
	//trigramy right - node x parent x parent-of-parent
	private int[][][] roleByRoleByRoleCountsRight3;
	private int[][] roleByRoleCountsRight3;
	
	
	//private double[] alphaLeft;
	//private double alphaSumLeft;
	//private double[] alphaRight;
	//private double alphaSumRight;

	private double beta;
	private double betaSum;
	
	private double[] gammaLeft;
	private double gammaSumLeft;
	private double[] gammaRight;
	private double gammaSumRight;
	
	//weight of trigram model
	//private MutableDouble weight = new MutableDouble(1.0d);
	
	private DirichletHyperparametersOptimizer optWordByRole;
	private DirichletHyperparametersOptimizer optRoleByRole;
	private DirichletHyperparametersOptimizer optRoleByRoleByRole;
	
	
	public LTLM2sides3gram(int roles, Vocabulary vocabulary) {
		this.roles = roles;
		this.vocabulary = vocabulary;
		this.words = vocabulary.size();
		
		//this.alphaSumLeft = 0.1d * roles;
		//this.alphaSumRight = 0.1d * roles;
		//this.alphaLeft = new double[roles];
		//Arrays.fill(this.alphaLeft, 0.1d);
		//this.alphaRight = new double[roles];
		//Arrays.fill(this.alphaRight, 0.1d);
		
		this.beta = 0.01d;
		this.betaSum = beta * words;
		
		this.gammaLeft = new double[roles];
		Arrays.fill(this.gammaLeft, 0.05d);
		this.gammaSumLeft = 0.05d * roles;
		this.gammaRight = new double[roles];
		Arrays.fill(this.gammaRight, 0.05d);
		this.gammaSumRight = 0.05d * roles;
		
		this.optWordByRole = new DirichletHyperparametersOptimizer(words, roles);
		this.optRoleByRole = new DirichletHyperparametersOptimizer(roles, roles);
		this.optRoleByRoleByRole = new DirichletHyperparametersOptimizer(roles, roles*roles);
		
		initialize();
	}
	
	public void initialize() {
		this.wordRoleCounts = new int[roles];
		this.wordByRoleCounts = new int[words][roles];
		//this.roleCountsLeft2 = new int[roles];
		//this.roleByRoleCountsLeft2 = new int[roles][roles];
		//this.roleCountsRight2 = new int[roles];
		//this.roleByRoleCountsRight2 = new int[roles][roles];
		this.roleByRoleByRoleCountsLeft3 = new int[roles][roles][roles];
		this.roleByRoleCountsLeft3 = new int[roles][roles];
		this.roleByRoleByRoleCountsRight3 = new int[roles][roles][roles];
		this.roleByRoleCountsRight3 = new int[roles][roles];
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
			
			int parentOfParent;
			if (token.getRole().getParent().getPosition() < 0) {
				parentOfParent = parent;
			} else {
				parentOfParent = token.getRole().getParent().getParent().getKey();
			}	
			
			wordByRoleCounts[key][roleKey]++;
			wordRoleCounts[roleKey]++;
					
			if (token.getRole().isOnLeft()) {
				//roleByRoleCountsLeft2[roleKey][parent]++;
				//roleCountsLeft2[parent]++;
				roleByRoleByRoleCountsLeft3[roleKey][parent][parentOfParent]++;
				roleByRoleCountsLeft3[parent][parentOfParent]++;
			} else {
				//roleByRoleCountsRight2[roleKey][parent]++;
				//roleCountsRight2[parent]++;
				roleByRoleByRoleCountsRight3[roleKey][parent][parentOfParent]++;
				roleByRoleCountsRight3[parent][parentOfParent]++;
			}	
		}
	}
	
	@Override
	public double getProbabilityAtPosition(Sentence sentence, int position, boolean joint) {
		Token token = sentence.getTokens()[position];
		int key = token.getKey();
		int parentKey = token.getRole().getParent().getKey();
		int parentOfParentKey;
		if (token.getRole().getParent().getPosition() < 0) {
			parentOfParentKey = parentKey;
		} else {
			parentOfParentKey = token.getRole().getParent().getParent().getKey();
		}
		
		double sum = 0;
		if (token.getRole().isOnLeft()) {
			if (joint) {
				int role = token.getRole().getKey();
				return ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleByRoleCountsLeft3[role][parentKey][parentOfParentKey] + gammaLeft[role]) / (roleByRoleCountsLeft3[parentKey][parentOfParentKey] + gammaSumLeft));
			} else {
				for (int role=0; role<roles; role++) {
					sum += ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleByRoleCountsLeft3[role][parentKey][parentOfParentKey] + gammaLeft[role]) / (roleByRoleCountsLeft3[parentKey][parentOfParentKey] + gammaSumLeft));
				}
			}
		} else {
			if (joint) {
				int role = token.getRole().getKey();
				return ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleByRoleCountsRight3[role][parentKey][parentOfParentKey] + gammaRight[role]) / (roleByRoleCountsRight3[parentKey][parentOfParentKey] + gammaSumRight));
			} else {
				for (int role=0; role<roles; role++) {
					sum += ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleByRoleCountsRight3[role][parentKey][parentOfParentKey] + gammaRight[role]) / (roleByRoleCountsRight3[parentKey][parentOfParentKey] + gammaSumRight));
				}
			}
		}
		
		return sum;
	}
	
	@Override
	public double getProbabilityAtPosition(Sentence sentence, int position, int key, boolean joint) {
		Token token = sentence.getTokens()[position];
		int parentKey = token.getRole().getParent().getKey();
		int parentOfParentKey;
		if (token.getRole().getParent().getPosition() < 0) {
			parentOfParentKey = parentKey;
		} else {
			parentOfParentKey = token.getRole().getParent().getParent().getKey();
		}
		
		double sum = 0;
		if (token.getRole().isOnLeft()) {
			if (joint) {
				int role = token.getRole().getKey();
				return ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleByRoleCountsLeft3[role][parentKey][parentOfParentKey] + gammaLeft[role]) / (roleByRoleCountsLeft3[parentKey][parentOfParentKey] + gammaSumLeft));
			} else {
				for (int role=0; role<roles; role++) {
					sum += ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleByRoleCountsLeft3[role][parentKey][parentOfParentKey] + gammaLeft[role]) / (roleByRoleCountsLeft3[parentKey][parentOfParentKey] + gammaSumLeft));
				}
			}
		} else {
			if (joint) {
				int role = token.getRole().getKey();
				return ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleByRoleCountsRight3[role][parentKey][parentOfParentKey] + gammaRight[role]) / (roleByRoleCountsRight3[parentKey][parentOfParentKey] + gammaSumRight));
			} else {
				for (int role=0; role<roles; role++) {
					sum += ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum)) * ((roleByRoleByRoleCountsRight3[role][parentKey][parentOfParentKey] + gammaRight[role]) / (roleByRoleCountsRight3[parentKey][parentOfParentKey] + gammaSumRight));
				}
			}
		}
		
		return sum;
	}

	/*
	@Override
	public double getLogLikelihood(Sentence sentence) {
		double log = 0d;

		for (Token token : sentence.getTokens()) {
			int key = token.getKey();
			
			int role = token.getRole().getKey();
			int parentKey = token.getRole().getParent().getKey();
			
			int parentOfParentKey;
			if (token.getRole().getParent().getPosition() < 0) {
				parentOfParentKey = parentKey;
			} else {
				parentOfParentKey = token.getRole().getParent().getParent().getKey();
			}
			
			double sum = ((wordByRoleCounts[key][role] + beta) / (wordRoleCounts[role] + betaSum));
			
			double probTrigram;
			double probBigram;
			if (token.getRole().isOnLeft()) {
				probBigram = ((roleByRoleCountsLeft2[role][parentKey] + alphaLeft[role]) / (roleCountsLeft2[parentKey] + alphaSumLeft));
				probTrigram = ((roleByRoleByRoleCountsLeft3[role][parentKey][parentOfParentKey] + gammaLeft[role]) / (roleByRoleCountsLeft3[parentKey][parentOfParentKey] + gammaSumLeft));
			} else {
				probBigram = ((roleByRoleCountsRight2[role][parentKey] + alphaRight[role]) / (roleCountsRight2[parentKey] + alphaSumRight));
				probTrigram = ((roleByRoleByRoleCountsRight3[role][parentKey][parentOfParentKey] + gammaRight[role]) / (roleByRoleCountsRight3[parentKey][parentOfParentKey] + gammaSumRight));
			}
			
			sum *=  ((weight.get() * probTrigram) + ((1.0d-weight.get()) * probBigram));
			log += Math.log(sum);
			//log += FastMath.log(sum);
		}
		
		return log;
	}*/
	
	public boolean test() {
		boolean test1 = true;
		for (int role=0; role<roles; role++) {
			double sum = 0;
			for (int word=1; word<words; word++) {
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
		
		boolean test4 = true;
		for (int parentOfParent=0; parentOfParent<roles; parentOfParent++) {
			for (int parent=0; parent<roles; parent++) {
				double sum = 0;
				for (int role=0; role<roles; role++) {
					sum += (roleByRoleByRoleCountsLeft3[role][parent][parentOfParent] + gammaLeft[role]) / (roleByRoleCountsLeft3[parent][parentOfParent] + gammaSumLeft);
				}
				if (sum < 0.99d || sum > 1.01d) {
					logger.info(sum);
					test4 = false;
					break;
				}
			}
		}
		if (test4) {
			logger.info("ROLE BY ROLE BY ROLE LEFT TEST je OK");
		} else logger.info("ROLE BY ROLE BY ROLE LEFT TEST je spatne");
		
		boolean test5 = true;
		for (int parentOfParent=0; parentOfParent<roles; parentOfParent++) {
			for (int parent=0; parent<roles; parent++) {
				double sum = 0;
				for (int role=0; role<roles; role++) {
					sum += (roleByRoleByRoleCountsRight3[role][parent][parentOfParent] + gammaRight[role]) / (roleByRoleCountsRight3[parent][parentOfParent] + gammaSumRight);
				}
				if (sum < 0.99d || sum > 1.01d) {
					logger.info(sum);
					test5 = false;
					break;
				}
			}
		}
		if (test5) {
			logger.info("ROLE BY ROLE BY ROLE RIGHT TEST je OK");
		} else logger.info("ROLE BY ROLE BY ROLE RIGHT TEST je spatne");

		
		if (test1 && test4 && test5)	{
			return true;
		} else {
			logger.error("PRAVDEPODOBNOSTNI ROZDELENI JSOU SPATNE");
			System.exit(1);
		}
		return false;
	}
	
	public void optimizeHyperParameters() {
		//alphaLeft = this.optRoleByRole.optimize(alphaLeft, this.roleByRoleCountsLeft2, this.roleCountsLeft2);
		//alphaSumLeft = 0;
		//for (double a : alphaLeft) alphaSumLeft += a;
		
		//alphaRight = this.optRoleByRole.optimize(alphaRight, this.roleByRoleCountsRight2, this.roleCountsRight2);
		//alphaSumRight = 0;
		//for (double a : alphaRight) alphaSumRight += a;
		
		betaSum = this.optWordByRole.optimize(betaSum, this.wordByRoleCounts, this.wordRoleCounts);
		beta = betaSum / words;
		
		int[][] roleByRoleByRoleCounts2 = new int[roles][roles*roles];
		int[] roleByRoleCounts2 = new int[roles*roles];
		for (int r3=0; r3<roles; r3++) {
			for (int r2=0; r2<roles; r2++) {
				for (int r1=0; r1<roles; r1++) {
					roleByRoleByRoleCounts2[r1][(r2*roles)+r3] = roleByRoleByRoleCountsLeft3[r1][r2][r3];
					roleByRoleCounts2[(r2*roles)+r3] += roleByRoleByRoleCountsLeft3[r1][r2][r3];
				}
			}
		}
		
		gammaSumLeft = 0;
		gammaLeft = this.optRoleByRoleByRole.optimize(gammaLeft, roleByRoleByRoleCounts2, roleByRoleCounts2);
		for (double g : gammaLeft) gammaSumLeft += g;
		
		roleByRoleByRoleCounts2 = new int[roles][roles*roles];
		roleByRoleCounts2 = new int[roles*roles];
		for (int r3=0; r3<roles; r3++) {
			for (int r2=0; r2<roles; r2++) {
				for (int r1=0; r1<roles; r1++) {
					roleByRoleByRoleCounts2[r1][(r2*roles)+r3] = roleByRoleByRoleCountsRight3[r1][r2][r3];
					roleByRoleCounts2[(r2*roles)+r3] += roleByRoleByRoleCountsRight3[r1][r2][r3];
				}
			}
		}
		
		gammaSumRight = 0;
		gammaRight = this.optRoleByRoleByRole.optimize(gammaRight, roleByRoleByRoleCounts2, roleByRoleCounts2);
		for (double g : gammaRight) gammaSumRight += g;
	}
	/*
	public void optimizeWeight(List<Sentence> sentences) {
		System.out.println("OPTIMALIZACE VAH");
		final float epsilon = 0.001f;
		
		double lastWeight = 0.0d;
		double newWeight = weight.get();
		
		
		while (true) {
			double sumTrigram = 0;
			double sumBigram = 0;
			
			for (Sentence sentence : sentences) {
				for (Token token : sentence.getTokens()) {
					int role = token.getRole().getKey();
					int parent = token.getRole().getParent().getKey();
					int parentOfParent;
					if (token.getRole().getParent().getPosition() < 0) {
						parentOfParent = parent;
					} else {
						parentOfParent = token.getRole().getParent().getParent().getKey();
					}
					
					double probTrigram;
					double probBigram;
					if (token.getRole().isOnLeft()) {
						probBigram = ((roleByRoleCountsLeft2[role][parent] + alphaLeft[role]) / (roleCountsLeft2[parent] + alphaSumLeft));
						probTrigram = ((roleByRoleByRoleCountsLeft3[role][parent][parentOfParent] + gammaLeft[role]) / (roleByRoleCountsLeft3[parent][parentOfParent] + gammaSumLeft));
					} else {
						probBigram = ((roleByRoleCountsRight2[role][parent] + alphaRight[role]) / (roleCountsRight2[parent] + alphaSumRight));
						probTrigram = ((roleByRoleByRoleCountsRight3[role][parent][parentOfParent] + gammaRight[role]) / (roleByRoleCountsRight3[parent][parentOfParent] + gammaSumRight));
					}
					
					double prob = ((newWeight * probTrigram) + ((1.0d-newWeight) * probBigram));
					sumTrigram += (newWeight * probTrigram) / prob;
					sumBigram += ((1.0d-newWeight) * probBigram) / prob;
				}
			}
			
			lastWeight = newWeight;
			newWeight = sumTrigram / (sumBigram + sumTrigram);
			
			System.out.println(newWeight);
			if (Math.abs(newWeight - lastWeight) < epsilon) break;
		}
		
		this.weight.set(newWeight);
	}*/
	
	public String toString() {
		String s = "MODEL SETTING\n";

		//s += "weight trigram="+weight.get()+" weight bigram="+(1.0d-weight.get())+"\n";
		//s += "alphaSumLeft="+alphaSumLeft+" alpha="+Arrays.toString(alphaLeft)+"\n";
		//s += "alphaSumRight="+alphaSumRight+" alpha="+Arrays.toString(alphaRight)+"\n";
		s += "gammaSumLeft="+gammaSumLeft+" gamma="+Arrays.toString(gammaLeft)+"\n";
		s += "gammaSumRight="+gammaSumRight+" gamma="+Arrays.toString(gammaRight)+"\n";
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
/*
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
*/

	public double getBeta() {
		return beta;
	}


	public double getBetaSum() {
		return betaSum;
	}

/*
	public MutableDouble getWeight() {
		return weight;
	}


	public void setWeight(MutableDouble weight) {
		this.weight = weight;
	}
*/
/*
	public int[][] getRoleByRoleCountsLeft2() {
		return roleByRoleCountsLeft2;
	}


	public int[] getRoleCountsLeft2() {
		return roleCountsLeft2;
	}


	public int[][] getRoleByRoleCountsRight2() {
		return roleByRoleCountsRight2;
	}


	public int[] getRoleCountsRight2() {
		return roleCountsRight2;
	}*/


	public int[][][] getRoleByRoleByRoleCountsLeft3() {
		return roleByRoleByRoleCountsLeft3;
	}


	public int[][] getRoleByRoleCountsLeft3() {
		return roleByRoleCountsLeft3;
	}


	public int[][][] getRoleByRoleByRoleCountsRight3() {
		return roleByRoleByRoleCountsRight3;
	}


	public int[][] getRoleByRoleCountsRight3() {
		return roleByRoleCountsRight3;
	}


	public double[] getGammaLeft() {
		return gammaLeft;
	}


	public double getGammaSumLeft() {
		return gammaSumLeft;
	}


	public double[] getGammaRight() {
		return gammaRight;
	}


	public double getGammaSumRight() {
		return gammaSumRight;
	}

	@Override
	public Model createCopy() {
		LTLM2sides3gram copy = new LTLM2sides3gram(roles, vocabulary);
		
		//relative copies
		//copy.weight = weight;
		//copy.alphaLeft = alphaLeft;
		//copy.alphaRight = alphaRight;
		//copy.alphaSumLeft = alphaSumLeft;
		//copy.alphaSumRight = alphaSumRight;
		copy.beta = beta;
		copy.betaSum = betaSum;
		copy.gammaLeft = gammaLeft;
		copy.gammaRight = gammaRight;
		copy.gammaSumLeft = gammaSumLeft;
		copy.gammaSumRight = gammaSumRight;
		copy.optRoleByRole = optRoleByRole;
		copy.optRoleByRoleByRole = optRoleByRoleByRole;
		copy.optWordByRole = optWordByRole;
				
		//absolute copies
		copy.wordByRoleCounts = Cloner.copyArray(wordByRoleCounts);
		copy.wordRoleCounts = Cloner.copyArray(wordRoleCounts);
		copy.roleByRoleByRoleCountsLeft3 = Cloner.copyArray(roleByRoleByRoleCountsLeft3);
		copy.roleByRoleByRoleCountsRight3 = Cloner.copyArray(roleByRoleByRoleCountsRight3);
		//copy.roleByRoleCountsLeft2 = Cloner.copyArray(roleByRoleCountsLeft2);
		//copy.roleByRoleCountsRight2 = Cloner.copyArray(roleByRoleCountsRight2);
		copy.roleByRoleCountsLeft3 = Cloner.copyArray(roleByRoleCountsLeft3);
		copy.roleByRoleCountsRight3 = Cloner.copyArray(roleByRoleCountsRight3);
		//copy.roleCountsLeft2 = Cloner.copyArray(roleCountsLeft2);
		//copy.roleCountsRight2 = Cloner.copyArray(roleCountsRight2);
		
		return copy;
	}

	
}
