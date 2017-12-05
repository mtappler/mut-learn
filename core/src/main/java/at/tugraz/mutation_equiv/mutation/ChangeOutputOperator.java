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
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import at.tugraz.mutation_equiv.mutation.sampling.CompoundSample;
import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSample;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.automata.transout.impl.FastMealy;
import net.automatalib.automata.transout.impl.FastMealyState;
import net.automatalib.automata.transout.impl.MealyTransition;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Symbol;
/**
 * The change output operator. 
 * It creates one mutant for each transition t, i.e. for each pair of state and input. 
 * The mutant produces an output different from the original output at transition t.
 * It uses outputs from the original output, i.e. only works the model contains at least 
 * two outputs.
 *  
 * @author Martin Tappler
 *
 */
public class ChangeOutputOperator extends MutationOperator {

	public ChangeOutputOperator(Alphabet<Symbol> inputAlphabet){
		super(inputAlphabet);
	}

	@Override
	protected MutantSample createMutantsAbstr(FastMealy<Symbol,String> machine) {
		Collection<FastMealyState<String>> allStates = machine.getStates();
		CompoundSample fullSample = new CompoundSample("operator:change-output");
		for(FastMealyState<String> s : allStates){
			CompoundSample stateSample = new CompoundSample("change-out-state");
			fullSample.addChild(stateSample);
			
			for(Symbol input : inputAlphabet){ 
				for(String output : outputAlphabet) { 
					if(!output.equals(machine.getOutput(s, input))){

						final MealyTransition<FastMealyState<String>,String> mutTransition = machine.getTransition(s, input);
						final CriticalTransition critTrans = new CriticalTransition(mutTransition, true,s);
						
						MutantProducer aMutant = new MutantProducer(idGen++, critTrans,() -> {
							Pair<FastMealy<Symbol, String>,Map<Object,FastMealyState<String>>> newMachineAndMapping = copyMealyMachine(
									(MealyMachine<?,Symbol,?,String>)machine, inputAlphabet, Collections.singleton(mutTransition));
							FastMealy<Symbol, String> newMachine = newMachineAndMapping.getLeft();
							Map<Object, FastMealyState<String>> stateMapping = newMachineAndMapping.getRight();
							newMachine.addTransition(stateMapping.get(s), input, 
									new MealyTransition<FastMealyState<String>,String>(
											stateMapping.get(machine.getSuccessor(s,input)),
											output));
							return cast(newMachine);
						},true);
						stateSample.addChild(MutantSample.create(aMutant));
						break; 
						// create only mutant per transition as other mutants would be covered by the same trace
					}
				}
			}
		}
		if("true".equals(System.getProperty("ranked_random.debug")))
			System.out.println("Change output created "+ fullSample.getMutants().size() + " mutants in total");
		return fullSample;
	}

	@Override
	public String description() {
		return "change-output";
	}

	@Override
	public String shortDescription() {
		return description();
	}
	

}
