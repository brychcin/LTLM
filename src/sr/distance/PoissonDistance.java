package sr.distance;

import java.util.Arrays;
import java.util.List;

import sr.Sentence;
import sr.Token;

public class PoissonDistance extends Distance {

	private double[][] lambdasRight;
	private double[][] expMinusLambdaRight;
	private double[][] lambdasLeft;
	private double[][] expMinusLambdaLeft;
	
	
	public PoissonDistance(int roles) {
		super(roles);
		this.lambdasRight = new double[roles][roles];
		this.expMinusLambdaRight = new double[roles][roles];
		this.lambdasLeft = new double[roles][roles];
		this.expMinusLambdaLeft = new double[roles][roles];
		this.factorials = new double[MAX_DISTANCE];
		for (int k=0; k<MAX_DISTANCE; k++) this.factorials[k] = factorial(k);
	}
	
	public void calculate(List<Sentence> sentences) {
		int[][] countsRight = new int[roles][roles];
		lambdasRight = new double[roles][roles];
		int[][] countsLeft = new int[roles][roles];
		lambdasLeft = new double[roles][roles];
		
		for (Sentence sentence : sentences) {
			for (Token token : sentence.getTokens()) {
				int distance = token.getRole().getPosition() - token.getRole().getParent().getPosition();
				if (distance < 0) {
					lambdasLeft[token.getRole().getKey()][token.getRole().getParent().getKey()] += Math.abs(distance)-1;
					countsLeft[token.getRole().getKey()][token.getRole().getParent().getKey()]++;
				} else {
					lambdasRight[token.getRole().getKey()][token.getRole().getParent().getKey()] += Math.abs(distance)-1;
					countsRight[token.getRole().getKey()][token.getRole().getParent().getKey()]++;
				}
				
			}
		}
		
		for (int parent=0; parent<roles; parent++) {
			for (int role=0; role<roles; role++) {
				if (countsRight[role][parent] != 0) {
					lambdasRight[role][parent] /= countsRight[role][parent];
				}
				if (countsLeft[role][parent] != 0) {
					lambdasLeft[role][parent] /= countsLeft[role][parent];
				}
				expMinusLambdaRight[role][parent] = Math.exp(-lambdasRight[role][parent]);
				expMinusLambdaLeft[role][parent] = Math.exp(-lambdasLeft[role][parent]);
			}
		}
		
		System.out.println("==RIGHT==");
		for (int role=0; role<roles; role++) {
			System.out.println(Arrays.toString(lambdasRight[role]));
		}
		System.out.println("==LEFT==");
		for (int role=0; role<roles; role++) {
			System.out.println(Arrays.toString(lambdasLeft[role]));
		}
		
		calculateHistogram(sentences);
	}
	
	
	public double getProbability(int distance, int key, int condition) {
		int absDistance = Math.abs(distance) - 1;
		if (absDistance >= MAX_DISTANCE) return 0;
		
		if (distance < 0) {
			return Math.pow(lambdasLeft[key][condition], absDistance) * expMinusLambdaLeft[key][condition] / factorials[absDistance];
		} else {
			return Math.pow(lambdasRight[key][condition], absDistance) * expMinusLambdaRight[key][condition] / factorials[absDistance];
		}
		
	}

	@Override
	public int next(double lambda) {
		int k = 0;
		double p = 1.0d;
		double exp = Math.exp(-lambda);
		
		do {
			k++;
			p *= random.nextDouble();
		} while (p>exp);
		
		return k-1;
	}
	
	
}
