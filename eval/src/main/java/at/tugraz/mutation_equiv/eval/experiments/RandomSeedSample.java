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
package at.tugraz.mutation_equiv.eval.experiments;

import java.util.Random;

/**
 * Util class providing statically chosen seeds for random number generation, to
 * have same random number/traces for different selection configurations in one
 * learning experiments. If <code>useFixedSeeds</code> is true, some previously
 * chosen seeds are used to have reproducible measurement results. However,
 * this works only to a limited extend as parallel computation affects test-case
 * generation which introduces some non-determinism.
 * @author Martin Tappler
 *
 */
public class RandomSeedSample {
	public static long[] seeds = null;

	public static long[] fixedSeeds = new long[] { 424271491869455350l, 3876661151609256396l, 4891061194604086035l,
			6083593393196384794l, 407645173041627067l, 2177122647153779394l, 
			275444125766290254l, 6724325726020334044l, 6919994675707731206l, 
			792858487688984612l, 2900381008119179268l, 7901306166753668562l, 
			3848535434972041196l, 1769775870242230738l, 1566364691520009086l, 
			3157604124192896880l, 8632003436304334185l, 2253002578648896337l, 
			7085778450070907064l, 1076169323759247046l, 2613229955376311884l, 
			136613831651615150l, 7781938786501729607l, 3175406881254582378l, 
			3836705272164508888l, 5424917895042231536l, 8759317094860034580l, 
			4684009181308741162l, 6097612817458983829l, 7780907218950100368l,
			1345997643600913002l, 5638494193506171368l, 4555963856814834120l, 
			8069554303727358606l, 9153039411110596581l, 742528579234364393l, 
			1011317996060056899l, 7889426122422570472l, 2326410284558429372l,
			4674639109234761149l, 2591958671886104494l, 1761829129177447604l, 
			8739970788054706508l, 8261287184968630902l, 2356949954958606960l, 
			4790987648243067832l, 2061329295945294693l,	7274055987823290652l, 
			4677529211982294369l, 4768648120224182225l };
	public static final boolean useFixedSeeds = true;
	static {
		if (useFixedSeeds) {
			seeds = fixedSeeds;
		} else {
			Random r = new Random(System.currentTimeMillis());
			seeds = new long[fixedSeeds.length];
			for (int i = 0; i < seeds.length; i++)
				seeds[i] = r.nextLong();
		}
	}
}
