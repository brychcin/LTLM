package sr.prob;

import java.util.Arrays;
import java.util.Random;

public class PoissonDistribution {

	private int keys;
	private int conditions;
	
	private double[][] sums;
	private Random random;
	private double[][] lambdas;
	private double[][] expMinusLambda;
	private static final int MAX = 15;
	
	public PoissonDistribution(int keys, int conditions, float lambda) {
		this.random = new Random(System.currentTimeMillis());
		
		this.keys = keys;
		this.conditions = conditions;
		this.sums = new double[MAX][conditions];
		this.expMinusLambda = new double[keys][conditions];
		this.lambdas = new double[keys][conditions];
		
		for (int key=0; key<keys; key++) {
			for (int condition=0; condition<conditions; condition++) {
				this.lambdas[key][condition] = lambda;
			}
		}
		
		this.expMinusLambda = new double[keys][conditions];
		init();
	}
	
	private void init() {
		for (int pos=0; pos<MAX; pos++) Arrays.fill(sums[pos], 0);
		
		for (int condition=0; condition<conditions; condition++) {
			for (int key=0; key<keys; key++) {
				this.expMinusLambda[key][condition] = Math.exp(-lambdas[key][condition]);
				
				for (int pos=0; pos<MAX; pos++) {
					this.sums[pos][condition] += getProbability(pos, key, condition);
				}
			}
		}
	}
	
	public int next(int key, int condition) {
		int k = 0;
		double p = 1.0d;
		
		do {
			k++;
			p *= random.nextDouble();
		} while (p>expMinusLambda[key][condition]);
		
		return k-1;
	}
	
	public double getProbability(int k, int key, int condition) {
		if (k > MAX) return 0;
		return Math.pow(lambdas[key][condition], k) * expMinusLambda[key][condition] / factorial(k);
	}
	
	public double getConditionalProbability(int k, int key, int condition) {
		return getProbability(k, key, condition) / sums[k][condition];
	}
	
	public static double getProbability(double lambda, double expMinusLambda, int k) {
		return Math.pow(lambda, k) * expMinusLambda / factorial(k);
	}
	
	public static double factorial(int n) {
		double fact = 1;
        for (int i = 2; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }
	
	public void print() {
		for (int condition=0; condition<conditions; condition++) {
			System.out.print(condition+":\t");
			for (int pos=0; pos<MAX; pos++) {
				System.out.print(sums[pos][condition]+"\t");
			}
			System.out.println();
		}
	}
		
	public static void main(String[] args) {
		PoissonDistribution pd = new PoissonDistribution(10, 10, 5f);
		
		int[] histogram = new int[MAX];
		int N = 1000000;
		int sum = 0;
		for (int i=0; i<N; i++) {
			int sample = pd.next(0, 0);
			sum += sample;
			
			if (sample < histogram.length) histogram[sample]++;
		}
		
		double p = getProbability(2d, Math.exp(-2.d), 50);
		System.out.println(p);
		System.out.println(p*p);
		System.out.println(p*p*p);
		System.out.println(p*p*p*p);
		System.out.println(p*p*p*p*p);
		System.out.println(p*p*p*p*p*p);
		System.out.println(p*p*p*p*p*p*p);
		System.out.println(p*p*p*p*p*p*p*p);
		System.out.println(p*p*p*p*p*p*p*p*p);
		
		/*
		for (int i=0; i<histogram.length; i++) {
			System.out.print(i+"="+pd.getProbability(i, 0, 0)+", ");
		}
		System.out.println();
		
		System.out.println(Arrays.toString(histogram));
		*/
		System.out.println("lambda estimate="+(double)sum/(double)N);

		//pd.print();
	}

}
