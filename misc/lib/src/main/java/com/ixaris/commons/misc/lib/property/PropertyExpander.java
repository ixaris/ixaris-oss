package com.ixaris.commons.misc.lib.property;

import java.util.Properties;

/**
 * A utility class to expand properties embedded in a string. Strings of the form ${some.property.name} are expanded to be the value of the
 * property. Also, the special ${/} property is expanded to be the same as file.separator. If a property is not set, a GeneralSecurityException
 * will be thrown.
 */
public class PropertyExpander {
    
    public static String expand(final String value) {
        return expand(value, System.getProperties());
    }
    
    public static String expand(final String value, final Properties properties) {
        if (value == null) {
            return null;
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties is null");
        }
        
        int p = value.indexOf("${");
        
        // no special characters
        if (p == -1) {
            return value;
        }
        
        final StringBuffer sb = new StringBuffer(value.length());
        int max = value.length();
        int i = 0; // index of last character we copied
        
        while ((i < max) && (p != -1)) {
            if (p > i) {
                // copy in anything before the special stuff
                sb.append(value.substring(i, p));
                i = p;
            }
            int pEnd = value.indexOf('}', p + 2);
            int pNext = value.indexOf("${", p + 2);
            if (pEnd == -1) {
                break;
                
            } else if ((pNext != -1) && (pNext < pEnd)) {
                sb.append("${");
                i += 2;
                
            } else {
                final String prop = value.substring(p + 2, pEnd);
                if (prop.equals("/")) {
                    sb.append(java.io.File.separatorChar);
                } else {
                    String val = properties.getProperty(prop);
                    if (val != null) {
                        
                        sb.append(val);
                    } else {
                        int colon = prop.indexOf(':');
                        if (colon > 0) {
                            final String realProp = prop.substring(0, colon);
                            val = properties.getProperty(realProp);
                            
                            if (val != null) {
                                sb.append(val);
                            } else {
                                sb.append(prop.substring(colon + 1));
                            }
                        } else {
                            sb.append("${" + prop + "}");
                        }
                    }
                }
                
                i = pEnd + 1;
            }
            p = pNext; // value.indexOf("${", i);
        }
        
        if (i < max) {
            sb.append(value.substring(i));
        }
        
        return sb.toString();
    }
}
