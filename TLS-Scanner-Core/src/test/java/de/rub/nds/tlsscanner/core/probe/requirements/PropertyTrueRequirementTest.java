/*
 * TLS-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2023 Ruhr University Bochum, Paderborn University, Technology Innovation Institute, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsscanner.core.probe.requirements;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.rub.nds.scanner.core.constants.TestResults;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import org.junit.jupiter.api.Test;

public class PropertyTrueRequirementTest {

    @Test
    public void testPropertyRequirement() {
        TestReport report = new TestReport();
        TlsAnalyzedProperty[] property =
                new TlsAnalyzedProperty[] {TlsAnalyzedProperty.ACCEPTS_RANDOM_MESSAGE_SEQUENCES};

        PropertyValueRequirement<TestReport> requirement = new PropertyTrueRequirement<>();
        assertTrue(requirement.evaluate(report));

        requirement = new PropertyTrueRequirement<>(new TlsAnalyzedProperty[0]);
        assertTrue(requirement.evaluate(report));

        requirement = new PropertyTrueRequirement<>(property);
        assertArrayEquals(requirement.getParameters().toArray(), property);
        assertFalse(requirement.evaluate(report));

        report.putResult(TlsAnalyzedProperty.ACCEPTS_RANDOM_MESSAGE_SEQUENCES, TestResults.FALSE);
        assertFalse(requirement.evaluate(report));
        report.putResult(TlsAnalyzedProperty.ACCEPTS_RANDOM_MESSAGE_SEQUENCES, TestResults.TRUE);
        assertTrue(requirement.evaluate(report));
    }
}
