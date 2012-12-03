/**
* Copyright 2012 University of Massachusetts Amherst
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
*   
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.googlecode.clearnlp.classification.algorithm;

import java.util.ArrayList;
import java.util.List;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.googlecode.clearnlp.classification.prediction.IntPrediction;
import com.googlecode.clearnlp.classification.train.AbstractTrainSpace;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * AdaGrad algorithm.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class AdaGrad extends AbstractAlgorithm
{
	protected int    n_iter;
	protected double d_alpha;
	protected double d_delta;
	
	/**
	 * @param alpha the learning rate.
	 * @param delta the smoothing denominator.
	 */
	public AdaGrad(int iter, double alpha, double delta)
	{
		n_iter  = iter;
		d_alpha = alpha;
		d_delta = delta;
	}
	
	@Override
	public double[] getWeight(AbstractTrainSpace space, int numThreads)
	{
		double[] weights = new double[space.getFeatureSize() * space.getLabelSize()];
		
		updateWeight(space, weights);
		return weights;
	}
	
	public void updateWeight(AbstractTrainSpace space)
	{
		updateWeight(space, space.getModel().getWeights());
	}
	
	public void updateWeight(AbstractTrainSpace space, double[] weights)
	{	
		final int D = space.getFeatureSize();
		final int L = space.getLabelSize();
		final int N = space.getInstanceSize();
		double[] gs = new double[D*L];
		
		IntArrayList        ys = space.getYs();
		ArrayList<int[]>    xs = space.getXs();
		ArrayList<double[]> vs = space.getVs();
		
		Pair<IntPrediction,IntPrediction> ps;
		IntPrediction fst, snd;
		int i, j, sum;
		double acc;
		
		int      yi;
		int[]    xi;
		double[] vi = null;

	/*	List<IntPrediction> pss;
		IntPrediction cur;
		IntArrayList yns;
		int   k, max;*/
		
		for (i=0; i<n_iter; i++)
		{
			sum = 0;
			
			for (j=0; j<N; j++)
			{
				yi = ys.get(j);
				xi = xs.get(j);
				if (space.hasWeight())	vi = vs.get(j);
				
				ps  = getPredictions(L, xi, vi, weights);
				fst = ps.o1;
				snd = ps.o2;
				
				if (fst.label == yi)
				{
					if (fst.score - snd.score < 1)
					{
						updateCounts (L, gs, yi, snd.label, xi, vi);
						updateWeights(L, gs, yi, snd.label, xi, vi, weights);
					}
					else
						sum++;
				}
				else
				{
					updateCounts (L, gs, yi, fst.label, xi, vi);
					updateWeights(L, gs, yi, fst.label, xi, vi, weights);
				}
				
			/*	pss = getSortedPredictions(L, xi, vi, weights);
				yns = new IntArrayList();
				cur = pss.get(yi);
				
				for (k=0; k<L; k++)
				{
					if (k != yi)
					{
						snd = pss.get(k);
						
						if (cur.score - snd.score < 1)
							yns.add(snd.label);						
					}
				}
				
				max = UTHppc.max(yns);*/
			}
			
			acc = 100d * sum / N;
			System.out.printf("- %3d: acc = %7.4f\n", i+1, acc);
			
			if (acc >= 97)	break;
		}
	}
	
	protected Pair<IntPrediction,IntPrediction> getPredictions(int L, int[] x, double[] v, double[] weights)
	{
		double[] scores = new double[L];
		int i, label, size = x.length;
		
		if (v != null)
		{
			for (i=0; i<size; i++)
				for (label=0; label<L; label++)
					scores[label] += weights[getWeightIndex(L, label, x[i])] * v[i];
		}
		else
		{
			for (i=0; i<size; i++)
				for (label=0; label<L; label++)
					scores[label] += weights[getWeightIndex(L, label, x[i])];
		}
		
		IntPrediction fst, snd;
		
		if (scores[0] > scores[1])
		{
			fst = new IntPrediction(0, scores[0]);
			snd = new IntPrediction(1, scores[1]);
		}
		else
		{
			fst = new IntPrediction(1, scores[1]);
			snd = new IntPrediction(0, scores[0]);
		}
		
		for (label=2; label<L; label++)
		{
			if (fst.score < scores[label])
			{
				snd.set(fst.label, fst.score);
				fst.set(label, scores[label]);
			}
			else if (snd.score < scores[label])
				snd.set(label, scores[label]);
		}
		
		return new Pair<IntPrediction,IntPrediction>(fst, snd);
	}
	
	protected List<IntPrediction> getSortedPredictions(int L, int[] x, double[] v, double[] weights)
	{
		List<IntPrediction> ps = new ArrayList<IntPrediction>();
		int i, label, size = x.length;
		
		if (v != null)
		{
			for (i=0; i<size; i++)
				for (label=0; label<L; label++)
					ps.add(new IntPrediction(i, weights[getWeightIndex(L, label, x[i])] * v[i]));
		}
		else
		{
			for (i=0; i<size; i++)
				for (label=0; label<L; label++)
					ps.add(new IntPrediction(i, weights[getWeightIndex(L, label, x[i])]));
		}
		
	//	Collections.sort(ps);
		return ps;
	}
	
	protected void updateCounts(int L, double[] gs, int yp, int yn, int[] x, double[] v)
	{
		int i, len = x.length;
		
		if (v != null)
		{
			double d;
			
			for (i=0; i<len; i++)
			{
				d = v[i] * v[i];
				
				gs[getWeightIndex(L, yp, x[i])] += d;
				gs[getWeightIndex(L, yn, x[i])] += d;
			}
		}
		else
		{
			for (i=0; i<len; i++)
			{
				gs[getWeightIndex(L, yp, x[i])]++;
				gs[getWeightIndex(L, yn, x[i])]++;
			}
		}
	}
	
	protected void updateCounts(int L, double[] gs, int yp, IntArrayList yns, int[] x, double[] v)
	{
		int i, len = x.length;
		
		if (v != null)
		{
			double d;
			
			for (i=0; i<len; i++)
			{
				d = v[i] * v[i];

				gs[getWeightIndex(L, yp, x[i])] += d;
				for (IntCursor cur : yns) gs[getWeightIndex(L, cur.value, x[i])] += d;
			}
		}
		else
		{
			for (i=0; i<len; i++)
			{
				gs[getWeightIndex(L, yp, x[i])]++;
				for (IntCursor cur : yns) gs[getWeightIndex(L, cur.value, x[i])]++;
			}
		}
	}
	
	protected void updateWeights(int L, double[] gs, int yp, int yn, int[] x, double[] v, double[] weights)
	{
		int i, len = x.length;
		double[] cp = new double[len];
		double[] cn = new double[len];
		
		for (i=0; i<len; i++)
		{
			cp[i] =  d_alpha / (d_delta + Math.sqrt(gs[getWeightIndex(L, yp, x[i])]));
			cn[i] = -d_alpha / (d_delta + Math.sqrt(gs[getWeightIndex(L, yn, x[i])]));
		}
		
		if (v != null)
		{
			for (i=0; i<len; i++)
			{
				weights[getWeightIndex(L, yp, x[i])] += cp[i] * v[i];
				weights[getWeightIndex(L, yn, x[i])] += cn[i] * v[i];
			}
		}
		else
		{
			for (i=0; i<len; i++)
			{
				weights[getWeightIndex(L, yp, x[i])] += cp[i];
				weights[getWeightIndex(L, yn, x[i])] += cn[i];
			}
		}
	}
	
	protected void updateWeights(int L, double[] gs, int yp, IntArrayList yns, int[] x, double[] v, double[] weights)
	{
		int i, len = x.length;
		double[] cp = new double[len];
		double[] cn = new double[len];
		
		for (i=0; i<len; i++)
		{
			cp[i] =  d_alpha / (d_delta + Math.sqrt(gs[getWeightIndex(L, yp, x[i])]));
			
			for (IntCursor cur : yns)
				cn[i] = -d_alpha / (d_delta + Math.sqrt(gs[getWeightIndex(L, cur.value, x[i])]));
		}
		
		if (v != null)
		{
			for (i=0; i<len; i++)
			{
				weights[getWeightIndex(L, yp, x[i])] += cp[i] * v[i];
				
				for (IntCursor cur : yns)
					weights[getWeightIndex(L, cur.value, x[i])] += cn[i] * v[i];
			}
		}
		else
		{
			for (i=0; i<len; i++)
			{
				weights[getWeightIndex(L, yp, x[i])] += cp[i];
				
				for (IntCursor cur : yns)
					weights[getWeightIndex(L, cur.value, x[i])] += cn[i];
			}
		}
	}
	
	protected double getAccuracy(AbstractTrainSpace space, int N, int L, double[] weights)
	{
		IntArrayList        ys = space.getYs();
		ArrayList<int[]>    xs = space.getXs();
		ArrayList<double[]> vs = space.getVs();
		
		int      yi;
		int[]    xi;
		double[] vi = null;
		
		int i, sum = 0;
		
		for (i=0; i<N; i++)
		{
			yi = ys.get(i);
			xi = xs.get(i);
			if (space.hasWeight())	vi = vs.get(i);
			
			if (yi == getPredictions(L, xi, vi, weights).o1.label)
				sum++;
		}
		
		return 100d * sum / N;
	}
}
	