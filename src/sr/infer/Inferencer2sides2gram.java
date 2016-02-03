package sr.infer;

import org.apache.log4j.Logger;

import sr.Sentence;
import sr.lm.LTLM2sides2gram;

public abstract class Inferencer2sides2gram extends Inferencer {

	private static final long serialVersionUID = 1269389691552568408L;
	static transient Logger logger = Logger.getLogger(Inferencer2sides2gram.class);
	
	protected LTLM2sides2gram ltlm;
	
	protected int roles;
	protected int words;
	
	protected int[][] wordByRoleCounts;
	protected int[] wordRoleCounts;
	
	protected int[][] roleByRoleCountsLeft;
	protected int[] roleCountsLeft;
	protected int[][] roleByRoleCountsRight;
	protected int[] roleCountsRight;
	
	protected double[] alphaLeft;
	protected double alphaSumLeft;
	protected double[] alphaRight;
	protected double alphaSumRight;

	protected double beta;
	protected double betaSum;

	public Inferencer2sides2gram(LTLM2sides2gram ltlm) {
		this.ltlm = ltlm;
		initialize();
	}
	
	@Override
	public void initialize() {
		this.roles = ltlm.getRoles();
		this.words = ltlm.getWords();
		this.wordByRoleCounts = ltlm.getWordByRoleCounts();
		this.wordRoleCounts = ltlm.getWordRoleCounts();
		this.roleByRoleCountsLeft = ltlm.getRoleByRoleCountsLeft();
		this.roleCountsLeft = ltlm.getRoleCountsLeft();
		this.roleByRoleCountsRight = ltlm.getRoleByRoleCountsRight();
		this.roleCountsRight = ltlm.getRoleCountsRight();
		this.alphaLeft = ltlm.getAlphaLeft();
		this.alphaSumLeft = ltlm.getAlphaSumLeft();
		this.alphaRight = ltlm.getAlphaRight();
		this.alphaSumRight = ltlm.getAlphaSumRight();
		this.beta = ltlm.getBeta();
		this.betaSum = ltlm.getBetaSum();
	}
	
	public abstract void infer(Sentence sentence, boolean best, boolean trainMode);
	
}
