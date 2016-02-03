package sr.infer;

import java.util.Random;

import sr.Role;
import sr.Sentence;
import sr.Token;

public class RandomInferencer extends Inferencer {

	private Random random;
	private int roles;

	public RandomInferencer(int roles) {
		this.roles = roles;
		this.random = new Random(System.currentTimeMillis());
	}
	
	@Override
	public void infer(Sentence sentence, boolean best, boolean trainMode) {
		sentence.getRoot().getRole().getChildrens().clear();
		
		for (short pos=0; pos<sentence.getTokens().length; pos++) {
			Token token = sentence.getTokens()[pos];
			
			Role role = new Role((short) (random.nextInt(roles-1)+1), token, pos);
			token.setRole(role);
					
			sentence.getRoot().getRole().setChild(role);
			role.setParent(sentence.getRoot().getRole());
		}
		sentence.getRoot().getRole().calculateMinMaxPositions();
	}

	@Override
	public void initialize() {}
	
}


