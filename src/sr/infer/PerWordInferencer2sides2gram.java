package sr.infer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import sr.Role;
import sr.Sentence;
import sr.Token;
import sr.lm.LTLM2sides2gram;


public class PerWordInferencer2sides2gram extends Inferencer2sides2gram {

	private Random random;
	//do x role
	private double[][] tempScores;
	private double[] tempChildScores;
	
	public PerWordInferencer2sides2gram(LTLM2sides2gram ltlm, int maxSentenceLength) {
		super(ltlm);
		this.random = new Random(System.currentTimeMillis());
		this.tempScores = new double[maxSentenceLength+1][roles];
		this.tempChildScores = new double[roles];
	}
	
	public void infer(Sentence sentence, boolean best, boolean trainMode) {
		PossibleChanges changes = new PossibleChanges(sentence);
		
		//zamichani tokenu pro odstraneni autokorelace
		List<Token> tokens = new ArrayList<Token>();
		for (Token token : sentence.getTokens()) tokens.add(token);
		Collections.shuffle(tokens);
		
		for (Token token : tokens) {	
			Role role = token.getRole();
			int key = role.getToken().getKey();
			//int position = role.getPosition();
			
			//Timer.start("find changes");
			Set<Role> nodeChanges = changes.findPossibleChanges(role);
			//Timer.stop("find changes");
			
			if (nodeChanges.size() == 0) continue;
			
			if (trainMode) {
				//odecist pred samplingem
				wordByRoleCounts[key][role.getKey()]--;
				wordRoleCounts[role.getKey()]--;
				
				if (role.isOnLeft()) {
					roleByRoleCountsLeft[role.getKey()][role.getParent().getKey()]--;
					roleCountsLeft[role.getParent().getKey()]--;
				} else {
					roleByRoleCountsRight[role.getKey()][role.getParent().getKey()]--;
					roleCountsRight[role.getParent().getKey()]--;
				}
				
				for (Role child : role.getChildrens()) {
					if (child.isOnLeft()) {
						roleByRoleCountsLeft[child.getKey()][role.getKey()]--;
						roleCountsLeft[role.getKey()]--;
					} else {
						roleByRoleCountsRight[child.getKey()][role.getKey()]--;
						roleCountsRight[role.getKey()]--;
					}
				}
				
			}
			
		//	Timer.start("scores");
			for (int roleKey=1; roleKey<roles; roleKey++) {
				tempChildScores[roleKey] = (wordByRoleCounts[key][roleKey] + beta) / (wordRoleCounts[roleKey] + betaSum);
			}
			for (Role child : role.getChildrens()) {
				int childKey = child.getKey();
				//int distance = child.getPosition() - position;

				if (child.isOnLeft()) {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double prob = ((roleByRoleCountsLeft[childKey][roleKey] + alphaLeft[childKey]) / (roleCountsLeft[roleKey] + alphaSumLeft));
								//(useDistance ? distanceEstimator.getProbability(distance, childKey, roleKey) : 1.0d);
						tempChildScores[roleKey] *= prob;
					}
				} else {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double prob = ((roleByRoleCountsRight[childKey][roleKey] + alphaRight[childKey]) / (roleCountsRight[roleKey] + alphaSumRight));
						tempChildScores[roleKey] *= prob;
					}
				}

			}

			double scoreSum = 0;
			//int step = 0;
			for (Role change : nodeChanges) {
				int changeKey = change.getKey();
				//int distance = position - change.getPosition();
				//double wordProb = (wordByWordCounts.get(bigramToLong(change.getToken().getKey(), key)) + gamma) / (wordCounts[change.getToken().getKey()] + gammaSum);
				
				//double sum = 0.0d;
				if (role.getPosition() < change.getPosition()) {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double prob = ((roleByRoleCountsLeft[roleKey][changeKey] + alphaLeft[roleKey]) / (roleCountsLeft[changeKey] + alphaSumLeft));
								//(useDistance ? distanceEstimator.getProbability(distance, roleKey, changeKey) : 1.0d);
						tempScores[change.getPosition()+1][roleKey] = tempChildScores[roleKey] * prob;
						//tempScores[((roles-1)*step)+roleKey-1] = tempChildScores[roleKey] * prob;
						//sum += prob;
						scoreSum += tempScores[change.getPosition()+1][roleKey];
					}
				} else {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double prob = ((roleByRoleCountsRight[roleKey][changeKey] + alphaRight[roleKey]) / (roleCountsRight[changeKey] + alphaSumRight));
								//(useDistance ? distanceEstimator.getProbability(distance, roleKey, changeKey) : 1.0d);
						tempScores[change.getPosition()+1][roleKey] = tempChildScores[roleKey] * prob;
						//tempScores[((roles-1)*step)+roleKey-1] = tempChildScores[roleKey] * prob;
						//sum += prob;
						scoreSum += tempScores[change.getPosition()+1][roleKey];
					}
				}
				
				/*
				for (int roleKey=1; roleKey<roles; roleKey++) {
					tempScores[((roles-1)*step)+roleKey-1] /= sum;
					scoreSum += tempScores[((roles-1)*step)+roleKey-1];
				}
				 */
				//step++;
			}
			//Timer.stop("scores");
			
			
			//Timer.start("sample");
			
			if (best) {
				sampledChange = bestChange(token, nodeChanges);
			} else {
				sampledChange = sampleChange(token, scoreSum, nodeChanges);
			}
			//Timer.stop("sample");
			
			
			//Timer.start("do change");
			//fyzicky to prendat ve strome
			//if (sampledChange.getNodeToChange() == null) {
			//	System.out.println(Arrays.toString(Arrays.copyOfRange(tempScores, 1, 100)));
			//}
			
			Role oldParent = sampledChange.getNodeToChange().getParent();
			Role newParent = sampledChange.getTargetNode();
			
			sampledChange.getNodeToChange().getParent().getChildrens().remove(sampledChange.getNodeToChange());
			sampledChange.getTargetNode().setChild(sampledChange.getNodeToChange());
			sampledChange.getNodeToChange().setParent(sampledChange.getTargetNode());
			sampledChange.getNodeToChange().setKey(sampledChange.getNewKey());
			
			Sentence.calculateMinMaxPositions(oldParent);
			Sentence.calculateMinMaxPositions(newParent);
			role = token.getRole();
			//Timer.stop("do change");
			
			if (trainMode) {
				//pricist po samplingu
				wordByRoleCounts[key][role.getKey()]++;
				wordRoleCounts[role.getKey()]++;
				
				if (role.isOnLeft()) {
					roleByRoleCountsLeft[role.getKey()][role.getParent().getKey()]++;
					roleCountsLeft[role.getParent().getKey()]++;
				} else {
					roleByRoleCountsRight[role.getKey()][role.getParent().getKey()]++;
					roleCountsRight[role.getParent().getKey()]++;
				}
				
				for (Role child : role.getChildrens()) {
					if (child.isOnLeft()) {
						roleByRoleCountsLeft[child.getKey()][role.getKey()]++;
						roleCountsLeft[role.getKey()]++;
					} else {
						roleByRoleCountsRight[child.getKey()][role.getKey()]++;
						roleCountsRight[role.getKey()]++;
					}
				}
				
			}
		}
	}
	
	
	private Change bestChange(Token token, Set<Role> nodeChanges) {
		this.sampledChange.setNewKey((short) 0);
		this.sampledChange.setNodeToChange(null);
		this.sampledChange.setTargetNode(null);
		this.sampledChange.setScore(Double.NEGATIVE_INFINITY);
		
		Role role = token.getRole();

		for (Role change : nodeChanges) {
			for (short key=1; key<roles; key++) {
				if (tempScores[change.getPosition()+1][key] > sampledChange.getScore()) {
					sampledChange.setNodeToChange(role);
					sampledChange.setTargetNode(change);
					sampledChange.setNewKey(key);
					sampledChange.setScore(tempScores[change.getPosition()+1][key]);
				}
			}
		}
		
		return sampledChange;
	}
	
	private Change sampleChange(Token token, double scoreSum, Set<Role> nodeChanges) {
		this.sampledChange.setNewKey((short) 0);
		this.sampledChange.setNodeToChange(null);
		this.sampledChange.setTargetNode(null);
		this.sampledChange.setScore(Double.NEGATIVE_INFINITY); 
		
		double sample = random.nextDouble() * scoreSum;

		Role role = token.getRole();
		sampledChange.setNodeToChange(role);

		for (Role change : nodeChanges) {
			sampledChange.setTargetNode(change);
				
			for (short key=1; key<roles; key++) {
				sampledChange.setNewKey(key);
				sample -= tempScores[change.getPosition()+1][key];
				if (sample < 0) {
					return sampledChange;
				}
			}
		}
		
		return sampledChange;
	}

	
	
	
}
