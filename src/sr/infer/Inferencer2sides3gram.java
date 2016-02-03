package sr.infer;

import org.apache.log4j.Logger;

import sr.Sentence;
import sr.lm.LTLM2sides3gram;
import sr.utils.MutableDouble;

public abstract class Inferencer2sides3gram extends Inferencer{

	private static final long serialVersionUID = 1269389691552568408L;
	static transient Logger logger = Logger.getLogger(Inferencer2sides3gram.class);
	
	protected LTLM2sides3gram ltlm;
	
	protected int roles;
	protected int words;
	
	protected int[][] wordByRoleCounts;
	protected int[] wordRoleCounts;
	
	//bigramy vlevo - node x parent
	//protected int[][] roleByRoleCountsLeft2;
	//protected int[] roleCountsLeft2;
	//bigramy vpravo - node x parent
	//protected int[][] roleByRoleCountsRight2;
	//protected int[] roleCountsRight2;
	//trigramy vlevo - node x parent x parent-of-parent
	protected int[][][] roleByRoleByRoleCountsLeft3;
	protected int[][] roleByRoleCountsLeft3;
	//trigramy right - node x parent x parent-of-parent
	protected int[][][] roleByRoleByRoleCountsRight3;
	protected int[][] roleByRoleCountsRight3;
	
	//protected double[] alphaLeft;
	//protected double alphaSumLeft;
	//protected double[] alphaRight;
	//protected double alphaSumRight;

	protected double beta;
	protected double betaSum;
	
	protected double[] gammaLeft;
	protected double gammaSumLeft;
	protected double[] gammaRight;
	protected double gammaSumRight;
	
	protected MutableDouble weight;

	public Inferencer2sides3gram(LTLM2sides3gram ltlm) {
		this.ltlm = ltlm;
		this.sampledChange = new Change();
		initialize();
	}
	
	@Override
	public void initialize() {
		this.roles = ltlm.getRoles();
		this.words = ltlm.getWords();
		this.wordByRoleCounts = ltlm.getWordByRoleCounts();
		this.wordRoleCounts = ltlm.getWordRoleCounts();
		//this.roleByRoleCountsLeft2 = ltlm.getRoleByRoleCountsLeft2();
		//this.roleCountsLeft2 = ltlm.getRoleCountsLeft2();
		//this.roleByRoleCountsRight2 = ltlm.getRoleByRoleCountsRight2();
		//this.roleCountsRight2 = ltlm.getRoleCountsRight2();
		this.roleByRoleByRoleCountsLeft3 = ltlm.getRoleByRoleByRoleCountsLeft3();
		this.roleByRoleCountsLeft3 = ltlm.getRoleByRoleCountsLeft3();
		this.roleByRoleByRoleCountsRight3 = ltlm.getRoleByRoleByRoleCountsRight3();
		this.roleByRoleCountsRight3 = ltlm.getRoleByRoleCountsRight3();
		//this.alphaLeft = ltlm.getAlphaLeft();
		//this.alphaSumLeft = ltlm.getAlphaSumLeft();
		//this.alphaRight = ltlm.getAlphaRight();
		//this.alphaSumRight = ltlm.getAlphaSumRight();
		this.beta = ltlm.getBeta();
		this.betaSum = ltlm.getBetaSum();
		this.gammaLeft = ltlm.getGammaLeft();
		this.gammaSumLeft = ltlm.getGammaSumLeft();
		this.gammaRight = ltlm.getGammaRight();
		this.gammaSumRight = ltlm.getGammaSumRight();
		//this.weight = ltlm.getWeight();
	}
	
	public abstract void infer(Sentence sentence, boolean best, boolean trainMode);
	
}
