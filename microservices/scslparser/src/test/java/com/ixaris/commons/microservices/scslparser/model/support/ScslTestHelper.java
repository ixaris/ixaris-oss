package com.ixaris.commons.microservices.scslparser.model.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.ixaris.commons.microservices.scslparser.model.ScslDefinition;
import com.ixaris.commons.microservices.scslparser.model.ScslModelObject;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslParseException;

/**
 * Created by ian.grima on 16/03/2016.
 */
public class ScslTestHelper {
    
    public static <S extends ScslModelObject<S>> S createDummyChild(Class<S> childType, String name) {
        try {
            final Constructor<S> childTypeConstructor = childType.getConstructor(ScslModelObject.class, String.class);
            return childTypeConstructor.newInstance(new ScslDefinition(), name);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new ScslParseException("Error Creating Child node of type :" + childType.getSimpleName() + " and name :" + name);
        }
    }
}
