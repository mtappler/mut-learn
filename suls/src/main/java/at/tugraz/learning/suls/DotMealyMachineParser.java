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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.automata.transout.impl.compact.CompactMealy;
import net.automatalib.automata.transout.impl.compact.CompactMealyTransition;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.FastAlphabet;
import net.automatalib.words.impl.Symbol;


/**
 * Parser Graphviz-dot-files containing Mealy machines. The parser
 * assumes that the LearnLib's/AutomataLib's syntax is used.
 * 
 * @author Martin Tappler
 * 
 */
public class DotMealyMachineParser {
	
	private static class Transition{
		public Transition(String source, String target, String input, String output) {
			super();
			this.source = source;
			this.target = target;
			this.input = input;
			this.output = output;
		}
		String source;
		String target;
		String input;
		String output;
	}
	
	  Pattern statePattern = Pattern.compile("\\s*(s\\d+) \\[shape=\"circle\" label=\"s?\\d+\"\\];\\s*");
	  Pattern transitionPattern = Pattern.compile("\\s*(s\\d+) -> (s\\d+)\\s*\\[label=\"(.+?)\\s*/\\s*(.+)\"\\];\\s*");
	  Pattern startStatePattern = Pattern.compile("\\s*__start0 -> (s\\d+);\\s*");
	private Alphabet<Symbol> alphabet;
	
	public MealyMachine<?, Symbol, ?, String> parse(String dotFileName) {
		String line;
		List<String> lines = new ArrayList<>();
		try (
		    InputStream fis = new FileInputStream(dotFileName);
		    InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
		    BufferedReader br = new BufferedReader(isr);
		) {
		    while ((line = br.readLine()) != null) {
		    	lines.add(line);
		    }
			return parse(lines);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
	}

	private MealyMachine<?, Symbol, ?, String> parse(List<String> lines) {
		Stream<String> stateLines = lines.stream().filter(statePattern.asPredicate()); 
		Stream<String> transitionLines = lines.stream().filter(transitionPattern.asPredicate()); 
		String startState = lines.stream().filter(startStatePattern.asPredicate()).findFirst().get();
		List<String> stateNames = extractStateNames(stateLines);
		List<Transition> transitions = extractTransitions(transitionLines);
		alphabet = new FastAlphabet<>();
		transitions.stream().forEach(t -> addSymbol(t.input));
		
		Matcher startStateMatcher = startStatePattern.matcher(startState);
		String startStateName = null;
		if(startStateMatcher.matches())
			startStateName = startStateMatcher.group(1);
		CompactMealy<Symbol,String> mealyMachine = new CompactMealy<>(alphabet);

		Map<String,Integer> stateMapping = new HashMap<>();
		
		Integer init = mealyMachine.addInitialState();
		stateMapping.put(startStateName, init);
		for(String s : stateNames){
			if(!s.equals(startStateName)){
				stateMapping.put(s, mealyMachine.addState());
			}
		}
		for(Transition t : transitions){			
			Symbol s = lookUpSymbol(t.input);
			mealyMachine.addTransition(stateMapping.get(t.source),s, 
					new CompactMealyTransition<String>(stateMapping.get(t.target), t.output));
		}
		return mealyMachine;
	}

	private Symbol lookUpSymbol(String input) {
		for(Symbol inputSymbol : alphabet){
			if(inputSymbol.getUserObject().equals(input))
				return inputSymbol;
		}
		return null;
	}

	private void addSymbol(String input) {
		if(lookUpSymbol(input) != null)
			return;
		int id = alphabet.size();
		Symbol s = new Symbol(input);
		s.setId(id);
		alphabet.add(s);
	}

	private List<Transition> extractTransitions(Stream<String> transitionLines) {
		return transitionLines.map(line -> { 
			Matcher m = transitionPattern.matcher(line);
			if(m.matches())
				return new Transition(m.group(1), m.group(2), m.group(3), m.group(4));
			return null;
		}).collect(Collectors.toList());
	}

	private List<String> extractStateNames(Stream<String> stateLines) {
		return stateLines.map(line -> { 
			Matcher m = statePattern.matcher(line);
			if(m.matches())
				return m.group(1);
			else 
				return null;
		}).collect(Collectors.toList());
	}

	public Collection<? extends Symbol> getAlphabet() {
		return alphabet;
	}

}
