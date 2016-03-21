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

package it.cnr.jatecs.indexing.corpus.cmc

class Category {
    def code = "";
    def parent = "";
    def name = "";
}


catsMap = [:]


extractOnlyUfficialCategory = { line ->
    regex = /.*<code origin="CMC_MAJORITY" type="ICD-9-CM">([0-9V]{1,3}([\.][0-9]+)?)<.*/;
    matcher = (line =~ regex);
    if (matcher.matches()) {
        code = matcher[0][1];
        vars = code.split("\\.");
        parent = "NULL";
        //println vars.size();
        if (!catsMap.containsKey(code)) {
            c = new Category();
            c.code = code;
            c.parent = parent;
            c.name = code;
            catsMap[code] = c;
        }
    }
}


extractOnlyCoderACategory = { line ->
    regex = /.*<code origin="COMPANY1" type="ICD-9-CM">([0-9V]{1,3}([\.][0-9]+)?)<.*/;
    matcher = (line =~ regex);
    if (matcher.matches()) {
        code = matcher[0][1];
        vars = code.split("\\.");
        parent = "NULL";
        //println vars.size();
        if (!catsMap.containsKey(code)) {
            c = new Category();
            c.code = code;
            c.parent = parent;
            c.name = code;
            catsMap[code] = c;
        }
    }
}


extractOnlyCoderBCategory = { line ->
    regex = /.*<code origin="COMPANY2" type="ICD-9-CM">([0-9V]{1,3}([\.][0-9]+)?)<.*/;
    matcher = (line =~ regex);
    if (matcher.matches()) {
        code = matcher[0][1];
        vars = code.split("\\.");
        parent = "NULL";
        //println vars.size();
        if (!catsMap.containsKey(code)) {
            c = new Category();
            c.code = code;
            c.parent = parent;
            c.name = code;
            catsMap[code] = c;
        }
    }
}


extractOnlyCoderCCategory = { line ->
    regex = /.*<code origin="COMPANY3" type="ICD-9-CM">([0-9V]{1,3}([\.][0-9]+)?)<.*/;
    matcher = (line =~ regex);
    if (matcher.matches()) {
        code = matcher[0][1];
        vars = code.split("\\.");
        parent = "NULL";
        //println vars.size();
        if (!catsMap.containsKey(code)) {
            c = new Category();
            c.code = code;
            c.parent = parent;
            c.name = code;
            catsMap[code] = c;
        }
    }
}



extractCategory = { line ->
    regex = /.*<code origin="(COMPANY1|COMPANY2|COMPANY3)" type="ICD-9-CM">([0-9V]{1,3}([\.][0-9]+)?)<.*/;
    matcher = (line =~ regex);
    if (matcher.matches()) {
        code = matcher[0][2];
        vars = code.split("\\.");
        parent = "NULL";
        //println vars.size();
        if (vars.size() == 2) {
            parent = vars[0];
            UpperParent = assignUpperLevelCodes(parent);
            if ((UpperParent != "") && (!catsMap.containsKey(parent))) {
                c = new Category();
                c.code = parent;
                c.parent = UpperParent;
                c.name = parent;
                catsMap[parent] = c;
            }
        } else {
            parent = assignUpperLevelCodes(code);
        }

        //println "origin="+matcher[0][1]+", code="+matcher[0][2]+", parent="+parent;


        if (!catsMap.containsKey(code)) {
            c = new Category();
            c.code = code;
            c.parent = parent;
            c.name = code;
            catsMap[code] = c;
        }

    }
}

extractOnlyUfficialHierarchicalCategory = { line ->
    regex = /.*<code origin="(CMC_MAJORITY)" type="ICD-9-CM">([0-9V]{1,3}([\.][0-9]+)?)<.*/;
    matcher = (line =~ regex);
    if (matcher.matches()) {
        code = matcher[0][2];
        vars = code.split("\\.");
        parent = "NULL";
        //println vars.size();
        if (vars.size() == 2) {
            parent = vars[0];
            UpperParent = assignUpperLevelCodes(parent);
            if ((UpperParent != "") && (!catsMap.containsKey(parent))) {
                c = new Category();
                c.code = parent;
                c.parent = UpperParent;
                c.name = parent;
                catsMap[parent] = c;
            }
        } else {
            parent = assignUpperLevelCodes(code);
        }

        //println "origin="+matcher[0][1]+", code="+matcher[0][2]+", parent="+parent;


        if (!catsMap.containsKey(code)) {
            c = new Category();
            c.code = code;
            c.parent = parent;
            c.name = code;
            catsMap[code] = c;
        }

    }
}


def String assignUpperLevelCodes(code) {
    try {
        int codeInt = Integer.parseInt(code);

        switch (codeInt) {
            case 1..139: return "Infectious";
            case 140..239: return "Neoplasms";
            case 240..279: return "Endocrine";
            case 280..289: return "Diseases of blood";
            case 290..319: return "Mental disorders";
            case 320..389: return "Diseases of nevous system";
            case 390..459: return "Diseases of circulatory system";
            case 460..519: return "Diseases of respiratory system";
            case 520..579: return "Diseases of digestive system";
            case 580..629: return "Diseases of genitourinary system";
            case 630..677: return "Complications";
            case 680..709: return "Diseases skin";
            case 710..739: return "Diseases musculoskeletal";
            case 740..759: return "Congenital anomalies";
            case 760..779: return "Newborn";
            case 780..799: return "Signs";
            case 800..999: return "Injury";
        }

    }
    catch (e) {
        return "Factors Influencing Health Status";
    }
}


File f = new File("/home/fagni/Personale/Progetti/corpus/CMC/2007ChallengeTrainData.xml");
f.eachLine(extractOnlyUfficialHierarchicalCategory);
for (i in catsMap) {
    parent = i.value.parent == "NULL" ? "_null_" : i.value.parent;
    println i.value.code + "\t" + parent + "\t" + i.value.name;
}