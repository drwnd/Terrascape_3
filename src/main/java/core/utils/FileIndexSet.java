package core.utils;

public final class FileIndexSet {

    public FileIndexSet(StringGetter[] values, String suffix) {
        this.values = values;
        this.suffix = suffix;
    }

    public String getFileName(int index) {
        return values[index].get() + suffix;
    }

    public int getCount() {
        return values.length;
    }

    private final StringGetter[] values;
    private final String suffix;
}
