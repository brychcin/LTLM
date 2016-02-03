package sr;

import org.apache.log4j.Logger;

import sr.data.DataProvider;
import sr.lm.Model;

public abstract class TreeTrainer {

	static Logger logger = Logger.getLogger(TreeTrainer.class);
	
	//default iterations
	protected final static int BIGRAM_ITERATIONS_PER_WORD_DEFAULT = 500;
	protected final static int BIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT = 0;
	protected final static int TRIGRAM_ITERATIONS_PER_WORD_DEFAULT = 500;
	protected final static int TRIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT = 500;
	
	protected final int BIGRAM_ITERATIONS_PER_WORD;
	protected final int BIGRAM_ITERATIONS_PER_SENTENCE;
	protected final int TRIGRAM_ITERATIONS_PER_WORD;
	protected final int TRIGRAM_ITERATIONS_PER_SENTENCE;
	
	protected final int OPTIMIZE_FROM_ITERATION = 50;
	protected final int OPTIMIZE_PER_ITERATION = 5;
	
	protected final int MAX_SENTENCE_LENGTH;
	protected final int BEST_ITERATION;
	protected int roles;
	protected Vocabulary vocabulary;
	
	public TreeTrainer(int roles, Vocabulary vocabulary, int MAX_SENTENCE_LENGTH, int BEST_ITERATION) {
		this.roles = roles;
		this.MAX_SENTENCE_LENGTH = MAX_SENTENCE_LENGTH;
		this.vocabulary = vocabulary;
		this.BEST_ITERATION = BEST_ITERATION;
		this.BIGRAM_ITERATIONS_PER_WORD = BIGRAM_ITERATIONS_PER_WORD_DEFAULT;
		this.BIGRAM_ITERATIONS_PER_SENTENCE = BIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT;
		this.TRIGRAM_ITERATIONS_PER_WORD = TRIGRAM_ITERATIONS_PER_WORD_DEFAULT;
		this.TRIGRAM_ITERATIONS_PER_SENTENCE = TRIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT;
	}
	
	public TreeTrainer(int roles, Vocabulary vocabulary, int MAX_SENTENCE_LENGTH, int BEST_ITERATION, int BIGRAM_ITERATIONS_PER_WORD, int BIGRAM_ITERATIONS_PER_SENTENCE, int TRIGRAM_ITERATIONS_PER_WORD, int TRIGRAM_ITERATIONS_PER_SENTENCE) {
		this.roles = roles;
		this.MAX_SENTENCE_LENGTH = MAX_SENTENCE_LENGTH;
		this.vocabulary = vocabulary;
		this.BEST_ITERATION = BEST_ITERATION;
		this.BIGRAM_ITERATIONS_PER_WORD = BIGRAM_ITERATIONS_PER_WORD;
		this.BIGRAM_ITERATIONS_PER_SENTENCE = BIGRAM_ITERATIONS_PER_SENTENCE;
		this.TRIGRAM_ITERATIONS_PER_WORD = TRIGRAM_ITERATIONS_PER_WORD;
		this.TRIGRAM_ITERATIONS_PER_SENTENCE = TRIGRAM_ITERATIONS_PER_SENTENCE;
	}
	
	public abstract Model train(DataProvider provider, int NUM_OF_THREADS) throws Exception;
	
	
	
}
