/**
 * TLS-Scanner - A TLS configuration and analysis tool based on TLS-Attacker.
 *
 * Copyright 2017-2019 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsscanner.serverscanner.probe;

import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.util.CertificateFetcher;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.serverscanner.config.ScannerConfig;
import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.serverscanner.probe.certificate.CertificateChain;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;
import de.rub.nds.tlsscanner.serverscanner.report.result.CertificateResult;
import de.rub.nds.tlsscanner.serverscanner.report.result.ProbeResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.bouncycastle.crypto.tls.Certificate;

/**
 *
 * @author Robert Merget - robert.merget@rub.de
 */
public class CertificateProbe extends TlsProbe {

    private boolean scanForRsaCert = true;
    private boolean scanForDssCert = true;
    private boolean scanForEcdsaCert = true;
    private boolean scanForGostCert = true;
    private boolean scanForTls13 = true;

    // curves used for ecdsa in key exchange
    private List<NamedGroup> ecdsaPkGroupsStatic;
    private List<NamedGroup> ecdsaPkGroupsEphemeral;
    private List<NamedGroup> ecdsaPkGroupsTls13;

    // curves used for ecdsa certificate signatures
    private List<NamedGroup> ecdsaCertSigGroupsStatic;
    private List<NamedGroup> ecdsaCertSigGroupsEphemeral;
    private List<NamedGroup> ecdsaCertSigGroupsTls13;

    public CertificateProbe(ScannerConfig config, ParallelExecutor parallelExecutor) {
        super(parallelExecutor, ProbeType.CERTIFICATE, config);
    }

    @Override
    public ProbeResult executeTest() {
        try {
            ecdsaPkGroupsStatic = new LinkedList<>();
            ecdsaPkGroupsEphemeral = new LinkedList<>();
            ecdsaPkGroupsTls13 = new LinkedList<>();
            ecdsaCertSigGroupsStatic = new LinkedList<>();
            ecdsaCertSigGroupsEphemeral = new LinkedList<>();
            ecdsaCertSigGroupsTls13 = new LinkedList<>();

            LinkedList<CertificateChain> certificates = new LinkedList<>();
            if (scanForRsaCert) {
                addNewCertificates(certificates, getRsaCerts());
            }
            if (scanForDssCert) {
                addNewCertificates(certificates, getDssCerts());
            }
            if (scanForEcdsaCert) {
                addNewCertificates(certificates, getEcdsaCerts());
            }
            if (scanForGostCert) {
                addNewCertificates(certificates, getGostCert());
            }
            if (scanForTls13) {
                addNewCertificates(certificates, getTls13Certs());
            }

            if (certificates.isEmpty()) {
                return getCouldNotExecuteResult();
            } else {
                return new CertificateResult(certificates, ecdsaPkGroupsStatic, ecdsaPkGroupsEphemeral,
                        ecdsaCertSigGroupsStatic, ecdsaCertSigGroupsEphemeral, ecdsaPkGroupsTls13,
                        ecdsaCertSigGroupsTls13);
            }
        } catch (Exception E) {
            LOGGER.error("Could not scan for " + getProbeName(), E);
            return getCouldNotExecuteResult();
        }
    }

    @Override
    public boolean canBeExecuted(SiteReport report) {
        if (report.isProbeAlreadyExecuted(ProbeType.CIPHERSUITE)
                && report.isProbeAlreadyExecuted(ProbeType.PROTOCOL_VERSION)) {
            return true;
        }
        return false;
    }

    @Override
    public void adjustConfig(SiteReport report) {
        if (report.getResult(AnalyzedProperty.SUPPORTS_RSA_CERT) == TestResult.FALSE) {
            scanForRsaCert = false;
        }
        if (report.getResult(AnalyzedProperty.SUPPORTS_ECDSA) == TestResult.FALSE) {
            scanForEcdsaCert = false;
        }
        if (report.getResult(AnalyzedProperty.SUPPORTS_DSS) == TestResult.FALSE) {
            scanForDssCert = false;
        }
        if (report.getResult(AnalyzedProperty.SUPPORTS_GOST) == TestResult.FALSE) {
            scanForGostCert = false;
        }
        if (report.getResult(AnalyzedProperty.SUPPORTS_TLS_1_3) == TestResult.FALSE) {
            scanForTls13 = false;
        }
    }

    @Override
    public ProbeResult getCouldNotExecuteResult() {
        return new CertificateResult(null, null, null, null, null, null, null);
    }

    private List<CertificateChain> getRsaCerts() {
        LinkedList<CertificateChain> rsaCerts = new LinkedList<>();

        CertificateChain tlsRsaCert = getRsaCert();
        if (tlsRsaCert != null) {
            rsaCerts.add(tlsRsaCert);
        }

        CertificateChain dhRsaCert = getDhRsaCert();
        if (dhRsaCert != null) {
            rsaCerts.add(dhRsaCert);
        }

        CertificateChain ecDheRsaCert = getEcDheRsaCert();
        if (ecDheRsaCert != null) {
            rsaCerts.add(ecDheRsaCert);
        }

        rsaCerts.addAll(getEcdhRsaCerts());

        return rsaCerts;
    }

    private CertificateChain getRsaCert() {
        Config tlsConfig = getBasicConfig();
        LinkedList<CipherSuite> cipherSuitesToTest = new LinkedList<>();
        for (CipherSuite cipherSuite : CipherSuite.values()) {
            if (cipherSuite.isRSA() && cipherSuite.name().contains("TLS_RSA")) {
                cipherSuitesToTest.add(cipherSuite);
            }
        }
        return performCertScan(tlsConfig, cipherSuitesToTest);
    }

    private CertificateChain getDhRsaCert() {
        Config tlsConfig = getBasicConfig();
        LinkedList<CipherSuite> cipherSuitesToTest = new LinkedList<>();
        tlsConfig = getBasicConfig();
        cipherSuitesToTest = new LinkedList<>();
        for (CipherSuite cipherSuite : CipherSuite.values()) {
            if (cipherSuite.isRSA() && cipherSuite.name().contains("_DH_")) {
                cipherSuitesToTest.add(cipherSuite);
            }
        }
        return performCertScan(tlsConfig, cipherSuitesToTest);
    }

    private CertificateChain getEcDheRsaCert() {
        Config tlsConfig = getBasicConfig();
        LinkedList<CipherSuite> cipherSuitesToTest = new LinkedList<>();
        for (CipherSuite cipherSuite : CipherSuite.values()) {
            if (cipherSuite.isRSA() && (cipherSuite.name().contains("_DHE_") || cipherSuite.name().contains("_ECDHE_"))) {
                cipherSuitesToTest.add(cipherSuite);
            }
        }
        return performCertScan(tlsConfig, cipherSuitesToTest);
    }

    private List<CertificateChain> getEcdhRsaCerts() {
        List<CertificateChain> ecdhRsaCerts = new LinkedList<>();
        Config tlsConfig = getBasicConfig();
        LinkedList<CipherSuite> cipherSuitesToTest = new LinkedList<>();
        for (CipherSuite cipherSuite : CipherSuite.values()) {
            if (cipherSuite.isRSA() && cipherSuite.name().contains("_ECDH_")) {
                cipherSuitesToTest.add(cipherSuite);
            }
        }
        performEcCertScan(tlsConfig, getAllCurves(), cipherSuitesToTest, ecdhRsaCerts);
        return ecdhRsaCerts;
    }

    private List<CertificateChain> getEcdsaCerts() {
        LinkedList<CertificateChain> ecdsaCerts = new LinkedList<>();
        ecdsaCerts.addAll(getEcdhEcdsaCerts());
        ecdsaCerts.addAll(getEcdheEcdsaCerts());

        return ecdsaCerts;
    }

    private List<CertificateChain> getEcdhEcdsaCerts() {
        LinkedList<CertificateChain> ecdhEcdsaCerts = new LinkedList<>();
        Config tlsConfig = getBasicConfig();
        LinkedList<CipherSuite> cipherSuitesToTest = new LinkedList<>();
        for (CipherSuite cipherSuite : CipherSuite.values()) {
            if (cipherSuite.isECDSA() && cipherSuite.isEphemeral() == false) {
                cipherSuitesToTest.add(cipherSuite);
            }
        }
        performEcCertScanEcdsa(tlsConfig, getAllCurves(), cipherSuitesToTest, ecdhEcdsaCerts, ecdsaPkGroupsStatic, ecdsaCertSigGroupsStatic);
        return ecdhEcdsaCerts;
    }

    private List<CertificateChain> getEcdheEcdsaCerts() {
        LinkedList<CertificateChain> ecdheEcdsaCerts = new LinkedList<>();
        Config tlsConfig = getBasicConfig();
        LinkedList<CipherSuite> cipherSuitesToTest = new LinkedList<>();
        for (CipherSuite cipherSuite : CipherSuite.values()) {
            if (cipherSuite.isECDSA() && cipherSuite.isEphemeral()) {
                cipherSuitesToTest.add(cipherSuite);
            }
        }
        performEcCertScanEcdsa(tlsConfig, getAllCurves(), cipherSuitesToTest, ecdheEcdsaCerts, ecdsaPkGroupsEphemeral, ecdsaCertSigGroupsEphemeral);
        return ecdheEcdsaCerts;
    }

    private List<CertificateChain> getDssCerts() {
        LinkedList<CertificateChain> dssCerts = new LinkedList<>();

        CertificateChain dhDssCert = getDhDssCert();
        if (dhDssCert != null) {
            dssCerts.add(dhDssCert);
        }

        CertificateChain dheDssCert = getDheDssCert();
        if (dheDssCert != null) {
            dssCerts.add(dheDssCert);
        }
        return dssCerts;
    }

    private CertificateChain getDhDssCert() {
        Config tlsConfig = getBasicConfig();
        LinkedList<CipherSuite> cipherSuitesToTest = new LinkedList<>();
        for (CipherSuite cipherSuite : CipherSuite.values()) {
            if (cipherSuite.isDSS() && cipherSuite.isEphemeral() == false) {
                cipherSuitesToTest.add(cipherSuite);
            }
        }
        return performCertScan(tlsConfig, cipherSuitesToTest);
    }

    private CertificateChain getDheDssCert() {
        Config tlsConfig = getBasicConfig();
        LinkedList<CipherSuite> cipherSuitesToTest = new LinkedList<>();
        for (CipherSuite cipherSuite : CipherSuite.values()) {
            if (cipherSuite.isDSS() && cipherSuite.isEphemeral() == true) {
                cipherSuitesToTest.add(cipherSuite);
            }
        }
        return performCertScan(tlsConfig, cipherSuitesToTest);
    }

    private List<CertificateChain> getGostCert() {
        LinkedList<CertificateChain> gostCerts = new LinkedList<>();
        Config tlsConfig = getBasicConfig();

        LinkedList<CipherSuite> cipherSuitesToTest = new LinkedList<>();
        for (CipherSuite cipherSuite : CipherSuite.values()) {
            if (cipherSuite.isGOST()) {
                cipherSuitesToTest.add(cipherSuite);
            }
        }
        CertificateChain newCert = performCertScan(tlsConfig, cipherSuitesToTest);
        if (newCert != null) {
            gostCerts.add(newCert);
        }

        return gostCerts;
    }

    private List<CertificateChain> getTls13Certs() {
        LinkedList<CertificateChain> tls13Certs = new LinkedList<>();
        Config tlsConfig = getBasicConfig();
        tlsConfig.setHighestProtocolVersion(ProtocolVersion.TLS13);
        tlsConfig.setSupportedVersions(ProtocolVersion.TLS13);
        tlsConfig.setAddSupportedVersionsExtension(true);
        tlsConfig.setAddKeyShareExtension(true);

        CertificateChain rsaSigHashCert = getTls13CertRsaSigHash(tlsConfig);
        if (rsaSigHashCert != null) {
            tls13Certs.add(rsaSigHashCert);
        }

        tls13Certs.addAll(getTls13CertsEcdsaSigHash(tlsConfig));

        return tls13Certs;
    }

    private CertificateChain getTls13CertRsaSigHash(Config tlsConfig) {
        tlsConfig.setDefaultClientSupportedSignatureAndHashAlgorithms(getTls13RsaSigHash());
        tlsConfig.setDefaultClientNamedGroups(getTls13Curves());
        tlsConfig.setDefaultClientKeyShareNamedGroups(getTls13Curves());
        return performCertScan(tlsConfig, CipherSuite.getImplementedTls13CipherSuites());
    }

    private List<CertificateChain> getTls13CertsEcdsaSigHash(Config tlsConfig) {
        List<CertificateChain> tls13ecdsaCerts = new LinkedList<>();
        tlsConfig.setDefaultClientSupportedSignatureAndHashAlgorithms(getTls13EcdsaSigHash());
        performEcCertScanEcdsa(tlsConfig, getTls13Curves(), CipherSuite.getImplementedTls13CipherSuites(), tls13ecdsaCerts,
                ecdsaPkGroupsTls13, ecdsaCertSigGroupsTls13);
        return tls13ecdsaCerts;
    }

    private Config getBasicConfig() {
        Config tlsConfig = getScannerConfig().createConfig();
        tlsConfig.setQuickReceive(true);
        tlsConfig.setEarlyStop(true);
        tlsConfig.setStopActionsAfterIOException(true);
        tlsConfig.setWorkflowTraceType(WorkflowTraceType.HELLO);
        tlsConfig.setAddSignatureAndHashAlgorithmsExtension(true);
        tlsConfig.setAddServerNameIndicationExtension(true);
        tlsConfig.setAddECPointFormatExtension(true);
        tlsConfig.setAddEllipticCurveExtension(true);
        List<NamedGroup> namedGroups = new ArrayList<>(Arrays.asList(NamedGroup.values()));
        tlsConfig.setDefaultClientNamedGroups(namedGroups);
        List<SignatureAndHashAlgorithm> sigHashAlgos = Arrays.asList(SignatureAndHashAlgorithm.values());
        tlsConfig.setDefaultClientSupportedSignatureAndHashAlgorithms(sigHashAlgos);
        tlsConfig.setStopActionsAfterFatal(true);

        return tlsConfig;
    }

    private List<NamedGroup> getAllCurves() {
        LinkedList<NamedGroup> curves = new LinkedList<>();

        for (NamedGroup group : NamedGroup.values()) {
            if (group.isCurve()) {
                curves.add(group);
            }
        }

        return curves;
    }

    private List<NamedGroup> getTls13Curves() {
        LinkedList<NamedGroup> curves = new LinkedList<>();
        for (NamedGroup group : NamedGroup.values()) {
            if (group.isCurve() && group.isTls13()) {
                curves.add(group);
            }
        }

        return curves;
    }

    private CertificateChain performCertScan(Config tlsConfig, List<CipherSuite> cipherSuitesToTest) {
        tlsConfig.setDefaultClientSupportedCiphersuites(cipherSuitesToTest);
        State state = new State(tlsConfig);
        executeState(state);
        if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.CERTIFICATE, state.getWorkflowTrace())
                && cipherSuitesToTest.contains(state.getTlsContext().getSelectedCipherSuite())
                && state.getTlsContext().getServerCertificate() != null) {
            return new CertificateChain(state.getTlsContext().getServerCertificate(), tlsConfig
                    .getDefaultClientConnection().getHostname());
        } else {
            return null;
        }
    }

    private void performEcCertScan(Config tlsConfig, List<NamedGroup> groupsToTest,
            List<CipherSuite> cipherSuitesToTest, List<CertificateChain> certificateList) {
        tlsConfig.setDefaultClientSupportedCiphersuites(cipherSuitesToTest);
        tlsConfig.setDefaultClientNamedGroups(groupsToTest);
        do {
            State state = new State(tlsConfig);
            executeState(state);
            if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.CERTIFICATE, state.getWorkflowTrace())
                    && cipherSuitesToTest.contains(state.getTlsContext().getSelectedCipherSuite())
                    && state.getTlsContext().getServerCertificate() != null
                    && state.getTlsContext().getEcCertificateCurve() != null
                    && groupsToTest.contains(state.getTlsContext().getEcCertificateCurve())) {
                groupsToTest.remove(state.getTlsContext().getEcCertificateCurve());
                certificateList.add(new CertificateChain(state.getTlsContext().getServerCertificate(), tlsConfig
                        .getDefaultClientConnection().getHostname()));
            } else {
                // selected ciphersuite or certificate named group invalid
                cipherSuitesToTest.clear();
                groupsToTest.clear();
            }
        } while (groupsToTest.size() > 0 && cipherSuitesToTest.size() > 0);
    }

    private void performEcCertScanEcdsa(Config tlsConfig, List<NamedGroup> groupsToTest,
            List<CipherSuite> cipherSuitesToTest, List<CertificateChain> certificateList, List<NamedGroup> pkGroups,
            List<NamedGroup> sigGroups) {
        tlsConfig.setDefaultClientSupportedCiphersuites(cipherSuitesToTest);
        tlsConfig.setDefaultClientNamedGroups(groupsToTest);
        tlsConfig.setDefaultClientKeyShareNamedGroups(groupsToTest);
        do {
            State state = new State(tlsConfig);
            executeState(state);
            if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.CERTIFICATE, state.getWorkflowTrace())
                    && cipherSuitesToTest.contains(state.getTlsContext().getSelectedCipherSuite())
                    && state.getTlsContext().getServerCertificate() != null
                    && state.getTlsContext().getEcCertificateCurve() != null
                    && groupsToTest.contains(state.getTlsContext().getEcCertificateCurve())) {
                groupsToTest.remove(state.getTlsContext().getEcCertificateCurve());
                certificateList.add(new CertificateChain(state.getTlsContext().getServerCertificate(), tlsConfig
                        .getDefaultClientConnection().getHostname()));
                pkGroups.add(state.getTlsContext().getEcCertificateCurve());
                if (state.getTlsContext().getEcCertificateSignatureCurve() != null
                        && !sigGroups.contains(state.getTlsContext().getEcCertificateSignatureCurve())) {
                    sigGroups.add(state.getTlsContext().getEcCertificateSignatureCurve());
                }
            } else {
                // selected ciphersuite or certificate named group invalid
                cipherSuitesToTest.clear();
                groupsToTest.clear();
            }
        } while (groupsToTest.size() > 0 && cipherSuitesToTest.size() > 0);
    }

    private boolean certAlreadyInList(List<CertificateChain> certList, CertificateChain newCert) {
        for (CertificateChain cert : certList) {
            if (isSameCertificate(cert, newCert)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSameCertificate(CertificateChain cert1, CertificateChain cert2) {
        if (cert1.getCertificate().getCertificateList().length != cert2.getCertificate().getCertificateList().length) {
            return false;
        } else {
            for (int i = 0; i < cert1.getCertificate().getCertificateList().length; i++) {
                if (cert1.getCertificate().getCertificateList()[i].getSerialNumber().equals(
                        cert2.getCertificate().getCertificateList()[i].getSerialNumber()) == false) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<SignatureAndHashAlgorithm> getTls13RsaSigHash() {
        List<SignatureAndHashAlgorithm> algorithms = new LinkedList<>();
        for (SignatureAndHashAlgorithm algorithm : SignatureAndHashAlgorithm.getTls13SignatureAndHashAlgorithms()) {
            if (algorithm.name().contains("RSA")) {
                algorithms.add(algorithm);
            }
        }

        return algorithms;
    }

    private List<SignatureAndHashAlgorithm> getTls13EcdsaSigHash() {
        List<SignatureAndHashAlgorithm> algorithms = new LinkedList<>();
        for (SignatureAndHashAlgorithm algorithm : SignatureAndHashAlgorithm.getTls13SignatureAndHashAlgorithms()) {
            if (algorithm.name().contains("ECDSA")) {
                algorithms.add(algorithm);
            }
        }

        return algorithms;
    }

    private void addNewCertificates(List<CertificateChain> knownCertificates, List<CertificateChain> foundCertificates) {
        for (CertificateChain chain : foundCertificates) {
            if (!certAlreadyInList(knownCertificates, chain)) {
                knownCertificates.add(chain);
            }
        }
    }
}
