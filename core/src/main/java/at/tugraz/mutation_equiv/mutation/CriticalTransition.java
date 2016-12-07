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

import java.util.List;
import java.util.Optional;

import net.automatalib.words.impl.Symbol;

/**
 * A class used for optimisations. 
 * Remark: This is a somewhat non-general solution to cost reduction but works for change output 
 * and split state.
 * 
 * All of the existing mutation operators create mutants by deleting exactly one transition
 * from the original mutation in addition to adding states and transitions. 
 * Hence, there exists one <em>critical</em> transition, i.e. the deleted, after the mutation
 * may be triggered. In other words, if the critical transition is not executed by the tests,
 * then the mutant need not be analysed as it is definitely not killed.
 * 
 * In case of change out, executing the transition definitely kills the mutants. For change 
 * target and split state, executing the transition and some additional transition <em>may</em>
 * kill the mutant. 
 * 
 * Additionally, there is a sequence of inputs for split state returned by <code>getDefKillingSucc()</code>
 * which definitely kill a mutant, if executed after the critical solution. 
 * 
 * As noted in comments, changes in the implementation may affect optimisations built upon this class.
 * 
 * @author Martin Tappler
 *
 */
public class CriticalTransition {
	private Object criticalTrans; 
	// this is somewhat brittle as we have to make sure that all 
	// mutants are derived from the exact same machine 
	// and evaluation must be done with the same machine as well
	// but it is efficient
	private boolean definitelyKilled = false;

	private Object preState = null;

	private Optional<List<Symbol>> defKillingSucc = Optional.empty();
	public CriticalTransition(Object criticalTrans, boolean definitelyKilled, Object preState) {
		super();
		this.criticalTrans = criticalTrans;
		this.definitelyKilled = definitelyKilled;
		this.preState = preState;
	}
	public Object getCriticalTrans() {
		return criticalTrans;
	}
	public void setCriticalTrans(Object criticalTrans) {
		this.criticalTrans = criticalTrans;
	}
	public boolean isDefinitelyKilled() {
		return definitelyKilled;
	}
	public void setDefinitelyKilled(boolean definitelyKilled) {
		this.definitelyKilled = definitelyKilled;
	}
	public Object getPreState(){
		return preState;
	}
	public Optional<List<Symbol>> getDefKillingSucc() {
		return defKillingSucc;
	}
	public void setDefKillingSucc(Optional<List<Symbol>> defKillingSucc) {
		this.defKillingSucc = defKillingSucc;
	}
	// copy without defKilling (it's optional - well actually that's not nice but quick)
	public CriticalTransition copy() {
		return new CriticalTransition(criticalTrans, definitelyKilled, preState);
	}
	
}
