package sr.data;

import sr.Sentence;

public class Paraphrase {

	private Sentence sentence1;
	private Sentence sentence2;
	private boolean paraphrase;
	
	public Paraphrase(Sentence sentence1, Sentence sentence2, boolean paraphrase) {
		this.sentence1 = sentence1;
		this.sentence2 = sentence2;
		this.paraphrase = paraphrase;
	}
	
	public Sentence getSentence1() {
		return sentence1;
	}
	public void setSentence1(Sentence sentence1) {
		this.sentence1 = sentence1;
	}
	public Sentence getSentence2() {
		return sentence2;
	}
	public void setSentence2(Sentence sentence2) {
		this.sentence2 = sentence2;
	}
	public boolean isParaphrase() {
		return paraphrase;
	}
	public void setParaphrase(boolean paraphrase) {
		this.paraphrase = paraphrase;
	}
	
	
	
}
