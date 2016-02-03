package sr.infer;

import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;

import sr.Role;
import sr.Sentence;
import sr.Token;
import sr.data.BasicDataProvider;
import sr.data.DataProvider;
import sr.lm.LTLM2sides2gram;
import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;
import sr.utils.IOUtils;
import sr.utils.Timer;

public class ExactInferencer2gram extends Inferencer {

	private final int MAX_LENGTH;
	
	private final short roles;
	private LTLM2sides2gram ltlm;
	private double[][] logRoleByRoleLeft;
	private double[][] logRoleByRoleRight;
	
	public ExactInferencer2gram(LTLM2sides2gram ltlm, int MAX_LENGTH) {
		this.ltlm = ltlm;
		this.MAX_LENGTH = MAX_LENGTH;
		this.roles = (short) ltlm.getRoles();
		this.logRoleByRoleLeft = new double[ltlm.getRoles()][ltlm.getRoles()];
		this.logRoleByRoleRight = new double[ltlm.getRoles()][ltlm.getRoles()];
		
		for (int role=0; role<ltlm.getRoles(); role++) {			
			for (int parent=0; parent<ltlm.getRoles(); parent++) {
				this.logRoleByRoleLeft[role][parent] = Math.log((ltlm.getRoleByRoleCountsLeft()[role][parent] + ltlm.getAlphaLeft()[role]) / (ltlm.getRoleCountsLeft()[parent] + ltlm.getAlphaSumLeft()));
				this.logRoleByRoleRight[role][parent] = Math.log((ltlm.getRoleByRoleCountsRight()[role][parent] + ltlm.getAlphaRight()[role]) / (ltlm.getRoleCountsRight()[parent] + ltlm.getAlphaSumRight()));		
			}
		}
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
				sideSubTrees[i][j] = new SubTreeSidePossibilities((short)j, (short)(j+i), roles);
				
				short posLeft = (short) (j-1);
				short posRight = (short) (j+i+1);
				for (short role=0; role<roles; role++) {
					sideSubTrees[i][j].getLeft()[role] = new SubTree(role, posLeft);
					sideSubTrees[i][j].getRight()[role] = new SubTree(role, posRight);
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
				
				for (int parent=0; parent<roles; parent++) {
					double probRight = wordProb + logRoleByRoleLeft[role][parent];
					double probLeft = wordProb + logRoleByRoleRight[role][parent];
					
					if (probLeft > sideSubTrees[0][i].getLeft()[parent].getLogLikelihood()) {
						SubTree subtree = sideSubTrees[0][i].getLeft()[parent];
						subtree.setLogLikelihood(probLeft);
						
						subtree.getChildrens().clear();
						subtree.setChild(new SubTree(role, i));
					}
							
					if (probRight > sideSubTrees[0][i].getRight()[parent].getLogLikelihood()) {
						SubTree subtree = sideSubTrees[0][i].getRight()[parent];
						subtree.setLogLikelihood(probRight);
						
						subtree.getChildrens().clear();
						subtree.setChild(new SubTree(role, i));
					}
				}
			}
		}	
		Timer.stop("init-leafs");
		
		Timer.start("recognition");
		for (short i=1; i<sentence.size(); i++) {
			for (short j=0; j<sideSubTrees[i].length; j++) {
				SubTreeSidePossibilities subTreePossibilities = sideSubTrees[i][j];
				SubTree[] subTreePossibilitiesLeft = subTreePossibilities.getLeft();
				SubTree[] subTreePossibilitiesRight = subTreePossibilities.getRight();
				
				short from = subTreePossibilities.getFrom();
				short to = subTreePossibilities.getTo();
				
				//vic podstromu zakoncene v aktulnim rootu
				for (short pos=from; pos<to; pos++) {
					SubTreeSidePossibilities left = sideSubTrees[pos-from][from];
					SubTreeSidePossibilities right = sideSubTrees[to-pos-1][pos+1];
					
					for (short role=0; role<roles; role++) {
						double probLeft = left.getLeft()[role].getLogLikelihood() + right.getLeft()[role].getLogLikelihood();
						double probRight = left.getRight()[role].getLogLikelihood() + right.getRight()[role].getLogLikelihood();

						if (probLeft > subTreePossibilitiesLeft[role].getLogLikelihood()) {
							SubTree subtree = subTreePossibilitiesLeft[role];
							subtree.setLogLikelihood(probLeft);
							
							subtree.getChildrens().clear();
							subtree.getChildrens().addAll(left.getLeft()[role].getChildrens());
							subtree.getChildrens().addAll(right.getLeft()[role].getChildrens());
						}
						
						if (probRight > subTreePossibilitiesRight[role].getLogLikelihood()) {
							SubTree subtree = subTreePossibilitiesRight[role];
							subtree.setLogLikelihood(probRight);
							
							subtree.getChildrens().clear();
							subtree.getChildrens().addAll(left.getRight()[role].getChildrens());
							subtree.getChildrens().addAll(right.getRight()[role].getChildrens());
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
						double prob = leafs[pos][role];
						if (left != null) prob += left.getRight()[role].getLogLikelihood();
						if (right != null) prob += right.getLeft()[role].getLogLikelihood();
						
						for (short parent=0; parent<roles; parent++) {
							
							double probRight = prob + logRoleByRoleLeft[role][parent];
							double probLeft = prob + logRoleByRoleRight[role][parent];
							
							
							if (probLeft > subTreePossibilitiesLeft[parent].getLogLikelihood()) {
								SubTree subtree = subTreePossibilitiesLeft[parent];
								subtree.setLogLikelihood(probLeft);
								
								subtree.getChildrens().clear();
								
								SubTree lastRoot = new SubTree(role, pos);
								subtree.setChild(lastRoot);
								
								if (left != null) lastRoot.getChildrens().addAll(left.getRight()[role].getChildrens());
								if (right != null) lastRoot.getChildrens().addAll(right.getLeft()[role].getChildrens());
								
							}
							
							if (probRight > subTreePossibilitiesRight[parent].getLogLikelihood()) {
								SubTree subtree = subTreePossibilitiesRight[parent];
								subtree.setLogLikelihood(probRight);
								
								subtree.getChildrens().clear();

								SubTree lastRoot = new SubTree(role, pos);
								subtree.setChild(lastRoot);
								
								if (left != null) lastRoot.getChildrens().addAll(left.getRight()[role].getChildrens());
								if (right != null) lastRoot.getChildrens().addAll(right.getLeft()[role].getChildrens());
								
							}
						}
					}
				}
			}
		}
		Timer.stop("recognition");
		
		//find best
		//System.out.println(sideSubTrees[sentence.size()-1][0].getLeft()[0].getLogLikelihood());
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
		//asi -58.851010264287716
		
		
		
		PropertyConfigurator.configure("log4j.properties");
		LTLM2sides2gram ltlm = IOUtils.load2Gram("models/2gram_en_LTLM_100roles.bin");
		Inferencer infer = new ExactInferencer2gram(ltlm, 30);
		
		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
		DataProvider provider = new BasicDataProvider("data/test.txt", preprocessing, ltlm.getVocabulary(), 3, 30);
		
		//Evaluator eval = new Evaluator(ltlm, false);
		for (int s=0; s<1; s++) {
			Sentence sentence = provider.next();
			
			System.out.println(s);
			System.out.println(sentence.getText(ltlm.getVocabulary()));
			
			
			infer.infer(sentence, false, false);
			
			System.out.println(sentence.toPennFormat());
				
			
			//sentence.treeControll();
			
			//eval.processSentence(sentence);
			
			//TreeBuilder2gram builder = new TreeBuilder2gram(ltlm, 30);
			//builder.buildTree(sentence);
			
			
			double log = 0;
			for (int i=0; i<sentence.size(); i++) {
				log += Math.log(ltlm.getProbabilityAtPosition(sentence, i, true));
			}
			System.out.println("log="+log);
			
			//eval.print();
			Timer.print();
			//GraphViz.save(sentence, ltlm.getVocabulary(), "dot2.dot", "png2.png");
			
		}
	}
}
