package de.photon.AACAdditionPro.util.reflection;

import java.lang.reflect.Field;

/**
 * @author geNAZt
 * @version 1.0
 */
public class FieldReflect {

    private final Field field;

    FieldReflect( Field field ) {
        this.field = field;
    }

    public TempValueReflect from( Object obj ) {
        return new TempValueReflect( field, obj );
    }

}
