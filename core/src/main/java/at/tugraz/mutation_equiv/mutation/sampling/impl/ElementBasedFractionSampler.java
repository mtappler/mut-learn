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

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import at.tugraz.mutation_equiv.mutation.sampling.CompoundSample;
import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSample;
import at.tugraz.mutation_equiv.mutation.sampling.SingleSample;

/**
 * Samples mutants on the basis of elements by selecting a fraction
 * of mutants (@see ElementBasedBoundSampler). 
 * 
 * @author Martin Tappler
 *
 */
public class ElementBasedFractionSampler extends AbstractFractionSampler {

	private String kind = null;
	public ElementBasedFractionSampler(double powerOfOneHalf, Random randGen, String kind) {
		super(powerOfOneHalf,randGen);
		this.kind  = kind;
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
			sampledChildren = sampleList(sampledChildren);
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
		return String.format("element-based(fraction=1/(2^%f),kind=%s)",powerOfOneHalf,kind);
	}

}
