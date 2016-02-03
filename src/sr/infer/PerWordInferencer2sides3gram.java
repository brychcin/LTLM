package sr.infer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import sr.Role;
import sr.Sentence;
import sr.Token;
import sr.lm.LTLM2sides3gram;


public class PerWordInferencer2sides3gram extends Inferencer2sides3gram {

	private Random random;
	//do x role
	private double[][] tempScores;
	private double[] tempChildScores;
	
	public PerWordInferencer2sides3gram(LTLM2sides3gram ltlm, int maxSentenceLength) {
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
		//Collections.shuffle(tokens);
		
		for (Token token : tokens) {	
			Role role = token.getRole();
			int key = role.getToken().getKey();
			
			//Timer.start("find changes");
			Set<Role> nodeChanges = changes.findPossibleChanges(role);
			//Timer.stop("find changes");
			
			if (nodeChanges.size() == 0) continue;
			
			if (trainMode) {
				//odecist pred samplingem
				changeCounts(role, false);
			}
			
			//Timer.start("scores");
			for (int roleKey=1; roleKey<roles; roleKey++) {
				tempChildScores[roleKey] = (wordByRoleCounts[key][roleKey] + beta) / (wordRoleCounts[roleKey] + betaSum);
			}
			for (Role child : role.getChildrens()) {
				int childKey = child.getKey();
				
				for (Role childOfChild : child.getChildrens()) {
					int childOfChildKey = childOfChild.getKey();
					
					if (childOfChild.isOnLeft()) {
						for (int roleKey=1; roleKey<roles; roleKey++) {
							double probTrigram = ((roleByRoleByRoleCountsLeft3[childOfChildKey][childKey][roleKey] + gammaLeft[childOfChildKey]) / (roleByRoleCountsLeft3[childKey][roleKey] + gammaSumLeft));
							//double probBigram = ((roleByRoleCountsLeft2[childOfChildKey][childKey] + alphaLeft[childOfChildKey]) / (roleCountsLeft2[childKey] + alphaSumLeft));
							//tempChildScores[roleKey] *= (weight.get() * probTrigram) + ((1.0d-weight.get()) * probBigram);
							tempChildScores[roleKey] *= probTrigram;
						}
					} else {
						for (int roleKey=1; roleKey<roles; roleKey++) {
							double probTrigram = ((roleByRoleByRoleCountsRight3[childOfChildKey][childKey][roleKey] + gammaRight[childOfChildKey]) / (roleByRoleCountsRight3[childKey][roleKey] + gammaSumRight));
							//double probBigram = ((roleByRoleCountsRight2[childOfChildKey][childKey] + alphaRight[childOfChildKey]) / (roleCountsRight2[childKey] + alphaSumRight));
							//tempChildScores[roleKey] *= (weight.get() * probTrigram) + ((1.0d-weight.get()) * probBigram);
							tempChildScores[roleKey] *= probTrigram;
						}
					}
				}
			}

			
			double scoreSum = 0;
			for (Role change : nodeChanges) {
				int changeKey = change.getKey();
				int parentOfChange;
				if (change.getPosition() < 0) {
					parentOfChange = changeKey;
				} else {
					parentOfChange = change.getParent().getKey();
				}
				
				for (int roleKey=0; roleKey<roles; roleKey++) {
					tempScores[change.getPosition()+1][roleKey] = tempChildScores[roleKey];
				}
				
				for (Role child : role.getChildrens()) {
					int childKey = child.getKey();
					
					if (child.isOnLeft()) {
						for (int roleKey=1; roleKey<roles; roleKey++) {
							double probTrigram = ((roleByRoleByRoleCountsLeft3[childKey][roleKey][changeKey] + gammaLeft[childKey]) / (roleByRoleCountsLeft3[roleKey][changeKey] + gammaSumLeft));
							//double probBigram = ((roleByRoleCountsLeft2[childKey][roleKey] + alphaLeft[childKey]) / (roleCountsLeft2[roleKey] + alphaSumLeft));
							//tempScores[change.getPosition()+1][roleKey] *= ((weight.get() * probTrigram) + ((1.0d-weight.get()) * probBigram));
							tempScores[change.getPosition()+1][roleKey] *= probTrigram;
						}
					} else {
						for (int roleKey=1; roleKey<roles; roleKey++) {
							double probTrigram = ((roleByRoleByRoleCountsRight3[childKey][roleKey][changeKey] + gammaRight[childKey]) / (roleByRoleCountsRight3[roleKey][changeKey] + gammaSumRight));
							//double probBigram = ((roleByRoleCountsRight2[childKey][roleKey] + alphaRight[childKey]) / (roleCountsRight2[roleKey] + alphaSumRight));
							//tempScores[change.getPosition()+1][roleKey] *= ((weight.get() * probTrigram) + ((1.0d-weight.get()) * probBigram));
							tempScores[change.getPosition()+1][roleKey] *= probTrigram;
						}
					}
				}

				if (role.getPosition() < change.getPosition()) {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double probTrigram = ((roleByRoleByRoleCountsLeft3[roleKey][changeKey][parentOfChange] + gammaLeft[roleKey]) / (roleByRoleCountsLeft3[changeKey][parentOfChange] + gammaSumLeft));
						//double probBigram = ((roleByRoleCountsLeft2[roleKey][changeKey] + alphaLeft[roleKey]) / (roleCountsLeft2[changeKey] + alphaSumLeft));
						//tempScores[change.getPosition()+1][roleKey] *= (weight.get() * probTrigram) + ((1.0d-weight.get()) * probBigram);
						tempScores[change.getPosition()+1][roleKey] *= probTrigram;
						scoreSum += tempScores[change.getPosition()+1][roleKey];
					}
				} else {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double probTrigram = ((roleByRoleByRoleCountsRight3[roleKey][changeKey][parentOfChange] + gammaRight[roleKey]) / (roleByRoleCountsRight3[changeKey][parentOfChange] + gammaSumRight));
						//double probBigram = ((roleByRoleCountsRight2[roleKey][changeKey] + alphaRight[roleKey]) / (roleCountsRight2[changeKey] + alphaSumRight));
						//tempScores[change.getPosition()+1][roleKey] *= (weight.get() * probTrigram) + ((1.0d-weight.get()) * probBigram);
						tempScores[change.getPosition()+1][roleKey] *= probTrigram;
						scoreSum += tempScores[change.getPosition()+1][roleKey];
					}
				}
			}
			//Timer.stop("scores");
			
			
			//Timer.start("sample");
			//Change sampledChange = null;
			if (best) {
				sampledChange = bestChange(token, nodeChanges);
			} else {
				sampledChange = sampleChange(token, scoreSum, nodeChanges);
			}
			//Timer.stop("sample");
			
			
			//Timer.start("do change");			
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
				changeCounts(role, true);
			}
		}
	}
	
	private void changeCounts(Role role, boolean increase) {
		int key = role.getToken().getKey();
		int parent = role.getParent().getKey();
		int parentOfParent;
		if (role.getParent().getPosition() < 0) {
			parentOfParent = parent;
		} else {
			parentOfParent = role.getParent().getParent().getKey();
		}
		
		int add = (increase ? +1 : -1);
		
		wordByRoleCounts[key][role.getKey()] += add;
		wordRoleCounts[role.getKey()] += add;

		if (role.isOnLeft()) {
			//roleByRoleCountsLeft2[role.getKey()][parent] += add;
			//roleCountsLeft2[parent] += add;
			roleByRoleByRoleCountsLeft3[role.getKey()][parent][parentOfParent] += add;
			roleByRoleCountsLeft3[parent][parentOfParent] += add;
		} else {
			//roleByRoleCountsRight2[role.getKey()][parent] += add;
			//roleCountsRight2[parent] += add;
			roleByRoleByRoleCountsRight3[role.getKey()][parent][parentOfParent] += add;
			roleByRoleCountsRight3[parent][parentOfParent] += add;
		}
		
		for (Role child : role.getChildrens()) {
			if (child.isOnLeft()) {
				//roleByRoleCountsLeft2[child.getKey()][role.getKey()] += add;
				//roleCountsLeft2[role.getKey()] += add;
				roleByRoleByRoleCountsLeft3[child.getKey()][role.getKey()][parent] += add;
				roleByRoleCountsLeft3[role.getKey()][parent] += add;
			} else {
				//roleByRoleCountsRight2[child.getKey()][role.getKey()] += add;
				//roleCountsRight2[role.getKey()] += add;
				roleByRoleByRoleCountsRight3[child.getKey()][role.getKey()][parent] += add;
				roleByRoleCountsRight3[role.getKey()][parent] += add;
			}
			
			for (Role childOfChild : child.getChildrens()) {
				if (childOfChild.isOnLeft()) {
					roleByRoleByRoleCountsLeft3[childOfChild.getKey()][child.getKey()][role.getKey()] += add;
					roleByRoleCountsLeft3[child.getKey()][role.getKey()] += add;
				} else {
					roleByRoleByRoleCountsRight3[childOfChild.getKey()][child.getKey()][role.getKey()] += add;
					roleByRoleCountsRight3[child.getKey()][role.getKey()] += add;
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
