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
package at.tugraz.mutation_equiv.equiv_check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import at.tugraz.mutation_equiv.util.Cons;
import at.tugraz.mutation_equiv.util.Nil;
import at.tugraz.mutation_equiv.util.Trace;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Symbol;
/**
 * An equivalence checker between Mealy machines. Its most important function 
 * <code>killMutant</code> tries to find a trace showing non-equivalence between
 * two Mealy machines after executing some input sequence. If there exists such a 
 * trace, it is return, otherwise <code>Optional.empty()</code>, i.e.  is returned. 
 * 
 * Note that it is not a fully-fledged equivalence checker, but intended for checking 
 * equivalence between some model and derived model. The main reason for this restriction
 * is that both models must use the same alphabet.
 * 
 * It is possible to use precompute trace in the original model, to speed-up equivalence
 * checking in certain cases.
 * 
 * @author Martin Tappler
 *
 */
public class EquivalenceChecker {

	private Alphabet<Symbol> inputAlphabet = null;
	private boolean precompute = false;
	private Object hypoReference = null;
	private Map<Object,Map<Object,Trace<Symbol>>> precompTraces = new HashMap<>();

	public EquivalenceChecker(Alphabet<Symbol> inputAlphabet, boolean precompute) {
		super();
		this.inputAlphabet = inputAlphabet;
		this.precompute = precompute;
	}
	public EquivalenceChecker(Alphabet<Symbol> inputAlphabet) {
		this(inputAlphabet,false);
	}
	
	public void precomputeTraces(MealyMachine<Object,Symbol,?,String> machine){
		Map<Object,Map<Object,Trace<Symbol>>> precomputedTraces = new HashMap<>();
		for(Object s : machine.getStates())
			precomputedTraces.put(s, precomputeTraces(machine,s));
		precompTraces = precomputedTraces;
	}

	private Map<Object, Trace<Symbol>> precomputeTraces(MealyMachine<Object, Symbol, ?, String> machine, Object startState) {
		Set<Object> visited = new HashSet<>();
		Map<Object,Trace<Symbol>> tracesFromStart = new HashMap<>();
		LinkedList<Pair<Object,Trace<Symbol>>> schedule = new LinkedList<>();
		schedule.add(new ImmutablePair<Object, Trace<Symbol>>(startState, 
				new Nil<Symbol>()));
		while(!schedule.isEmpty()){
			Pair<Object, Trace<Symbol>> current = schedule.remove();
			Object currentState = current.getLeft();
			Trace<Symbol> currentTrace = current.getRight();
			if(!tracesFromStart.containsKey(currentState))
				tracesFromStart.put(currentState, currentTrace);
			if(!visited.contains(currentState)){
				visited.add(currentState);
				for (Symbol input : inputAlphabet){				
					Object nextState = machine.getSuccessor(currentState, input);
					Trace<Symbol> traceToNext = new Cons<Symbol>(input,currentTrace);
					
					schedule.add(new ImmutablePair<>(nextState, traceToNext));
				}
			}
		}
		return tracesFromStart;
	}

	public Optional<List<Symbol>> killMutant(MutantProducer mutant,
			MealyMachine<Object, Symbol, ?, String> hypothesis, List<Symbol> initialTrace) {
		if(precompute && hypothesis != hypoReference){
			precomputeTraces(hypothesis);
			hypoReference = hypothesis;
		}
		// this works in the current implementation and causes large speed-up so it is used
		// change output are the only mutants with getCritTrans().isDefinitelyKilled() == true
		// for these it suffices to find a transition from the current state to the prestate (and if there is none
		// then the mutant can't be killed)
		// for split state mutants, however, this would not work, as they may be killed even if the
		// pre state of the critical transition is not reachable
		if(precompute && mutant.getCritTrans().isDefinitelyKilled()){
			Map<Object,Trace<Symbol>> traceFromInit = precompTraces.get(hypothesis.getState(initialTrace));
			Trace<Symbol> traceToPreCritical = traceFromInit.get(mutant.getCritTrans().getPreState());
			if(traceToPreCritical != null)
				initialTrace.addAll(traceToPreCritical.toList());
			else	
				return Optional.empty();
		}
		 if(mutant.getCritTrans().isDefinitelyKilled()){
			// if mutant is definitely killed by visiting the critical transition 
			// we may also just avoid creating the mutant altogether and find a path to the transition
			// actually this may also be an option for the latter case where the critical transition may be
			// visited without killing the mutant, i.e. the kill trace would be 
			// initialTrace ++ traceToTrans ++ trace to wrong output (keep in mind for future experiments)
//			int sizeBefore = initialTrace.size();
			Optional<List<Symbol>> result = 
					traceToTransition(hypothesis,initialTrace,mutant.getCritTrans().getCriticalTrans());	

			return result;
		 } else {
			 return killMutant(mutant.get(), hypothesis, initialTrace);
		 }
	}
	private Optional<List<Symbol>> traceToTransition(MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<Symbol> initialTrace, Object criticalTrans) {
		Object state = hypothesis.getSuccessor(hypothesis.getInitialState(), initialTrace);
		Set<Object> visited = new HashSet<>();
		LinkedList<Pair<Object,Trace<Symbol>>> schedule = new LinkedList<>();
		schedule.add(new ImmutablePair<Object, Trace<Symbol>>(state, 
				Trace.fromList(initialTrace)));
		while(!schedule.isEmpty()){
			Pair<Object, Trace<Symbol>> current = schedule.remove();
			Object currentState = current.getLeft();
			Trace<Symbol> currentTrace = current.getRight();
			if(!visited.contains(currentState)){
				visited.add(currentState);
				for (Symbol input : inputAlphabet){
					Object currTrans = hypothesis.getTransition(currentState, input);
					if(currTrans.equals(criticalTrans)){
						Trace<Symbol> finalTrace = new Cons<Symbol>(input,currentTrace);
						return Optional.of(finalTrace.toList());
					}
					else {
						Object nextState = hypothesis.getSuccessor(currentState, input);
						Trace<Symbol> traceToNext = new Cons<Symbol>(input,currentTrace);
						schedule.add(new ImmutablePair<>(nextState, traceToNext));
					} 
				}
			}
		}
		return Optional.empty();
	}

	public Optional<List<Symbol>> killMutant(MealyMachine<Object, Symbol,?, String> mutant,
			MealyMachine<Object, Symbol, ?, String> hypothesis, List<Symbol> initialTrace) {
		Object mutantState = mutant.getSuccessor(mutant.getInitialState(), initialTrace);
		Object hypState = hypothesis.getSuccessor(hypothesis.getInitialState(), initialTrace);
		Pair<Object,Object> initState = new ImmutablePair<Object, Object>(mutantState,hypState);
		Set<Pair<Object,Object>> visitedStates = new HashSet<>();
		LinkedList<Pair<Pair<Object,Object>, Trace<Symbol>>> schedule = new LinkedList<>();
		schedule.add(new ImmutablePair<>(initState, Trace.fromList(initialTrace)));
		return killMutant(mutant,hypothesis,schedule,visitedStates);
	}
	private Optional<List<Symbol>> killMutant(MealyMachine<Object, Symbol, ?, String> mutant,
			MealyMachine<Object, Symbol, ?, String> hypothesis, LinkedList<Pair<Pair<Object,Object>, 
			Trace<Symbol>>> schedule,
			Set<Pair<Object, Object>> visitedStates) {
		// a simple parallel breadth-first exploration of both Mealy machines
		while(!schedule.isEmpty()){
			Pair<Pair<Object, Object>, Trace<Symbol>> current = schedule.remove();
			Pair<Object, Object> currentState = current.getLeft();
			Trace<Symbol> currentTrace = current.getRight();
			if(!visitedStates.contains(currentState)){
				visitedStates.add(currentState);
				for (Symbol input : inputAlphabet){
					String outMut = mutant.getOutput(currentState.getLeft(),input);
					String outOrig = hypothesis.getOutput(currentState.getRight(),input);
					if(outMut.equals(outOrig)){
						Pair<Object,Object> nextState = new ImmutablePair<Object, Object>(
								mutant.getSuccessor(currentState.getLeft(), input), 
								hypothesis.getSuccessor(currentState.getRight(), input));
						Trace<Symbol> traceToNext = new Cons<Symbol>(input,currentTrace);
						
						schedule.add(new ImmutablePair<>(nextState, traceToNext));
					} else {
						Trace<Symbol> finalTrace = new Cons<Symbol>(input,currentTrace);
						return Optional.of(finalTrace.toList());
					}
				}
			}
		}
		return Optional.empty();
	}
	
	public List<List<Triple<String,String,String>>> allNonConformingTraces(
			MealyMachine<Object, Symbol, ?, String> sut,
			MealyMachine<Object, Symbol, ?, String> reference,
			Alphabet<Symbol> sutAlphabet, // need alphabets of both machines as some alphabet implementations like
			Alphabet<Symbol> referenceAlphabet){
		List<String> stringAlphabet = getStringAlphabet(referenceAlphabet);
		List<List<Triple<String,String,String>>> result = new ArrayList<>();
		Object sutState = sut.getInitialState();
		Object referenceState = reference.getInitialState();
		Set<Pair<Object,Object>> visitedStates = new HashSet<>();
		
		Pair<Object,Object> initState = new ImmutablePair<Object, Object>(sutState,referenceState);
		LinkedList<Pair<Pair<Object,Object>, Trace<Triple<String,String,String>>>> schedule = new LinkedList<>();
		schedule.add(new ImmutablePair<Pair<Object,Object>, Trace<Triple<String,String,String>>>
					(initState, new Nil<Triple<String,String,String>>()));
		
		
		while(!schedule.isEmpty()){
			Pair<Pair<Object, Object>, Trace<Triple<String,String,String>>> current = schedule.remove();
			Pair<Object, Object> currentState = current.getLeft();
			Trace<Triple<String,String,String>> currentTrace = current.getRight();
			if(!visitedStates.contains(currentState)){
				visitedStates.add(currentState);
				for (String input : stringAlphabet){
					String outSut = sut.getOutput(currentState.getLeft(),
							symbolForString(sutAlphabet,input));
					String outRef = reference.getOutput(currentState.getRight(),
							symbolForString(referenceAlphabet,input));
					Triple<String,String,String> traceElem = 
							new ImmutableTriple<String, String, String>(input, outSut, outRef);
					if(outSut.equals(outRef)){
						Pair<Object,Object> nextState = new ImmutablePair<Object, Object>(
								sut.getSuccessor(currentState.getLeft(), 
										symbolForString(sutAlphabet,input)), 
								reference.getSuccessor(currentState.getRight(), 
										symbolForString(referenceAlphabet,input)));
						Trace<Triple<String,String,String>> traceToNext = new Cons<>(traceElem,currentTrace);
						schedule.add(new ImmutablePair<>(nextState, traceToNext));
					} else {
						Trace<Triple<String,String,String>> finalTrace = new Cons<>(traceElem,currentTrace);
						result.add(finalTrace.toList());
					}
				}
			}
		}
		return result;
	}
	private Symbol symbolForString(Alphabet<Symbol> alphabet, String input) {
		for(Symbol s : alphabet)
			if(s.getUserObject().toString().equals(input))
				return s;
		return null;
	}
	private List<String> getStringAlphabet(Alphabet<Symbol> referenceAlphabet) {
		List<String> result = new ArrayList<>(referenceAlphabet.size());
		for(Symbol s : referenceAlphabet)
			result.add(s.getUserObject().toString());
		return result;
	}

}
