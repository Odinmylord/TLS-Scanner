/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe;

import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlgorithmResolver;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.KeyExchangeAlgorithm;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.serverscanner.config.ScannerConfig;
import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResults;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;
import de.rub.nds.tlsscanner.serverscanner.report.result.CipherSuiteResult;
import de.rub.nds.tlsscanner.serverscanner.report.result.ProbeResult;
import de.rub.nds.tlsscanner.serverscanner.report.result.VersionSuiteListPair;
import de.rub.nds.tlsscanner.serverscanner.requirements.ProbeRequirement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CipherSuiteProbe extends TlsProbe {

    private final List<ProtocolVersion> protocolVersions;

    public CipherSuiteProbe(ScannerConfig config, ParallelExecutor parallelExecutor) {
        super(parallelExecutor, ProbeType.CIPHER_SUITE, config);
        protocolVersions = new LinkedList<>();
        super.properties.add(AnalyzedProperty.SUPPORTS_NULL_CIPHERS);
        super.properties.add(AnalyzedProperty.SUPPORTS_ANON);
        super.properties.add(AnalyzedProperty.SUPPORTS_EXPORT);
        super.properties.add(AnalyzedProperty.SUPPORTS_DES);
        super.properties.add(AnalyzedProperty.SUPPORTS_SEED);
        super.properties.add(AnalyzedProperty.SUPPORTS_IDEA);
        super.properties.add(AnalyzedProperty.SUPPORTS_RC2);
        super.properties.add(AnalyzedProperty.SUPPORTS_RC4);
        super.properties.add(AnalyzedProperty.SUPPORTS_3DES);
        super.properties.add(AnalyzedProperty.SUPPORTS_POST_QUANTUM);
        super.properties.add(AnalyzedProperty.SUPPORTS_AEAD);
        super.properties.add(AnalyzedProperty.SUPPORTS_PFS);
        super.properties.add(AnalyzedProperty.SUPPORTS_ONLY_PFS);
        super.properties.add(AnalyzedProperty.SUPPORTS_AES);
        super.properties.add(AnalyzedProperty.SUPPORTS_CAMELLIA);
        super.properties.add(AnalyzedProperty.SUPPORTS_ARIA);
        super.properties.add(AnalyzedProperty.SUPPORTS_CHACHA);
        super.properties.add(AnalyzedProperty.SUPPORTS_RSA);
        super.properties.add(AnalyzedProperty.SUPPORTS_DH);
        super.properties.add(AnalyzedProperty.SUPPORTS_STATIC_ECDH);
        super.properties.add(AnalyzedProperty.SUPPORTS_ECDSA);
        super.properties.add(AnalyzedProperty.SUPPORTS_RSA_CERT);
        super.properties.add(AnalyzedProperty.SUPPORTS_DSS);
        super.properties.add(AnalyzedProperty.SUPPORTS_ECDH);
        super.properties.add(AnalyzedProperty.SUPPORTS_GOST);
        super.properties.add(AnalyzedProperty.SUPPORTS_SRP);
        super.properties.add(AnalyzedProperty.SUPPORTS_KERBEROS);
        super.properties.add(AnalyzedProperty.SUPPORTS_PSK_PLAIN);
        super.properties.add(AnalyzedProperty.SUPPORTS_PSK_RSA);
        super.properties.add(AnalyzedProperty.SUPPORTS_PSK_DHE);
        super.properties.add(AnalyzedProperty.SUPPORTS_PSK_ECDHE);
        super.properties.add(AnalyzedProperty.SUPPORTS_FORTEZZA);
        super.properties.add(AnalyzedProperty.SUPPORTS_NEWHOPE);
        super.properties.add(AnalyzedProperty.SUPPORTS_ECMQV);
        super.properties.add(AnalyzedProperty.PREFERS_PFS);
        super.properties.add(AnalyzedProperty.SUPPORTS_STREAM_CIPHERS);
        super.properties.add(AnalyzedProperty.SUPPORTS_BLOCK_CIPHERS);
        super.properties.add(AnalyzedProperty.SUPPORTS_LEGACY_PRF);
        super.properties.add(AnalyzedProperty.SUPPORTS_SHA256_PRF);
        super.properties.add(AnalyzedProperty.SUPPORTS_SHA384_PRF);
    }

    @Override
    public void executeTest() {
        List<VersionSuiteListPair> pairLists = new LinkedList<>();
        for (ProtocolVersion version : protocolVersions) {
            LOGGER.debug("Testing:" + version.name());
            if (version.isTLS13()) {
                pairLists.add(new VersionSuiteListPair(version, getSupportedCipherSuites()));
            } else {
                List<CipherSuite> toTestList = new LinkedList<>();
                List<CipherSuite> versionSupportedSuites = new LinkedList<>();
                if (version == ProtocolVersion.SSL3) {
                    toTestList.addAll(CipherSuite.SSL3_SUPPORTED_CIPHERSUITES);
                    versionSupportedSuites = getSupportedCipherSuitesWithIntolerance(toTestList, version);
                } else {
                    toTestList.addAll(Arrays.asList(CipherSuite.values()));
                    toTestList.remove(CipherSuite.TLS_FALLBACK_SCSV);
                    toTestList.remove(CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV);
                    versionSupportedSuites = getSupportedCipherSuitesWithIntolerance(toTestList, version);
                    if (versionSupportedSuites.isEmpty()) {
                        versionSupportedSuites = getSupportedCipherSuitesWithIntolerance(version);
                    }
                }
                if (versionSupportedSuites.size() > 0) {
                    pairLists.add(new VersionSuiteListPair(version, versionSupportedSuites));
                }
            }
        }
        return;// new CipherSuiteResult(pairLists);
    }

    private List<CipherSuite> getSupportedCipherSuites() {
        CipherSuite selectedSuite = null;
        List<CipherSuite> toTestList = new LinkedList<>();
        List<CipherSuite> supportedSuits = new LinkedList<>();
        for (CipherSuite suite : CipherSuite.values()) {
            if (suite.isTLS13()) {
                toTestList.add(suite);
            }
        }
        do {
            selectedSuite = getSelectedCipherSuite(toTestList);

            if (selectedSuite != null) {
                if (!toTestList.contains(selectedSuite)) {
                    LOGGER.warn("Server chose a CipherSuite we did not propose!");
                    // TODO write to site report
                    break;
                }
                supportedSuits.add(selectedSuite);
                toTestList.remove(selectedSuite);
            }
        } while (selectedSuite != null && !toTestList.isEmpty());
        return supportedSuits;
    }

    private CipherSuite getSelectedCipherSuite(List<CipherSuite> toTestList) {
        Config tlsConfig = getScannerConfig().createConfig();
        tlsConfig.setQuickReceive(true);
        tlsConfig.setDefaultClientSupportedCipherSuites(toTestList);
        tlsConfig.setHighestProtocolVersion(ProtocolVersion.TLS13);
        tlsConfig.setSupportedVersions(ProtocolVersion.TLS13);
        tlsConfig.setEnforceSettings(false);
        tlsConfig.setEarlyStop(true);
        tlsConfig.setStopReceivingAfterFatal(true);
        tlsConfig.setStopActionsAfterFatal(true);
        tlsConfig.setWorkflowTraceType(WorkflowTraceType.HELLO);
        tlsConfig.setDefaultClientNamedGroups(NamedGroup.getImplemented());
        tlsConfig.setAddECPointFormatExtension(false);
        tlsConfig.setAddEllipticCurveExtension(true);
        tlsConfig.setAddSignatureAndHashAlgorithmsExtension(true);
        tlsConfig.setAddSupportedVersionsExtension(true);
        tlsConfig.setDefaultClientKeyShareNamedGroups(new LinkedList<>());
        tlsConfig.setAddKeyShareExtension(true);
        tlsConfig.setAddCertificateStatusRequestExtension(true);
        tlsConfig.setUseFreshRandom(true);
        tlsConfig.setDefaultClientSupportedSignatureAndHashAlgorithms(
            SignatureAndHashAlgorithm.getImplementedTls13SignatureAndHashAlgorithms());

        State state = new State(tlsConfig);
        executeState(state);
        if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO, state.getWorkflowTrace())) {
            return state.getTlsContext().getSelectedCipherSuite();
        } else if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.HELLO_RETRY_REQUEST,
            state.getWorkflowTrace())) {
            return state.getTlsContext().getSelectedCipherSuite();
        } else {
            LOGGER.debug("Did not receive ServerHello Message");
            LOGGER.debug(state.getWorkflowTrace().toString());
            return null;
        }
    }

    public List<CipherSuite> getSupportedCipherSuitesWithIntolerance(ProtocolVersion version) {
        return getSupportedCipherSuitesWithIntolerance(new ArrayList<>(CipherSuite.getImplemented()), version);
    }

    public List<CipherSuite> getSupportedCipherSuitesWithIntolerance(List<CipherSuite> toTestList,
        ProtocolVersion version) {
        List<CipherSuite> listWeSupport = new LinkedList<>(toTestList);
        List<CipherSuite> supported = new LinkedList<>();

        boolean supportsMore = false;
        do {
            Config config = getScannerConfig().createConfig();
            config.setDefaultClientSupportedCipherSuites(listWeSupport);
            config.setDefaultSelectedProtocolVersion(version);
            config.setHighestProtocolVersion(version);
            config.setEnforceSettings(true);
            boolean containsEc = false;
            for (CipherSuite suite : config.getDefaultClientSupportedCipherSuites()) {
                KeyExchangeAlgorithm keyExchangeAlgorithm = AlgorithmResolver.getKeyExchangeAlgorithm(suite);
                if (keyExchangeAlgorithm != null && keyExchangeAlgorithm.name().toUpperCase().contains("EC")) {
                    containsEc = true;
                    break;
                }
            }
            config.setAddEllipticCurveExtension(containsEc);
            config.setAddECPointFormatExtension(containsEc);
            config.setAddSignatureAndHashAlgorithmsExtension(true);
            config.setAddRenegotiationInfoExtension(true);
            config.setWorkflowTraceType(WorkflowTraceType.DYNAMIC_HELLO);
            config.setQuickReceive(true);
            config.setEarlyStop(true);
            config.setStopReceivingAfterFatal(true);
            config.setStopActionsAfterIOException(true);
            config.setStopActionsAfterFatal(true);
            List<NamedGroup> namedGroup = new LinkedList<>();
            namedGroup.addAll(Arrays.asList(NamedGroup.values()));
            config.setDefaultClientNamedGroups(namedGroup);
            State state = new State(config);
            executeState(state);
            if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO, state.getWorkflowTrace())) {
                if (state.getTlsContext().getSelectedProtocolVersion() != version) {
                    LOGGER.debug("Server does not support " + version);
                    return new LinkedList<>();
                }
                LOGGER.debug("Server chose " + state.getTlsContext().getSelectedCipherSuite().name());
                if (listWeSupport.contains(state.getTlsContext().getSelectedCipherSuite())) {
                    supportsMore = true;
                    supported.add(state.getTlsContext().getSelectedCipherSuite());
                    listWeSupport.remove(state.getTlsContext().getSelectedCipherSuite());
                } else {
                    supportsMore = false;
                    LOGGER.warn("Server chose not proposed cipher suite");
                }
            } else {
                supportsMore = false;
                LOGGER.debug("Server did not send ServerHello");
                LOGGER.debug(state.getWorkflowTrace().toString());
                if (state.getTlsContext().isReceivedFatalAlert()) {
                    LOGGER.debug("Received Fatal Alert");
                    AlertMessage alert = (AlertMessage) WorkflowTraceUtil
                        .getFirstReceivedMessage(ProtocolMessageType.ALERT, state.getWorkflowTrace());
                    LOGGER.debug("Type:" + alert.toString());

                }
            }
        } while (supportsMore);
        return supported;
    }

    @Override
    protected ProbeRequirement getRequirements(SiteReport report) {
        return new ProbeRequirement(report).requireProbeTypes(ProbeType.PROTOCOL_VERSION);
    }

    @Override
    public void adjustConfig(SiteReport report) {
        if (report.getResult(AnalyzedProperty.SUPPORTS_DTLS_1_0) == TestResults.TRUE) {
            protocolVersions.add(ProtocolVersion.DTLS10);
        }
        if (report.getResult(AnalyzedProperty.SUPPORTS_DTLS_1_2) == TestResults.TRUE) {
            protocolVersions.add(ProtocolVersion.DTLS12);
        }
        if (report.getResult(AnalyzedProperty.SUPPORTS_SSL_3) == TestResults.TRUE) {
            protocolVersions.add(ProtocolVersion.SSL3);
        }
        if (report.getResult(AnalyzedProperty.SUPPORTS_TLS_1_0) == TestResults.TRUE) {
            protocolVersions.add(ProtocolVersion.TLS10);
        }
        if (report.getResult(AnalyzedProperty.SUPPORTS_TLS_1_1) == TestResults.TRUE) {
            protocolVersions.add(ProtocolVersion.TLS11);
        }
        if (report.getResult(AnalyzedProperty.SUPPORTS_TLS_1_2) == TestResults.TRUE) {
            protocolVersions.add(ProtocolVersion.TLS12);
        }
        if (report.getResult(AnalyzedProperty.SUPPORTS_TLS_1_3) == TestResults.TRUE) {
            protocolVersions.add(ProtocolVersion.TLS13);
        }
    }

    @Override
    public ProbeResult getCouldNotExecuteResult() {
        return new CipherSuiteResult(null);
    }
}
