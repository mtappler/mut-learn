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

import java.util.Iterator;
import java.util.List;

import at.tugraz.mutation_equiv.MutationTestCase;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Symbol;

/**
 * A simple form of mutation-based selection which simply selects those 
 * tests that individually achieve high coverage, i.e. implements only the third
 * step of <code>MutationSuiteBasedSelector</code>.
 * 
 * It also performs well in some case and may be a good choice if the 
 * number of mutants and the number of tests for selection is large. In such 
 * cases, greedy selection may be slow. 
 * 
 * @author Martin Tappler
 *
 */
public class MutationTestBasedSelector extends AbstractMutationTestSelector {

	public MutationTestBasedSelector(int testSuiteSize, Alphabet<Symbol> inputAlphabet) {
		super(testSuiteSize, inputAlphabet);
	}

	@Override
	public Iterator<List<Symbol>> select(List<MutationTestCase> tests) {
		Iterator<List<Symbol>> selectedTests = null;
		if(!mutantsWithIndexes.isEmpty()){	
			selectedTests = tests.stream()
					.sorted((o1, o2) -> o1.score < o2.score ? 1 : o1.score > o2.score ? -1 : 0)
					.limit(testSuiteSize).map(MutationTestCase::getTrace).iterator();			
		} else {
			selectedTests = tests.stream().limit(testSuiteSize).map(MutationTestCase::getTrace).iterator();
		}
		return selectedTests;
	}

	@Override
	public String description() {
		return "mutation-test-based";
	}

}
