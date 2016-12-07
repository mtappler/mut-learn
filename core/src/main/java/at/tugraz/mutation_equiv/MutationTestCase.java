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
package at.tugraz.mutation_equiv;

import java.util.List;

import net.automatalib.words.impl.Symbol;

/**
 * A test case basically consisting of a sequence of inputs. Additionally, 
 * the indexes of the mutants killed by the test case are stored, as well as
 * a mutation score (ratio of killed mutants and overall number mutants)
 * 
 * @author Martin Tappler
 *
 */
public class MutationTestCase implements Comparable<MutationTestCase>, TestCase{
	public double score = 0;
	public List<Symbol> trace = null;
	public int[] killedMutants;
	public MutationTestCase(double score, List<Symbol> trace) {
		super();
		this.score = score;
		this.trace = trace;
	}
	public MutationTestCase(double score, List<Symbol> trace,List<Integer> killedMutants) {
		this(score,trace);
		this.killedMutants = new int[killedMutants.size()];
		for(int i = 0; i < killedMutants.size(); i++)
			this.killedMutants[i] = killedMutants.get(i);
	}
	@Override
	public int compareTo(MutationTestCase o) {
		// actually reversed comparison to get elements with highest score first by calling Stream.sorted()
		return score < o.score ? 1 : score > o.score ? -1 : 0;
	}
	
	public List<Symbol> getTrace() {
		return trace;
	}
}