package sr.prob;

import java.util.Arrays;

import org.apache.log4j.Logger;

import sr.utils.MemoryUtils;

public class DirichletOptimizer {
	
	static Logger logger = Logger.getLogger(DirichletOptimizer.class);
	
	private final boolean symetricAlpha;
	private double[] alpha;
	private double alphaSum;
	private int keys;
	private int conditions;
	
	public static final double EULER_MASCHERONI = -0.5772156649015328606065121;
	public static final double DIGAMMA_COEF_1 = 1/12;
	public static final double DIGAMMA_COEF_2 = 1/120;
	public static final double DIGAMMA_COEF_3 = 1/252;
	public static final double DIGAMMA_COEF_4 = 1/240;
	public static final double DIGAMMA_COEF_5 = 1/132;
	public static final double DIGAMMA_COEF_6 = 691/32760;
	public static final double DIGAMMA_COEF_7 = 1/12;
	public static final double DIGAMMA_COEF_8 = 3617/8160;
	public static final double DIGAMMA_COEF_9 = 43867/14364;
	public static final double DIGAMMA_COEF_10 = 174611/6600;

	public static final double DIGAMMA_LARGE = 9.5;
	public static final double DIGAMMA_SMALL = .000001;
	
	public DirichletOptimizer(int keys, int conditions, double hyperParameter, boolean symetricAlpha) {
		this.symetricAlpha = symetricAlpha;
		this.keys = keys;
		this.conditions = conditions;
		
		this.alpha = new double[keys];
		Arrays.fill(alpha, hyperParameter);
		this.alphaSum = keys * hyperParameter;
	}
	
	public void optimize(int[][] counts, int[] countSums) {
		int maxConditionCount = 0;
		for (int i=0; i<conditions; i++) {
			if (countSums[i] > maxConditionCount) maxConditionCount = countSums[i];
		}
		
		logger.debug("conditionHistogram length="+maxConditionCount+1);
		//count
		int[] conditionHistogram = new int[maxConditionCount+1];
		for (int i=0; i<conditions; i++) {
			conditionHistogram[countSums[i]]++;
		}
		logger.debug(MemoryUtils.usedMemoryToString());
		
		int maxKeyCount = 0;
		for (int i=0; i<keys; i++) {
			for (int j=0; j<conditions; j++) {
				if (counts[i][j] > maxKeyCount) maxKeyCount = counts[i][j];
			}
		}
		
		logger.debug("keyHistogram length="+maxKeyCount+1);
		//key x count
		int[][] keyHistogram = new int[keys][maxKeyCount+1];
		for (int i=0; i<keys; i++) {
			for (int j=0; j<conditions; j++) {
				keyHistogram[i][counts[i][j]]++;
			}
		}
		logger.debug(MemoryUtils.usedMemoryToString());
		
		logger.debug("histogram length="+maxKeyCount+1);
		//count
		int[] histogram = new int[maxKeyCount+1];
		for (int i=0; i<keys; i++) {
			for (int j=0; j<conditions; j++) {
				histogram[counts[i][j]]++;
			}
		}
		logger.debug(MemoryUtils.usedMemoryToString());

		if (symetricAlpha) {
			alphaSum = learnSymmetricConcentration(histogram, conditionHistogram, keys, alphaSum);
			for (int key = 0; key < keys; key++) {
				alpha[key] = alphaSum / keys;
			}
		}
		else {
			alphaSum = learnParameters(alpha, keyHistogram, conditionHistogram, 1.001, 1.0, 5);
		}
	}
	
	private double digamma(double z) {
		double psi = 0;

		if (z < DIGAMMA_SMALL) {
			psi = EULER_MASCHERONI - (1 / z); 
			return psi;
		}

		while (z < DIGAMMA_LARGE) {
			psi -= 1 / z;
			z++;
		}

		double invZ = 1/z;
		double invZSquared = invZ * invZ;

		psi += Math.log(z) - .5 * invZ
		- invZSquared * (DIGAMMA_COEF_1 - invZSquared * 
				(DIGAMMA_COEF_2 - invZSquared * 
						(DIGAMMA_COEF_3 - invZSquared * 
								(DIGAMMA_COEF_4 - invZSquared * 
										(DIGAMMA_COEF_5 - invZSquared * 
												(DIGAMMA_COEF_6 - invZSquared *
														DIGAMMA_COEF_7))))));

		return psi;
	}
	
	/**
	 * Learn the concentration parameter of a symmetric Dirichlet using frequency histograms.
	 *  Since all parameters are the same, we only need to keep track of 
	 *  the number of observation/dimension pairs with count N
	 *
	 * @param countHistogram An array of frequencies. If the matrix X represents observations such that x<sub>dt</sub> is how many times word t occurs in document d, <code>countHistogram[3]</code> is the total number of cells <i>in any column</i> that equal 3.
	 * @param observationLengths A histogram of sample lengths, for example <code>observationLengths[20]</code> could be the number of documents that are exactly 20 tokens long.	 
	 * @param dimension The dimension.
	 * @param currentValue An initial starting value.
	 */
	private double learnSymmetricConcentration(int[] countHistogram, int[] observationLengths, int dimension, double currentValue) {
		double currentDigamma;

		int largestNonZeroCount = 0;
		int[] nonZeroLengthIndex = new int[ observationLengths.length ];
		
		for (int index = 0; index < countHistogram.length; index++) {
			if (countHistogram[index] > 0) { largestNonZeroCount = index; }
		}
		
		int denseIndex = 0;
		for (int index = 0; index < observationLengths.length; index++) {
			if (observationLengths[index] > 0) {
				nonZeroLengthIndex[denseIndex] = index;
				denseIndex++;
			}
		}
		
		int denseIndexSize = denseIndex;
		
		for (int iteration = 1; iteration <= 200; iteration++) {
			double currentParameter = currentValue / dimension;
			
			currentDigamma = 0;
			double numerator = 0;
			
			// Counts of 0 don't matter, so start with 1
			for (int index = 1; index <= largestNonZeroCount; index++) {
				currentDigamma += 1.0 / (currentParameter + index - 1);
				numerator += countHistogram[index] * currentDigamma;
			}
			
			// Now calculate the denominator, a sum over all observation lengths
			currentDigamma = 0;
			double denominator = 0;
			int previousLength = 0;
			
			double cachedDigamma = digamma(currentValue);
			
			for (denseIndex = 0; denseIndex < denseIndexSize; denseIndex++) {
				int length = nonZeroLengthIndex[denseIndex];
				
				if (length - previousLength > 20) {
					// If the next length is sufficiently far from the previous,
					//  it's faster to recalculate from scratch.
					currentDigamma = digamma(currentValue + length) - cachedDigamma;
				} else {
					// Otherwise iterate up. This looks slightly different
					//  from the previous version (no -1) because we're indexing differently.
					for (int index = previousLength; index < length; index++) {
						currentDigamma += 1.0 / (currentValue + index);
					}
				}
				
				denominator += currentDigamma * observationLengths[length];
			}
			
			currentValue = currentParameter * numerator / denominator;
		}
		
		return currentValue;
	}
	
	/** 
	 * Learn Dirichlet parameters using frequency histograms
	 * 
	 * @param parameters A reference to the current values of the parameters, which will be updated in place
	 * @param observations An array of count histograms. <code>observations[10][3]</code> could be the number of documents that contain exactly 3 tokens of word type 10.
	 * @param observationLengths A histogram of sample lengths, for example <code>observationLengths[20]</code> could be the number of documents that are exactly 20 tokens long.
	 * @returns The sum of the learned parameters.
	 */ 
	private double learnParameters(double[] parameters, int[][] observations, int[] observationLengths) {
		return learnParameters(parameters, observations, observationLengths, 1.00001, 1.0, 200);
	}

	/** 
	 * Learn Dirichlet parameters using frequency histograms
	 * 
	 * @param parameters A reference to the current values of the parameters, which will be updated in place
	 * @param observations An array of count histograms. <code>observations[10][3]</code> could be the number of documents that contain exactly 3 tokens of word type 10.
	 * @param observationLengths A histogram of sample lengths, for example <code>observationLengths[20]</code> could be the number of documents that are exactly 20 tokens long.
	 * @param shape Gamma prior E(X) = shape * scale, var(X) = shape * scale<sup>2</sup>
	 * @param scale 
	 * @param numIterations 200 to 1000 generally insures convergence, but 1-5 is often enough to step in the right direction
	 * @returns The sum of the learned parameters.
	 */ 
	private double learnParameters(double[] parameters, int[][] observations, int[] observationLengths, double shape, double scale, int numIterations) {
		int i, k;

		double parametersSum = 0;

		for (k=0; k < parameters.length; k++) {
			parametersSum += parameters[k];
		}

		double oldParametersK;
		double currentDigamma;
		double denominator;

		int nonZeroLimit;
		int[] nonZeroLimits = new int[observations.length];
		Arrays.fill(nonZeroLimits, -1);

		int[] histogram;

		for (i=0; i<observations.length; i++) {
			histogram = observations[i];

			//StringBuffer out = new StringBuffer();
			for (k = 0; k < histogram.length; k++) {
				if (histogram[k] > 0) {
					nonZeroLimits[i] = k;
					//out.append(k + ":" + histogram[k] + " ");
				}
			}
			//System.out.println(out);
		}

		for (int iteration=0; iteration<numIterations; iteration++) {

			// Calculate the denominator
			denominator = 0;
			currentDigamma = 0;

			// Iterate over the histogram:
			for (i=1; i<observationLengths.length; i++) {
				currentDigamma += 1 / (parametersSum + i - 1);
				denominator += observationLengths[i] * currentDigamma;
			}

			// Bayesian estimation Part I
			denominator -= 1/scale;

			// Calculate the individual parameters

			parametersSum = 0;
			
			for (k=0; k<parameters.length; k++) {

				// What's the largest non-zero element in the histogram?
				nonZeroLimit = nonZeroLimits[k];

				oldParametersK = parameters[k];
				parameters[k] = 0;
				currentDigamma = 0;

				histogram = observations[k];

				for (i=1; i <= nonZeroLimit; i++) {
					currentDigamma += 1 / (oldParametersK + i - 1);
					parameters[k] += histogram[i] * currentDigamma;
				}

				// Bayesian estimation part II
				parameters[k] = (oldParametersK * parameters[k] + shape) / denominator;

				parametersSum += parameters[k];
			}
		}

		return parametersSum;
	}

	public double[] getAlpha() {
		return alpha;
	}

	public double getAlphaSum() {
		return alphaSum;
	}

	public boolean isSymetricAlpha() {
		return symetricAlpha;
	}
}
