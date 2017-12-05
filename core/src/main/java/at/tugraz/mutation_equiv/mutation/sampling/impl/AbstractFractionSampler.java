/*******************************************************************************
 * mut-learn
 * Copyright (C) 2016 TU Graz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package at.tugraz.mutation_equiv.mutation.sampling.impl;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSamplingStrategy;

/**
 * Randomly selects a fraction of the number of mutants, whereby 
 * the fraction is 1/2^k, where k can be set to any double value. 
 * This choice of powers of 1/2 is not essential for sampling to work, but is 
 * rather flexible (as used in Rahul Gopinath's technical report 
 * "An Empirical Comparison of Mutant Selection Approaches"). 
 * 
 * @author Martin Tappler
 *
 */
public abstract class AbstractFractionSampler implements MutantSamplingStrategy {
	@Override
	public void updateSeed(long seed) {
		randGen = new Random(seed);
	}
	protected double powerOfOneHalf = 1;
	protected Random randGen = null;
	public AbstractFractionSampler(double powerOfOneHalf, Random randGen) {
		super();
		this.powerOfOneHalf = powerOfOneHalf;
		this.randGen = randGen;
	}
	protected List<MutantProducer> sampleList(List<MutantProducer> mutants){
		Collections.shuffle(mutants, randGen);
		int nrAfterSampling = (int)Math.round(mutants.size() / Math.pow(2, powerOfOneHalf)); 
		mutants.subList(nrAfterSampling, mutants.size()).clear();
		return mutants;
	}
}
