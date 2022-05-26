/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe;

import de.rub.nds.scanner.core.constants.ListResult;
import de.rub.nds.scanner.core.constants.TestResults;
import de.rub.nds.scanner.core.probe.requirements.Requirement;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlsscanner.core.constants.TlsProbeType;
import de.rub.nds.tlsscanner.core.probe.requirements.ProbeRequirement;
import de.rub.nds.tlsscanner.serverscanner.report.ServerReport;
import de.rub.nds.tlsscanner.serverscanner.selector.ConfigSelector;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ProtocolVersionProbe extends TlsServerProbe<ConfigSelector, ServerReport> {

    private List<ProtocolVersion> toTestList;
    private List<ProtocolVersion> supportedProtocolVersions;
    private List<ProtocolVersion> unsupportedProtocolVersions;

    public ProtocolVersionProbe(ConfigSelector configSelector, ParallelExecutor parallelExecutor) {
        super(parallelExecutor, TlsProbeType.PROTOCOL_VERSION, configSelector);
        toTestList = new LinkedList<>();
        if (configSelector.getScannerConfig().getDtlsDelegate().isDTLS()) {
            toTestList.add(ProtocolVersion.DTLS10_DRAFT);
            toTestList.add(ProtocolVersion.DTLS10);
            toTestList.add(ProtocolVersion.DTLS12);
        } else {
            toTestList.add(ProtocolVersion.SSL2);
            toTestList.add(ProtocolVersion.SSL3);
            toTestList.add(ProtocolVersion.TLS10);
            toTestList.add(ProtocolVersion.TLS11);
            toTestList.add(ProtocolVersion.TLS12);
            toTestList.add(ProtocolVersion.TLS13);
        }
        super.register(TlsAnalyzedProperty.SUPPORTS_DTLS_1_0_DRAFT, TlsAnalyzedProperty.SUPPORTS_DTLS_1_0, TlsAnalyzedProperty.SUPPORTS_DTLS_1_2,
            TlsAnalyzedProperty.SUPPORTS_SSL_2, TlsAnalyzedProperty.SUPPORTS_SSL_3,
            TlsAnalyzedProperty.SUPPORTS_TLS_1_0, TlsAnalyzedProperty.SUPPORTS_TLS_1_1,
            TlsAnalyzedProperty.SUPPORTS_TLS_1_2, TlsAnalyzedProperty.SUPPORTS_TLS_1_3,
            TlsAnalyzedProperty.LIST_SUPPORTED_PROTOCOLVERSIONS);
    }

    @Override
    public void executeTest() {
        supportedProtocolVersions = new LinkedList<>();
        unsupportedProtocolVersions = new LinkedList<>();

        for (ProtocolVersion version : toTestList) {
            if (isProtocolVersionSupported(version, false))
                supportedProtocolVersions.add(version);
            else
                unsupportedProtocolVersions.add(version);
        }
        if (supportedProtocolVersions.isEmpty()) {
            unsupportedProtocolVersions = new LinkedList<>();
            for (ProtocolVersion version : toTestList) {
                if (isProtocolVersionSupported(version, true))
                    supportedProtocolVersions.add(version);
                else
                    unsupportedProtocolVersions.add(version);
            }
        }
    }

    public boolean isProtocolVersionSupported(ProtocolVersion toTest, boolean intolerance) {
        if (toTest == ProtocolVersion.SSL2) {
            return isSSL2Supported();
        }
        Config tlsConfig;
        List<CipherSuite> cipherSuites = new LinkedList<>();
        if (!toTest.isTLS13()) {
            tlsConfig = configSelector.getBaseConfig();
            if (intolerance) {
                cipherSuites.addAll(CipherSuite.getImplemented());
            } else {
                cipherSuites.addAll(Arrays.asList(CipherSuite.values()));
                cipherSuites.remove(CipherSuite.TLS_FALLBACK_SCSV);
                cipherSuites.remove(CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV);
            }
        } else {
            tlsConfig = configSelector.getTls13BaseConfig();
            cipherSuites.addAll(CipherSuite.getTls13CipherSuites());
        }
        tlsConfig.setDefaultClientSupportedCipherSuites(cipherSuites);
        tlsConfig.setHighestProtocolVersion(toTest);
        tlsConfig.setWorkflowTraceType(WorkflowTraceType.DYNAMIC_HELLO);
        configSelector.repairConfig(tlsConfig);
        State state = new State(tlsConfig);
        executeState(state);

        if (toTest == ProtocolVersion.DTLS10_DRAFT) {
            Record record = (Record) WorkflowTraceUtil.getLastReceivedRecord(state.getWorkflowTrace());
            if (record != null) {
                ProtocolVersion version = ProtocolVersion.getProtocolVersion(record.getProtocolVersion().getValue());
                if (version != null) {
                    return version == ProtocolVersion.DTLS10_DRAFT;
                }
            }
            return false;
        } else {
            if (!WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO, state.getWorkflowTrace())) {
                LOGGER.debug("Did not receive ServerHello Message");
                LOGGER.debug(state.getWorkflowTrace().toString());
                return false;
            } else {
                LOGGER.debug("Received ServerHelloMessage");
                LOGGER.debug(state.getWorkflowTrace().toString());
                LOGGER.debug("Selected Version:" + state.getTlsContext().getSelectedProtocolVersion().name());
                return state.getTlsContext().getSelectedProtocolVersion() == toTest;
            }
        }
    }

    private boolean isSSL2Supported() {
        Config tlsConfig = configSelector.getSSL2BaseConfig();
        tlsConfig.setWorkflowTraceType(WorkflowTraceType.SSL2_HELLO);
        State state = new State(tlsConfig);
        executeState(state);
        return state.getWorkflowTrace().executedAsPlanned();
    }

    @Override
    public void adjustConfig(ServerReport report) {
    }

    @Override
    protected Requirement getRequirements(ServerReport report) {
        return ProbeRequirement.NO_REQUIREMENT;
    }

    @Override
    protected void mergeData(ServerReport report) {
        if (supportedProtocolVersions != null) {
            for (ProtocolVersion version : supportedProtocolVersions) {
            	if (version == ProtocolVersion.DTLS10_DRAFT)
                    super.put(TlsAnalyzedProperty.SUPPORTS_DTLS_1_0_DRAFT, TestResults.TRUE);
                if (version == ProtocolVersion.DTLS10)
                    super.put(TlsAnalyzedProperty.SUPPORTS_DTLS_1_0, TestResults.TRUE);
                if (version == ProtocolVersion.DTLS12)
                    super.put(TlsAnalyzedProperty.SUPPORTS_DTLS_1_2, TestResults.TRUE);
                if (version == ProtocolVersion.SSL2)
                    super.put(TlsAnalyzedProperty.SUPPORTS_SSL_2, TestResults.TRUE);
                if (version == ProtocolVersion.SSL3)
                    super.put(TlsAnalyzedProperty.SUPPORTS_SSL_3, TestResults.TRUE);
                if (version == ProtocolVersion.TLS10)
                    super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_0, TestResults.TRUE);
                if (version == ProtocolVersion.TLS11)
                    super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_1, TestResults.TRUE);
                if (version == ProtocolVersion.TLS12)
                    super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_2, TestResults.TRUE);
                if (version == ProtocolVersion.TLS13)
                    super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_3, TestResults.TRUE);
            }

            for (ProtocolVersion version : unsupportedProtocolVersions) {
            	if (version == ProtocolVersion.DTLS10_DRAFT)
                    super.put(TlsAnalyzedProperty.SUPPORTS_DTLS_1_0_DRAFT, TestResults.FALSE);
            	if (version == ProtocolVersion.DTLS10)
                    super.put(TlsAnalyzedProperty.SUPPORTS_DTLS_1_0, TestResults.FALSE);
                if (version == ProtocolVersion.DTLS12)
                    super.put(TlsAnalyzedProperty.SUPPORTS_DTLS_1_2, TestResults.FALSE);
                if (version == ProtocolVersion.SSL2)
                    super.put(TlsAnalyzedProperty.SUPPORTS_SSL_2, TestResults.FALSE);
                if (version == ProtocolVersion.SSL3)
                    super.put(TlsAnalyzedProperty.SUPPORTS_SSL_3, TestResults.FALSE);
                if (version == ProtocolVersion.TLS10)
                    super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_0, TestResults.FALSE);
                if (version == ProtocolVersion.TLS11)
                    super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_1, TestResults.FALSE);
                if (version == ProtocolVersion.TLS12)
                    super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_2, TestResults.FALSE);
                if (version == ProtocolVersion.TLS13)
                    super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_3, TestResults.FALSE);
            }
        } else {
            super.put(TlsAnalyzedProperty.SUPPORTS_DTLS_1_0_DRAFT, TestResults.COULD_NOT_TEST);
            super.put(TlsAnalyzedProperty.SUPPORTS_DTLS_1_0, TestResults.COULD_NOT_TEST);
            super.put(TlsAnalyzedProperty.SUPPORTS_DTLS_1_2, TestResults.COULD_NOT_TEST);
            super.put(TlsAnalyzedProperty.SUPPORTS_SSL_2, TestResults.COULD_NOT_TEST);
            super.put(TlsAnalyzedProperty.SUPPORTS_SSL_3, TestResults.COULD_NOT_TEST);
            super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_0, TestResults.COULD_NOT_TEST);
            super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_1, TestResults.COULD_NOT_TEST);
            super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_2, TestResults.COULD_NOT_TEST);
            super.put(TlsAnalyzedProperty.SUPPORTS_TLS_1_3, TestResults.COULD_NOT_TEST);
        }
        super.put(TlsAnalyzedProperty.LIST_SUPPORTED_PROTOCOLVERSIONS,
            new ListResult<ProtocolVersion>(supportedProtocolVersions, "SUPPORTED_PROTOCOLVERSIONS"));
    }
}
