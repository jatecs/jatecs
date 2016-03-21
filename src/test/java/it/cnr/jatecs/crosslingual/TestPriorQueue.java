/*
 * This file is part of JaTeCS.
 *
 * JaTeCS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JaTeCS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JaTeCS.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The software has been mainly developed by (in alphabetical order):
 * - Andrea Esuli (andrea.esuli@isti.cnr.it)
 * - Tiziano Fagni (tiziano.fagni@isti.cnr.it)
 * - Alejandro Moreo Fernández (alejandro.moreo@isti.cnr.it)
 * Other past contributors were:
 * - Giacomo Berardi (giacomo.berardi@isti.cnr.it)
 */

package it.cnr.jatecs.crosslingual;

import java.util.PriorityQueue;
import java.util.TreeSet;

import org.junit.Test;

public class TestPriorQueue {

	@Test
	public void test() {
		TreeSet<WeigtedFeat> tree = new TreeSet<TestPriorQueue.WeigtedFeat>();
		PriorityQueue<WeigtedFeat> queue = new PriorityQueue<WeigtedFeat>();

		int f = 0;
		WeigtedFeat f1 = new WeigtedFeat(1.0, f++);
		WeigtedFeat f2 = new WeigtedFeat(2.0, f++);
		WeigtedFeat f3 = new WeigtedFeat(1.0, f++);
		WeigtedFeat f4 = new WeigtedFeat(0.0, f++);
		WeigtedFeat f5 = new WeigtedFeat(4.0, f++);
		tree.add(f1);

		tree.add(f3);
		tree.add(f4);
		tree.add(f5);
		tree.add(f2);

		queue.add(f5);
		queue.add(f1);
		queue.add(f2);
		queue.add(f3);
		queue.add(f4);

		System.out.println("Size " + tree.size());
		while (tree.size() > 0) {
			WeigtedFeat next = tree.pollLast();
			System.out.println(next.toString());
		}

		System.out.println("Size " + queue.size());
		while (queue.size() > 0) {
			WeigtedFeat next = queue.poll();
			System.out.println(next.toString());
		}

	}

	class WeigtedFeat implements Comparable<WeigtedFeat> {
		double weight;
		int feat;

		public WeigtedFeat(double w, int f) {
			weight = w;
			feat = f;
		}

		@Override
		public int compareTo(WeigtedFeat o) {
			return Double.compare(this.weight, o.weight);
		}

		public String toString() {
			return "(" + feat + ", w=" + weight + ")";
		}
	}

}
