package at.tugraz.mutation_equiv.util;

import java.util.List;

import at.tugraz.mutation_equiv.equiv_check.EquivalenceChecker;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

/**
 * A simple sequence type providing Nil and Cons to save 
 * memory in precomputation.
 * 
 * toList and toWord reverse the Traces actually
 * 
 * @author Martin Tappler
 *
 */
public interface Trace<T>{
	List<T> toList();
	List<T> toList(List<T> prefix);
	static<T> Trace<T> fromList(List<T> trace){
		Trace<T> result = new Nil<T>();
		for(T s : trace){
			result = new Cons<T>(s,result);
		}
		return result;
	}
	int size();
	Word<T> toWord();
	Word<T> toWord(WordBuilder<T> prefix);
}