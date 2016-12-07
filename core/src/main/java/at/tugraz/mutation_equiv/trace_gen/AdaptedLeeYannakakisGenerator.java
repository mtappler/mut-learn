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
/*******************************************************************************
 * The Java-code in this file is based on the LearnLib equivalence-oracle 
 * implemented in Java by Joshua Moerman (available at 
 * https://gitlab.science.ru.nl/moerman/Yannakakis), but adapted to our setup. 
 * 
 * It was originally published under the MIT license by Joshua Moerman as the
 * copyright holder. The original copyright and permission notices are as 
 * follows:
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Joshua Moerman
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package at.tugraz.mutation_equiv.trace_gen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.util.graphs.dot.GraphDOT;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Symbol;

/**
 * This test-case generator actually an external test-case generator implemented by Joshua 
 * Moerman which is available at https://gitlab.science.ru.nl/moerman/Yannakakis. 
 * 
 * The test-case generators makes use of adaptive distinguishing sequences augmented
 * with additional separating sequences (for an in-depth description we refer to the
 * original implementation). In this test-case generator we only randomised test-case generation.
 *
 * 
 * To be able to use this test-case generator you have to download the external compile
 * and move it to the path given by <code>generatorPath</code>.
 * 
 * @author Martin Tappler
 *
 */
public class AdaptedLeeYannakakisGenerator implements TraceGenerator{
	private int seed = 0;
	private String prefixStrategy = "minimal";
	private int expectedRandInfix = 0;
	private ProcessBuilder pb = null;
	private Process process;
	private OutputStreamWriter processInput;
	private BufferedReader processOutput;
	private Alphabet<Symbol> inputAlphabet;
	private String generatorPath;
	
	public AdaptedLeeYannakakisGenerator(String generatorPath, String prefixStrategy, int expectedRandInfix, 
			Alphabet<Symbol> inputAlphabet){
		this.generatorPath = generatorPath;
		pb = new ProcessBuilder(generatorPath, "--prefix", 
				prefixStrategy, "--seed", Integer.toString(seed) ,
				"=", 
				"random", "0", Integer.toString(expectedRandInfix));
		this.prefixStrategy = prefixStrategy;
		this.expectedRandInfix = expectedRandInfix;
		this.inputAlphabet = inputAlphabet;
	}
	private void setupProcess() throws IOException {
		process = pb.start();
		processInput = new OutputStreamWriter(process.getOutputStream());
		processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
	}

	@Override
	public void updateRandomSeed(long seed) {
		this.seed = (int) seed;
		pb = new ProcessBuilder(generatorPath, "--prefix",prefixStrategy,
				"--seed", Integer.toString(this.seed) ,
				"=", 
				"random", "0", Integer.toString(expectedRandInfix));
	}

	@Override
	public String description() {
		return String.format("adapted-lee-yannakakis(exp-len=%d,prefix-strategy=%s)",
				expectedRandInfix,prefixStrategy);
	}

	private Symbol toSymbol(String input){
		for(Symbol s : inputAlphabet)
			if(s.getUserObject().equals(input))
				return s;
		
		return null;
	}
	private void closeAll() {
		try {
			process.destroy();
			process.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		processInput = null;
		processOutput = null;
	}
	
	@Override
	public List<List<Symbol>> generateTraces(int nrTraces, MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants) {
		try {
			setupProcess();
		} catch (IOException e) {
			throw new RuntimeException("Unable to start the external program: " + e);
		}

		List<List<Symbol>> allTraces = new ArrayList<>();
		try{
			GraphDOT.write(hypothesis, inputAlphabet, processInput);
			processInput.flush();
			String line;
			while ((line = processOutput.readLine()) != null && allTraces.size() < nrTraces) {
				// Read every string of the line, this will be a symbol of the input sequence.
				List<Symbol> trace = new ArrayList<>();
				Scanner s = new Scanner(line);
				while(s.hasNext())
					trace.add(toSymbol(s.next()));
				s.close();
				allTraces.add(trace);
			}
		} catch (IOException e) {
			throw new RuntimeException("Unable to communicate with the external program: " + e);
		} finally{
			closeAll();
		}
		if(process.isAlive()){
			System.err.println("ERROR> log> Done creating traces but process stream still active!");
			closeAll();
			throw new RuntimeException("Done creating traces but process stream still active!");
		}

		// If the program exited with a non-SIGTERM (143) value, something went wrong (for example a segfault)!
		int ret = process.exitValue();
		if(ret != 143){ // we kill it -> it will return with 143
			System.err.println("ERROR> log> Something went wrong with the process: return value = " + ret);
			closeAll();
			throw new RuntimeException("Something went wrong with the process: return value = " + ret);
		}

		return allTraces;
	}

	@Override
	public List<Symbol> generateTrace(MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants) {
		return generateTraces(1, hypothesis, mutants).get(0);
	}
}
