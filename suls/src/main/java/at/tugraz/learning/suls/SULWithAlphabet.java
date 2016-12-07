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
package at.tugraz.learning.suls;

import de.learnlib.api.SUL;
import net.automatalib.words.Alphabet;

/**
 * A utility interface implemented by SULs which explicitly provide an input alphabet.
 * @author Martin Tappler
 *
 * @param <I> type of inputs
 * @param <O> type of outputs
 */
public interface SULWithAlphabet<I,O> extends SUL<I, O>{
	/**
	 * Implementation note: if an implementation of FastAlphabet is returned
	 * make sure that this method always returns the same alphabet, whereby same
	 * means that reference equality is given the returned alphabet and the 
	 * contained input symbols.
	 * 
	 * @return an input alphabet
	 */
	public Alphabet<I> getAlphabet();
}
