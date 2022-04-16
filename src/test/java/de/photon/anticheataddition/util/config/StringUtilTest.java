package de.photon.anticheataddition.util.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StringUtilTest
{
    @Test
    void configCommentTest()
    {
        Assertions.assertTrue(StringUtil.isConfigComment(null));
        Assertions.assertTrue(StringUtil.isConfigComment(""));
        Assertions.assertTrue(StringUtil.isConfigComment("#"));
        Assertions.assertTrue(StringUtil.isConfigComment("# Some comment"));
        Assertions.assertTrue(StringUtil.isConfigComment("    # Some comment"));
        Assertions.assertTrue(StringUtil.isConfigComment("        # Some comment"));
        Assertions.assertTrue(StringUtil.isConfigComment("           # Some comment"));

        Assertions.assertFalse(StringUtil.isConfigComment("value: something"));
        Assertions.assertFalse(StringUtil.isConfigComment("    value: something"));
        Assertions.assertFalse(StringUtil.isConfigComment("    value: something       # Some comment"));
    }

    @Test
    void depthTest()
    {
        Assertions.assertEquals(0, StringUtil.depth(null));
        Assertions.assertEquals(0, StringUtil.depth(""));

        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(i, StringUtil.depth(" ".repeat(i)));
            Assertions.assertEquals(i, StringUtil.depth(" ".repeat(i) + "dada daa2541    "));
        }
    }
}
