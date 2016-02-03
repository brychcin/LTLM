package sr.infer;

import sr.Sentence;
import sr.lm.LTLM2sides3gram;

public class PseudoInferencer3gram extends Inferencer {

	private RandomInferencer randomInferencer;
	private PerWordInferencer2sides3gram perWordInferencer2sides3gram;
	private PerSentenceInferencer2sides3gram perSentenceInferencer2sides3gram;
	private final int MAX_SENTENCE_LENGTH;
	private final int TRIGRAM_ITERATIONS_PER_WORD_DEFAULT = 100;
	private final int TRIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT = 100;
	
	
	public PseudoInferencer3gram(LTLM2sides3gram LTLM3gram, int MAX_SENTENCE_LENGTH) {
		this.MAX_SENTENCE_LENGTH = MAX_SENTENCE_LENGTH;
		this.randomInferencer = new RandomInferencer(LTLM3gram.getRoles());
		this.perWordInferencer2sides3gram = new PerWordInferencer2sides3gram(LTLM3gram, MAX_SENTENCE_LENGTH);
		this.perSentenceInferencer2sides3gram = new PerSentenceInferencer2sides3gram(LTLM3gram, MAX_SENTENCE_LENGTH);
	}
	
	@Override
	public void infer(Sentence sentence, boolean best, boolean trainMode) {
		if (sentence.size() > MAX_SENTENCE_LENGTH) throw new IllegalArgumentException("Sentence is too long.");
	
		randomInferencer.infer(sentence, false, false);
		testInfer(perWordInferencer2sides3gram, sentence, TRIGRAM_ITERATIONS_PER_WORD_DEFAULT);
		testInfer(perSentenceInferencer2sides3gram, sentence, TRIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT);
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
