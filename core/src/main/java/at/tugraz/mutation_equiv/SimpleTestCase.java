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
package at.tugraz.mutation_equiv;

import java.util.List;

import net.automatalib.words.impl.Symbol;

/**
 * A simple test case solely consisting of a sequence of inputs.
 * 
 * @author Martin Tappler
 *
 */
public class SimpleTestCase implements TestCase {

	public List<Symbol> trace = null;
	public SimpleTestCase(List<Symbol> trace) {
		super();
		this.trace = trace;
	}
	@Override
	public List<Symbol> getTrace() {
		return trace;
	}

}
