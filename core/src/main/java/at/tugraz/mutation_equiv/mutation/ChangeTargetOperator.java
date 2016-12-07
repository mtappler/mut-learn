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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
 * The change target operator. 
 * 
 * The change output operator. 
 * It creates up to |S|-1 mutants for each transition t, i.e. it creates several mutants
 * for each pair of state and input.
 * If created for the state-input pair (s,i) the mutant changes into a state different
 * from the state the original model would reach. This may be up to |S|-1 different states,
 * as change may lead to any of the states. 
 * 
 *  However, it is possible to limit the maximum number of mutated inputs per state and
 *  the maximum number of target states for a state-transition pair.
 *  
 *  Since these mutants are hard to interpret, their usage is not recommended.
 * 
 * @author Martin Tappler
 *
 */
public class ChangeTargetOperator extends MutationOperator {

	// do not create too much of these mutants to avoid putting 
	// too much emphasis on the ChangeTarget operator
	private int maxDiffTargets = 2;
	private int maxDiffInputs = 2;
	private Random rndGen = new Random(1); 
	public ChangeTargetOperator(Alphabet<Symbol> inputAlphabet) {
		super(inputAlphabet);
	}
	public ChangeTargetOperator(Alphabet<Symbol> inputAlphabet, int maxDiffTargets, int maxDiffInputs) {
		super(inputAlphabet);
		this.maxDiffInputs = maxDiffInputs;
		this.maxDiffTargets = maxDiffTargets;
	}
	@Override
	protected MutantSample createMutantsAbstr(FastMealy<Symbol,String> machine) {
		Collection<FastMealyState<String>> allStates = machine.getStates();
		CompoundSample fullSample = new CompoundSample("operator:change-target");
		for(FastMealyState<String> s : allStates){
			CompoundSample stateSample = new CompoundSample("state");
			fullSample.addChild(stateSample);
			
			List<Symbol> shuffledInputs = new ArrayList<>(inputAlphabet);
			Collections.shuffle(shuffledInputs,rndGen);
			for(Symbol input : shuffledInputs.subList(0, Math.min(maxDiffInputs,shuffledInputs.size()))){
				List<FastMealyState<String>> shuffledStates = new ArrayList<>(allStates);
				Collections.shuffle(shuffledStates,rndGen);
				for(FastMealyState<String> target : shuffledStates.subList(0, 
						Math.min(maxDiffTargets,shuffledStates.size()))){
					if(target != machine.getSuccessor(s, input)){

						final MealyTransition<FastMealyState<String>,String> mutTransition = machine.getTransition(s, input);
						CriticalTransition critTrans = new CriticalTransition(mutTransition, false, s);
						MutantProducer aMutant = new MutantProducer(idGen++, critTrans,() -> {
							Pair<FastMealy<Symbol, String>,Map<Object,FastMealyState<String>>> newMachineAndMapping = copyMealyMachine(
									(MealyMachine<?,Symbol,?,String>)machine, inputAlphabet, Collections.singleton(mutTransition));
							FastMealy<Symbol, String> newMachine = newMachineAndMapping.getLeft();
							Map<Object, FastMealyState<String>> stateMapping = newMachineAndMapping.getRight();
							newMachine.addTransition(stateMapping.get(s), input, 
									new MealyTransition<FastMealyState<String>,String>(
											stateMapping.get(target),machine.getOutput(s, input)));
							return cast(newMachine);
						});
						stateSample.addChild(MutantSample.create(aMutant));
					}
				}
			}
		}
		if("true".equals(System.getProperty("ranked_random.debug")))
			System.out.println("Change target created "+ fullSample.getMutants().size() + " mutants in total");
		return fullSample;
	}
	@Override
	public String description() {
		return "change-target";
	}
	@Override
	public String shortDescription() {
		return description();
	}

}
