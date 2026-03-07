package core.settings;

public interface ToggleSetting extends KeyBound {

    void setValue(boolean value);

    boolean value();

    boolean defaultValue();
}
