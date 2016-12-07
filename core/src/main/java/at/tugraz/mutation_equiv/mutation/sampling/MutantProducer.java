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
package at.tugraz.mutation_equiv.mutation.sampling;

import java.util.function.Supplier;

import at.tugraz.mutation_equiv.mutation.CriticalTransition;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.impl.Symbol;

/**
 * A single mutant in our implementation with some additional information
 * such as the critical transtion (@see at.tugraz.mutation_equiv.mutation.CriticalTransition). 
 * 
 * Beside that it is used to lazily create mutants. This makes sense if large number of mutants
 * is created such that sampling is necessary. In these cases you would not want to eagerly
 * create all mutants and sample afterwards, but rather sample and then create mutants.
 * 
 * However, the critical transition contains a lot of information such that creation is
 * generally not necessary anyway.
 * 
 * @author Martin Tappler
 *
 */
public class MutantProducer implements Supplier<MealyMachine<Object,Symbol,?,String>>{
	private int id;
	private Supplier<MealyMachine<Object,Symbol,?,String>> actualSupplier = null;
	private CriticalTransition critTrans = null;
	private MealyMachine<Object,Symbol,?,String> memoized = null;
	private boolean dontMemoize = false; // to avoid instantiation if that would require too much memory
	@Override
	public MealyMachine<Object, Symbol, ?, String> get() {
		if(!dontMemoize && memoized == null){
			memoized = actualSupplier.get();
			return memoized;
		}
		else {
			return actualSupplier.get();
		}
	}
	public void create(){
		if(!dontMemoize && memoized == null){
			memoized = actualSupplier.get();
		} 
	}
	public MutantProducer(int id,CriticalTransition critTrans, 
			Supplier<MealyMachine<Object, Symbol, ?, String>> actualSupplier, boolean dontMemoize) {
		super();
		this.id = id;
		this.actualSupplier = actualSupplier;
		this.critTrans = critTrans;
		this.dontMemoize = dontMemoize;
	}
		
	public MutantProducer(int id,CriticalTransition critTrans, 
			Supplier<MealyMachine<Object, Symbol, ?, String>> actualSupplier) {
		this(id,critTrans,actualSupplier,false);
	}
	public CriticalTransition getCritTrans() {
		return critTrans;
	}
	public void setCritTrans(CriticalTransition critTrans) {
		this.critTrans = critTrans;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}


}
