package de.photon.AACAdditionPro.oldneural;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Output
{
    private final String label;
    private final double confidence;

    /**
     * Creates a new {@link Output} with a different confidence, but the same label.
     */
    public Output newConfidenceOutput(double confidence)
    {
        return new Output(this.label, confidence);
    }
}