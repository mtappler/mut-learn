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
package at.tugraz.mutation_equiv.mutation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import at.tugraz.mutation_equiv.mutation.sampling.MutantSample;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.automata.transout.impl.FastMealy;
import net.automatalib.automata.transout.impl.FastMealyState;
import net.automatalib.automata.transout.impl.MealyTransition;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.SimpleAlphabet;
import net.automatalib.words.impl.Symbol;

/**
 * Base class for mutation operators, i.e. functions that transform a model 
 * into a set of mutated models. It provides basic functionality as creating
 * a partial copy of the original model (basis for mutants), implements simple
 * integer IDs and defines the actual mutant creation as abstract.
 * 
 * @author Martin Tappler
 *
 */
public abstract class MutationOperator {

	protected Alphabet<Symbol> inputAlphabet;
	protected Alphabet<String> outputAlphabet;
	protected static int idGen = 0;
	
	public static void resetIds(){
		idGen = 0;
	}
	
	public MutationOperator(Alphabet<Symbol> inputAlphabet) {
		this.inputAlphabet = inputAlphabet;
	}
	public MutantSample createMutants(FastMealy<Symbol,String> fastMachine){
		this.outputAlphabet = getOutputAlphabet(fastMachine);
		return createMutantsAbstr(fastMachine);
	}
	protected Alphabet<String> getOutputAlphabet(FastMealy<Symbol,String> hypothesis) {
		Collection<FastMealyState<String>> allStates = hypothesis.getStates();

		Stream<String> outputStream = inputAlphabet.stream().
				flatMap(i -> allStates.stream().map(s -> hypothesis.getOutput(s, i))).distinct();
		SimpleAlphabet<String> outputAlphabet = new SimpleAlphabet<>();
		outputStream.forEach(o -> outputAlphabet.add(o));
		return outputAlphabet;
	}
	protected abstract MutantSample createMutantsAbstr(FastMealy<Symbol,String> machine);

	public abstract String description();
	public abstract String shortDescription();
	
	@SuppressWarnings("unchecked")
	protected MealyMachine<Object,Symbol,?,String> cast(FastMealy<Symbol, String> machine){
		// that should be a save conversion
		return  (MealyMachine<Object,Symbol,?,String>) ((MealyMachine<?,Symbol,?,String>) machine);
	}

	public static Pair<FastMealy<Symbol, String>,Map<Object,FastMealyState<String>>> copyMealyMachine(
			MealyMachine<?,Symbol,?,String> machineUncasted, 
			Alphabet<Symbol> alphabet, Collection<Object> ignoreTrans){
		@SuppressWarnings("unchecked")
		MealyMachine<Object,Symbol,?,String> machine = (MealyMachine<Object,Symbol,?,String>) machineUncasted;		
		FastMealy<Symbol, String> copy = new FastMealy<>(alphabet);
		FastMealyState<String> initCopy = copy.addInitialState();
		Map<Object,FastMealyState<String>> oldToNewStates = new HashMap<>();
		oldToNewStates.put(machine.getInitialState(),initCopy);
		for(Object s : machine.getStates()){
			if(s != machine.getInitialState()) 
				oldToNewStates.put(s, copy.addState());
		};
		for(Object s : machine.getStates()){
			for(Symbol input : alphabet){
				Object trans = machine.getTransition(s, input);
				if(!ignoreTrans.contains(trans)){
					FastMealyState<String> succ = oldToNewStates.get(machine.getSuccessor(s, input));
					String output = machine.getOutput(s, input);
					copy.addTransition(oldToNewStates.get(s), input, new MealyTransition<>(succ, output));
				}
			}
		}
		
		return new ImmutablePair<FastMealy<Symbol,String>, Map<Object,FastMealyState<String>>>(copy, oldToNewStates);
	}

	public void setAccSequences(Map<FastMealyState<String>, List<Word<Symbol>>> accessSeqForStates) {
	}
}
