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
package at.tugraz.mutation_equiv.trace_gen;

import java.util.ArrayList;
import java.util.List;
import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.impl.Symbol;

/**
 * Abstract base class for test-case generation.
 * 
 * It declares the basic functionality of test-case generators. A test-case generator
 * has (1) a description and (2) a seed for random generation of tests (for reproducibility of results)
 * and (3) it can generate tests.
 * 
 * @author Martin Tappler
 *
 */
public interface TraceGenerator {
	
	void updateRandomSeed(long seed);
	
	String description();
	
	List<Symbol> generateTrace(MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants);
	
	default List<List<Symbol>> generateTraces(int nrTraces,MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants){
		List<List<Symbol>> traces = new ArrayList<>(nrTraces);
		for(int i = 0; i < nrTraces; i++) traces.add(generateTrace(hypothesis,mutants));
		return traces;
	}
}
