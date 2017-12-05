package at.tugraz.mutation_equiv;

import java.util.List;
import java.util.Map;

import net.automatalib.automata.transout.impl.FastMealy;
import net.automatalib.automata.transout.impl.FastMealyState;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Symbol;

public interface AccessSequenceProvider {

	Map<FastMealyState<String>,List<Word<Symbol>>> accessSeqForStates(FastMealy<Symbol, String> mealy);
}
