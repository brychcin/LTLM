package sr.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.log4j.Logger;

import sr.Sentence;
import sr.lm.LTLM2sides2gram;
import sr.lm.LTLM2sides3gram;
import sr.lm.ModifiedKneserNeyInterpolation;

public class IOUtils {

	static transient Logger logger = Logger.getLogger(IOUtils.class);
	
	public static void saveLM(String fileName, ModifiedKneserNeyInterpolation mkn) throws IOException {
		logger.info("Saving into "+fileName);
		FileOutputStream fos = new FileOutputStream(fileName);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(mkn);
	    fos.close();
	}
	
	public static ModifiedKneserNeyInterpolation loadLM(String fileName) throws ClassNotFoundException, IOException {
		logger.info("Loading from "+fileName);
		FileInputStream fis = new FileInputStream(fileName);
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    ModifiedKneserNeyInterpolation mkn = (ModifiedKneserNeyInterpolation) ois.readObject();
	    fis.close();
		return mkn;
	}
	
	public static void save3Gram(String fileName, LTLM2sides3gram ltlm) throws IOException {
		logger.info("Saving into "+fileName);
		FileOutputStream fos = new FileOutputStream(fileName);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(ltlm);
	    fos.close();
	}
	
	public static LTLM2sides3gram load3Gram(String fileName) throws IOException, ClassNotFoundException {
		logger.info("Loading from "+fileName);
		FileInputStream fis = new FileInputStream(fileName);
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    LTLM2sides3gram ltlm = (LTLM2sides3gram) ois.readObject();
	    fis.close();
		return ltlm;
	}
	
	public static void save2Gram(String fileName, LTLM2sides2gram ltlm) throws IOException {
		logger.info("Saving into "+fileName);
		FileOutputStream fos = new FileOutputStream(fileName);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(ltlm);
	    fos.close();
	}
	
	public static LTLM2sides2gram load2Gram(String fileName) throws IOException, ClassNotFoundException {
		logger.info("Loading from "+fileName);
		FileInputStream fis = new FileInputStream(fileName);
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    LTLM2sides2gram ltlm = (LTLM2sides2gram) ois.readObject();
	    fis.close();
		return ltlm;
	}
	
	public static void saveSentences(String fileName, List<Sentence> sentences) throws IOException {
		logger.info("Saving into "+fileName);
		FileOutputStream fos = new FileOutputStream(fileName);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(sentences);
	    fos.close();
	}
	
	public static List<Sentence> loadSentences(String fileName) throws IOException, ClassNotFoundException {
		logger.info("Loading from "+fileName);
		FileInputStream fis = new FileInputStream(fileName);
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    List<Sentence> sentences = (List<Sentence>) ois.readObject();
	    fis.close();
		return sentences;
	}
}
