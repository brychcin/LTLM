package sr.infer;

import java.util.Random;
import java.util.Set;

import sr.Role;
import sr.Sentence;
import sr.Token;
import sr.lm.LTLM1side2gram;


public class PerWordInferencer1side2gram extends Inferencer1side2gram {

	private Random random;
	//do x role
	private double[][] tempScores;
	private double[] tempChildScores;
	
	public PerWordInferencer1side2gram(LTLM1side2gram ltlm, int maxSentenceLength) {
		super(ltlm);
		this.random = new Random(System.currentTimeMillis());
		this.tempScores = new double[maxSentenceLength+1][roles];
		this.tempChildScores = new double[roles];
	}
	
	public void infer(Sentence sentence, boolean best, boolean trainMode) {
		PossibleChanges changes = new PossibleChanges(sentence);
		
		for (Token token : sentence.getTokens()) {	
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
				
				roleByRoleCounts[role.getKey()][role.getParent().getKey()]--;
				roleCounts[role.getParent().getKey()]--;
				
				for (Role child : role.getChildrens()) {
					roleByRoleCounts[child.getKey()][role.getKey()]--;
					roleCounts[role.getKey()]--;
				}
			}
			
			//Timer.start("scores");
			for (int roleKey=1; roleKey<roles; roleKey++) {
				tempChildScores[roleKey] = (wordByRoleCounts[key][roleKey] + beta) / (wordRoleCounts[roleKey] + betaSum);
			}
			for (Role child : role.getChildrens()) {
				int childKey = child.getKey();

				for (int roleKey=1; roleKey<roles; roleKey++) {
					double prob = ((roleByRoleCounts[childKey][roleKey] + alpha[childKey]) / (roleCounts[roleKey] + alphaSum));
					tempChildScores[roleKey] *= prob;
				}
			}

			double scoreSum = 0;
			//int step = 0;
			for (Role change : nodeChanges) {
				int changeKey = change.getKey();

				for (int roleKey=1; roleKey<roles; roleKey++) {
					double prob = ((roleByRoleCounts[roleKey][changeKey] + alpha[roleKey]) / (roleCounts[changeKey] + alphaSum));
					tempScores[change.getPosition()+1][roleKey] = tempChildScores[roleKey] * prob;
					scoreSum += tempScores[change.getPosition()+1][roleKey];
				}
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
				
				roleByRoleCounts[role.getKey()][role.getParent().getKey()]++;
				roleCounts[role.getParent().getKey()]++;

				for (Role child : role.getChildrens()) {
					roleByRoleCounts[child.getKey()][role.getKey()]++;
					roleCounts[role.getKey()]++;
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
