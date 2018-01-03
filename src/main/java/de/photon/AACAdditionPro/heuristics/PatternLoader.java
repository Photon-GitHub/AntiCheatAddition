package de.photon.AACAdditionPro.heuristics;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author geNAZt
 * @version 1.0
 */
public class PatternLoader
{

    public PatternLoader(Set<Pattern> patterns)
    {
        try
        {
            File jarFile = new File( PatternLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() );
            try ( JarFile pluginFile = new JarFile( jarFile ) )
            {
                Enumeration<JarEntry> entries = pluginFile.entries();
                while ( entries.hasMoreElements() )
                {
                    JarEntry entry = entries.nextElement();
                    if ( entry.getName().endsWith( ".ptrn" ) )
                    {
                        PatternDeserializer deserializer = new PatternDeserializer( entry.getName() );
                        try
                        {
                            patterns.add( deserializer.load() );
                        } catch ( IOException e )
                        {
                            e.printStackTrace();
                        }
                    }
                }
            } catch ( IOException e )
            {
                e.printStackTrace();
            }
        } catch ( URISyntaxException e )
        {
            e.printStackTrace();
        }
    }

}
