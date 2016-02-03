package sr.infer;

import sr.Sentence;
import sr.lm.LTLM2sides2gram;

public class PseudoInferencer2gram extends Inferencer {

	private static final long serialVersionUID = 6543702532646612740L;
	private RandomInferencer randomInferencer;
	private PerWordInferencer2sides2gram perWordInferencer2sides2gram;
	private PerSentenceInferencer2sides2gram perSentenceInferencer2sides2gram;
	private final int MAX_SENTENCE_LENGTH;
	private final int BIGRAM_ITERATIONS_PER_WORD_DEFAULT = 100;
	private final int BIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT = 100;

	
	public PseudoInferencer2gram(LTLM2sides2gram LTLM2gram, int MAX_SENTENCE_LENGTH) {
		this.MAX_SENTENCE_LENGTH = MAX_SENTENCE_LENGTH;
		this.randomInferencer = new RandomInferencer(LTLM2gram.getRoles());
		this.perWordInferencer2sides2gram = new PerWordInferencer2sides2gram(LTLM2gram, MAX_SENTENCE_LENGTH);
		this.perSentenceInferencer2sides2gram = new PerSentenceInferencer2sides2gram(LTLM2gram, MAX_SENTENCE_LENGTH);
	}


	@Override
	public void infer(Sentence sentence, boolean best, boolean trainMode) {
		if (sentence.size() > MAX_SENTENCE_LENGTH) throw new IllegalArgumentException("Sentence is too long.");
		
		randomInferencer.infer(sentence, false, false);
		testInfer(perWordInferencer2sides2gram, sentence, BIGRAM_ITERATIONS_PER_WORD_DEFAULT);
		testInfer(perSentenceInferencer2sides2gram, sentence, BIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT);
	}
	
	private void testInfer(Inferencer inferencer, Sentence sentence, int ITERATIONS) {
		for (int iteration=0; iteration<=ITERATIONS; iteration++) {
			inferencer.infer(sentence, false, false);
			//sentence.treeControll();
		}
	}

	@Override
	public void initialize() {}
	
}
