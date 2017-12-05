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
import net.automatalib.commons.util.mappings.MutableMapping;
import net.automatalib.util.automata.Automata;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Symbol;

/**
 * This generators generates tests via randomised version of the partial W-method
 * It is based on the RandomWpMethodEQOracle class in the the develop branch
 * of LearnLib and was originally implemented by Joshua Moerman. 
 * (see in LearnLib repository
 * learnlib/eqtests/basic-eqtests/src/main/java/de/learnlib/eqtests/basic/RandomWpMethodEQOracle.java)
 * 
 * Basically, it generates test randomly selecting a state and consequently a
 * sequence leading to it. It then appends a middle sequence of some expected length
 * (which is geometrically distributed). Finally it chooses randomly a suffix from 
 * the set of global suffixes or from the set of local suffixes (as defined by the partial
 * W-method).
 * 
 * @author Martin Tappler
 *
 */
public class RandomWpTraceGenerator implements TraceGenerator {
    private final int minimalSize;
    private final int rndLength;
    private final Alphabet<Symbol> inputAlphabet;
    private Random rand = null;
	private ArrayList<Word<Symbol>> stateCover;
	private ArrayList<Symbol>  arrayAlphabet;
	private ArrayList<Word<Symbol>> globalSuffixes;
	private MutableMapping<Object, ArrayList<Word<Symbol>>>  localSuffixSets;
    
	public RandomWpTraceGenerator(int minimalSize, int rndLength, Alphabet<Symbol> inputAlphabet, 
			Random rand) {
		this.minimalSize = minimalSize;
        this.rndLength = rndLength;
        this.inputAlphabet = inputAlphabet;
        this.rand = rand;
	}
	@Override
	public List<List<Symbol>> generateTraces(int nrTraces, MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants) {
        stateCover = new ArrayList<>(hypothesis.size());
        Automata.cover(hypothesis, inputAlphabet, stateCover, null);

        // Then repeatedly from this for a random word
        arrayAlphabet = new ArrayList<>(inputAlphabet);

        // Finally we test the state with a suffix, sometimes a global one, sometimes local
        globalSuffixes = new ArrayList<>();
        Automata.characterizingSet(hypothesis, inputAlphabet, globalSuffixes);

        localSuffixSets = hypothesis.createStaticStateMapping();
        for (Object state : hypothesis.getStates()) {
            ArrayList<Word<Symbol>> suffixSet = new ArrayList<>();
            Automata.stateCharacterizingSet(hypothesis, inputAlphabet, state, suffixSet);
            localSuffixSets.put(state, suffixSet);
        }

		return TraceGenerator.super.generateTraces(nrTraces, hypothesis, mutants);
	}
	@Override
	public void updateRandomSeed(long seed) {
		rand = new Random(seed);
	}

	@Override
	public String description() {
		return String.format("random-wp(min-size=%d,rnd-len=%d)",minimalSize,rndLength);
	}

	@Override
	public List<Symbol> generateTrace(MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants) {

        WordBuilder<Symbol> wb = new WordBuilder<>(minimalSize + rndLength + 1);

        // pick a random state
        wb.append(stateCover.get(rand.nextInt(stateCover.size())));

        // construct random middle part (of some expected length)
        int size = minimalSize;
        while ((size > 0) || (rand.nextDouble() > 1 / (rndLength + 1.0))) {
            wb.append(arrayAlphabet.get(rand.nextInt(arrayAlphabet.size())));
            if (size > 0) size--;
        }

        // pick a random suffix for this state
        // 50% chance for state testing, 50% chance for transition testing
        if (rand.nextBoolean()) {
            // global
            if (!globalSuffixes.isEmpty()) {
                wb.append(globalSuffixes.get(rand.nextInt(globalSuffixes.size())));
            }
        } else {
            // local
            Object state2 = hypothesis.getState(wb);
            ArrayList<Word<Symbol>> localSuffixes = localSuffixSets.get(state2);
            if (!localSuffixes.isEmpty()) {
                wb.append(localSuffixes.get(rand.nextInt(localSuffixes.size())));
            }
        }
        List<Symbol> trace = new ArrayList<>();
        wb.forEach(trace::add);
        return trace;
	}
}
