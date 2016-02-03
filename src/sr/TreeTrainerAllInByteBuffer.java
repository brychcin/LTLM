package sr;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;

import sr.data.BasicDataProvider;
import sr.data.DataProvider;
import sr.eval.Evaluator;
import sr.infer.Inferencer;
import sr.infer.InferencerTypeEnum;
import sr.infer.RandomInferencer;
import sr.lm.LTLM2sides2gram;
import sr.lm.LTLM2sides3gram;
import sr.lm.Model;
import sr.lm.ModelTypeEnum;
import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;
import sr.utils.IOUtils;
import sr.utils.Timer;


public class TreeTrainerAllInByteBuffer extends TreeTrainer {

	private static Options options = new Options();
	private final int MAX_BUFFER_SIZE = 100000000;
	private QueueOfSentences queue;
	private List<ByteBuffer> bufferList;
	
	public TreeTrainerAllInByteBuffer(int roles, Vocabulary vocabulary, int MAX_SENTENCE_LENGTH, int BEST_ITERATION) {
		super(roles, vocabulary, MAX_SENTENCE_LENGTH, BEST_ITERATION);
	}
	
	public TreeTrainerAllInByteBuffer(int roles, Vocabulary vocabulary, int MAX_SENTENCE_LENGTH, int BEST_ITERATION, int BIGRAM_ITERATIONS_PER_WORD, int BIGRAM_ITERATIONS_PER_SENTENCE, int TRIGRAM_ITERATIONS_PER_WORD, int TRIGRAM_ITERATIONS_PER_SENTENCE) {
		super(roles, vocabulary, MAX_SENTENCE_LENGTH, BEST_ITERATION, BIGRAM_ITERATIONS_PER_WORD, BIGRAM_ITERATIONS_PER_SENTENCE, TRIGRAM_ITERATIONS_PER_WORD, TRIGRAM_ITERATIONS_PER_SENTENCE);
	}
	
	@Override
	public Model train(DataProvider provider, int NUM_OF_THREADS) throws Exception {
		if (NUM_OF_THREADS < 2) throw new IllegalArgumentException("number of threads must be at least 2");
		
		logger.info("START TRAINING LTLM roles="+roles+" bigramPerWordIter="+BIGRAM_ITERATIONS_PER_WORD+" bigramPerSentenceIter"+BIGRAM_ITERATIONS_PER_SENTENCE+" trigramPerWordIter="+TRIGRAM_ITERATIONS_PER_WORD+" trigramPerSentenceIter="+TRIGRAM_ITERATIONS_PER_SENTENCE);
		logger.info("INITIAL RANDOM SETTING");
		RandomInferencer randomInferencer = new RandomInferencer(roles);

		this.bufferList = new ArrayList<ByteBuffer>();
        long sentences = 0;
        int offset = 8;
        boolean first = true;
        long totalCapacity = 0;
        
        List<Sentence> sentencesToWrite = new ArrayList<Sentence>();
        
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {
			if (sentence.size() > MAX_SENTENCE_LENGTH) continue;
			if (sentence.getNumberOfBytes() > MAX_BUFFER_SIZE) throw new IllegalArgumentException("too small buffer");
						
			randomInferencer.infer(sentence, false, true);
			
			if (offset + sentence.getNumberOfBytes() > MAX_BUFFER_SIZE) {
				totalCapacity += offset;
				ByteBuffer buffer = ByteBuffer.allocate(offset);
				bufferList.add(buffer);
				
				if (first) buffer.putLong(0);
				
				for (Sentence s : sentencesToWrite) {
					writeSentence(buffer, s);
				}
				sentencesToWrite.clear();
				offset = 0;
				first = false;
			} 
			
			sentence.setOffset(offset);
			sentencesToWrite.add(sentence);
			offset += sentence.getNumberOfBytes();
			sentences++;
		}
		
		//dozapis to co bylo na konec
		if (sentencesToWrite.size() > 0) {
			totalCapacity += offset;
			ByteBuffer buffer = ByteBuffer.allocate(offset);
			bufferList.add(buffer);
			
			if (first) buffer.putLong(0, 0);
			
			for (Sentence s : sentencesToWrite) {
				writeSentence(buffer, s);
			}
		}
		
		//zapis pocet vet na zacatek
		ByteBuffer buffer = bufferList.get(0);
		buffer.putLong(0, sentences);
		provider.close();

		logger.info("total buffer capacity="+totalCapacity+"B ="+(totalCapacity/1e9D)+"GB");

		Model model = multiThreadTraining(NUM_OF_THREADS);
		return model;
	}
	
	
	private Model multiThreadTraining(int NUM_OF_THREADS) throws IOException {
		Model model = null;
		try {
			if (BIGRAM_ITERATIONS_PER_WORD > 0) model = multiThreadTraining(NUM_OF_THREADS, ModelTypeEnum.LTLM_2SIDE_2GRAM, InferencerTypeEnum.INF_2SIDE_2GRAM_PER_WORD, BIGRAM_ITERATIONS_PER_WORD);
			if (BIGRAM_ITERATIONS_PER_SENTENCE > 0) model = multiThreadTraining(NUM_OF_THREADS, ModelTypeEnum.LTLM_2SIDE_2GRAM, InferencerTypeEnum.INF_2SIDE_2GRAM_PER_SENTENCE, BIGRAM_ITERATIONS_PER_SENTENCE);
			if (TRIGRAM_ITERATIONS_PER_WORD > 0) model = multiThreadTraining(NUM_OF_THREADS, ModelTypeEnum.LTLM_2SIDE_3GRAM, InferencerTypeEnum.INF_2SIDE_3GRAM_PER_WORD, TRIGRAM_ITERATIONS_PER_WORD);
			if (TRIGRAM_ITERATIONS_PER_SENTENCE > 0) model = multiThreadTraining(NUM_OF_THREADS, ModelTypeEnum.LTLM_2SIDE_3GRAM, InferencerTypeEnum.INF_2SIDE_3GRAM_PER_SENTENCE, TRIGRAM_ITERATIONS_PER_SENTENCE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return model;
	}
	
	private Model multiThreadTraining(int NUM_OF_THREADS, ModelTypeEnum modelType, InferencerTypeEnum inferencerType, int ITERATIONS) throws IOException, InterruptedException {
		logger.info("MULTI THREAD TRAINING ("+NUM_OF_THREADS+")");
		
		Model model = Model.newInstance(modelType, roles, vocabulary);
		learnCountsFromInferedData(model);
		Model[] models = new Model[NUM_OF_THREADS-1];
		models[0] = model;
		Thread[] threads = new ThreadForTreeTrainingMemorySafe[NUM_OF_THREADS-1];
		
		
		for (int iteration=0; iteration<ITERATIONS; iteration++) {		
			Timer.start("iteration");

			boolean best = false;
			if (BEST_ITERATION > 0) best = iteration % BEST_ITERATION == 0;
			
			logger.info(inferencerType+"============================================");
			logger.info("ITERATION "+iteration+" - best sampling "+best);
			logger.info("CREATE "+NUM_OF_THREADS+" THREADS");
			
			Timer.start("deep copies");
			for (int i=1; i<NUM_OF_THREADS-1; i++) models[i] = model.createCopy();
			Timer.stop("deep copies");
			
			Timer.start("inference");
			this.queue = new QueueOfSentences();
			for (int i=0; i<threads.length; i++) {
				Inferencer inferencer = Inferencer.newInstance(models[i], inferencerType, MAX_SENTENCE_LENGTH);
				threads[i] = new ThreadForTreeTrainingMemorySafe(inferencer, queue, best);
			}
			for (int i=0; i<threads.length; i++) threads[i].start();
			processIteration();
			Timer.stop("inference");
			
			
			Timer.start("reconstruction from data");
			learnCountsFromInferedData(model);
			Timer.stop("reconstruction from data");
			
			//neoptimalizovat parametry hned od zacatku, je tam nekde nejaka chyba a obcas vyjdou zaporny
			if (iteration >= OPTIMIZE_FROM_ITERATION && iteration % OPTIMIZE_PER_ITERATION == 0) {
				Timer.start("optimalizace");
				model.optimizeHyperParameters();
				Timer.stop("optimalizace");
			}
			
			Timer.start("perplexity");
			calculateLogLikelihood(model);
			Timer.stop("perplexity");

			Timer.stop("iteration");
			Timer.print();
		}
		
		return model;
	}
	
	private void calculateLogLikelihood(Model model) throws IOException {
		Evaluator eval = new Evaluator(model, true);
		eval.newRound();
		
		int bufferIndex = 0;
		ByteBuffer buffer = this.bufferList.get(bufferIndex);
		buffer.rewind();
		long sentences = buffer.getLong();
		int offset = 8;
		
		for (long i=0; i<sentences; i++) {
			if (buffer.remaining() < 2) {
				bufferIndex++;
				buffer = this.bufferList.get(bufferIndex);
				offset = 0;
				buffer.rewind();
			} 
				
			int nextSize = (buffer.getShort()*8)+2;
			buffer.position(offset);
				
			if (buffer.remaining() < nextSize) {
				bufferIndex++;
				buffer = this.bufferList.get(bufferIndex);
				offset = 0;
				buffer.rewind();
			}
			
			Sentence sentence = readSentence(buffer, offset);
			eval.processSentence(sentence);
			offset += sentence.getNumberOfBytes();
		}
		
		eval.print();
	}
	
	private void learnCountsFromInferedData(Model model) throws IOException {
		model.initialize();
		
		int bufferIndex = 0;
		ByteBuffer buffer = this.bufferList.get(bufferIndex);
		buffer.rewind();
		long sentences = buffer.getLong();
		int offset = 8;
		
		for (long i=0; i<sentences; i++) {
			if (buffer.remaining() < 2) {
				bufferIndex++;
				buffer = this.bufferList.get(bufferIndex);
				offset = 0;
				buffer.rewind();
			} 
				
			int nextSize = (buffer.getShort()*8)+2;
			buffer.position(offset);
				
			if (buffer.remaining() < nextSize) {
				bufferIndex++;
				buffer = this.bufferList.get(bufferIndex);
				offset = 0;
				buffer.rewind();
			}
			
			Sentence sentence = readSentence(buffer, offset);
			model.learnCountsFromInferedData(sentence);
			
			offset += sentence.getNumberOfBytes();
		}
	}
	
	private void processIteration() throws IOException, InterruptedException {
		ByteBuffer buffer = this.bufferList.get(0);
		buffer.rewind();
		long sentences = buffer.getLong();
		int bufferIndex = 0;
		int offset = 8;
		
		long writtenSentences = 0;
		int sentencesToWrite = 0;
		for (long i=0; i<sentences; i++) {
			
			if (buffer.remaining() < 2) {
				writeSentences(buffer, sentencesToWrite);
				writtenSentences += sentencesToWrite;
				sentencesToWrite = 0;
				bufferIndex++;
				buffer = this.bufferList.get(bufferIndex);
				offset = 0;
				buffer.rewind();
			} 
				
			int nextSize = (buffer.getShort()*8)+2;
			buffer.position(offset);
				
			if (buffer.remaining() < nextSize) {
				writeSentences(buffer, sentencesToWrite);
				writtenSentences += sentencesToWrite;
				sentencesToWrite = 0;
				bufferIndex++;
				buffer = this.bufferList.get(bufferIndex);
				offset = 0;
				buffer.rewind();
			}
			
			Sentence sentence = readSentence(buffer, offset);
			queue.getSentencesToProcess().add(sentence);
			sentencesToWrite++;
			
			offset += sentence.getNumberOfBytes();
		}
		
		queue.setFinished(true);
		writeSentences(buffer, sentencesToWrite);
		writtenSentences += sentencesToWrite;
		//logger.info(writtenSentences+" finished sentences");
	}
	
	
	//uloz zmeny a cekej nez to vsechno dobehne
	private void writeSentences(ByteBuffer buffer, int sentencesToWrite) throws InterruptedException {
		int written = 0;
		while (true) {
			Sentence sentence = queue.getSentencesToSave().poll();
			
			if (sentence == null) {
				if (written == sentencesToWrite) return;
				Thread.sleep(10);
			} else {
				writeSentence(buffer, sentence);
				written++;
			}
		}
	}
	
	
	private Sentence readSentence(ByteBuffer buffer, int offset) {
		buffer.position(offset);
		short size = buffer.getShort();
		
		int[] keys = new int[size];
		short[] roleKeys = new short[size];
		short[] parentPoss = new short[size];
		
		for (int i=0; i<size; i++) {
			keys[i] = buffer.getInt();
			roleKeys[i] = buffer.getShort();
			parentPoss[i] = buffer.getShort();
		}
		
		Sentence sentence = new Sentence(keys);
		
		for (short pos=0; pos<sentence.getTokens().length; pos++) {
			Token token = sentence.getTokens()[pos];
			Role role = new Role(roleKeys[pos], token, pos);
			token.setRole(role);
			role.setToken(token);
		}
		
		for (short pos=0; pos<sentence.getTokens().length; pos++) {	
			Token token = sentence.getTokens()[pos];
			Role role = token.getRole();
			Role parent = null;
			if (parentPoss[pos] < 0) {
				parent = sentence.getRoot().getRole();
			} else {
				parent = sentence.getTokens()[parentPoss[pos]].getRole();
			}
			
			parent.setChild(role);
			role.setParent(parent);
		}
		
		sentence.getRoot().getRole().calculateMinMaxPositions();
		sentence.setOffset(offset);
		return sentence;
	}
	
	private void writeSentence(ByteBuffer buffer, Sentence sentence) {
		int offset = sentence.getOffset();
		
		buffer.position(offset);
		buffer.putShort((short) sentence.getTokens().length); 
		for (Token token : sentence.getTokens()) {
			buffer.putInt(token.getKey());
			buffer.putShort(token.getRole().getKey());
			buffer.putShort(token.getRole().getParent().getPosition());
		}
	}
	
	
	static {
		options.addOption(OptionBuilder.withArgName("input").hasArg().withDescription("input file (required)").isRequired().create("input"));
		options.addOption(OptionBuilder.withArgName("output").hasArg().withDescription("output file (required)").isRequired().create("output"));
		options.addOption(OptionBuilder.withArgName("vocabulary").hasArg().withDescription("vocabulary file (required)").isRequired().create("vocabulary"));
		options.addOption(OptionBuilder.withArgName("roles").hasArg().withDescription("number of roles (required)").isRequired().create("roles"));
		options.addOption(OptionBuilder.withArgName("threads").hasArg().withDescription("number of threads (default 2)").create("threads"));
		
		options.addOption(OptionBuilder.withArgName("bwit").hasArg().withDescription("number of iteration for bigram inference per word (default "+BIGRAM_ITERATIONS_PER_WORD_DEFAULT+")").create("bwit"));
		options.addOption(OptionBuilder.withArgName("twit").hasArg().withDescription("number of iteration for trigram inference per word (default "+TRIGRAM_ITERATIONS_PER_WORD_DEFAULT+")").create("twit"));
		options.addOption(OptionBuilder.withArgName("bsit").hasArg().withDescription("number of iteration for bigram inference per sentence (default "+BIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT+")").create("bsit"));
		options.addOption(OptionBuilder.withArgName("tsit").hasArg().withDescription("number of iteration for trigram inference per sentence (default "+TRIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT+")").create("tsit"));
	}
	
	public static void main(String[] args) {
		/*
		args = new String[]{
				"-input", "D:/korpusy/czeng/en-trainFull.txt",
				"-output", "models/pokus.bin",
				"-vocabulary", "D:/korpusy/czeng/en-vocabularyFull100000.txt",
				"-threads", "12",
				"-roles", "100"
				};
		*/
		
		PropertyConfigurator.configure("log4j.properties");
		
		try {
			CommandLineParser parser = new GnuParser();
			CommandLine line = parser.parse(options, args);
			
			int THREADS = 2;
			if (line.hasOption("threads")) {
				THREADS = Integer.parseInt(line.getOptionValue("threads"));
			}
			
			String INPUT_FILE = line.getOptionValue("input");
			String VOCABULARY_FILE = line.getOptionValue("vocabulary");
			String OUTPUT_FILE = line.getOptionValue("output");
			int ROLES = Integer.parseInt(line.getOptionValue("roles"));
			
			int BIGRAM_ITERATIONS_PER_WORD = BIGRAM_ITERATIONS_PER_WORD_DEFAULT;
			if (line.hasOption("bwit")) {
				BIGRAM_ITERATIONS_PER_WORD = Integer.parseInt(line.getOptionValue("bwit"));
			}
			int BIGRAM_ITERATIONS_PER_SENTENCE = BIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT;
			if (line.hasOption("bsit")) {
				BIGRAM_ITERATIONS_PER_SENTENCE = Integer.parseInt(line.getOptionValue("bsit"));
			}
			int TRIGRAM_ITERATIONS_PER_WORD = TRIGRAM_ITERATIONS_PER_WORD_DEFAULT;
			if (line.hasOption("twit")) {
				TRIGRAM_ITERATIONS_PER_WORD = Integer.parseInt(line.getOptionValue("twit"));
			}
			int	TRIGRAM_ITERATIONS_PER_SENTENCE = TRIGRAM_ITERATIONS_PER_SENTENCE_DEFAULT;
			if (line.hasOption("tsit")) {
				TRIGRAM_ITERATIONS_PER_SENTENCE = Integer.parseInt(line.getOptionValue("tsit"));
			}
			
			int BEST_ITERATION = -1;
			int MAX_SENTENCE_LENGTH = 30;
			
			Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
			Vocabulary vocabulary = new Vocabulary(VOCABULARY_FILE);
			DataProvider provider = new BasicDataProvider(INPUT_FILE, preprocessing, vocabulary, 3, MAX_SENTENCE_LENGTH);
			
			TreeTrainerAllInByteBuffer treeTrainer = new TreeTrainerAllInByteBuffer(ROLES, vocabulary, MAX_SENTENCE_LENGTH, BEST_ITERATION, BIGRAM_ITERATIONS_PER_WORD, BIGRAM_ITERATIONS_PER_SENTENCE, TRIGRAM_ITERATIONS_PER_WORD, TRIGRAM_ITERATIONS_PER_SENTENCE);
			Model LTLM = treeTrainer.train(provider, THREADS);
			if (LTLM instanceof LTLM2sides3gram) {
				IOUtils.save3Gram(OUTPUT_FILE,  (LTLM2sides3gram)LTLM);
			} else {
				IOUtils.save2Gram(OUTPUT_FILE,  (LTLM2sides2gram)LTLM);
			}
			
		} catch (ParseException e) {
			System.err.println("Parsing failed: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("TreeTrainerMemorySafe", options);
		} catch (NumberFormatException e) {
			System.err.println("Parsing numeric fields failed: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
