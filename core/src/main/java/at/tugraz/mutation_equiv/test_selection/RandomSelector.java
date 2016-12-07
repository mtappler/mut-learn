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
import java.util.stream.Collectors;

import at.tugraz.mutation_equiv.SimpleTestCase;
import net.automatalib.words.impl.Symbol;

/**
 * Simple random selector, selects a random subset of fixed size.
 * It acts as a base line for comparison.
 * 
 * @author Martin Tappler
 *
 */
public class RandomSelector extends TestSelector<SimpleTestCase> {

	public RandomSelector(int testSuiteSize) {
		super(testSuiteSize);
	}

	@Override
	public Iterator<List<Symbol>> select(List<SimpleTestCase> tests) {
		return tests.stream().limit(testSuiteSize).map(SimpleTestCase::getTrace).iterator();
	}

	@Override
	public List<SimpleTestCase> evaluate(List<List<Symbol>> tests) {
		return tests.stream().map(trace -> new SimpleTestCase(trace)).collect(Collectors.toList());
	}

	@Override
	public String description() {
		return "random";
	}

}
