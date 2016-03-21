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
import gnu.trove.TShortDoubleIterator;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.quantification.interfaces.IQuantifier;
import it.cnr.jatecs.quantification.interfaces.IQuantifierDataManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ScaledQuantifierDataManager {

    private static final String ACCsuffix = "_rates";
    private IQuantifierDataManager internalQuantifierDataManager;

    public ScaledQuantifierDataManager(
            IQuantifierDataManager internalQuantifierDataManager) {
        this.internalQuantifierDataManager = internalQuantifierDataManager;
    }

    public void write(IStorageManager storageManager, String modelName,
                      IQuantifier quantifier) throws IOException {
        ScaledQuantifier scaledQuantifier = (ScaledQuantifier) quantifier;
        internalQuantifierDataManager.write(storageManager, modelName,
                scaledQuantifier.getInternalQuantifier());
        DataOutputStream output = new DataOutputStream(
                storageManager
                        .getOutputStreamForResource(modelName + ACCsuffix));
        output.writeUTF(scaledQuantifier.getNamePrefix());
        TShortDoubleHashMap fpr = scaledQuantifier.getFPRs();
        output.writeInt(fpr.size());
        TShortDoubleIterator iterator = fpr.iterator();
        while (iterator.hasNext()) {
            iterator.advance();
            output.writeShort(iterator.key());
            output.writeDouble(iterator.value());
        }
        TShortDoubleHashMap tpr = scaledQuantifier.getTPRs();
        output.writeInt(tpr.size());
        iterator = tpr.iterator();
        while (iterator.hasNext()) {
            iterator.advance();
            output.writeShort(iterator.key());
            output.writeDouble(iterator.value());
        }
        output.close();
    }

    public IQuantifier read(IStorageManager storageManager, String modelName)
            throws IOException {
        IQuantifier internalQuantifier = internalQuantifierDataManager.read(
                storageManager, modelName);
        DataInputStream reader = new DataInputStream(
                storageManager.getInputStreamForResource(modelName + ACCsuffix));
        String name = reader.readUTF();
        TShortDoubleHashMap fpr = new TShortDoubleHashMap();
        int count = reader.readInt();
        for (int i = 0; i < count; ++i) {
            short cat = reader.readShort();
            double value = reader.readDouble();
            fpr.put(cat, value);
        }
        TShortDoubleHashMap tpr = new TShortDoubleHashMap();
        count = reader.readInt();
        for (int i = 0; i < count; ++i) {
            short cat = reader.readShort();
            double value = reader.readDouble();
            tpr.put(cat, value);
        }
        reader.close();
        return new ScaledQuantifier(internalQuantifier, tpr, fpr, name);
    }
}
