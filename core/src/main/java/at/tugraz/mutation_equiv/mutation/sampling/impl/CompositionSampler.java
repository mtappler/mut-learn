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

import java.util.Arrays;
import java.util.stream.Collectors;

import at.tugraz.mutation_equiv.mutation.sampling.MutantSample;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSamplingStrategy;

/**
 * This class composes several mutant samplers into a chain, applying
 * them consecutively on the result of preceding samplers.
 * 
 * @author Martin Tappler
 *
 */
public class CompositionSampler implements MutantSamplingStrategy{

	private MutantSamplingStrategy[] sampler = null;
	public CompositionSampler(MutantSamplingStrategy... sampler){
		this.sampler = sampler;
	}
	@Override
	public MutantSample sample(MutantSample original) {
		MutantSample result = original;
		for(MutantSamplingStrategy s : sampler){
			result = s.sample(result);
		}
		return result;
	}
	@Override
	public String description() {
		Iterable<String> innerDescription = Arrays.asList(sampler).stream()
				.map(MutantSamplingStrategy::description)
				.collect(Collectors.toList());
		return String.join("*", innerDescription);
	}

}
