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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A mutant sample consisting of other samples. 
 * 
 * @author Martin Tappler
 *
 */
public class CompoundSample implements MutantSample {

	private String kind = "none";
	public CompoundSample(String kind) {
		super();
		this.kind = kind;
	}

	private List<MutantSample> children = new ArrayList<>();
	public void addChild(MutantSample child){
		children.add(child);
	}
	@Override
	public List<MutantProducer> getMutants() {
		return children.stream().flatMap(c -> 
		c.getMutants().stream()).collect(Collectors.toList());
	}

	@Override
	public List<MutantSample> getChildren() {
		return children;
	}

	@Override
	public String getKind() {
		return kind;
	}

}
