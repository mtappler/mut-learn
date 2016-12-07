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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Utility class for unordered pairs, i.e. sets with one (pair with equal elements)
 * or two elements. See also (but with generated equals/hashCode): 
 * http://stackoverflow.com/questions/8905528/how-to-write-write-a-set-for-unordered-pair-in-java
 * 
 * @author Martin Tappler
 *
 * @param <T> Type of elements
 */
public class UnorderedPair<T> {
     private final Set<T> set;

     public UnorderedPair(T a, T b) {
          set = new HashSet<T>();
          set.add(a);
          set.add(b);
     }

     @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UnorderedPair<?> other = (UnorderedPair<?>) obj;
		if (set == null) {
			if (other.set != null)
				return false;
		} else if (!set.equals(other.set))
			return false;
		return true;
	}
     	
    public boolean isActualPair(){
    	return set.size() == 2;
    }
    public Pair<T,T> toOrderedPair(){
		Iterator<T> iter = set.iterator();
    	return new ImmutablePair<T, T>(iter.next(), iter.next());
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((set == null) ? 0 : set.hashCode());
		return result;
	}
}

