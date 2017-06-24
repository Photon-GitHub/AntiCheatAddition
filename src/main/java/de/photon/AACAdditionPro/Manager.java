package de.photon.AACAdditionPro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Manager<T>
{
    protected final List<T> managedObjects;

    @SafeVarargs
    protected Manager(final T... initialObjects)
    {
        managedObjects = new ArrayList<>(Arrays.asList(initialObjects));
        managedObjects.forEach(this::registerObject);
    }

    protected abstract void registerObject(T object);

    public List<T> getManagedObjects()
    {
        return managedObjects;
    }
}