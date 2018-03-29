package de.photon.AACAdditionPro.util.files.configs;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import de.photon.AACAdditionPro.exceptions.ConfigException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileConfig
{

    @Getter
    private final File file;
    private YamlConfiguration cfg = null;
    private ImmutableList<ConfigField> cfgFields;
    private final Map<String, ConfigField> pathsToFields = new HashMap<>();
    private final Map<String, ConfigField> fieldNameToFields = new HashMap<>();
    @Getter
    @Setter(AccessLevel.PROTECTED)
    protected String header = null;

    public FileConfig(@NonNull final File file) throws ConfigException
    {
        this(file, true);
    }

    public FileConfig(@NonNull final File file, @NonNull final String header) throws ConfigException
    {
        this.file = file;
        this.header = header;
        init();
    }

    protected FileConfig(@NonNull final File file, final boolean doFirstLoad) throws ConfigException
    {
        this.file = file;
        if (doFirstLoad)
        {
            init();
        }
    }

    /**
     * You have to call this method at the end of your constructor!
     *
     * @throws ConfigException if an error occurred
     */
    protected void init() throws ConfigException
    {
        final Class<?> clazz = getClass();
        //final Field[] fields = clazz.getDeclaredFields();
        final ImmutableList.Builder<ConfigField> builder = new ImmutableList.Builder<>();
        /*for (final Field field : fields) {
            if (field.isAnnotationPresent(InConfig.class)) {
                field.setAccessible(true);
                String path = field.getName();
                final InConfig configOptions = field.getAnnotation(InConfig.class);
                final String customPath = configOptions.path();
                if (customPath != null && !customPath.isEmpty()) {
                    path = customPath.replace('.', '_');
                }
                String comment = configOptions.comment();
                if (comment != null && comment.isEmpty()) {
                    comment = null;
                }
                builder.add(new ConfigField(field, path, comment));
            }
        }*/
        this.cfgFields = builder.build();
        for (final ConfigField field : this.cfgFields)
        {
            pathsToFields.put(field.getResultingPath().intern(), field);
            fieldNameToFields.put(field.getReflectionField().getName().toLowerCase(), field);
        }
        reload();
    }

    public void save() throws ConfigException
    {
        try
        {
            for (final ConfigField field : cfgFields)
            {
                cfg.set(field.getResultingPath(), field.getReflectionField().get(this));
            }
        } catch (final Throwable wrapIt)
        {
            throw new ConfigException(wrapIt);
        }
        addComments();
    }

    /**
     * gets the value of the configuration with the given fieldName or path
     *
     * @param fieldNameOrPath the fieldName or the path, preferring the path
     * @param <T>             the type of the returned value
     *
     * @return the value or null if not found or an error occurred
     */
    @SuppressWarnings("unchecked")
    public <T> T get(@NonNull final String fieldNameOrPath)
    {
        try
        {
            return (T) (pathsToFields.containsKey(fieldNameOrPath.intern()) ?
                        pathsToFields.get(fieldNameOrPath).getReflectionField().get(this) :
                        cfg.get(fieldNameOrPath));
        } catch (Throwable t)
        {
            return null;
        }
    }

    public void set(final String fieldNameOrPath, Object value)
    {
        try
        {
            pathsToFields.get(fieldNameOrPath.intern()).getReflectionField().set(this, value);
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void addComments() throws ConfigException
    {
        try
        {
            final Path path = file.toPath();
            final List<String> input = Files.readAllLines(path, Charsets.UTF_8);
            final List<String> output = new ArrayList<>((int) ((double) input.size() * 1.25D));
            final int size = input.size() - 1;
            String currentGroup = "";
            for (int i = 0; i < size; i++)
            {
                final String currLine = removeComment(input.get(i));
                if (currLine.isEmpty())
                {
                    continue;
                }
                final String nextLine = removeComment(input.get(i + 1));
                if (isGroupStart(currLine, nextLine))
                {
                    currentGroup += (currentGroup.isEmpty() ? "" : ".") + getPathPart(currLine);
                }
                else
                {
                    if (isGroupEnd(currLine, nextLine))
                    {
                        currentGroup = currentGroup.substring(0, currentGroup.lastIndexOf('.'));
                        if (getLastCharacter(currentGroup) == '.')
                        {
                            currentGroup = currentGroup.substring(0, currentGroup.length() - 1);
                        }
                        if (getFirstCharacter(currentGroup) == '.')
                        {
                            currentGroup = currentGroup.substring(1, currentGroup.length());
                        }
                    }
                    if (isDefinitionLine(currLine, nextLine, false))
                    {
                        final String key = (currentGroup + "." + getPathPart(currLine)).intern();
                        if (pathsToFields.containsKey(key))
                        {
                            final StringBuilder leftSpace = new StringBuilder();
                            final int neededSpaces = 2 * (currentGroup.length() - currentGroup.replace(".", "").length());
                            for (int j = 0; j < neededSpaces; j++)
                            {
                                leftSpace.append(' ');
                            }
                            output.add(leftSpace + "#" + pathsToFields.get(key).getComment().replace("\n", "\n" + leftSpace + "#"));
                        }
                    }
                }
                output.add(currLine);
            }
            Files.delete(path);
            Files.write(path, output, Charsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (final Throwable wrapIt)
        {
            throw new ConfigException(wrapIt);
        }
    }

    private static boolean isGroupStart(final String line, final String nextLine)
    {
        return getSpacesAmountInFront(line) + 2 == getSpacesAmountInFront(nextLine);
    }

    private static boolean isGroupEnd(final String line, final String nextLine)
    {
        return getSpacesAmountInFront(line) - 2 == getSpacesAmountInFront(nextLine);
    }

    private static boolean isDefinitionLine(final String cL, final String nL, boolean groupStartCheck)
    {
        if (groupStartCheck && isGroupStart(cL, nL))
        {
            return false;
        }
        final char cLTrimFirstChar = getFirstCharacter(cL.trim());
        final char cLTrimLastChar = getLastCharacter(cL.trim());
        final char nLTrimFirstChar = getFirstCharacter(nL.trim());
        if (cLTrimFirstChar != '-')
        { // no current multiline definition
            if (cLTrimLastChar == ':' && nLTrimFirstChar == '-')
            { // = start of multiline definition
                return true;
            }
            final boolean hasQuotes = getFirstCharacter(cL.trim()) == '\'';
            if (hasQuotes)
            {
                final int endPath = cL.indexOf('\'', 1);
                if (cL.indexOf(": ") == endPath + 1)
                {
                    return true;
                }
            }
            else
            {
                final String cLTrim = cL.trim();
                if (cLTrim.contains(": ") && !cLTrim.substring(0, cLTrim.indexOf(": ")).contains(" "))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getPathPart(final String line)
    {
        return line.replace("\'", "").replace(":", "").trim();
    }

    private static String removeComment(final String line)
    {
        if (line.contains("#"))
        {
            return line.substring(0, line.indexOf("#"));
        }
        else
        {
            return line;
        }
    }

    private static char getFirstCharacter(final String str)
    {
        return str.charAt(0);
    }

    private static char getLastCharacter(final String str)
    {
        return str.charAt(str.length() - 1);
    }

    private static int getSpacesAmountInFront(final String str)
    {
        int i = 0;
        for (char c : str.toCharArray())
        {
            if (c == ' ')
            {
                i++;
            }
            else
            {
                break;
            }
        }
        return i;
    }

    public void reload() throws ConfigException
    {
        try
        {
            if (cfg == null)
            {
                cfg = YamlConfiguration.loadConfiguration(file);
                cfg.options().header(header);
            }
            else
            {
                cfg.load(file);
            }
            cfg.options().copyDefaults(true).copyHeader(true).indent(2).pathSeparator('.');
            for (final ConfigField field : cfgFields)
            {
                final Object value = field.getReflectionField().get(this);
                cfg.addDefault(field.getResultingPath(), value);
                field.getReflectionField().set(this, cfg.get(field.getResultingPath()));
                cfg.save(file);
                addComments();
            }
        } catch (final Throwable wrapIt)
        {
            throw new ConfigException(wrapIt);
        }
    }
}
