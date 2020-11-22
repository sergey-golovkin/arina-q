package arina.utils.jmx;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 09.06.16
 * Time: 16:30
 * To change this template use File | Settings | File Templates.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JMXAttribute
{
    String name();
    String description() default "";
    boolean isReadable() default true;
    boolean isWritable() default false;
    boolean isIs() default false;
}
