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
package at.tugraz.mutation_equiv.configuration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import at.tugraz.mutation_equiv.TestCase;
import at.tugraz.mutation_equiv.mutation.MutationOperator;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSamplingStrategy;
import at.tugraz.mutation_equiv.mutation.sampling.impl.IdentitySampler;
import at.tugraz.mutation_equiv.test_selection.TestSelector;
import at.tugraz.mutation_equiv.trace_gen.TraceGenerator;

/**
 * Defines configuration parameters for the test-case generation technique.
 * 
 * @author Martin Tappler
 *
 * @param <T> type of test case
 */
public class RandomCovSelEquivConfiguration<T extends TestCase> {
	public List<MutationOperator> mutationOperators = new ArrayList<>();
	public int sizeTestSelectionSuite;
	public boolean reuseRemaining;
	public TestSelector<T> selector; 
	public TraceGenerator traceGen;
	public MutantSamplingStrategy mutantSampler = new IdentitySampler();
	public MutantSamplingStrategy mutantGenerationSampler = null;
	public String nameSuffix = "";
	public boolean keepExecutedTests = false;
	public List<Integer> initialSuiteSizes = new LinkedList<>();
	
	public RandomCovSelEquivConfiguration(){
		
	}
	public RandomCovSelEquivConfiguration(List<MutationOperator> mutationOperators, 
			int sizeTestSelectionSuite,
			boolean reuseRemaining, 
			TestSelector<T> selector, 
			TraceGenerator traceGen,
			MutantSamplingStrategy mutantSampler) {
		super();
		this.mutationOperators = mutationOperators;
		this.sizeTestSelectionSuite = sizeTestSelectionSuite;
		this.reuseRemaining = reuseRemaining;
		this.selector = selector;
		this.traceGen = traceGen;
		this.mutantSampler = mutantSampler;
	}
	
	public void updateRandomSeed(long seed){
		traceGen.updateRandomSeed(seed);
	}

	public String mutationDescription() {
		if(mutationOperators.isEmpty())
			return "none";
		else
			return String.join("_", mutationOperators.stream()
					.map(MutationOperator::description)
					.collect(Collectors.toList()));
	}

	public String shortMutationDescription() {
		if(mutationOperators.isEmpty())
			return "none";
		else
			return String.join("_", mutationOperators.stream()
					.map(MutationOperator::shortDescription)
					.collect(Collectors.toList()));
	}
}
