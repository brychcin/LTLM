package sr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;

import sr.data.BasicDataProvider;
import sr.data.DataProvider;
import sr.eval.Evaluator;
import sr.infer.Inferencer;
import sr.infer.InferencerTypeEnum;
import sr.infer.PerSentenceInferencer2sides3gram;
import sr.infer.PerWordInferencer2sides3gram;
import sr.infer.RandomInferencer;
import sr.lm.LTLM2sides2gram;
import sr.lm.LTLM2sides3gram;
import sr.lm.Model;
import sr.lm.ModelTypeEnum;
import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;
import sr.utils.Timer;



public class TreeTrainerAllInMemory extends TreeTrainer {

	public TreeTrainerAllInMemory(int roles, Vocabulary vocabulary, int MAX_SENTENCE_LENGTH, int BEST_ITERATION) {
		super(roles, vocabulary, MAX_SENTENCE_LENGTH, BEST_ITERATION);
	}
	
	@Override
	public LTLM2sides3gram train(DataProvider provider, int NUM_OF_THREADS) throws Exception {
		if (NUM_OF_THREADS < 1) throw new IllegalArgumentException("number of threads must be greater then 0");
		
		List<Sentence> sentences = new ArrayList<Sentence>();
		
		logger.info("INITIAL RANDOM SETTING");
		RandomInferencer randomInferencer = new RandomInferencer(roles);
		
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {
			if (sentence.size() > MAX_SENTENCE_LENGTH) continue;
			randomInferencer.infer(sentence, false, true);
			sentences.add(sentence);
		}
		provider.close();
		
		if (NUM_OF_THREADS == 1) {
			return singleThreadTraining(sentences);
		} else return (LTLM2sides3gram) multiThreadTraining(sentences, NUM_OF_THREADS);
	}
	
	private Model multiThreadTraining(List<Sentence> sentences, int NUM_OF_THREADS) {
		Model model = null;
		try {
			model = multiThreadTraining(sentences, NUM_OF_THREADS, ModelTypeEnum.LTLM_2SIDE_2GRAM, InferencerTypeEnum.INF_2SIDE_2GRAM_PER_WORD, BIGRAM_ITERATIONS_PER_WORD);
			model = multiThreadTraining(sentences, NUM_OF_THREADS, ModelTypeEnum.LTLM_2SIDE_3GRAM, InferencerTypeEnum.INF_2SIDE_3GRAM_PER_WORD, TRIGRAM_ITERATIONS_PER_WORD);
			model = multiThreadTraining(sentences, NUM_OF_THREADS, ModelTypeEnum.LTLM_2SIDE_3GRAM, InferencerTypeEnum.INF_2SIDE_3GRAM_PER_SENTENCE, TRIGRAM_ITERATIONS_PER_SENTENCE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return model;
	}
	
	private Model multiThreadTraining(List<Sentence> sentences, int NUM_OF_THREADS, ModelTypeEnum modelType, InferencerTypeEnum inferencerType, int ITERATIONS) throws InterruptedException {
		logger.info("MULTI THREAD TRAINING ("+NUM_OF_THREADS+")");
		
		Model model = Model.newInstance(modelType, roles, vocabulary);
		model.learnCountsFromInferedData(sentences);
		Model[] models = new Model[NUM_OF_THREADS];
		models[0] = model;
		ThreadForTreeTraining[] threads = new ThreadForTreeTraining[NUM_OF_THREADS];
		
		for (int iteration=0; iteration<=ITERATIONS; iteration++) {		
			Timer.start("iteration");

			boolean best = false;
			if (BEST_ITERATION > 0) best = iteration % BEST_ITERATION == 0;
			
			Collections.shuffle(sentences);
			
			logger.info(inferencerType+"============================================");
			logger.info("ITERATION "+iteration+" - best sampling "+best);
			logger.info("CREATE "+NUM_OF_THREADS+" THREADS");
			
			Timer.start("deep copies");
			for (int i=1; i<NUM_OF_THREADS; i++) models[i] = model.createCopy();
			Timer.stop("deep copies");
			
			Timer.start("inference");
			for (int i=0; i<NUM_OF_THREADS; i++) {
				int size = sentences.size() / NUM_OF_THREADS;
				int start = i*size;
				int end = ((i+1)*size)-1;
				if (i == NUM_OF_THREADS-1) end = sentences.size();
				
				Inferencer inferencer = Inferencer.newInstance(models[i], inferencerType, MAX_SENTENCE_LENGTH);
				threads[i] = new ThreadForTreeTraining(inferencer, sentences.subList(start, end), best);
			}
			
			for (int i=0; i<threads.length; i++) threads[i].start();
			for (int i=0; i<threads.length; i++) threads[i].join();
			Timer.stop("inference");
			
			Timer.start("reconstruction from data");
			model.learnCountsFromInferedData(sentences);
			Timer.stop("reconstruction from data");
			
			//neoptimalizovat parametry hned od zacatku, je tam nekde nejaka chyba a obcas vyjdou zaporny
			if (iteration >= OPTIMIZE_FROM_ITERATION && iteration % OPTIMIZE_PER_ITERATION == 0) {
				Timer.start("optimalizace");
				model.optimizeHyperParameters();
				Timer.stop("optimalizace");
			}
			//logger.info(model.toString());
			//spocitani perplexity
			Timer.start("perplexity");
			Evaluator eval = new Evaluator(model, true);
			eval.newRound();
			for (Sentence s : sentences) eval.processSentence(s);
			eval.print();
			Timer.stop("perplexity");
			Timer.stop("iteration");
			
			Timer.print();
		}
		
		return model;
	}
	
	
	private LTLM2sides3gram singleThreadTraining(List<Sentence> sentences) {
		logger.info("SINGLE THREAD TRAINING");
		LTLM2sides2gram LTLM2gram = new LTLM2sides2gram(roles, vocabulary);
		LTLM2sides3gram LTLM3gram = new LTLM2sides3gram(roles, vocabulary);
		/*
		logger.info("BIGRAM TREE TRAINING PER WORD");
		LTLM2gram.learnCountsFromInferedData(sentences);
		PerWordInferencer2sides2gram perWordInferencer2sides2gram = new PerWordInferencer2sides2gram(LTLM2gram, MAX_SENTENCE_LENGTH);
		singleThreadInference(LTLM2gram, perWordInferencer2sides2gram, sentences, BIGRAM_ITERATIONS_PER_WORD);
		*/
		logger.info("TRIGRAM TREE TRAINING PER WORD");
		LTLM3gram.learnCountsFromInferedData(sentences);
		PerWordInferencer2sides3gram perWordInferencer2sides3gram = new PerWordInferencer2sides3gram(LTLM3gram, MAX_SENTENCE_LENGTH);
		singleThreadInference(LTLM3gram, perWordInferencer2sides3gram, sentences, TRIGRAM_ITERATIONS_PER_WORD);
		
		logger.info("TRIGRAM TREE TRAINING PER SENTENCE");
		PerSentenceInferencer2sides3gram perSentenceInferencer2sides3gram = new PerSentenceInferencer2sides3gram(LTLM3gram, MAX_SENTENCE_LENGTH);
		singleThreadInference(LTLM3gram, perSentenceInferencer2sides3gram, sentences, TRIGRAM_ITERATIONS_PER_SENTENCE);
		
		return LTLM3gram;
	}
	
	private void singleThreadInference(Model LTLM, Inferencer inferencer, List<Sentence> sentences, int ITERATIONS) {
		Evaluator eval = new Evaluator(LTLM, true);
			
		for (int iteration=0; iteration<=ITERATIONS; iteration++) {			
			Timer.start("iteration");
			inferencer.initialize();
			
			boolean best = false;
			if (BEST_ITERATION > 0) best = iteration % BEST_ITERATION == 0;
			
			logger.info(inferencer.getClass()+"============================================");
			logger.info("ITERATION "+iteration+" - best sampling "+best);

			//Timer.start("test");
			//LTLM.test();
			//Timer.stop("test");
			
			eval.newRound();
			
			for (Sentence s : sentences) {
				Timer.start("inference");
				inferencer.infer(s, best, true);
				
				Timer.stop("inference");
				s.treeControll();
			
				Timer.start("likelihood");
				eval.processSentence(s);
				Timer.stop("likelihood");
			}
	
			//neoptimalizovat parametry hned od zacatku, je tam nekde nejaka chyba a obcas vyjdou zaporny
			if (iteration >= OPTIMIZE_FROM_ITERATION && iteration % OPTIMIZE_PER_ITERATION == 0) {
				Timer.start("optimize");
				LTLM.optimizeHyperParameters();
				Timer.stop("optimize");
			}
			
			Timer.stop("iteration");
			
			eval.print();
			//logger.info(LTLM.toString());
			//Timer.print();
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		
		PropertyConfigurator.configure("log4j.properties");
		
		/*
		Properties props = new Properties();
		props.load(new FileInputStream(new File("/storage/plzen1/home/brychcin/LTLM/log4j.properties")));
		props.load(System.in);
		PropertyConfigurator.configure(props);

		String dataFile = props.getProperty("data");
		String modelFile = props.getProperty("model");
		String vocabularyFile = props.getProperty("vocabulary");
		int roles = Integer.parseInt(props.getProperty("roles"));
		int threads = Integer.parseInt(props.getProperty("threads"));
		*/
		
		int roles = 100;
		int threads = 1;
		int BEST_ITERATION = -1;
		int MAX_SENTENCE_LENGTH = 30;
		
		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
		//Vocabulary vocabulary = new Vocabulary("D:/korpusy/czeng/cs-vocabularyQuarter50000.txt");
		//DataProvider provider = new BasicDataProvider("D:/korpusy/czeng/cs-trainQuarter.txt", preprocessing, vocabulary, 3, MAX_SENTENCE_LENGTH);
		
		Vocabulary vocabulary = new Vocabulary("data/vocabulary.txt");
		DataProvider provider = new BasicDataProvider("data/train.txt", preprocessing, vocabulary, 3, MAX_SENTENCE_LENGTH);
		
		TreeTrainerAllInMemory treeTrainer = new TreeTrainerAllInMemory(roles, vocabulary, MAX_SENTENCE_LENGTH, BEST_ITERATION);
		LTLM2sides3gram LTLM = treeTrainer.train(provider, threads);
		//IOUtils.save("models/LTLM"+roles+".bin", LTLM);
		
	}

}
