package at.tugraz.mutation_equiv.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

public class Nil<T> implements Trace<T>{

	@Override
	public List<T> toList() {
		return new ArrayList<>();
	}

	@Override
	public List<T> toList(List<T> prefix) {
		Collections.reverse(prefix);
		return prefix;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Word<T> toWord() {
		return Word.epsilon();
	}

	@Override
	public Word<T> toWord(WordBuilder<T> prefix) {
		prefix.reverse();
		return prefix.toWord();
	}
	
}