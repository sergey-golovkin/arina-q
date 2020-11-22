package arina.utils;

import org.apache.camel.Exchange;

/**
 * Created with IntelliJ IDEA.
 * User: GSM
 * Date: 30.04.17
 * Time: 23:52
 * To change this template use File | Settings | File Templates.
 */

public class CamelHeaders
{
    public static String simpleName(String name)
    {
        return "${header." + name + "}";
    }

    public static <T> T getValue(Exchange exchange, String name, Class<T> clazz)
    {
        return exchange.getIn().getHeader(name, clazz);
    }

    public static void setValue(Exchange exchange, String name, Object value)
    {
        exchange.getIn().setHeader(name, value);
    }
}
