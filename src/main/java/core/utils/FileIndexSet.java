package core.utils;

public final class FileIndexSet {

    public FileIndexSet(FileNamer[] values, String suffix) {
        this.values = values;
        this.suffix = suffix;
    }

    public String getFileName(int index) {
        return values[index].name() + suffix;
    }

    public int getCount() {
        return values.length;
    }

    private final FileNamer[] values;
    private final String suffix;
}
