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

import java.util.logging.Level;
import java.util.logging.LogManager;

public class JatecsLogger {

    public static StatusLogger _status = null;
    public static ExecutionLogger _notification = null;

    public static StatusLogger status() {
        if (_status == null) {
            StatusLogger status = new StatusLogger();
            _status = status;
            LogManager.getLogManager().addLogger(_status);
        }

        return _status;
    }

    public static ExecutionLogger execution() {
        if (_notification == null) {
            ExecutionLogger notif = new ExecutionLogger();
            _notification = notif;
            LogManager.getLogManager().addLogger(_notification);
        }

        return _notification;
    }

    /**
     * Disable Java logging API system.
     */
    public static void disableJavaLogging() {
        status().setLevel(Level.OFF);
        execution().setLevel(Level.OFF);
    }


    /**
     * Enable Java logging API system.
     */
    public static void enableJavaLogging() {
        status().setLevel(Level.ALL);
        execution().setLevel(Level.INFO);
    }

    /**
     * Disable Log4J logging API system.
     */
    public static void disableLog4JLogging() {
        status().getLog4JLogger().setLevel(org.apache.log4j.Level.OFF);
        execution().getLog4JLogger().setLevel(org.apache.log4j.Level.OFF);
    }

    /**
     * Enable Log4J logging API system.
     */
    public static void enableLog4JLogging() {
        status().getLog4JLogger().setLevel(org.apache.log4j.Level.ALL);
        execution().getLog4JLogger().setLevel(org.apache.log4j.Level.INFO);
    }
}
