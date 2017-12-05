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
 * A mutant sampler which aims at achieving a more uniform distribution of
 * mutants from a given kind. Since it does provide a lot of benefits according
 * to experience, it's not used in the evaluation.
 * 
 * However, it works as follows, it looks for a specific kind of samples and
 * calculates the average number of mutants in the same kind at the same level
 * in the hierarchy. It then selects a subset of mutants from each samples of
 * the specified kind which may be as large as the average sample size.
 * 
 * @author Martin Tappler
 *
 */
public class ReduceToMeanSampler implements MutantSamplingStrategy {
	private String kindPattern = null;
	private Random randGen;

	public ReduceToMeanSampler(Random randGen, String kindPattern) {
		this.randGen = randGen;
		this.kindPattern = kindPattern;
	}

	@Override
	public MutantSample sample(MutantSample original) {
		if (kindInChildren(original)) {
			CompoundSample newSample = new CompoundSample(original.getKind());
			List<MutantSample> matchingChildren = original.getChildren().stream()
					.filter(s -> s.getKind().contains(kindPattern)).collect(Collectors.toList());
			original.getChildren().stream().filter(s -> !s.getKind().contains(kindPattern))
					.forEach(newSample::addChild);
			 int nrMutants = matchingChildren.stream().reduce(0,
			 (x,sample) -> x + sample.getMutants().size(), Integer::sum);
			 int mean = Math.round((float)nrMutants/matchingChildren.size());
			
			List<MutantProducer> sampledMatching = matchingChildren.stream()
					.flatMap(ms -> sampleList(ms.getMutants(), mean).stream()).collect(Collectors.toList());
			newSample.addChild(MutantSample.create(sampledMatching, "sampled"));
			return newSample;
		} else if (original instanceof SingleSample)
			return original;
		else {
			CompoundSample newSample = new CompoundSample(original.getKind());
			original.getChildren().stream().map(c -> sample(c)).forEach(newSample::addChild);
			return newSample;
		}
	}

	private List<MutantProducer> sampleList(List<MutantProducer> mutants, int mean) {
		Collections.shuffle(mutants, this.randGen);
		return mutants.subList(0, Math.min(mean, mutants.size()));

	}

	@Override
	public String description() {
		return String.format("reduce-to-mean(kind=%s)", kindPattern);
	}

	private boolean kindInChildren(MutantSample original) {
		return original.getChildren().stream().filter(s -> s.getKind().equals(kindPattern)).findAny().isPresent();
	}

	@Override
	public void updateSeed(long seed) {
		randGen = new Random(seed);
	}
}
