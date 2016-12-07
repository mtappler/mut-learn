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
package at.tugraz.mutation_equiv.eval;

import de.learnlib.api.SUL;
import de.learnlib.api.SULException;
import net.automatalib.words.impl.Symbol;

/**
 * Utility class for counting the actual number of steps executed by the system under learning.
 * Like LearnLib's ResetCounterSUL it also logs resets. 
 * 
 * I actually could have used LearnLib's SymbolCounterSUL combined with ResetCounterSUL, but
 * I was not aware of these classes.
 * 
 * @author Martin Tappler
 *
 */
public class StepCounterSUL implements SUL<Symbol,String>{
	private SUL<Symbol, String> actualSUL;
	private long stepCount = 0;
	private int resets = 0;

	public StepCounterSUL(SUL<Symbol, String> actualSUL){
		this.actualSUL = actualSUL;
	}
	@Override
	public void pre() {
		setResets(getResets() + 1);
		actualSUL.pre();
	}

	@Override
	public void post() {
		actualSUL.post();
	}

	@Override
	public String step(Symbol in) throws SULException {
		stepCount++;
		return actualSUL.step(in);
	}

	public long getStepCount() {
		return stepCount;
	}
	public void resetStepCount() {
		this.stepCount = 0;
	}
	public int getResets() {
		return resets;
	}
	public void setResets(int resets) {
		this.resets = resets;
	}
	
}