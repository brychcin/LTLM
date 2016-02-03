package sr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class Sentence implements Serializable {

	private static final long serialVersionUID = 8418051007342191968L;
	static Logger logger = Logger.getLogger(Sentence.class);
	
	private int offset;
	private Token[] tokens;
	private final Token root;
	
	public Sentence(int[] keys) {
		this.tokens = new Token[keys.length];
		for (int i=0; i<keys.length; i++) {
			tokens[i] = new Token(keys[i]);
		}

		this.root = new Token(2);
		this.root.setRole(new Role((short) 0, this.root, (short) -1));
		this.root.getRole().setMinPosition((short) -1);
		this.root.getRole().setMaxPosition((short) (keys.length-1));
	}
	
	public int size() {
		return tokens.length;
	}
	
	public Token[] getTokens() {
		return tokens;
	}

	public void setTokens(Token[] tokens) {
		this.tokens = tokens;
	}

	public Token getRoot() {
		return root;
	}

	public String toString() {
		String s = "";
		for (Token token : tokens) {
			s += token.getRole().toString()+"\n";
		}
		
		return s;
	}
	
	public String getText(Vocabulary vocabulary) {
		String s = "";
		for (Token token : tokens) {
			s += vocabulary.getWordByKey(token.getKey())+" ";
		}
		
		return s;
	}
	
	public void treeControll() {
		List<Token> tokensWithRoot = new ArrayList<Token>();
		tokensWithRoot.add(root);
		for (Token token : tokens) tokensWithRoot.add(token);
		
		
		for (Token token : tokensWithRoot) {
			//Role role = token.getRole();

			Role lastChild = null;
			for (Role child : token.getRole().getChildrens()) {
				if (lastChild != null) {
					if (child.getMinPosition() <= lastChild.getMaxPosition()) {
						logger.error("pozice si nesouhlasi "+child.getPosition()+" "+child.getMinPosition()+">"+lastChild.getMaxPosition());
						System.exit(1);
					}
				}
				
				
				if (child.getChildrens().contains(token.getRole())) {
					logger.error("nasel jsem klicku");
					System.exit(1);
				}
				lastChild = child;
			}
		}
	}
	

	public static void calculateMinMaxPositions(Role role) {
		while (role.getParent() != null) {
			short oldMinPosition = role.getMinPosition();
			short oldMaxPosition = role.getMaxPosition();
			
			if (role.getChildrens().size() == 0) {
				role.setMinPosition(role.getPosition());
				role.setMaxPosition(role.getPosition());
			} else {
				role.setMinPosition(role.getChildrens().first().getMinPosition());
				if (role.getMinPosition() > role.getPosition()) role.setMinPosition( role.getPosition());
				role.setMaxPosition(role.getChildrens().last().getMaxPosition());
				if (role.getMaxPosition() < role.getPosition()) role.setMaxPosition( role.getPosition());
			}
			
			if (oldMinPosition == role.getMinPosition() && oldMaxPosition == role.getMaxPosition()) return;
			role = role.getParent();
		}
	}
	
	public final List<Token[]> extractNGramsFromSentence(int order, int startTokens) {
		List<Token[]> ngrams = new ArrayList<Token[]>();
		
		List<Token> allTokens = new ArrayList<Token>();
		for (int i=0; i<startTokens; i++) allTokens.add(new Token(2));
		for (int i=0; i<tokens.length; i++) allTokens.add(tokens[i]);
		
		
		if (allTokens.size() < order) return ngrams;
		if (allTokens.size() == order) {
			ngrams.add(allTokens.toArray(new Token[0]));
			return ngrams;
		}
			
		for (int i=0; i<=allTokens.size() - order; i++) {
			Token[] ngram = new Token[order];
			for (int j = 0; j < ngram.length; j++) {
				ngram[j] = allTokens.get(i + j);
			}
			ngrams.add(ngram);	
		}
		
		return ngrams;
	}
	
	public String toPennFormat() {
		return this.root.getRole().toPennFormat();
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}
	
	public int getNumberOfBytes() {
		return 2+(this.size()*8);
	}
	
}
