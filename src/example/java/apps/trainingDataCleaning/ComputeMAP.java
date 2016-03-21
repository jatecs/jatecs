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

package apps.trainingDataCleaning;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TShortObjectHashMap;
import it.cnr.jatecs.utils.Os;

import java.io.*;

public class ComputeMAP {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err
                    .println("Usage: ComputeMAP <perturbationFile_withoutExtension> <rankFile_withoutExtension>");
            return;
        }

        File file = new File(args[0]);
        String pertPath = file.getParentFile().getPath();
        String pertName = file.getName();

        file = new File(args[1]);
        String rankPath = file.getParentFile().getPath();
        String rankName = file.getName();

        TShortObjectHashMap<TIntHashSet> perts = new TShortObjectHashMap<TIntHashSet>();

        FileReader freader = new FileReader(pertPath + Os.pathSeparator()
                + pertName + ".txt");
        BufferedReader in = new BufferedReader(freader);
        String line;
        while ((line = in.readLine()) != null) {
            String[] fields = line.split("\t");
            int doc = Integer.parseInt(fields[0]);
            short cat = Short.parseShort(fields[1]);

            TIntHashSet set = (TIntHashSet) perts.get(cat);

            if (set == null) {
                set = new TIntHashSet();
                perts.put(cat, set);
            }

            set.add(doc);
        }
        in.close();

        TShortObjectHashMap<TIntArrayList> ranks = new TShortObjectHashMap<TIntArrayList>();

        freader = new FileReader(rankPath + Os.pathSeparator() + rankName
                + ".txt");
        in = new BufferedReader(freader);
        while ((line = in.readLine()) != null) {
            String[] fields = line.split("\t");
            int doc = Integer.parseInt(fields[0]);
            short cat = Short.parseShort(fields[1]);

            TIntArrayList vect = (TIntArrayList) ranks.get(cat);

            if (vect == null) {
                vect = new TIntArrayList();
                ranks.put(cat, vect);
            }

            vect.add(doc);
        }
        in.close();

        FileOutputStream fstream = new FileOutputStream(rankPath
                + Os.pathSeparator() + rankName + "_MAP.txt");
        PrintStream out = new PrintStream(fstream);
        double avg = 0.0;
        short[] cats = perts.keys();
        for (int i = 0; i < cats.length; ++i) {
            short cat = cats[i];
            TIntHashSet pert = (TIntHashSet) perts.get(cat);
            TIntArrayList rank = (TIntArrayList) ranks.get(cat);
            int total = rank.size();
            int perturbed = pert.size();
            int progress = 0;
            double sum = 0.0;
            for (int j = 0; j < total; ++j) {
                int doc = rank.getQuick(j);
                if (pert.contains(doc)) {
                    ++progress;
                    sum += ((double) progress) / (j + 1);
                }
            }
            assert (progress == perturbed);
            double AP = sum / perturbed;
            avg += AP;
            out.println(cat + "\t" + AP);
        }
        avg /= cats.length;
        out.println("avg\t" + avg);
        out.close();
        System.out.println(rankName + "_MAP\t" + avg);
    }
}
