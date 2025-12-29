package dev.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.CrudSpecification;

class SpringBootVersionUtilsTest {

    private static CrudSpecification specWithSpringBootVersion(final String version) {
        
        final CrudConfiguration cfg = new CrudConfiguration();
        cfg.setSpringBootVersion(version);

        final CrudSpecification spec = new CrudSpecification();
        spec.setConfiguration(cfg);
        return spec;
    }

    @Test
    void usesSpecVersion_whenSpecIsValidMajor3() {
        
        final CrudSpecification spec = specWithSpringBootVersion("3.0.2");
        final Integer major = SpringBootVersionUtils.resolveAndSetSpringBootMajor(spec, "4.0.1");

        assertEquals(3, major);
        assertEquals("3", spec.getConfiguration().getSpringBootVersion(), "Should normalize to major only");
    }

    @Test
    void usesSpecVersion_whenSpecIsValidMajor4WithSpaces() {
        
        final CrudSpecification spec = specWithSpringBootVersion(" 4.0.1 ");
        final Integer major = SpringBootVersionUtils.resolveAndSetSpringBootMajor(spec, "3.3.0");

        assertEquals(4, major);
        assertEquals("4", spec.getConfiguration().getSpringBootVersion());
    }

    @Test
    void usesSpecVersion_whenSpecIsJustMajorNumber() {
        
        final CrudSpecification spec = specWithSpringBootVersion("3");
        final Integer major = SpringBootVersionUtils.resolveAndSetSpringBootMajor(spec, "4.0.1");

        assertEquals(3, major);
        assertEquals("3", spec.getConfiguration().getSpringBootVersion());
    }

    @Test
    void fallsBackToParent_whenSpecIsBlank() {
        
        final CrudSpecification spec = specWithSpringBootVersion("  ");
        final Integer major = SpringBootVersionUtils.resolveAndSetSpringBootMajor(spec, "4.0.1");

        assertEquals(4, major);
        assertEquals("4", spec.getConfiguration().getSpringBootVersion());
    }

    @Test
    void fallsBackToParent_whenSpecIsUnsupportedMajor() {
        
        final CrudSpecification spec = specWithSpringBootVersion("2.7.18");
        final Integer major = SpringBootVersionUtils.resolveAndSetSpringBootMajor(spec, "3.2.5");

        assertEquals(3, major);
        assertEquals("3", spec.getConfiguration().getSpringBootVersion());
    }

    @Test
    void fallsBackToParent_whenSpecIsNotParseable() {
        
        final CrudSpecification spec = specWithSpringBootVersion("abc");
        final Integer major = SpringBootVersionUtils.resolveAndSetSpringBootMajor(spec, "4.0.1");

        assertEquals(4, major);
        assertEquals("4", spec.getConfiguration().getSpringBootVersion());
    }

    @Test
    void defaultsTo4_whenSpecInvalidAndParentNull() {
        
        final CrudSpecification spec = specWithSpringBootVersion("2.7.18");
        final Integer major = SpringBootVersionUtils.resolveAndSetSpringBootMajor(spec, null);

        assertEquals(4, major);
        assertEquals("4", spec.getConfiguration().getSpringBootVersion());
    }

    @Test
    void defaultsTo4_whenSpecNullOrBlankAndParentUnsupported() {
        
        final CrudSpecification spec = specWithSpringBootVersion(null);
        final Integer major = SpringBootVersionUtils.resolveAndSetSpringBootMajor(spec, "1.5.22");

        assertEquals(4, major);
        assertEquals("4", spec.getConfiguration().getSpringBootVersion());
    }

    @Test
    void defaultsTo4_whenBothSpecAndParentNotParseable() {
        
        final CrudSpecification spec = specWithSpringBootVersion("wat");
        final Integer major = SpringBootVersionUtils.resolveAndSetSpringBootMajor(spec, "nope");

        assertEquals(4, major);
        assertEquals("4", spec.getConfiguration().getSpringBootVersion());
    }

    @Test
    void prefersSpecOverParent_whenBothSupported() {
        
        final CrudSpecification spec = specWithSpringBootVersion("3.3.7");
        final Integer major = SpringBootVersionUtils.resolveAndSetSpringBootMajor(spec, "4.0.1");

        assertEquals(3, major);
        assertEquals("3", spec.getConfiguration().getSpringBootVersion());
    }

    @Test
    void isSpringBoot3_returnsFalse_whenNull() {
        assertFalse(SpringBootVersionUtils.isSpringBoot3(null));
    }

    @Test
    void isSpringBoot3_returnsTrue_whenExactly3() {
        assertTrue(SpringBootVersionUtils.isSpringBoot3("3"));
    }

    @Test
    void isSpringBoot3_returnsFalse_whenNot3() {
        assertFalse(SpringBootVersionUtils.isSpringBoot3("4"));
        assertFalse(SpringBootVersionUtils.isSpringBoot3("2"));
        assertFalse(SpringBootVersionUtils.isSpringBoot3("5"));
        assertFalse(SpringBootVersionUtils.isSpringBoot3("0"));
    }

    @Test
    void isSpringBoot3_returnsFalse_forSemverAndWhitespace() {
        assertFalse(SpringBootVersionUtils.isSpringBoot3("3.0.2"));
        assertFalse(SpringBootVersionUtils.isSpringBoot3(" 3 "));
        assertFalse(SpringBootVersionUtils.isSpringBoot3(""));
        assertFalse(SpringBootVersionUtils.isSpringBoot3(" "));
        assertFalse(SpringBootVersionUtils.isSpringBoot3("three"));
    }

    @Test
    void isSpringBoot4_returnsFalse_whenNull() {
        assertFalse(SpringBootVersionUtils.isSpringBoot4(null));
    }

    @Test
    void isSpringBoot4_returnsTrue_whenExactly4() {
        assertTrue(SpringBootVersionUtils.isSpringBoot4("4"));
    }

    @Test
    void isSpringBoot4_returnsFalse_whenNot4() {
        assertFalse(SpringBootVersionUtils.isSpringBoot4("3"));
        assertFalse(SpringBootVersionUtils.isSpringBoot4("2"));
        assertFalse(SpringBootVersionUtils.isSpringBoot4("5"));
        assertFalse(SpringBootVersionUtils.isSpringBoot4("0"));
    }

    @Test
    void isSpringBoot4_returnsFalse_forSemverAndWhitespace() {
        assertFalse(SpringBootVersionUtils.isSpringBoot4("4.0.1"));
        assertFalse(SpringBootVersionUtils.isSpringBoot4(" 4 "));
        assertFalse(SpringBootVersionUtils.isSpringBoot4(""));
        assertFalse(SpringBootVersionUtils.isSpringBoot4(" "));
        assertFalse(SpringBootVersionUtils.isSpringBoot4("four"));
    }

}
