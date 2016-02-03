package sr.infer;

import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;

import sr.Role;
import sr.Sentence;
import sr.Token;
import sr.data.BasicDataProvider;
import sr.data.DataProvider;
import sr.lm.LTLM2sides3gram;
import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;
import sr.utils.IOUtils;
import sr.utils.Timer;

public class ExactInferencer3gram extends Inferencer {

	private final int MAX_LENGTH;
	
	private final short roles;
	private final int histories;
	private LTLM2sides3gram ltlm;
	private double[][][] logRoleByRoleLeft;
	private double[][][] logRoleByRoleRight;
	
	public ExactInferencer3gram(LTLM2sides3gram ltlm, int MAX_LENGTH) {
		this.MAX_LENGTH = MAX_LENGTH;
		this.roles = (short) ltlm.getRoles();
		this.histories = ltlm.getRoles() * ltlm.getRoles();
		this.ltlm = ltlm;
		this.logRoleByRoleLeft = new double[roles][roles][roles];
		this.logRoleByRoleRight = new double[roles][roles][roles];
		
		for (int role=0; role<roles; role++) {			
			for (int parent=0; parent<roles; parent++) {
				for (int parentOfParent=0; parentOfParent<roles; parentOfParent++) {
					this.logRoleByRoleLeft[role][parent][parentOfParent] = Math.log((ltlm.getRoleByRoleByRoleCountsLeft3()[role][parent][parentOfParent] + ltlm.getGammaLeft()[role]) / (ltlm.getRoleByRoleCountsLeft3()[parent][parentOfParent] + ltlm.getGammaSumLeft()));
					this.logRoleByRoleRight[role][parent][parentOfParent] = Math.log((ltlm.getRoleByRoleByRoleCountsRight3()[role][parent][parentOfParent] + ltlm.getGammaRight()[role]) / (ltlm.getRoleByRoleCountsRight3()[parent][parentOfParent] + ltlm.getGammaSumRight()));		
				}
			}
		}
	}
	
	private int getIndex(short parent, short parentOfParent) {
		return (parent * roles) + parentOfParent;
	}
	
	
	@Override
	public void infer(Sentence sentence, boolean best, boolean trainMode) {
		if (sentence.size() > MAX_LENGTH) {
			throw new IllegalArgumentException("Sentence is too long. Max size is "+MAX_LENGTH);
		}
		
		Timer.start("exact-infer");
		
		//side subtrees - root na strane
		SubTreeSidePossibilities[][] sideSubTrees = new SubTreeSidePossibilities[sentence.size()][];

		Timer.start("create-objects");
		for (short i=0; i<sentence.size(); i++) {
			sideSubTrees[i] = new SubTreeSidePossibilities[sentence.size()-i];

			for (short j=0; j<sideSubTrees[i].length; j++) {
				sideSubTrees[i][j] = new SubTreeSidePossibilities((short)j, (short)(j+i), histories);
				
				short posLeft = (short) (j-1);
				short posRight = (short) (j+i+1);
				for (short parent=0; parent<roles; parent++) {
					for (short parentOfParent=0; parentOfParent<roles; parentOfParent++) {
						int history = getIndex(parent, parentOfParent);
						sideSubTrees[i][j].getLeft()[history] = new SubTree(parent, posLeft);
						sideSubTrees[i][j].getRight()[history] = new SubTree(parent, posRight);
					}
				}
			}
		}
		Timer.stop("create-objects");
		
		Timer.start("init-leafs");
		//init leafs
		double[][] leafs = new double[sentence.size()][];
		for (short i=0; i<sentence.size(); i++) {
			leafs[i] = new double[roles];
			int key = sentence.getTokens()[i].getKey();
			for (short role=0; role<roles; role++) {			
				double wordProb = Math.log((ltlm.getWordByRoleCounts()[key][role] + ltlm.getBeta()) / (ltlm.getWordRoleCounts()[role] + ltlm.getBetaSum()));	
				leafs[i][role] = wordProb;
				
				for (short parent=0; parent<roles; parent++) {
					for (short parentOfParent=0; parentOfParent<roles; parentOfParent++) {
						double probRight = wordProb + logRoleByRoleLeft[role][parent][parentOfParent];
						double probLeft = wordProb + logRoleByRoleRight[role][parent][parentOfParent];
						
						int history = getIndex(parent, parentOfParent);
						
						if (probLeft > sideSubTrees[0][i].getLeft()[history].getLogLikelihood()) {
							SubTree subtree = sideSubTrees[0][i].getLeft()[history];
							subtree.setLogLikelihood(probLeft);
							subtree.getChildrens().clear();
							subtree.setChild(new SubTree(role, i));
						}
								
						if (probRight > sideSubTrees[0][i].getRight()[history].getLogLikelihood()) {
							SubTree subtree = sideSubTrees[0][i].getRight()[history];
							subtree.setLogLikelihood(probRight);
							subtree.getChildrens().clear();
							subtree.setChild(new SubTree(role, i));
						}
					}
				}
			}
		}	
		Timer.stop("init-leafs");
		
		Timer.start("recognition");
		for (short i=1; i<sentence.size(); i++) {
			for (short j=0; j<sideSubTrees[i].length; j++) {
				SubTreeSidePossibilities subTreePossibilities = sideSubTrees[i][j];
				short from = subTreePossibilities.getFrom();
				short to = subTreePossibilities.getTo();
				SubTree[] subTreePossibilitiesLeft = subTreePossibilities.getLeft();
				SubTree[] subTreePossibilitiesRight = subTreePossibilities.getRight();
				
				//vic podstromu zakoncene v aktulnim rootu
				for (short pos=from; pos<to; pos++) {
					SubTreeSidePossibilities left = sideSubTrees[pos-from][from];
					SubTreeSidePossibilities right = sideSubTrees[to-pos-1][pos+1];
					
					for (int history=0; history<histories; history++) {
						double probLeft = left.getLeft()[history].getLogLikelihood() + right.getLeft()[history].getLogLikelihood();
						double probRight = left.getRight()[history].getLogLikelihood() + right.getRight()[history].getLogLikelihood();
						
						//tyhlety dve sracky trvaji nejdyl
						if (probLeft > subTreePossibilitiesLeft[history].getLogLikelihood()) {
							SubTree subtree = subTreePossibilitiesLeft[history];
							subtree.setLogLikelihood(probLeft);
							subtree.getChildrens().clear();							
							subtree.getChildrens().addAll(left.getLeft()[history].getChildrens());
							subtree.getChildrens().addAll(right.getLeft()[history].getChildrens());
						}
						
						if (probRight > subTreePossibilitiesRight[history].getLogLikelihood()) {
							SubTree subtree = subTreePossibilitiesRight[history];
							subtree.setLogLikelihood(probRight);
							subtree.getChildrens().clear();							
							subtree.getChildrens().addAll(left.getRight()[history].getChildrens());
							subtree.getChildrens().addAll(right.getRight()[history].getChildrens());
						}
					}
				}
				
			
				//jeden podstrom
				for (short pos=from; pos<=to; pos++) {
					SubTreeSidePossibilities left = null; 
					if (pos-from-1 >= 0) left = sideSubTrees[pos-from-1][from];
					SubTreeSidePossibilities right = null;
					if (pos < to) right = sideSubTrees[to-pos-1][pos+1];
					
					for (short role=0; role<roles; role++) {
						for (short parent=0; parent<roles; parent++) {
							double prob = leafs[pos][role];
							int history = (role * roles) + parent;
							
							SubTree leftRightHistory = (left != null ? left.getRight()[history] : null);
							SubTree rightLeftHistory = (right != null ? right.getLeft()[history] : null);
							
							if (left != null) prob += leftRightHistory.getLogLikelihood();
							if (right != null) prob += rightLeftHistory.getLogLikelihood();
						
							for (short parentOfParent=0; parentOfParent<roles; parentOfParent++) {
								double probRight = prob + logRoleByRoleLeft[role][parent][parentOfParent];
								double probLeft = prob + logRoleByRoleRight[role][parent][parentOfParent];
								int parentHistory = (parent * roles) + parentOfParent;
								
								//tyhlety dve sracky trvaji nejdyl
								if (probLeft > subTreePossibilitiesLeft[parentHistory].getLogLikelihood()) {
									SubTree subtree = subTreePossibilitiesLeft[parentHistory];
									subtree.setLogLikelihood(probLeft);
									subtree.getChildrens().clear();
									
									SubTree lastRoot = new SubTree(role, pos);
									subtree.setChild(lastRoot);
									
									if (left != null) lastRoot.getChildrens().addAll(leftRightHistory.getChildrens());
									if (right != null) lastRoot.getChildrens().addAll(rightLeftHistory.getChildrens());
								}
								
								if (probRight > subTreePossibilitiesRight[parentHistory].getLogLikelihood()) {
									SubTree subtree = subTreePossibilitiesRight[parentHistory];
									subtree.setLogLikelihood(probRight);
									subtree.getChildrens().clear();
									
									SubTree lastRoot = new SubTree(role, pos);
									subtree.setChild(lastRoot);
									
									if (left != null) lastRoot.getChildrens().addAll(leftRightHistory.getChildrens());
									if (right != null) lastRoot.getChildrens().addAll(rightLeftHistory.getChildrens());
								}
							}
						}
						
					}
					
				}
				
			}
		}
		Timer.stop("recognition");
		
		//find best
		System.out.println(sideSubTrees[sentence.size()-1][0].getLeft()[0].getLogLikelihood());
		//System.out.println(sideSubTrees[sentence.size()-1][0].getLeft()[0].toPennFormat());
		
		
		sentence.getRoot().setRole(sideSubTrees[sentence.size()-1][0].getLeft()[0]);
		sideSubTrees[sentence.size()-1][0].getLeft()[0].setToken(sentence.getRoot());
		updateLinks(sentence.getRoot().getRole(), sentence);
		sentence.getRoot().getRole().calculateMinMaxPositions();
		
		
		Timer.stop("exact-infer");
	}
	
	private void updateLinks(Role root, Sentence sentence) {
		if (root.getPosition() >= 0) {
			Token token = sentence.getTokens()[root.getPosition()];
			root.setToken(token);
			token.setRole(root);
		}
		
		for (Role child : root.getChildrens()) {
			child.setParent(root);
			updateLinks(child, sentence);
		}
	}

	@Override
	public void initialize() {}

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		//asi -63.34098904619115
		
		PropertyConfigurator.configure("log4j.properties");
		LTLM2sides3gram ltlm = IOUtils.load3Gram("models/3gram_en_LTLM_10roles.bin");
		Inferencer infer = new ExactInferencer3gram(ltlm, 30);
		
		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
		DataProvider provider = new BasicDataProvider("data/test.txt", preprocessing, ltlm.getVocabulary(), 3, 30);
		
		//Evaluator eval = new Evaluator(ltlm, false);
		for (int s=0; s<1000; s++) {
			Sentence sentence = provider.next();
			
			System.out.println(s);
			System.out.println(sentence.getText(ltlm.getVocabulary()));

			infer.infer(sentence, false, false);
	

			Timer.print();
			
			double log = 0;
			for (int i=0; i<sentence.size(); i++) {
				log += Math.log(ltlm.getProbabilityAtPosition(sentence, i, true));
			}
			System.out.println("log="+log);
			
			
			/*
			double bestLog = Double.NEGATIVE_INFINITY;
			TreeBuilder3gram builder = new TreeBuilder3gram(ltlm, 30);
			
			for (int n=0; n<1000; n++) {
				builder.buildTree(sentence);
				double log = 0;
				for (int i=0; i<sentence.size(); i++) {
					log += Math.log(ltlm.getProbabilityAtPosition(sentence, i, true));
				}
				System.out.println("log="+log+" best="+bestLog);
				
				if (log > bestLog) {
					bestLog = log;
					GraphViz.save(sentence, ltlm.getVocabulary(), "dot.dot", "png.png");
				}
			}
			*/
		}
	}
}
