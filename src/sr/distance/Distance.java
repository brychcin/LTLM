package sr.distance;

import java.util.List;
import java.util.Random;

import sr.Sentence;
import sr.Token;

public abstract class Distance {

	protected Random random;
	protected final int MAX_DISTANCE = 30;
	protected final int roles;
	protected double[] factorials;
	
	public Distance(int roles) {
		this.roles = roles;
		this.random = new Random(System.currentTimeMillis());
		this.factorials = new double[MAX_DISTANCE];
		for (int k=0; k<MAX_DISTANCE; k++) this.factorials[k] = factorial(k);
	}

	
	public abstract double getProbability(int distance, int key, int condition);
	
	public abstract int next(double lambda);
	
	protected double factorial(int n) {
		double fact = 1;
        for (int i = 2; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }
	
	public abstract void calculate(List<Sentence> sentences);
	
	protected void calculateHistogram(List<Sentence> sentences) {
		int[][][] histogram = new int[MAX_DISTANCE][roles][roles];
		
		for (Sentence sentence : sentences) {
			for (Token token : sentence.getTokens()) {
				int distance = token.getRole().getPosition() - token.getRole().getParent().getPosition();
				int absDistance = Math.abs(distance) - 1;
				if (distance < 0) histogram[absDistance][token.getRole().getKey()][token.getRole().getParent().getKey()]++;
			}
		}
		/*
		System.out.println("histogram");
		for (int role=0; role<roles; role++) {
			for (int i=0; i<MAX_DISTANCE; i++) {
				System.out.print(histogram[i][role][0]+", ");
			}
			System.out.println();
		}*/
	}
}
