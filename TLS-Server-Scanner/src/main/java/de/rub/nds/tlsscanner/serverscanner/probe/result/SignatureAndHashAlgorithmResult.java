/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe.result;

import de.rub.nds.scanner.core.probe.result.ProbeResult;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlsscanner.core.constants.TlsProbeType;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;

import de.rub.nds.tlsscanner.serverscanner.report.ServerReport;
import java.util.List;

/**
 *
 * @author Robert Merget {@literal <robert.merget@rub.de>}
 */
public class SignatureAndHashAlgorithmResult extends ProbeResult<ServerReport> {

    private final List<SignatureAndHashAlgorithm> signatureAndHashAlgorithmListSke;
    private final List<SignatureAndHashAlgorithm> signatureAndHashAlgorithmListTls13;
    private final TestResult respectsExtension;

    public SignatureAndHashAlgorithmResult(List<SignatureAndHashAlgorithm> signatureAndHashAlgorithmListSke,
        List<SignatureAndHashAlgorithm> signatureAndHashAlgorithmListTls13, TestResult respectsExtension) {
        super(ProbeType.SIGNATURE_AND_HASH);
        this.signatureAndHashAlgorithmListSke = signatureAndHashAlgorithmListSke;
        this.respectsExtension = respectsExtension;
        this.signatureAndHashAlgorithmListTls13 = signatureAndHashAlgorithmListTls13;
    }

    @Override
    public void mergeData(SiteReport report) {
        report.setSupportedSignatureAndHashAlgorithmsSke(signatureAndHashAlgorithmListSke);
        report.setSupportedSignatureAndHashAlgorithmsTls13(signatureAndHashAlgorithmListTls13);
        report.putResult(AnalyzedProperty.RESPECTS_SIGNATURE_ALGORITHMS_EXTENSION, respectsExtension);
    }

}
