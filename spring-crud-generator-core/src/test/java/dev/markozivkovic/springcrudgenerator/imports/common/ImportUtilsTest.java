package dev.markozivkovic.springcrudgenerator.imports.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImportUtilsTest {

    @Test
    @DisplayName("sortAndFormatImports: sorts fully-qualified names and formats as import statements")
    void sortAndFormatImports_sortsAndFormats() {

        final Set<String> imports = new LinkedHashSet<>();
        imports.add("org.springframework.data.domain.Pageable");
        imports.add("java.util.Optional");
        imports.add("org.springframework.data.domain.Page");

        final String result = ImportUtils.sortAndFormatImports(imports);
        final String nl = System.lineSeparator();
        final String expected = "import java.util.Optional;" + nl
                + "import org.springframework.data.domain.Page;" + nl
                + "import org.springframework.data.domain.Pageable;" + nl;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("sortAndJoinFormattedImports: sorts already formatted import statements")
    void sortAndJoinFormattedImports_sortsPreformattedStatements() {

        final String nl = System.lineSeparator();
        final Set<String> imports = new LinkedHashSet<>();
        imports.add("import org.springframework.data.domain.Pageable;" + nl);
        imports.add("import java.util.Optional;" + nl);
        imports.add("import org.springframework.data.domain.Page;" + nl);

        final String result = ImportUtils.sortAndJoinFormattedImports(imports);
        final String expected = "import java.util.Optional;" + nl
                + "import org.springframework.data.domain.Page;" + nl
                + "import org.springframework.data.domain.Pageable;" + nl;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("joinImportGroups: joins non-empty groups with a blank line between groups")
    void joinImportGroups_joinsNonEmptyGroups() {

        final String nl = System.lineSeparator();
        final String javaGroup = "import java.util.Optional;" + nl;
        final String orgGroup = "import org.springframework.data.domain.Page;" + nl;
        final String projectGroup = "import com.example.app.models.UserEntity;" + nl;

        final String result = ImportUtils.joinImportGroups(javaGroup, "", null, orgGroup, "   ", projectGroup);
        final String expected = javaGroup + nl + orgGroup + nl + projectGroup;

        assertEquals(expected, result);
    }
}
