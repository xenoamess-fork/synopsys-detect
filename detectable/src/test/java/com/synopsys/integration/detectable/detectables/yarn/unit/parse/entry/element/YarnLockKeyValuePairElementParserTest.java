package com.synopsys.integration.detectable.detectables.yarn.unit.parse.entry.element;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.detectable.detectables.yarn.parse.YarnLockLineAnalyzer;
import com.synopsys.integration.detectable.detectables.yarn.parse.entry.YarnLockEntry;
import com.synopsys.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryBuilder;
import com.synopsys.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryId;
import com.synopsys.integration.detectable.detectables.yarn.parse.entry.element.YarnLockKeyValuePairElementParser;

public class YarnLockKeyValuePairElementParserTest {
    private static YarnLockKeyValuePairElementParser yarnLockKeyValuePairElementParser;

    @BeforeAll
    static void setup() {
        yarnLockKeyValuePairElementParser = new YarnLockKeyValuePairElementParser(
            new YarnLockLineAnalyzer(), "version", YarnLockEntryBuilder::setVersion);
    }

    @Test
    void testWithoutColonWithoutQuotes() {
        doTest("  version test.version.value", "test.version.value");
    }

    @Test
    void testWithoutColonWithQuotes() {
        doTest("  version \"test.version.value\"", "test.version.value");
    }

    @Test
    void testWithColonWithoutQuotes() {
        doTest("  version: test.version.value", "test.version.value");
    }

    @Test
    void testWithColonWithQuotes() {
        doTest("  version: \"test.version.value\"", "test.version.value");
    }

    private void doTest(String line, String versionValue) {
        Assertions.assertTrue(yarnLockKeyValuePairElementParser.applies(line));

        YarnLockEntryBuilder builder = new YarnLockEntryBuilder();
        builder.addId(new YarnLockEntryId("idname", "idversion"));
        List<String> lines = Arrays.asList(line);
        yarnLockKeyValuePairElementParser.parseElement(builder, lines, 0);

        Optional<YarnLockEntry> entry = builder.build();
        Assertions.assertTrue(entry.isPresent());
        Assertions.assertEquals(versionValue, entry.get().getVersion());
    }
}
