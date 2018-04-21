package sjipano.com.abc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: yifan.lin
 * @description:
 * @projectName: SJGLSurfaceView
 * @date: 2018-04-21
 * @time: 11:28
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE,
        ElementType.METHOD,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD})
public @interface NotProguard {
}
