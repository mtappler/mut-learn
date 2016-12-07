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
import java.util.Random;

import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Symbol;

/**
 * This test case generator generates test with random length uniformly 
 * chosen from the interval [0..<code>maxTestLength</code>]. All inputs
 * of the test are also randomly chosen from the set of all inputs according
 * to a uniform distribution.
 * 
 * @author Martin Tappler
 *
 */
public class RandomWordGenerator implements TraceGenerator{
	private Alphabet<Symbol> inputAlphabet = null;
	private Random randGen = null;
	private int maxTestLength = -1;
	public int getMaxTestLength() {
		return maxTestLength;
	}
	public void setMaxTestLength(int maxTestLength) {
		this.maxTestLength = maxTestLength;
	}
	public RandomWordGenerator(Alphabet<Symbol> inputAlphabet, Random randGen, int maxTestLength){
		this.inputAlphabet = inputAlphabet;
		this.randGen  = randGen;
		this.maxTestLength = maxTestLength;
	}
	public Symbol createRandomInput() {
		return inputAlphabet.getSymbol(randGen.nextInt(inputAlphabet.size()));
	}
	@Override
	public List<Symbol> generateTrace(MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants) {
		if(maxTestLength == -1)
			return new ArrayList<>();
		int length = randGen.nextInt(maxTestLength  + 1);
		List<Symbol> test = new ArrayList<>();
		for(int i = 0; i < length; i++){
			test.add( createRandomInput());
		}
		return test;
	}
	@Override
	public void updateRandomSeed(long seed) {
		randGen = new Random(seed);
	}
	@Override
	public String description() {
		return String.format("rand-word(len=%d)",maxTestLength);
	}
}
