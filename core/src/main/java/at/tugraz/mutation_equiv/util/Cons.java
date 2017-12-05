package at.tugraz.mutation_equiv.util;

import java.util.ArrayList;
import java.util.List;

import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

public class Cons<T> implements Trace<T>{
	private int size = 0;
	public Cons(T v, Trace<T> tail) {
		this.v = v;
		this.tail = tail;
		this.size = tail.size() + 1;
	}
	T v;
	Trace<T> tail;
	@Override
	public List<T> toList() {
		List<T> prefix = new ArrayList<>();
		prefix.add(v);
		return tail.toList(prefix);
	}
	@Override
	public List<T> toList(List<T> prefix) {
		prefix.add(v);
		return tail.toList(prefix);
	}
	@Override
	public int size() {
		return size;
	}
	@Override
	public Word<T> toWord() {
		WordBuilder<T> prefix = new WordBuilder<>(size());
		prefix.add(v);
		return tail.toWord(prefix);
	}
	@Override
	public Word<T> toWord(WordBuilder<T> prefix) {
		prefix.add(v);
		return tail.toWord(prefix);
	}
}