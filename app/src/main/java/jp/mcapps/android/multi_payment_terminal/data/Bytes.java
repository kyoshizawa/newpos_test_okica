package jp.mcapps.android.multi_payment_terminal.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.util.McUtils;

public class Bytes implements Iterable<Byte>, Iterator<Byte> {
    private final List<Byte> _bytes = new ArrayList<>();
    private int _loopIndex = 0;

    public Bytes() {
    }

    public Bytes(int... args) {
        for (int b  : args) {
            _bytes.add((byte) b);
        }
    }

    public Bytes(Bytes... args) {
        for (Bytes bytes: args) {
            for (Byte b : bytes) {
                _bytes.add(b);
            }
        }
    }

    public Bytes(byte[]... args) {
        for (byte[] bytes: args) {
            for (Byte b : bytes) {
                _bytes.add(b);
            }
        }
    }

   private Bytes(List<Byte> bytes) {
        _bytes.addAll(bytes);
    }

    public byte get(int i) {
        return _bytes.get(i);
    }

    public Bytes add(int b) {
        _bytes.add((byte) b);

        return this;
    }

    public Bytes add(int... bytes) {
        for (int b  : bytes) {
            _bytes.add((byte) b);
        }

        return this;
    }

    public Bytes add(Bytes bytes) {
        for (Byte b  : bytes) {
            _bytes.add((byte) b);
        }

        return this;
    }

    public Bytes add(byte[] bytes) {
        for (Byte b  : bytes) {
            _bytes.add((byte) b);
        }

        return this;
    }

    public Bytes set(int i, int b) {
        _bytes.set(i, (byte) b);

        return this;
    }

    public Bytes subList(int from, int to) {
        return new Bytes(_bytes.subList(from, to));
    }

    public byte[] copyOfRange(int from, int to) {
        byte[] arr = new byte[_bytes.size()];

        for (int i = 0; i < _bytes.size(); i++) {
            arr[i] = _bytes.get(i);
        }

        return Arrays.copyOfRange(arr, from, to);
    }

    public int size() {
        return _bytes.size();
    }

    public byte[] toArray() {
        byte[] arr = new byte[_bytes.size()];

        for (int i = 0; i < _bytes.size(); i++) {
            arr[i] = _bytes.get(i);
        }

        return arr;
    }

    @Override
    public String toString() {
        return McUtils.bytesToHexString(this.toArray());
    }

    @Override
    public Iterator<Byte> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        boolean b = _loopIndex < _bytes.size();
        if (!b) {
            _loopIndex = 0;
        }
        return b;
    }

    @Override
    public Byte next() {

        byte ret = _bytes.get(_loopIndex);
        _loopIndex += 1;

        return ret;
    }
}
