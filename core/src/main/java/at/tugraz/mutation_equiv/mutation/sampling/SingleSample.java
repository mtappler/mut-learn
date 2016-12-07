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

import java.util.Collections;
import java.util.List;

/**
 * A mutant sample consisting of a single mutant.
 * 
 * @author Martin Tappler
 *
 */
public class SingleSample implements MutantSample {

	private MutantProducer actualMutant = null;

	public SingleSample(MutantProducer actualMutant) {
		super();
		this.actualMutant = actualMutant;
	}

	@Override
	public List<MutantProducer> getMutants() {
		return Collections.singletonList(actualMutant);
	}

	@Override
	public List<MutantSample> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String getKind() {
		return "single";
	}
}
