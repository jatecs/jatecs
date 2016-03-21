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
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.utils.interfaces.INameable;
import it.cnr.jatecs.utils.interfaces.INamed;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class Quantification implements INamed, INameable {

    public static double NOQUANTIFICATIONVALUE = -1;
    private TShortDoubleHashMap quantifications;
    private ICategoryDB categories;
    private String name;

    public Quantification(String name, ICategoryDB categoryDB) {
        this.name = name;
        quantifications = new TShortDoubleHashMap();
        categories = categoryDB;
    }

    public Quantification(String name, IClassificationDB classificationDB) {
        this.name = name;
        quantifications = new TShortDoubleHashMap();
        categories = classificationDB.getCategoryDB();
        IShortIterator iterator = categories.getCategories();
        while (iterator.hasNext()) {
            short category = iterator.next();
            quantifications.put(category,
                    classificationDB.getCategoryDocumentsCount(category)
                            / (double) classificationDB.getDocumentDB()
                            .getDocumentsCount());
        }
    }

    public IShortIterator getCategories() {
        return categories.getCategories();
    }

    public double getQuantification(short category) {
        if (quantifications.containsKey(category)) {
            return quantifications.get(category);
        }

        return NOQUANTIFICATIONVALUE;
    }

    public void setQuantification(short category, double value) {
        if (categories.isValidCategory(category)) {
            quantifications.put(category, value);
        }
    }

    public void removeQuantification(short category) {
        if (quantifications.contains(category)) {
            quantifications.remove(category);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
