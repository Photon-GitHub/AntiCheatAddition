package de.photon.anticheataddition.util.protocol;

public record MetadataPositionIndex(int healthIndex, int arrowsInEntityIndex, int skinPartsIndex) {
    public MetadataPositionIndex
    {
        if (healthIndex < 0 || arrowsInEntityIndex < 0 || skinPartsIndex < 0) {
            throw new IllegalArgumentException("Tried to create MetadataPositionIndex with negative index.");
        }

        if (healthIndex == arrowsInEntityIndex || healthIndex == skinPartsIndex || arrowsInEntityIndex == skinPartsIndex) {
            throw new IllegalArgumentException("Tried to create MetadataPositionIndex with duplicate index.");
        }
    }
}
