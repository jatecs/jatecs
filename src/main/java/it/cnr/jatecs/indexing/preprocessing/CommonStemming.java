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

package it.cnr.jatecs.indexing.preprocessing;

import java.util.List;
import java.util.Vector;

public abstract class CommonStemming implements Stemming {

    public List<String> applyStemming(List<String> features) {
        Vector<String> feats = new Vector<String>();

        for (int i = 0; i < features.size(); i++) {
            String feature = features.get(i).toLowerCase();
            String stemmed = stemFeature(feature);

			
			/*boolean number = false;
			try
			{
				Long.parseLong(stemmed);
				number = true;
			}
			catch(Exception e)
			{
				number = false;
			}
			
			if (!number)
			{
				feats.add(stemmed);
			}*/
            if (!stemmed.equals("blah"))
                feats.add(stemmed);

        }

        return feats;
    }

    /**
     * Stem the passed feature to its morphological root.
     *
     * @param feature The feature to stem.
     * @return The stemmed feature.
     */
    protected abstract String stemFeature(String feature);

}
