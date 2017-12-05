package at.tugraz.mutation_equiv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.algorithms.lstargeneric.mealy.ExtensibleLStarMealy;
import net.automatalib.automata.transout.impl.FastMealy;
import net.automatalib.automata.transout.impl.FastMealyState;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Symbol;

public class LStarAccessSequenceProvider implements AccessSequenceProvider {

	public LStarAccessSequenceProvider(ExtensibleLStarMealy<Symbol, String> lstar) {
		super();
		this.lstar = lstar;
	}

	private ExtensibleLStarMealy<Symbol, String> lstar = null;

	@Override
	public Map<FastMealyState<String>, List<Word<Symbol>>> accessSeqForStates(FastMealy<Symbol, String> mealy) {
		Collection<? extends Word<Symbol>> prefixes = lstar.getObservationTable().getAllPrefixes();
//		System.out.println("Prefixes: " + prefixes.size());
		Map<FastMealyState<String>, List<Word<Symbol>>> accSequences = new HashMap<>();
		mealy.getStates().forEach(s -> accSequences.put(s, new ArrayList<>()));
		prefixes.stream().filter(p -> !p.isEmpty()).filter(prefix -> atMostOneLoop(prefix,mealy))
				.forEach(prefix -> accSequences.get(mealy.getSuccessor(mealy.getInitialState(), prefix)).add(prefix));
//		accSequences.values().forEach(p -> System.out.println(p.size()));
		return accSequences;
	}

	private boolean atMostOneLoop(Word<Symbol> prefix, FastMealy<Symbol, String> mealy) {
		FastMealyState<String> currentState = mealy.getInitialState();
		Map<FastMealyState<String>, Integer> stateCounts = new HashMap<>();
		stateCounts.put(currentState, 1);
		for(Symbol s: prefix){
			currentState = mealy.getSuccessor(currentState, s);
			if(stateCounts.containsKey(currentState)){
				stateCounts.put(currentState, stateCounts.get(currentState) + 1);
			} else{
				stateCounts.put(currentState, 1);
			}
		}
		return stateCounts.values().stream().allMatch(i -> i <= 2);
	}
}
