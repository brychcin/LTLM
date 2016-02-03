package sr.lm;

import java.io.Serializable;
import java.util.List;

import sr.Sentence;
import sr.Vocabulary;

public abstract class Model implements Serializable {

	private static final long serialVersionUID = -3045082188994645830L;
	
	public abstract Vocabulary getVocabulary();
	
	public abstract double getProbabilityAtPosition(Sentence sentence, int position, boolean joint);
	
	public abstract double getProbabilityAtPosition(Sentence sentence, int position, int key, boolean joint);
	
	public abstract void learnCountsFromInferedData(List<Sentence> sentences);
	
	public abstract void learnCountsFromInferedData(Sentence sentences);
	
	public abstract boolean test();
	
	public abstract void optimizeHyperParameters();
	
	public abstract Model createCopy();
	
	public abstract void initialize();
	
	public static Model newInstance(ModelTypeEnum modelType, int roles, Vocabulary vocabulary) {
		switch (modelType) {
		case LTLM_1SIDE_2GRAM : return new LTLM1side2gram(roles, vocabulary);
		case LTLM_2SIDE_2GRAM : return new LTLM2sides2gram(roles, vocabulary);
		case LTLM_2SIDE_3GRAM : return new LTLM2sides3gram(roles, vocabulary);
		}
		
		return null;
	}
}
