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
 * Created on 1-mar-2005
 *
 */
package it.cnr.jatecs.utils;

import java.util.Vector;
import java.util.logging.Logger;


/**
 * @author Tiziano Fagni
 */
public class TextualProgressBar {


    private int _previousStatus;

    private String _operation;


    private Vector<Logger> _loggers;


    /**
     * Indicate if the output of this progress bar is enabled or disabled.
     */
    private boolean _enabled;

    /**
     * Construct a new TextualProgressBar which by default
     * print operation progress informations on stdout.
     *
     * @param operation The string describing the operation showed
     *                  by this progress bar.
     */
    public TextualProgressBar(String operation) {
        _operation = operation;


        _enabled = true;

        // Empty loggers.
        _loggers = new Vector<Logger>();
        _loggers.add(JatecsLogger.status());
        reset();
    }


    /**
     * Enable or disable the output of this progress bar.
     *
     * @param enabled True if the output must be enabled, false otherwise.
     */
    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }


    public void reset() {
        _previousStatus = -1;
    }


    public void setOperation(String operation) {
        _operation = operation;

        reset();
    }


    public void signal(int status) {
        if (_enabled) {
            if (status != _previousStatus)
                signalOperationStatus(_operation, status, _previousStatus);
        }
    }


    protected void signalOperationStatus(String operation, int currentStatus, int previousStatus) {
        for (int i = previousStatus; i < currentStatus; i++)
            signalOperationStatus(operation, i);

        signalOperationStatus(operation, currentStatus);
    }

    protected void signalOperationStatus(String operation, int currentStatus) {
        String msg = "";

        if (currentStatus < 0)
            currentStatus = 0;

        if (currentStatus > 100)
            currentStatus = 100;

        if (currentStatus < _previousStatus)
            // Unfortunately can not decrement the status bar.
            currentStatus = _previousStatus;

        if (currentStatus == _previousStatus)
            return;

        if (currentStatus == 0) {
            msg += "<** Operation: " + operation + "\tProgress(%):\n";
            _previousStatus = 0;
        } else if (currentStatus == 100) {
            msg = "100\ndone **>\n";
            _previousStatus = 100;
        } else {
            if ((currentStatus % 10) == 0)
                msg += currentStatus;
            else if ((currentStatus % 2) == 0)
                msg += ".";

            // Change this if you want newline after %x (with x = 101)
            if (((currentStatus % 101) == 0))
                msg += "\n";

            _previousStatus = currentStatus;
        }

        if (!msg.equals("")) {

            for (int i = 0; i < _loggers.size(); i++)
                _loggers.get(i).info(msg);
        }
    }

}
