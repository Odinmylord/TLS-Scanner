/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.guideline;

import de.rub.nds.scanner.core.constants.ListResult;
import de.rub.nds.scanner.core.constants.TestResults;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlsscanner.core.guideline.GuidelineCheckResult;
import de.rub.nds.tlsscanner.serverscanner.guideline.checks.SignatureAndHashAlgorithmsGuidelineCheck;
import de.rub.nds.tlsscanner.serverscanner.report.ServerReport;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class SignatureAndHashAlgorithmsGuidelineCheckTest {

    @Test
    public void testPositive() {
        ServerReport report = new ServerReport("test", 443);
        report.putResult(TlsAnalyzedProperty.LIST_SUPPORTED_SIGNATURE_AND_HASH_ALGORITHMS_CERT,
            new ListResult<>(Collections.singletonList(SignatureAndHashAlgorithm.RSA_SHA1),
                "SUPPORTED_SIGNATUREANDHASH_ALGORITHMS_CERT"));

        SignatureAndHashAlgorithmsGuidelineCheck check = new SignatureAndHashAlgorithmsGuidelineCheck(null, null,
            Collections.singletonList(SignatureAndHashAlgorithm.RSA_SHA1), false);
        GuidelineCheckResult result = check.evaluate(report);
        Assert.assertEquals(TestResults.TRUE, result.getResult());
    }

    @Test
    public void testNegative() {
        ServerReport report = new ServerReport("test", 443);
        report.putResult(TlsAnalyzedProperty.LIST_SUPPORTED_SIGNATURE_AND_HASH_ALGORITHMS_CERT,
            new ListResult<>(Collections.singletonList(SignatureAndHashAlgorithm.DSA_SHA1),
                "SUPPORTED_SIGNATUREANDHASH_ALGORITHMS_CERT"));

        SignatureAndHashAlgorithmsGuidelineCheck check = new SignatureAndHashAlgorithmsGuidelineCheck(null, null,
            Collections.singletonList(SignatureAndHashAlgorithm.RSA_SHA1), false);
        GuidelineCheckResult result = check.evaluate(report);
        Assert.assertEquals(TestResults.FALSE, result.getResult());
    }
}
