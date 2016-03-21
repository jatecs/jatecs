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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class SoftwareInfo {

    private final String branch;
    private final String fullID;
    private final String shortID;
    private final String IDdescription;
    private final String buildUsername;
    private final String buildUsernameEmail;
    private final String buildTime;
    private final String commitUsername;
    private final String commitUsernameEmail;
    private final String commitMessageShort;
    private final String commitMessageFull;
    private final String commitTime;
    private final String version;
    private final int majorVersion;
    private final int minorVersion;
    private final int subminorVersion;
    private final String revisionVersion;
    private final String changeLog;

    public SoftwareInfo() {
        Properties properties = new Properties();
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(
                    "jatecs.properties");
            properties.load(getClass().getClassLoader().getResourceAsStream(
                    "jatecs.properties"));

            branch = properties.getProperty("jatecs.branch");
            fullID = properties.getProperty("jatecs.commit.id");
            shortID = properties.getProperty("jatecs.commit.id.abbrev");
            IDdescription = properties.getProperty("jatecs.commit.id.describe");
            buildUsername = properties.getProperty("jatecs.build.user.name");
            buildUsernameEmail = properties
                    .getProperty("jatecs.build.user.email");
            buildTime = properties.getProperty("jatecs.build.time");
            commitUsername = properties.getProperty("jatecs.commit.user.name");
            commitUsernameEmail = properties
                    .getProperty("jatecs.commit.user.email");
            commitMessageShort = properties
                    .getProperty("jatecs.commit.message.short");
            commitMessageFull = properties
                    .getProperty("jatecs.commit.message.full");
            commitTime = properties.getProperty("jatecs.commit.time");
            version = properties.getProperty("jatecs.version");
            String[] tags = version.trim().split("[\\.]|[\\-]");
            majorVersion = Integer.parseInt(tags[0]);
            minorVersion = Integer.parseInt(tags[1]);
            subminorVersion = Integer.parseInt(tags[2]);
            if (tags.length < 4) {
                revisionVersion = "";
            } else if (tags.length == 4 && tags[3].equals("SNAPSHOT")) {
                revisionVersion = "";
            } else
                revisionVersion = tags[3];

            is.close();

            is = getClass().getClassLoader().getResourceAsStream(
                    "changelog.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = reader.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line + "\n");
                line = reader.readLine();
            }
            changeLog = sb.toString();
            reader.close();

        } catch (Exception e) {
            throw new RuntimeException("Reading information about software", e);
        }
    }

    public String getFullID() {
        return fullID;
    }

    public String getShortID() {
        return shortID;
    }

    public String getIDdescription() {
        return IDdescription;
    }

    public String getBuildUsername() {
        return buildUsername;
    }

    public String getBuildUsernameEmail() {
        return buildUsernameEmail;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public String getCommitUsername() {
        return commitUsername;
    }

    public String getCommitUsernameEmail() {
        return commitUsernameEmail;
    }

    public String getCommitMessageShort() {
        return commitMessageShort;
    }

    public String getCommitMessageFull() {
        return commitMessageFull;
    }

    public String getCommitTime() {
        return commitTime;
    }

    public String getVersion() {
        return version;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public int getSubminorVersion() {
        return subminorVersion;
    }


    public String getRevisionVersion() {
        return revisionVersion;
    }

    public String getBranch() {
        return branch;
    }

    public String getChangeLog() {
        return changeLog;
    }
}
