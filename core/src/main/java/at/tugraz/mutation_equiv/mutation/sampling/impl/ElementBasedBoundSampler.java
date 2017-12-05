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
import java.util.stream.Collectors;

import at.tugraz.mutation_equiv.mutation.sampling.CompoundSample;
import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSample;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSamplingStrategy;
import at.tugraz.mutation_equiv.mutation.sampling.SingleSample;

/**
 * Samples mutants on the basis of elements by selecting a random
 * sample which may not be larger than an bound. This
 * mean that we recurvisely traverse the sample hierarchy looking 
 * a for a specific kind  of sample which is then sampled. After
 * sampling the union with samples on the same level of hierarchy is formed.
 * In pseudo code: do children.map(sample).union recursively
 * 
 * Sampling at different levels of granularity is also discussed in 
 * Rahul Gopinath's technical report 
 * "An Empirical Comparison of Mutant Selection Approaches"
 * 
 * This is actually rarely used, though, as considering hierarchy did not result in
 * improvements of test case quality.
 * 
 * @author Martin Tappler
 *
 */
public class ElementBasedBoundSampler implements MutantSamplingStrategy{

	private String kind = null;
	private int bound;
	private Random randGen;
	public ElementBasedBoundSampler(int bound, Random randGen, String kind) {
		this.kind  = kind;
		this.bound = bound;
		this.randGen = randGen;
	}
	@Override
	public MutantSample sample(MutantSample original) {
		if(original instanceof SingleSample)
			return original;
		else if (kind.equals(original.getKind())){
			List<MutantProducer> sampledChildren = 
					original.getChildren().stream()
					.flatMap(c -> sample(c).getMutants().stream())
					.collect(Collectors.toList());
			Collections.shuffle(sampledChildren, randGen);
			if (sampledChildren.size() > bound)
				sampledChildren.subList(bound, sampledChildren.size()).clear();
			return MutantSample.create(sampledChildren,original.getKind());
		} else {
			CompoundSample result = new CompoundSample(original.getKind());
			original.getChildren().stream()
					.map(c -> sample(c))
					.forEach(result::addChild);
			return result;
		}
	}
	@Override
	public String description() {
		return String.format("element-based(bound=%d,kind=%s)",bound,kind);
	}
	@Override
	public void updateSeed(long seed) {
		randGen = new Random(seed);
	}

}
