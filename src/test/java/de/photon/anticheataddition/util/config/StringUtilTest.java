package de.photon.anticheataddition.util.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StringUtilTest
{
    @Test
    void configCommentTest()
    {
        Assertions.assertTrue(ConfigUtil.isConfigComment(null));
        Assertions.assertTrue(ConfigUtil.isConfigComment(""));
        Assertions.assertTrue(ConfigUtil.isConfigComment("#"));
        Assertions.assertTrue(ConfigUtil.isConfigComment("# Some comment"));
        Assertions.assertTrue(ConfigUtil.isConfigComment("    # Some comment"));
        Assertions.assertTrue(ConfigUtil.isConfigComment("        # Some comment"));
        Assertions.assertTrue(ConfigUtil.isConfigComment("           # Some comment"));

        Assertions.assertFalse(ConfigUtil.isConfigComment("value: something"));
        Assertions.assertFalse(ConfigUtil.isConfigComment("    value: something"));
        Assertions.assertFalse(ConfigUtil.isConfigComment("    value: something       # Some comment"));
    }

    @Test
    void depthTest()
    {
        Assertions.assertEquals(0, ConfigUtil.depth(null));
        Assertions.assertEquals(0, ConfigUtil.depth(""));

        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(i, ConfigUtil.depth(" ".repeat(i)));
            Assertions.assertEquals(i, ConfigUtil.depth(" ".repeat(i) + "dada daa2541    "));
        }
    }
}
