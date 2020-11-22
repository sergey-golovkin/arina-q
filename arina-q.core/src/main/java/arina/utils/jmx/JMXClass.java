package arina.utils.jmx;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 09.06.16
 * Time: 16:30
 * To change this template use File | Settings | File Templates.
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ TYPE})
public @interface JMXClass
{
    String description();
}
