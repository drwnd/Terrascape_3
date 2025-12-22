package core.rendering_api;

public interface CrashCallback {

    CrashAction notify(Exception exception);

}
