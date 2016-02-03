package sr.infer;

import org.apache.log4j.Logger;

import sr.Sentence;
import sr.lm.LTLM1side2gram;

public abstract class Inferencer1side2gram extends Inferencer {

	private static final long serialVersionUID = 1269389691552568408L;
	static transient Logger logger = Logger.getLogger(Inferencer1side2gram.class);
	
	private LTLM1side2gram ltlm;
	
	protected int roles;
	protected int words;
	
	protected int[][] wordByRoleCounts;
	protected int[] wordRoleCounts;
	
	protected int[][] roleByRoleCounts;
	protected int[] roleCounts;
	
	protected double[] alpha;
	protected double alphaSum;


	protected double beta;
	protected double betaSum;

	public Inferencer1side2gram(LTLM1side2gram ltlm) {
		this.ltlm = ltlm;
		initialize();
	}
	
	@Override
	public void initialize() {
		this.roles = ltlm.getRoles();
		this.words = ltlm.getWords();
		this.wordByRoleCounts = ltlm.getWordByRoleCounts();
		this.wordRoleCounts = ltlm.getWordRoleCounts();
		this.roleByRoleCounts = ltlm.getRoleByRoleCounts();
		this.roleCounts = ltlm.getRoleCounts();
		this.alpha = ltlm.getAlpha();
		this.alphaSum = ltlm.getAlphaSum();
		this.beta = ltlm.getBeta();
		this.betaSum = ltlm.getBetaSum();
	}
	
	public abstract void infer(Sentence sentence, boolean best, boolean trainMode);
	
}
