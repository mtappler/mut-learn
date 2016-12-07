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

import java.util.List;
import java.util.Optional;
import java.util.Random;
import at.tugraz.mutation_equiv.equiv_check.EquivalenceChecker;
import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Symbol;

/**
 * A test-case generator which targets killing of randomly chosen mutants combined with random
 * prefixes and suffixes.
 * 
 * It generates tests consisting of three parts:
 * 1. a random word w of bounded length 
 * 2. a middle sequence k killing a mutant m which randomly chosen from the set of mutants, i.e.
 *    such that lambda_h(w ++ k) != lambda(w ++ k)
 * 	  If no such k exists, w is halved and we try again to find k. The number of tries is bounded
 * 	  by <code>maxTries</code>
 * 3. a random w' of bounded length
 * 
 * The result is w ++ k ++ w'.
 * 
 * @author Martin Tappler
 *
 */
public class MuDirectedRandomWordGenerator implements TraceGenerator{
	private Random randGen = null;
	private RandomWordGenerator randPrefGen = null;
	private RandomWordGenerator randSufGen = null;
	private EquivalenceChecker equivChecker = null;
	private int maxTries = 5;
	
	public MuDirectedRandomWordGenerator(int maxRandPreLen, int maxRandSufLen, Random randGen, 
			Alphabet<Symbol> inputAlphabet) {
		super();
		this.randGen = randGen;
		randPrefGen = new RandomWordGenerator(inputAlphabet, randGen, maxRandPreLen);
		randSufGen = new RandomWordGenerator(inputAlphabet, randGen, maxRandSufLen);
		this.equivChecker = new EquivalenceChecker(inputAlphabet,true);
	}

	@Override
	public List<Symbol> generateTrace(MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants) {
		List<Symbol> prefix = randPrefGen.generateTrace(hypothesis,mutants);
		List<Symbol> suffix = randSufGen.generateTrace(hypothesis,mutants);

		if(!mutants.isEmpty()){
			for(int tries = 0; tries < maxTries; tries ++) {
				MutantProducer chosenMutant = mutants.get(randGen.nextInt(mutants.size()));

				Optional<List<Symbol>> traceToKill = equivChecker.killMutant(
						chosenMutant, hypothesis,prefix);
		
				if(traceToKill.isPresent()){
					prefix.addAll(traceToKill.get());
					break;
				} else {
					if(prefix.isEmpty()){
						System.out.println("well that's odd");
						prefix = randPrefGen.generateTrace(hypothesis, mutants); // this should not happen, every mutant should be killable with empty prefix
					}else{
						prefix.subList(prefix.size()/2,prefix.size()).clear();
					}
				}
			}
		}
		prefix.addAll(suffix);
		return prefix;
	}


	@Override
	public void updateRandomSeed(long seed) {
		randGen = new Random(seed);
		randPrefGen.updateRandomSeed(seed);
		randSufGen.updateRandomSeed(seed);
	}


	@Override
	public String description() {
		return String.format("mu-directed-rand-word(pre=%d, suf=%d)",
				randPrefGen.getMaxTestLength(),
				randSufGen.getMaxTestLength());
	}
}
