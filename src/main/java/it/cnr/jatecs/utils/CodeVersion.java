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

package it.cnr.jatecs.utils;

/**
 * @author Tiziano
 * @deprecated This class is deprecated. Use the new {@link SoftwareInfo} class
 * which contains more useful information.
 */
public class CodeVersion implements Comparable<CodeVersion> {

    private SoftwareInfo si;

    public CodeVersion(int major, int minor, int subminor) {

        si = new SoftwareInfo();
    }

    /**
     * Get the major number of this version object.
     *
     * @return The major number.
     */
    public int getMajor() {
        return si.getMajorVersion();
    }

    /**
     * Get the minor number of this version object.
     *
     * @return The minor number of this version object.
     */
    public int getMinor() {
        return si.getMinorVersion();
    }

    /**
     * Get the subminor number of this version object.
     *
     * @return The subminor version of this version object.
     */
    public int getSubminor() {
        return si.getSubminorVersion();
    }

    @Override
    public String toString() {
        String v = "" + getMajor() + "." + getMinor() + "." + getSubminor();
        return v;
    }

    public int compareTo(CodeVersion o) {
        if (getMajor() < o.getMajor())
            return -1;
        else if (getMajor() == o.getMajor()) {
            if (getMinor() < o.getMinor())
                return -1;
            else if (getMinor() == o.getMinor()) {
                if (getSubminor() < o.getSubminor())
                    return -1;
                else if (getSubminor() == o.getSubminor())
                    return 0;
                else
                    return 1;
            } else
                return 1;
        } else
            return 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CodeVersion))
            return false;

        CodeVersion o = (CodeVersion) obj;
        return (compareTo(o) == 0);
    }

}
