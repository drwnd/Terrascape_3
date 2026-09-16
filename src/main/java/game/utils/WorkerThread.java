package game.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})@Documented
public @interface WorkerThread {
}
