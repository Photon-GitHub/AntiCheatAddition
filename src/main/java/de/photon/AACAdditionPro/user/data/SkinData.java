package de.photon.AACAdditionPro.user.data;

public class SkinData
{
    // This should be initialized with null to prevent bypasses with a certain number composition.
    private Integer skinComponents = null;

    /**
     * Updates the saved skin components.
     *
     * @return false if there weren't any old skin components or the skin components haven't changed, else true.
     */
    public boolean updateSkinComponents(int newSkinComponents)
    {
        if (this.skinComponents == null)
        {
            this.skinComponents = newSkinComponents;
            return false;
        }

        if (this.skinComponents == newSkinComponents)
        {
            return false;
        }

        this.skinComponents = newSkinComponents;
        return true;
    }
}
