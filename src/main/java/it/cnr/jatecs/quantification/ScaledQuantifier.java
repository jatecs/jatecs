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

package it.cnr.jatecs.quantification;

import gnu.trove.TShortDoubleHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.quantification.interfaces.IQuantifier;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import org.apache.log4j.Logger;

public class ScaledQuantifier implements IQuantifier {

    static Logger logger = Logger.getLogger(ScaledQuantifier.class);

    private IQuantifier internalQuantifier;
    private TShortDoubleHashMap TPRs;
    private TShortDoubleHashMap FPRs;
    private String namePrefix;

    public ScaledQuantifier(IQuantifier internalQuantifier,
                            TShortDoubleHashMap tPRs, TShortDoubleHashMap fPRs,
                            String namePrefix) {
        this.internalQuantifier = internalQuantifier;
        this.TPRs = tPRs;
        this.FPRs = fPRs;
        this.namePrefix = namePrefix;
    }

    @Override
    public Quantification quantify(IIndex index) {
        Quantification quantification = internalQuantifier.quantify(index);
        quantification.setName(namePrefix + "_" + quantification.getName());
        IShortIterator categories = index.getCategoryDB().getCategories();
        while (categories.hasNext()) {
            short category = categories.next();

            double tpr = TPRs.get(category);
            double fpr = FPRs.get(category);
            double internalQuantification = quantification
                    .getQuantification(category);

            double scaledQuantification = internalQuantification;
            if ((tpr - fpr) != 0) {
                scaledQuantification = (internalQuantification - fpr)
                        / (tpr - fpr);
            } else {
                logger.warn("tpr-ftp==0; using internal quantification.");
            }

            if (scaledQuantification < 0) {
                scaledQuantification = 0;
                logger.warn("Scaled quantification < 0; clipping it to 0.");
            }

            if (scaledQuantification > 1) {
                scaledQuantification = 1;
                logger.warn("Scaled quantification < 1; clipping it to 1.");
            }
            quantification.setQuantification(category, scaledQuantification);
        }
        return quantification;
    }

    public IQuantifier getInternalQuantifier() {
        return internalQuantifier;
    }

    public TShortDoubleHashMap getTPRs() {
        return TPRs;
    }

    public TShortDoubleHashMap getFPRs() {
        return FPRs;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    @Override
    public String getName() {
        return namePrefix + internalQuantifier.getName();
    }

}
