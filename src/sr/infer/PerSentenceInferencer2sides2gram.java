package sr.infer;

import java.util.Random;
import java.util.Set;

import sr.Role;
import sr.Sentence;
import sr.Token;
import sr.lm.LTLM2sides2gram;

/**
 * sampluje jednu zmenu ve vete
 * @author Brych
 *
 */
public class PerSentenceInferencer2sides2gram extends Inferencer2sides2gram {

	private Random random;
	//from x to x role
	//lze prendavat do rootu - proto je tam ta velikost vety +1
	private double[][][] tempScores;
	private double[] tempChildScores;
	
	public PerSentenceInferencer2sides2gram(LTLM2sides2gram ltlm, int maxSentenceLength) {
		super(ltlm);
		this.random = new Random(System.currentTimeMillis());
		this.tempScores = new double[maxSentenceLength][maxSentenceLength+1][roles];
		this.tempChildScores = new double[roles];
	}
	
	public void infer(Sentence sentence, boolean best, boolean trainMode) {
		//Timer.start("find changes");
		PossibleChanges changes = new PossibleChanges(sentence);
		Set<Role>[] nodeChanges = new Set[sentence.size()];
		for (int pos=0; pos<sentence.size(); pos++) {
			nodeChanges[pos] = changes.findPossibleChanges(sentence.getTokens()[pos].getRole());
		}
		//Timer.stop("find changes");
		
		
		//Timer.start("scores");
		double scoreSum = 0;
		
		for (Token token : sentence.getTokens()) {	
			Role role = token.getRole();
			int key = role.getToken().getKey();
			int position = role.getPosition();
			
			if (nodeChanges[position].size() == 0) continue;
			
			//spocitat starou pravdepodobnost
			double oldProb = getProbabilityAtPosition(role);
			
			if (trainMode) {
				decrease(role, key);
			}
			
			for (int roleKey=1; roleKey<roles; roleKey++) {
				tempChildScores[roleKey] = 1.0d / oldProb;
				tempChildScores[roleKey] *= (wordByRoleCounts[key][roleKey] + beta) / (wordRoleCounts[roleKey] + betaSum);
			}
			for (Role child : role.getChildrens()) {
				int childKey = child.getKey();

				if (child.isOnLeft()) {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double prob = ((roleByRoleCountsLeft[childKey][roleKey] + alphaLeft[childKey]) / (roleCountsLeft[roleKey] + alphaSumLeft));
						tempChildScores[roleKey] *= prob;
					}
				} else {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double prob = ((roleByRoleCountsRight[childKey][roleKey] + alphaRight[childKey]) / (roleCountsRight[roleKey] + alphaSumRight));
						tempChildScores[roleKey] *= prob;
					}
				}
			}

			for (Role change : nodeChanges[position]) {
				int changeKey = change.getKey();

				if (role.getPosition() < change.getPosition()) {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double prob = ((roleByRoleCountsLeft[roleKey][changeKey] + alphaLeft[roleKey]) / (roleCountsLeft[changeKey] + alphaSumLeft));
						tempScores[position][change.getPosition()+1][roleKey] = tempChildScores[roleKey] * prob;
						scoreSum += tempScores[position][change.getPosition()+1][roleKey];
					}
				} else {
					for (int roleKey=1; roleKey<roles; roleKey++) {
						double prob = ((roleByRoleCountsRight[roleKey][changeKey] + alphaRight[roleKey]) / (roleCountsRight[changeKey] + alphaSumRight));
						tempScores[position][change.getPosition()+1][roleKey] = tempChildScores[roleKey] * prob;
						scoreSum += tempScores[position][change.getPosition()+1][roleKey];
					}
				}
			}
			
			if (trainMode) {
				increase(role, key);
			}
		}	
		//Timer.stop("scores");	
		
		
		//Timer.start("sample");
		if (best) {
			sampledChange = bestChange(sentence, nodeChanges);
		} else {
			sampledChange = sampleChange(sentence, scoreSum, nodeChanges);
		}
		//Timer.stop("sample");
		
		
		//Timer.start("do change");
		if (trainMode) {
			decrease(sampledChange.getNodeToChange(), sampledChange.getNodeToChange().getToken().getKey());
		}
		
		Role oldParent = sampledChange.getNodeToChange().getParent();
		Role newParent = sampledChange.getTargetNode();
		
		sampledChange.getNodeToChange().getParent().getChildrens().remove(sampledChange.getNodeToChange());
		sampledChange.getTargetNode().setChild(sampledChange.getNodeToChange());
		sampledChange.getNodeToChange().setParent(sampledChange.getTargetNode());
		sampledChange.getNodeToChange().setKey(sampledChange.getNewKey());
		
		Sentence.calculateMinMaxPositions(oldParent);
		Sentence.calculateMinMaxPositions(newParent);
		
		if (trainMode) {
			increase(sampledChange.getNodeToChange(), sampledChange.getNodeToChange().getToken().getKey());
		}
		//Timer.stop("do change");
	}
	
	private double getProbabilityAtPosition(Role role) {
		double oldProb = (wordByRoleCounts[role.getToken().getKey()][role.getKey()] + beta) / (wordRoleCounts[role.getKey()] + betaSum);
		if (role.isOnLeft()) {
			oldProb *= (roleByRoleCountsLeft[role.getKey()][role.getParent().getKey()] + alphaLeft[role.getKey()]) / (roleCountsLeft[role.getParent().getKey()] + alphaSumLeft);
		} else {
			oldProb *= (roleByRoleCountsRight[role.getKey()][role.getParent().getKey()] + alphaRight[role.getKey()]) / (roleCountsRight[role.getParent().getKey()] + alphaSumRight);
		}
		
		for (Role child : role.getChildrens()) {
			int childKey = child.getKey();

			if (child.isOnLeft()) {
				oldProb *= ((roleByRoleCountsLeft[childKey][role.getKey()] + alphaLeft[childKey]) / (roleCountsLeft[role.getKey()] + alphaSumLeft));
			} else {
				oldProb *= ((roleByRoleCountsRight[childKey][role.getKey()] + alphaRight[childKey]) / (roleCountsRight[role.getKey()] + alphaSumRight));
			}
		}
		
		return oldProb;
	}
	
	private void increase(Role role, int key) {
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
	
	private void decrease(Role role, int key) {
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
	
	private Change bestChange(Sentence sentence, Set<Role>[] nodeChanges) {
		this.sampledChange.setNewKey((short) 0);
		this.sampledChange.setNodeToChange(null);
		this.sampledChange.setTargetNode(null);
		this.sampledChange.setScore(Double.NEGATIVE_INFINITY);

		for (Token token : sentence.getTokens()) {	
			Role role = token.getRole();
			int position = role.getPosition();
			
			for (Role change : nodeChanges[position]) {
				for (short key=1; key<roles; key++) {
					if (tempScores[position][change.getPosition()+1][key] > sampledChange.getScore()) {
						sampledChange.setNodeToChange(role);
						sampledChange.setTargetNode(change);
						sampledChange.setNewKey(key);
						sampledChange.setScore(tempScores[position][change.getPosition()+1][key]);
					}
				}
			}
		}
		
		return sampledChange;
	}
	
	private Change sampleChange(Sentence sentence, double scoreSum, Set<Role>[] nodeChanges) {
		this.sampledChange.setNewKey((short) 0);
		this.sampledChange.setNodeToChange(null);
		this.sampledChange.setTargetNode(null);
		this.sampledChange.setScore(Double.NEGATIVE_INFINITY);
		
		double sample = random.nextDouble() * scoreSum;

		for (Token token : sentence.getTokens()) {	
			Role role = token.getRole();
			int position = role.getPosition();
			sampledChange.setNodeToChange(role);

			for (Role change : nodeChanges[position]) {
				sampledChange.setTargetNode(change);
					
				for (short key=1; key<roles; key++) {
					sampledChange.setNewKey(key);
					sample -= tempScores[position][change.getPosition()+1][key];
					if (sample < 0) {
						return sampledChange;
					}
				}
			}
		}
		
		return sampledChange;
	}
	
}
