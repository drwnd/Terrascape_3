package game.server;

import game.utils.ServerThread;

public interface Function {

    @ServerThread
    boolean run();

}
