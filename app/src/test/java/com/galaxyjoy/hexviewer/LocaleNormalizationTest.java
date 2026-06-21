package com.galaxyjoy.hexviewer;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for MyApplication.normalizeLang().
 *
 * normalizeLang is private-static, accessed via reflection so we don't
 * widen its visibility just for tests.
 */
public class LocaleNormalizationTest {

    private String normalizeLang(String lang) throws Exception {
        Method m = MyApplication.class.getDeclaredMethod("normalizeLang", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, lang);
    }

    @Test
    public void normalizeLang_indonesian_mapsToId() throws Exception {
        assertEquals("id", normalizeLang("in"));
        assertEquals("id", normalizeLang("IN"));
    }

    @Test
    public void normalizeLang_hebrew_mapsToHe() throws Exception {
        assertEquals("he", normalizeLang("iw"));
        assertEquals("he", normalizeLang("IW"));
    }

    @Test
    public void normalizeLang_yiddish_mapsToYi() throws Exception {
        assertEquals("yi", normalizeLang("ji"));
    }

    @Test
    public void normalizeLang_standardCode_lowercased() throws Exception {
        assertEquals("en", normalizeLang("en"));
        assertEquals("en", normalizeLang("EN"));
        assertEquals("zh", normalizeLang("zh"));
        assertEquals("vi", normalizeLang("vi"));
        assertEquals("de", normalizeLang("de"));
    }

    @Test
    public void normalizeLang_null_returnsEmpty() throws Exception {
        assertEquals("", normalizeLang(null));
    }

    @Test
    public void normalizeLang_empty_returnsEmpty() throws Exception {
        assertEquals("", normalizeLang(""));
    }
}
