package sr.infer;

import java.io.Serializable;

import sr.Sentence;
import sr.lm.LTLM2sides2gram;
import sr.lm.LTLM2sides3gram;
import sr.lm.Model;

public abstract class Inferencer implements Serializable {

	protected Change sampledChange = null;
	
	public Inferencer() {
		this.sampledChange = new Change();
	}
	
	public abstract void infer(Sentence sentence, boolean best, boolean trainMode);
	
	public abstract void initialize();
	
	public static Inferencer newInstance(Model model, InferencerTypeEnum InferencerType, int MAX_SENTENCE_LENGTH) {
		switch (InferencerType) {
		case INF_2SIDE_2GRAM_PER_SENTENCE : return new PerSentenceInferencer2sides2gram((LTLM2sides2gram) model, MAX_SENTENCE_LENGTH);
		case INF_2SIDE_3GRAM_PER_SENTENCE : return new PerSentenceInferencer2sides3gram((LTLM2sides3gram) model, MAX_SENTENCE_LENGTH);
		case INF_2SIDE_2GRAM_PER_WORD : return new PerWordInferencer2sides2gram((LTLM2sides2gram) model, MAX_SENTENCE_LENGTH);
		case INF_2SIDE_3GRAM_PER_WORD : return new PerWordInferencer2sides3gram((LTLM2sides3gram) model, MAX_SENTENCE_LENGTH);
		}
		
		return null;
	}
	
}
