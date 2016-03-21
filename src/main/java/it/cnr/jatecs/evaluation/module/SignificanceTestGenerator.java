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

package it.cnr.jatecs.evaluation.module;

import it.cnr.jatecs.evaluation.util.SignificanceTestDataGenerator;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class SignificanceTestGenerator extends JatecsModule {

    private IClassificationDB _gold;
    private IClassificationDB _system;
    private IShortIterator _validCategories;
    private String _outputDir;

    public SignificanceTestGenerator(IClassificationDB gold,
                                     IClassificationDB system, String outputDir) {
        this(gold, system, gold.getCategoryDB().getCategories(), outputDir);
    }

    public SignificanceTestGenerator(IClassificationDB gold,
                                     IClassificationDB system, IShortIterator validCategories,
                                     String outputDir) {
        super(null, SignificanceTestGenerator.class.getName());

        _gold = gold;
        _system = system;
        _validCategories = validCategories;
        _outputDir = outputDir;
    }

    @Override
    protected void processModule() {
        String path_s_test = _outputDir + Os.pathSeparator()
                + "micro_sign_test.csv";
        String path_S_test = _outputDir + Os.pathSeparator()
                + "macro_sign_test.csv";
        String path_p_test = _outputDir + Os.pathSeparator()
                + "proportions_test.csv";

        SignificanceTestDataGenerator.generate_s_test(path_s_test, _gold,
                _system, _validCategories);
        _validCategories.begin();
        SignificanceTestDataGenerator.generate_S_test(path_S_test, _gold,
                _system, _validCategories);
        _validCategories.begin();
        SignificanceTestDataGenerator.generate_p_test(path_p_test, _gold,
                _system, _validCategories);
    }

}
