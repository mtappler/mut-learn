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
package at.tugraz.mutation_equiv.mutation.sampling;
/**
 * Interface for performing of mutant sampling. 
 * This is a simple technique the reduce the number of mutants.
 * (cf. Jia and Harman's Mutation Testing 
 * Survey "An Analysis and Survey of the Development of Mutation Testing" - 
 * doi: 10.1109/TSE.2010.62).
 * 
 * Basically a subset of mutants is randomly selected from a mutant sample 
 * while the remaining mutants are discarded.  
 * 
 * @author Martin Tappler
 *
 */
public interface MutantSamplingStrategy {
	MutantSample sample(MutantSample original);
	String description();
}
