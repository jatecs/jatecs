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

/*
 * Created on 1-dic-2004
 */
package it.cnr.jatecs.utils;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * @author Tiziano Fagni
 */
public class Os {

    /**
     * Get the newline string separator for current operating system.
     *
     * @return The newline string.
     */
    public static String newline() {
        return System.getProperty("line.separator");
    }

    /**
     * Get the path string separator for current operating system.
     *
     * @return The path string separator.
     */
    public static String pathSeparator() {
        return System.getProperty("file.separator");
    }

    public static boolean createDirectory(File path) {
        if (path.exists()) {
            if (path.isDirectory())
                return true;
            else
                return false;
        }
        return path.mkdirs();
    }

    /**
     * Delete a directory recursively.
     *
     * @param path The File object denoting the directory to remove.
     * @return True if the operation was successful, false otherwise.
     */
    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    if (!files[i].delete())
                        JatecsLogger.execution().println(
                                "Error deleting file "
                                        + files[i].getAbsolutePath());
                }
            }
        }
        return (path.delete());
    }

    /**
     * Release unused memory by calling the Java garbage collector and print on
     * stdout the status of memory before and after calling GC.
     */
    public static void releaseUnusedMemory() {
        releaseUnusedMemory(true);
    }

    /**
     * Release unused memory by calling the Java garbage collector and,
     * dependong on the value of "enableStdOut" parameter, print on stdout the
     * status of memory before and after calling GC.
     *
     * @param enableStdOut True if we must print the memory status on stdout, false
     *                     otherwise.
     */
    public static void releaseUnusedMemory(boolean enableStdOut) {
        // Call the garbage collector to hopefully release unused memeory.
        if (enableStdOut)
            JatecsLogger.status().print("Release unused memory...");

        long beforeGC = Runtime.getRuntime().freeMemory();
        System.gc();
        long afterGC = Runtime.getRuntime().freeMemory();

        if (enableStdOut)
            JatecsLogger.status().println(
                    "done. Memory free before GC: " + beforeGC
                            + " Memory free after GC: " + afterGC);
    }

    /**
     * Get the path of the jatecs temporary directory on this system.
     *
     * @return The path of the jatecs temporary directory on this system.
     */
    public static String getTemporaryDirectory() {
        String userName = System.getProperty("user.name");
        String tmpDir = System.getenv("temp");
        if (tmpDir != null && tmpDir.length() > 0)
            return tmpDir + Os.pathSeparator() + "jatecs-" + userName;
        tmpDir = System.getenv("TEMP");
        if (tmpDir != null && tmpDir.length() > 0)
            return tmpDir + Os.pathSeparator() + "jatecs-" + userName;
        tmpDir = System.getenv("tmp");
        if (tmpDir != null && tmpDir.length() > 0)
            return tmpDir + Os.pathSeparator() + "jatecs-" + userName;
        tmpDir = System.getenv("TMP");
        if (tmpDir != null && tmpDir.length() > 0)
            return tmpDir + Os.pathSeparator() + "jatecs-" + userName;
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows"))
            tmpDir = "c:\\windows\\temp";
        else
            tmpDir = "/tmp";
        return tmpDir + Os.pathSeparator() + "jatecs-" + userName;
    }

    public static boolean delete(final File file) {
        boolean ok = false;
        try {
            ok = file.delete();
        } catch (Exception e) {
            JatecsLogger.execution().info(
                    "file.delete(" + file.getName() + "): deletion failed: "
                            + e.toString());
        }
        return ok;
    }

    public static boolean move(File src, File dest) {
        if (!src.exists())
            return false;
        if (dest.exists())
            delete(dest);

        boolean renamed = false;

        // first try, rename
        try {
            renamed = src.renameTo(dest);

            if (renamed)
                return true;
        } catch (Exception e) {
            JatecsLogger.execution().info(
                    "Os.move(): renameTo() method failed, trying exec");
        }

        // second try, exec
        String command = "";

        String os = System.getProperty("os.name");
        if (os.equals("Windows 95") || os.equals("Windows 98")
                || os.equals("Windows Me") || os.equals("Windows NT")
                || os.equals("Windows 2000") || os.equals("Windows XP")
                || os.equals("Windows 2003") || os.equals("Windows 7") || os.equals("Windows 8") || os.equals("Windows NT (unknown)"))
            command = "move \"" + src.getAbsolutePath() + "\" \""
                    + dest.getAbsolutePath() + "\"";
        else
            command = "mv " + src.getAbsolutePath() + " "
                    + dest.getAbsolutePath();

        try {
            Process p = Runtime.getRuntime().exec(command);
            int status = p.waitFor();
            if (status >= 0)
                return true;
        } catch (Exception e) {
            JatecsLogger.execution().info("Os.move(): exec(): " + e.toString());
        }
        JatecsLogger.execution().info(
                "Os.move(): exec() method failed, trying data transfer");

        // last try, data transfer
        try {
            FileInputStream oFIS = new FileInputStream(src);
            FileOutputStream oFOS = new FileOutputStream(dest);
            byte[] oBuffer = new byte[1024 * 4];
            int n = 0;
            while ((n = oFIS.read(oBuffer)) != -1)
                oFOS.write(oBuffer, 0, n);
            oFIS.close();
            oFOS.close();
        } catch (Exception e) {
            JatecsLogger.execution().info("Os.move(): data transfer failed");
            JatecsLogger.execution().info("Os.move(): failed");
            return false;
        }

        return delete(src);
    }


    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] oBuffer = new byte[1024 * 4];
        int n = 0;
        while ((n = is.read(oBuffer)) != -1)
            os.write(oBuffer, 0, n);

    }


    public static boolean copy(File src, File dest) {
        if (!src.exists())
            return false;
        if (dest.exists())
            delete(dest);

        Os.createDirectory(dest.getParentFile());

        try {
            FileInputStream oFIS = new FileInputStream(src);
            FileOutputStream oFOS = new FileOutputStream(dest);
            byte[] oBuffer = new byte[1024 * 4];
            int n = 0;
            while ((n = oFIS.read(oBuffer)) != -1)
                oFOS.write(oBuffer, 0, n);
            oFIS.close();
            oFOS.close();
        } catch (Exception e) {
            JatecsLogger.execution().info("Os.copy(): data transfer failed");
            return false;
        }

        return true;
    }

    /**
     * Generate a double value string begininning from initial double value and
     * with the specified number of decimals numbers.
     *
     * @param v           The double value to trasform into string.
     * @param numDecimals The number of fixed decimals to include in the generated
     *                    string number.
     * @return The string number generated.
     */
    public static String generateDoubleString(double v, int numDecimals) {

        return String.format("%." + numDecimals + "f", v);


//		DecimalFormatSymbols fs = new DecimalFormatSymbols();
//		fs.setDecimalSeparator('.');
//		String format = "##0.";
//		for (int i = 0; i < numDecimals; i++)
//			format += "0";
//
//		DecimalFormat decformat = new DecimalFormat(format, fs);
//		return decformat.format(v).toString();
    }

    public static double generateDouble(double v, int numDecimals) {
        DecimalFormatSymbols fs = new DecimalFormatSymbols();
        fs.setDecimalSeparator('.');
        String format = "##0.";
        for (int i = 0; i < numDecimals; i++)
            format += "0";

        DecimalFormat decformat = new DecimalFormat(format, fs);
        return Double.parseDouble(decformat.format(v).toString());
    }
}
