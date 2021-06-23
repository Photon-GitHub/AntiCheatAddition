package de.photon.aacadditionpro.util.mathematics;

import de.photon.aacadditionpro.util.datastructure.buffer.RingBuffer;
import lombok.Value;

@Value
public class FloatingAverage
{
    RingBuffer<Double> data;

    public FloatingAverage(int dataPoints, double defaultObject)
    {
        this.data = new RingBuffer<>(dataPoints, defaultObject);
    }

    public void addDataPoint(double t)
    {
        this.data.add(t);
    }

    public double getFloatingAverage()
    {
        double sum = 0;
        for (Double datum : data) sum += datum;
        return sum / data.size();
    }
}
