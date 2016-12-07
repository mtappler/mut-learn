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

import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSample;

/**
 * Samples mutants by selecting a fraction of the overall number of mutants. 
 * 
 * @author Martin Tappler
 *
 */
public class OverallFractionSampler extends AbstractFractionSampler {

	public OverallFractionSampler(double powerOfOneHalf, Random randGen) {
		super(powerOfOneHalf,randGen);
	}
	@Override
	public MutantSample sample(MutantSample original) {
		List<MutantProducer> mutants = sampleList(original.getMutants());
		return MutantSample.create(mutants,"sampled");
	}
	@Override
	public String description() {
		return String.format("overall(fraction=1/(2^%f))",powerOfOneHalf);
	}
}
