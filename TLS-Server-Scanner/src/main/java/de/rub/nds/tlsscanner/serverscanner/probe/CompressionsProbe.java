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
import de.rub.nds.tlsattacker.core.constants.CompressionMethod;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlsscanner.core.constants.TlsProbeType;
import de.rub.nds.tlsscanner.core.probe.requirements.ProbeRequirement;
import de.rub.nds.tlsscanner.serverscanner.report.ServerReport;
import de.rub.nds.tlsscanner.serverscanner.selector.ConfigSelector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

       
public class CompressionsProbe extends TlsServerProbe<ConfigSelector, ServerReport> {

    private List<CompressionMethod> compressions;

    public CompressionsProbe(ConfigSelector configSelector, ParallelExecutor parallelExecutor) {
        super(parallelExecutor, TlsProbeType.COMPRESSIONS, configSelector);
        super.register(TlsAnalyzedProperty.VULNERABLE_TO_CRIME, TlsAnalyzedProperty.SUPPORTS_TLS_COMPRESSION,
                TlsAnalyzedProperty.LIST_SUPPORTED_COMPRESSION_METHODS);
    }

    @Override
    public void executeTest() {
        compressions = getSupportedCompressionMethods();
    }

    private List<CompressionMethod> getSupportedCompressionMethods() {
        CompressionMethod selectedCompressionMethod;
        List<CompressionMethod> supportedCompressionMethods = new LinkedList<>();
        List<CompressionMethod> toTestList = new ArrayList<>(Arrays.asList(CompressionMethod.values()));
        do {
            selectedCompressionMethod = testCompressionMethods(toTestList);
            if (!toTestList.contains(selectedCompressionMethod)) {
                LOGGER.debug("Server chose a CompressionMethod we did not offer!");
                break;
            }
            if (selectedCompressionMethod != null) {
                supportedCompressionMethods.add(selectedCompressionMethod);
                toTestList.remove(selectedCompressionMethod);
            }
        } while (selectedCompressionMethod != null || toTestList.size() > 0);
        return supportedCompressionMethods;
    }

    private CompressionMethod testCompressionMethods(List<CompressionMethod> compressionList) {
        Config tlsConfig = configSelector.getBaseConfig();
        tlsConfig.setWorkflowTraceType(WorkflowTraceType.DYNAMIC_HELLO);
        tlsConfig.setDefaultClientSupportedCompressionMethods(compressionList);
        State state = new State(tlsConfig);
        executeState(state);
        if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO, state.getWorkflowTrace())) {
            return state.getTlsContext().getSelectedCompressionMethod();
        } else {
            LOGGER.debug("Did not receive a ServerHello, something went wrong or the Server has some intolerance");
            return null;
        }
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
        if (compressions != null) {
            super.put(TlsAnalyzedProperty.LIST_SUPPORTED_COMPRESSION_METHODS,
                new ListResult<CompressionMethod>(compressions, "COMPRESSION_METHODS"));
            if (compressions.contains(CompressionMethod.LZS) || compressions.contains(CompressionMethod.DEFLATE)) {
                super.put(TlsAnalyzedProperty.VULNERABLE_TO_CRIME, TestResults.TRUE);
                super.put(TlsAnalyzedProperty.SUPPORTS_TLS_COMPRESSION, TestResults.TRUE);
            } else {
                super.put(TlsAnalyzedProperty.VULNERABLE_TO_CRIME, TestResults.FALSE);
                super.put(TlsAnalyzedProperty.SUPPORTS_TLS_COMPRESSION, TestResults.FALSE);
            }
        } else {
            super.put(TlsAnalyzedProperty.VULNERABLE_TO_CRIME, TestResults.COULD_NOT_TEST);
            super.put(TlsAnalyzedProperty.SUPPORTS_TLS_COMPRESSION, TestResults.COULD_NOT_TEST);
            super.put(TlsAnalyzedProperty.LIST_SUPPORTED_COMPRESSION_METHODS, null);
        }
    }
}
