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

import java.util.List;

/**
 * A mutant sample, i.e. a set of mutants. The samples are hierarchically 
 * structured, so each sample may have children and it has a kind, which 
 * is used to give some meta information about the mutants contained in the sample.
 * 
 * @author Martin Tappler
 *
 */
public interface MutantSample {
	List<MutantProducer> getMutants();
	List<MutantSample> getChildren();
	String getKind();
	public static MutantSample create(MutantProducer producer){
		return new SingleSample(producer);
	}
	public static MutantSample create(List<MutantProducer> mutants, String kind){
		CompoundSample sample = new CompoundSample(kind);
		mutants.forEach(m -> sample.addChild(create(m)));
		return sample;
	}
}
