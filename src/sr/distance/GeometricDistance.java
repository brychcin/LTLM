package sr.distance;

import java.util.Arrays;
import java.util.List;

import sr.Sentence;
import sr.Token;

public class GeometricDistance extends Distance {

	private double[][] lambdas;
	private double alpha = 1.0d;
	private double beta = 10.0d;
	
	public GeometricDistance(int roles) {
		super(roles);
		this.lambdas = new double[roles][roles];
	}

	@Override
	public int next(double lambda) {
		double u = random.nextDouble();
		return (int) (Math.log(u)/Math.log(1-lambda));
	}
	
	@Override
	public double getProbability(int distance, int key, int condition) {
		int absDistance = Math.abs(distance) - 1;
		return lambdas[key][condition] * Math.pow(1 - lambdas[key][condition], absDistance);
	}

	@Override
	public void calculate(List<Sentence> sentences) {
		int[][] counts = new int[roles][roles];
		lambdas = new double[roles][roles];
		
		for (Sentence sentence : sentences) {
			for (Token token : sentence.getTokens()) {
				int absDistance = Math.abs(token.getRole().getPosition() - token.getRole().getParent().getPosition()) - 1;
				lambdas[token.getRole().getKey()][token.getRole().getParent().getKey()] += absDistance;
				counts[token.getRole().getKey()][token.getRole().getParent().getKey()]++;
			}
		}
		
		for (int parent=0; parent<roles; parent++) {
			for (int role=0; role<roles; role++) {
				lambdas[role][parent] = (double)((counts[role][parent] + alpha) / (alpha + beta + counts[role][parent] + lambdas[role][parent]));
			}
		}
		/*
		System.out.println("==PROBABILITY==");
		for (int role=0; role<roles; role++) {
			System.out.println(Arrays.toString(lambdas[role]));
		}
		
		System.out.println("==DISTANCE==");
		for (int role=0; role<roles; role++) {
			for (int r=0; r<roles; r++) {
				System.out.print((1.0d - lambdas[role][r]) / lambdas[role][r]+", ");
			}
			System.out.println();
		}*/
		
		
		calculateHistogram(sentences);
	}

	public static void main(String[] args) {
		
		Distance distance = new GeometricDistance(10);
		double p = 0.1;
		
		int[] histogram = new int[20];
		
		int samples = 10288;
		int sum = 0;
		for (int i=0; i<samples; i++) {
			int sample = distance.next(p);
			if (sample < 20) {
				histogram[sample]++;
			}
			
			sum += sample;
		}
		
		System.out.println(Arrays.toString(histogram));
		double newP = ((double)samples / ((double)samples + (double)sum));
		System.out.println(newP);
	}

}
