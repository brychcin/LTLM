package sr;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import sr.data.BasicDataProvider;
import sr.data.ConllDataWriter;
import sr.data.DataProvider;
import sr.data.DataWriter;
import sr.infer.ExactInferencer2gram;
import sr.infer.ExactInferencer3gram;
import sr.infer.Inferencer;
import sr.infer.PseudoInferencer2gram;
import sr.infer.PseudoInferencer3gram;
import sr.lm.LTLM2sides2gram;
import sr.lm.LTLM2sides3gram;
import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;
import sr.utils.IOUtils;
import sr.utils.Timer;

public class Main {

	private static Options options = new Options();
	static Logger logger = Logger.getLogger(Main.class);
	
	static {
		options.addOption(OptionBuilder.withArgName("input").hasArg().withDescription("input file (required)").isRequired().create("input"));
		options.addOption(OptionBuilder.withArgName("output").hasArg().withDescription("output file (required)").isRequired().create("output"));
		options.addOption(OptionBuilder.withArgName("model").hasArg().withDescription("model file (required)").isRequired().create("model"));
		options.addOption(OptionBuilder.withArgName("order").hasArg().withDescription("order of model (required)").isRequired().create("order"));
		options.addOption(new Option("exact", "use for the exact inferencer, otherwise the pseudo inferencer will be used"));
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		// TODO Auto-generated method stub

		/*
		args = new String[]{
				"-input", "D:/korpusy/czeng/en-test/99etest.surf.en",
				"-output", "pokus.txt",
				"-model", "models/2gram_en_LTLM_100roles.bin",
				"-order", "2",
				//"-exact",
				};
		*/
		
		final int MAX_SENTENCE_LENGTH = 30;
		final int MIN_SENTENCE_LENGTH = 3;
		
		PropertyConfigurator.configure("log4j.properties");
		
		try {
			CommandLineParser parser = new GnuParser();
			CommandLine line = parser.parse(options, args);
			
			String INPUT_FILE = line.getOptionValue("input");
			String OUTPUT_FILE = line.getOptionValue("output");
			String MODEL_FILE = line.getOptionValue("model");
			int order = Integer.parseInt(line.getOptionValue("order"));
			
			boolean exact = true;
			if (line.hasOption("exact")) {
				exact = true;
			} else exact = false;
			
			if (order < 2 || order > 3) {
				throw new IllegalArgumentException("order must be 2 or 3");
			}
			

			Vocabulary vocabulary = null;
			Inferencer infer = null;
			
			if (order == 2) {
				LTLM2sides2gram ltlm = IOUtils.load2Gram(MODEL_FILE);
				if (exact) {
					infer = new ExactInferencer2gram(ltlm, MAX_SENTENCE_LENGTH);
				} else {
					infer = new PseudoInferencer2gram(ltlm, MAX_SENTENCE_LENGTH);
				}
				vocabulary = ltlm.getVocabulary();
			} else {
				LTLM2sides3gram ltlm = IOUtils.load3Gram(MODEL_FILE);
				if (exact) {
					infer = new ExactInferencer3gram(ltlm, MAX_SENTENCE_LENGTH);
				} else {
					infer = new PseudoInferencer3gram(ltlm, MAX_SENTENCE_LENGTH);
				}
				vocabulary = ltlm.getVocabulary();
			}
			logger.info(infer.getClass().getName()+" is used for inference");
			
			Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
			DataProvider provider = new BasicDataProvider(INPUT_FILE, preprocessing, vocabulary, MIN_SENTENCE_LENGTH, MAX_SENTENCE_LENGTH);
			DataWriter writer = new ConllDataWriter(OUTPUT_FILE, vocabulary);
			
			int counter = 0;
			Sentence sentence = null;
			while ((sentence = provider.next()) != null) {
				if (sentence.size() > MAX_SENTENCE_LENGTH || sentence.size() < MIN_SENTENCE_LENGTH) continue;
				
				infer.infer(sentence, false, false);
				writer.write(sentence);
				
				logger.info(counter);
				Timer.print();
				
				counter++;
			}
			
			writer.close();
			provider.close();
			
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
