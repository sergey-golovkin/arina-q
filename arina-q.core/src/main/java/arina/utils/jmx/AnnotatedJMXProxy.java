package arina.utils.jmx;

import javax.management.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 09.06.16
 * Time: 16:51
 * To change this template use File | Settings | File Templates.
 */
public class AnnotatedJMXProxy implements DynamicMBean
{
    private final Object jmxObject;
    private Map<String, Field> jmxGetAttributes = new HashMap<>();
    private Map<String, Field> jmxSetAttributes = new HashMap<>();

    public AnnotatedJMXProxy()
    {
        this.jmxObject = this;
    }

    public AnnotatedJMXProxy(Object jmxobject)
    {
        this.jmxObject = jmxobject;
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException
    {
        try
        {
            Field f = this.jmxGetAttributes.get(attribute);
            if(f != null)
            {
                synchronized (this.jmxObject)
                {
                    return f.get(this.jmxObject);
                }
            }
        }
        catch (Exception ignore)
        {
        }
        return null;
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
    {
        try
        {
            Field f = this.jmxSetAttributes.get(attribute.getName());
            if(f != null)
            {
                synchronized (this.jmxObject)
                {
                   f.set(this.jmxObject, attribute.getValue());
                }
            }
        }
        catch (Exception ignore)
        {
        }
    }

    @Override
    public AttributeList getAttributes(String[] attributes)
    {
        AttributeList al = new AttributeList();
        try
        {
            for(String attribute : attributes)
                al.add(getAttribute(attribute));
        }
        catch (Exception ignore)
        {
        }
        return al;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes)
    {
        AttributeList al = new AttributeList();
        try
        {
            for(Object attribute : attributes)
            {
                setAttribute((Attribute)attribute);
                al.add(new Attribute(((Attribute)attribute).getName(), getAttribute(((Attribute)attribute).getName())));
            }
        }
        catch (Exception ignore)
        {
        }
        return al;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException
    {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo()
    {
        JMXClass c = this.jmxObject.getClass().getAnnotation(JMXClass.class);
        String classDescription = "";
        if (c != null)
            classDescription = c.description();

        ArrayList<MBeanAttributeInfo> ai = new ArrayList<>();
        for(Field f : this.jmxObject.getClass().getDeclaredFields())
        {
            JMXAttribute a = f.getAnnotation(JMXAttribute.class);
            if(a != null)
            {
                ai.add(new MBeanAttributeInfo(a.name(), f.getType().getCanonicalName(), a.description(), a.isReadable(), a.isWritable(), a.isIs()));
                if(a.isReadable())
                    this.jmxGetAttributes.put(a.name(), f);
                if(a.isWritable())
                    this.jmxSetAttributes.put(a.name(), f);
            }
        }

        return new MBeanInfo(this.jmxObject.getClass().getName(), classDescription, ai.toArray(new MBeanAttributeInfo[0]), new MBeanConstructorInfo[0], new MBeanOperationInfo[0], new MBeanNotificationInfo[0]);
    }
}
