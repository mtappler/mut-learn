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
package at.tugraz.mutation_equiv.test_selection;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import at.tugraz.mutation_equiv.TestCase;
import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.impl.Symbol;

/**
 * Abstract class for test-case selection.
 * 
 * While this project mainly targets mutation-based test selection
 * (this class "is aware of" mutants for this reason), this 
 * class allows for convenient comparison of different selection 
 * strategies.  
 * 
 * Note that this class contains both a method for evaluation (to 
 * analyse individual coverage) and a method for the actual selection.
 * The selection returns an iterator rather than a set of tests to enable
 * lazy selection which may improve performance if selection is computationally
 * intensive.
 * 
 * @author Martin Tappler
 *
 * @param <T> type of test case, specific types may store meta data for selection
 */
public abstract class TestSelector<T extends TestCase> {

	protected int testSuiteSize = -1;
	// only implemented for MutationSuiteBasedSelector
	protected List<Integer> initialSizes = null;
	public TestSelector(int testSuiteSize, List<Integer> initialSuiteSizes) {
		super();
		this.testSuiteSize = testSuiteSize;
		this.initialSizes = initialSuiteSizes;
	}
	public TestSelector(int testSuiteSize) {
		this(testSuiteSize, Collections.emptyList());
	}

	protected List<MutantProducer> mutants = null;
	protected MealyMachine<Object,Symbol,Object,String> hypothesis = null;
	
	@SuppressWarnings("unchecked")
	public void 
	setCurrentMachines(List<MutantProducer> mutants, MealyMachine<?,Symbol,?,String> hypothesis,
			List<List<Symbol>> executedTests){
		this.mutants = mutants;
		this.hypothesis = (MealyMachine<Object,Symbol,Object,String>)hypothesis;
	}
	
	public abstract Iterator<List<Symbol>> select(List<T> tests);
	
	public abstract List<T> evaluate(List<List<Symbol>> tests);

	public void updateSuiteSize(int suiteSize) {
		testSuiteSize = suiteSize;
	}
	public void updateInitialSuiteSizes(List<Integer> suiteSizes) {
		initialSizes = suiteSizes;
	}
	public int getSuiteSize(){
		return testSuiteSize;
	}
	public abstract String description();
}
