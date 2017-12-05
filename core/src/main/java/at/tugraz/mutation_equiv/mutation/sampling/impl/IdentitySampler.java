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

import at.tugraz.mutation_equiv.mutation.sampling.MutantSample;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSamplingStrategy;

/**
 * Sampling identity function, i.e. does not discard any mutant.
 * 
 * @author Martin Tappler
 *
 */
public class IdentitySampler implements MutantSamplingStrategy {

	@Override
	public MutantSample sample(MutantSample original) {
		return original;
	}

	@Override
	public String description(){
		return "identity";
	}

	@Override
	public void updateSeed(long seed) {
		
	}
}
