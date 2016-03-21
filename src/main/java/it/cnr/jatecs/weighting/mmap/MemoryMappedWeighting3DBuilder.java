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
import it.cnr.jatecs.weighting.interfaces.IWeighting3DManager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.DoubleBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MemoryMappedWeighting3DBuilder implements IWeighting3DManager {

    private int _firstDim;
    private int _secondDim;
    private int _thirdDim;
    private String _name;
    private MappedByteBuffer _bytebuffer;
    private DoubleBuffer _mmap;
    private FileChannel _filechannel;
    private RandomAccessFile _raf;

    public MemoryMappedWeighting3DBuilder(int firstDimensionSize,
                                          int secondDimensionSize, int thirdDimensionSize) {
        _name = "no file";
        _firstDim = firstDimensionSize;
        _secondDim = secondDimensionSize;
        _thirdDim = thirdDimensionSize;
    }

    public MemoryMappedWeighting3DBuilder() {
        _name = "no file";
        _firstDim = 10;
        _secondDim = 10;
        _thirdDim = 10;
    }

    public IWeighting3D getWeighting() {
        return this;
    }

    public void setWeight(double weight, int firstIndex, int secondIndex,
                          int thirdIndex) {
        _mmap.put((_firstDim * ((_secondDim * thirdIndex) + secondIndex))
                + firstIndex, weight);
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

    public void setName(String name) {
        _name = name;
    }

    public boolean isOpen() {
        return _filechannel.isOpen();
    }

    private void create(File file) throws IOException {
        _raf = new RandomAccessFile(file, "rw");
        // 3 ints + (_1stDim * _2ndDim * 3rdDim) doubles
        _raf.setLength((_firstDim * _secondDim * _thirdDim * 8) + 12);
        _filechannel = _raf.getChannel();
        _bytebuffer = _filechannel.map(FileChannel.MapMode.READ_WRITE, 0,
                (int) _filechannel.size());
        _bytebuffer.position(12);
        _mmap = _bytebuffer.asDoubleBuffer();
    }

    public void open(String path, String filename, boolean overwrite)
            throws IOException {
        Os.createDirectory(new File(path));
        String fullpath = path + Os.pathSeparator() + filename;
        _name = filename;
        File file = new File(fullpath);
        if (file.exists()) {
            if (overwrite) {
                file.delete();
                create(file);
            } else {
                _raf = new RandomAccessFile(file, "rw");
                _filechannel = _raf.getChannel();
                _bytebuffer = _filechannel.map(FileChannel.MapMode.READ_WRITE,
                        0, (int) _filechannel.size());
                _firstDim = _bytebuffer.getInt();
                _secondDim = _bytebuffer.getInt();
                _thirdDim = _bytebuffer.getInt();
                _mmap = _bytebuffer.asDoubleBuffer();
            }
        } else
            create(file);
    }

    public void close() throws IOException {
        _bytebuffer.rewind();
        _bytebuffer.putInt(_firstDim);
        _bytebuffer.putInt(_secondDim);
        _bytebuffer.putInt(_thirdDim);
        _bytebuffer.force();
        _filechannel.close();
        _raf.close();
        _name = "no file";
    }
}
