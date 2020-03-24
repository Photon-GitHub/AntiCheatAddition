package de.photon.aacadditionpro.olduser.data;

public class SkinData
{
    // This should be initialized with null to prevent bypasses with a certain number composition.
    private Integer skinComponents = null;

    /**
     * Updates the saved skin components.
     *
     * @return true if the skinComponents changed and there have already been some skin components beforehand.
     */
    public boolean updateSkinComponents(int newSkinComponents)
    {
        if (this.skinComponents == null) {
            this.skinComponents = newSkinComponents;
            return false;
        }

        if (this.skinComponents == newSkinComponents) {
            return false;
        }

        this.skinComponents = newSkinComponents;
        return true;
    }
}
