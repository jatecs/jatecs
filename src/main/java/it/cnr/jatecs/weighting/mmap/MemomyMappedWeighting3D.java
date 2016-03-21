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

package it.cnr.jatecs.weighting.mmap;

import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.weighting.interfaces.IWeighting3D;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.DoubleBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MemomyMappedWeighting3D implements IWeighting3D {

    private int _firstDim;
    private int _secondDim;
    private int _thirdDim;
    private String _name;
    private MappedByteBuffer _bytebuffer;
    private DoubleBuffer _mmap;
    private RandomAccessFile _file;
    private FileChannel _filechannel;

    public MemomyMappedWeighting3D() {
        reset();
    }

    private void reset() {
        _name = "no file";
        _bytebuffer = null;
        _mmap = null;
        _firstDim = 0;
        _secondDim = 0;
        _thirdDim = 0;
        _filechannel = null;
    }

    public double getWeight(int firstIndex, int secondIndex, int thirdIndex) {
        return _mmap
                .get((_firstDim * ((_secondDim * thirdIndex) + secondIndex))
                        + firstIndex);
    }

    public int getFirstDimensionSize() {
        return _firstDim;
    }

    public int getSecondDimensionSize() {
        return _secondDim;
    }

    public int getThirdDimensionSize() {
        return _thirdDim;
    }

    public String getName() {
        return _name;
    }

    public boolean isOpen() {
        if (_filechannel == null) {
            return false;
        }
        return _filechannel.isOpen();
    }

    public void open(String path, String filename, boolean overwrite)
            throws IOException {
        if (overwrite)
            throw new IOException("Can't overwrite: readonly access");
        Os.createDirectory(new File(path));
        String fullpath = path + Os.pathSeparator() + filename;
        _name = filename;
        File file = new File(fullpath);
        _file = new RandomAccessFile(file, "r");
        _filechannel = _file.getChannel();
        _bytebuffer = _filechannel.map(FileChannel.MapMode.READ_ONLY, 0,
                (int) _filechannel.size());
        _firstDim = _bytebuffer.getInt();
        _secondDim = _bytebuffer.getInt();
        _thirdDim = _bytebuffer.getInt();
        _mmap = _bytebuffer.asDoubleBuffer();
    }

    public void close() throws IOException {
        _filechannel.close();
        _file.close();
        reset();
    }
}
