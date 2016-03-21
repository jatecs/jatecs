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

package it.cnr.jatecs.indexing.corpus.JRCAcquis;

import it.cnr.jatecs.utils.Os;

import java.io.*;

public class AlignmentDocumentSplitter {
    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Usage: " + AlignmentDocumentSplitter.class + " <alignementFile> <outpuDir>");
            System.exit(-1);
        }

        String alignmentFile = args[0];
        String outputDir = args[1];


        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(alignmentFile), "UTF-8"));

            String line = br.readLine();
            while (line != null) {
                int startIdx = line.indexOf("<div type=\"body\" n=\"");
                if (startIdx != -1) {
                    int start = startIdx + "<div type=\"body\" n=\"".length();
                    int end = line.indexOf("\"", start);
                    String id = new String(line.substring(start, end));
                    id = id.replace('/', '_');
                    id = id.replace('\\', '_');
                    id = id.replace('(', '_');
                    id = id.replace(")", "");

                    StringBuilder sb = new StringBuilder();
                    sb.append(line + "\n");
                    boolean done = false;
                    while (!done) {
                        String l = br.readLine();
                        sb.append(l + "\n");
                        start = l.indexOf("</div>");
                        if (start != -1) {
                            done = true;
                            continue;
                        }
                    }

                    File fi = new File(outputDir);
                    if (!fi.exists())
                        fi.mkdirs();

                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(fi.getAbsolutePath() + Os.pathSeparator() + id + ".xml"), "UTF-8"));
                    bw.write(sb.toString());
                    bw.close();
                }

                line = br.readLine();
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
