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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import at.tugraz.mutation_equiv.MutationTestCase;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Symbol;

/**
 * Strictly speaking this is not test-case selector, but rather a test-case generator.
 * 
 * It ignores all pre-generated tests and generates a set of tests such that all mutants
 * are killed. To do that it does not make use randomisation. 
 * 
 * @author Martin Tappler
 *
 */
public class NonProbMutationSelector extends AbstractMutationTestSelector{

	
	public NonProbMutationSelector(int testSuiteSize, Alphabet<Symbol> inputAlphabet) {
		super(testSuiteSize,inputAlphabet);
	}

	@Override
	public List<MutationTestCase> evaluate(List<List<Symbol>> tests) {
		if(mutantsWithIndexes.isEmpty())
			return tests.stream().map(trace -> new MutationTestCase(0, trace)).collect(Collectors.toList());
		return new ArrayList<>();
	}

	@Override
	public Iterator<List<Symbol>> select(List<MutationTestCase> tests) {
		Iterator<List<Symbol>> selectedTests = null;
		if(mutantsWithIndexes.isEmpty()){
			selectedTests = tests.stream()
					.map(MutationTestCase::getTrace).limit(testSuiteSize).iterator();
		}
		else {
			Set<Integer> mutantIndexes = new HashSet<>(mutantsWithIndexes.keySet());
			List<Pair<Integer, MealyMachine<Object, Symbol, Object, String>>> mutantsAsPairList = 
					mutantsWithIndexes.entrySet().stream()
					.map(entry -> new ImmutablePair<>(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());
			selectedTests = killMutants(mutantsAsPairList,mutantIndexes)
					.stream().map(MutationTestCase::getTrace).iterator();
		}
//		System.out.println("Have " + selectedTests.size() + " tests");
		return selectedTests;
	}

	@Override
	public String description() {
		return "mutation-non-prob";
	}

}
