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

import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecutionLogger extends Logger {

    private static org.apache.log4j.Logger _logger = org.apache.log4j.Logger.getLogger(ExecutionLogger.class);

    public ExecutionLogger() {
        super(ExecutionLogger.class.getName(), null);

        Handler h = new ConsoleHandler();
        h.setFormatter(new StatusOutputFormatter());
        addHandler(h);
        setUseParentHandlers(false);
        setLevel(Level.INFO);
    }

    public org.apache.log4j.Logger getLog4JLogger() {
        return _logger;
    }

    public void removeAllHandlers() {
        Vector<Handler> toRemove = new Vector<Handler>();
        Handler[] handlers = getHandlers();
        for (int i = 0; i < handlers.length; i++)
            toRemove.add(handlers[i]);

        for (int i = 0; i < toRemove.size(); i++)
            removeHandler(toRemove.get(i));
    }

    public void println(String msg) {
        String msgF = msg + Os.newline();
        info(msgF);
        _logger.info(msgF);

        Handler handlers[] = getHandlers();
        for (int i = 0; i < handlers.length; i++)
            handlers[i].flush();
    }

    public void print(String msg) {
        info(msg);
        _logger.info(msg);

        Handler handlers[] = getHandlers();
        for (int i = 0; i < handlers.length; i++)
            handlers[i].flush();
    }

}
