package de.photon.AACAdditionPro.neural;

import com.google.common.base.Preconditions;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This class saves {@link Data} used for training and evaluation of Graphs.
 */
public class DataSet implements Iterable<DataSet.Data>
{
    @Getter
    private final Set<String> rows;
    private final List<Data> dataList = new ArrayList<>();

    public DataSet(final Set<String> rows)
    {
        this.rows = rows;
    }

    /**
     * Validates and adds {@link Data} to this {@link DataSet}.
     */
    public void addData(final Data data)
    {
        Preconditions.checkArgument(data.isDataCompatible(rows), "Data does not match rows.");
        this.dataList.add(data);
    }

    @Override
    public Iterator<Data> iterator()
    {
        return dataList.iterator();
    }

    public static class Data extends HashMap<String, Double>
    {
        /**
         * The label of the data.
         */
        private final String label;

        public Data(final String label) {this.label = label;}

        /**
         * This can be used to check if {@link Data} is compatible to some other {@link Data}.
         */
        public boolean isDataCompatible(final Set<String> rows)
        {
            return this.keySet().equals(rows);
        }
    }
}
