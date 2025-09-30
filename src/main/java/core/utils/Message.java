package core.utils;

public record Message(String string) implements StringGetter {

    @Override
    public String get() {
        return string;
    }
}
