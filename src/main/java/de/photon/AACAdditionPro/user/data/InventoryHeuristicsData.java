package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.heuristics.NeuralPattern;
import de.photon.AACAdditionPro.user.Data;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.datastructures.Buffer;
import de.photon.AACAdditionPro.util.datawrappers.InventoryClick;

import java.util.DoubleSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryHeuristicsData extends Data
{
    /**
     * Used to record inventory interactions for training the neural net.
     */
    public final Buffer<InventoryClick.BetweenClickInformation> inventoryClicks = new Buffer<>(InventoryHeuristics.SAMPLES);
    private InventoryClick lastClick = null;

    public String trainingLabel = null;
    public NeuralPattern trainedPattern = null;

    private final Map<String, Double> patternMap = new ConcurrentHashMap<>();

    public InventoryHeuristicsData(User user)
    {
        super(user);
    }

    public boolean bufferClick(final InventoryClick inventoryClick)
    {
        if (lastClick != null)
        {
            return this.inventoryClicks.bufferObject(new InventoryClick.BetweenClickInformation(lastClick, inventoryClick));
        }
        lastClick = inventoryClick;

        return false;
    }

    //TODO: UNUSED?

    /**
     * This should be called prior to {@link #setPatternConfidence(String, double)} to make the confidences decay with legit actions.
     */
    public void decayCycle()
    {
        patternMap.forEach((patternName, confidence) -> {
            final double newConfidence = confidence - 0.1;

            if (newConfidence > 0)
            {
                patternMap.put(patternName, newConfidence);
            }
            else
            {
                patternMap.remove(patternName);
            }
        });
    }

    /**
     * Sets the confidence of a {@link NeuralPattern}.
     * If the present confidence is higher than the new one, no confidence will be set.
     *
     * @param patternName the name of the {@link NeuralPattern}
     * @param confidence  the new confidence.
     */
    public void setPatternConfidence(final String patternName, final double confidence)
    {
        // Only set if the confidence level increases.
        if (patternMap.getOrDefault(patternName, 0D) < confidence)
        {
            patternMap.put(patternName, confidence);
        }
    }

    /**
     * Calculates the global confidence based on the sum of the single confidences.
     */
    public double calculateGlobalConfidence()
    {
        DoubleSummaryStatistics summaryStatistics = new DoubleSummaryStatistics();
        patternMap.forEach((patternName, value) -> {
            // Make sure too many low-confidence violations won't flag high global confidence
            // -> use cubic function.
            summaryStatistics.accept((value * value * value) * 1.2D * Objects.requireNonNull(InventoryHeuristics.PATTERNS.get(patternName), "Invalid pattern name: " + patternName).getWeight());
        });

        // Make sure that the result is greater or equal than 0.
        return Math.max(0D, Math.tanh(summaryStatistics.getSum() - 0.42));
    }

    @Override
    public void unregister()
    {
        this.inventoryClicks.clear();
        this.lastClick = null;
        this.trainingLabel = null;
        this.trainedPattern = null;
        this.patternMap.clear();
        super.unregister();
    }
}
