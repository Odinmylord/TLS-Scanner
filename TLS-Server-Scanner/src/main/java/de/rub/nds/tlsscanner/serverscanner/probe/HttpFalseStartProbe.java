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
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.https.HttpsResponseMessage;
import de.rub.nds.tlsattacker.core.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ChangeCipherSpecMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.FinishedMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloDoneMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveTillAction;
import de.rub.nds.tlsattacker.core.workflow.action.ReceivingAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendDynamicClientKeyExchangeAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.serverscanner.config.ScannerConfig;
import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResults;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;
import de.rub.nds.tlsscanner.serverscanner.report.result.HttpFalseStartResult;
import de.rub.nds.tlsscanner.serverscanner.report.result.ProbeResult;
import de.rub.nds.tlsscanner.serverscanner.requirements.ProbeRequirement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class HttpFalseStartProbe extends HttpsProbe {

    public HttpFalseStartProbe(ScannerConfig scannerConfig, ParallelExecutor parallelExecutor) {
        super(parallelExecutor, ProbeType.HTTP_FALSE_START, scannerConfig);
        super.properties.add(AnalyzedProperty.SUPPORTS_HTTP_FALSE_START);
    }

    @Override
    public void executeTest() {
        Config tlsConfig = getConfig();

        WorkflowConfigurationFactory factory = new WorkflowConfigurationFactory(tlsConfig);
        WorkflowTrace trace = factory.createTlsEntryWorkflowTrace(tlsConfig.getDefaultClientConnection());
        trace.addTlsAction(new SendAction(new ClientHelloMessage(tlsConfig)));
        trace.addTlsAction(new ReceiveTillAction(new ServerHelloDoneMessage()));
        trace.addTlsAction(new SendDynamicClientKeyExchangeAction());
        trace
            .addTlsAction(new SendAction(new ChangeCipherSpecMessage(), new FinishedMessage(), this.getHttpsRequest()));
        trace.addTlsAction(
            new ReceiveAction(new ChangeCipherSpecMessage(), new FinishedMessage(), new HttpsResponseMessage()));

        State state = new State(tlsConfig, trace);
        executeState(state);

        boolean receivedServerFinishedMessage = false;
        ReceivingAction action = trace.getLastReceivingAction();
        if (action.getReceivedMessages() != null) {
            for (ProtocolMessage message : action.getReceivedMessages()) {
                if (message instanceof HttpsResponseMessage) {
                    // if http response was received the server handled the
                    // false start
                   // return new HttpFalseStartResult(TestResults.TRUE);
                	return;
                } else if (message instanceof FinishedMessage) {
                    receivedServerFinishedMessage = true;
                }
            }
        }
        if (!receivedServerFinishedMessage) {
            // server sent no finished message, false start messed up the
            // handshake
          //  return new HttpFalseStartResult(TestResults.FALSE);
        	return;
        }
        // received no http response -> maybe server did not understand
        // request
       // return new HttpFalseStartResult(TestResults.UNCERTAIN);
    }

    private Config getConfig() {
        Config tlsConfig = getScannerConfig().createConfig();
        tlsConfig.setQuickReceive(true);
        tlsConfig.setDefaultClientSupportedCipherSuites(this.getCipherSuites());
        tlsConfig.setHighestProtocolVersion(ProtocolVersion.TLS12);
        tlsConfig.setEnforceSettings(false);
        tlsConfig.setEarlyStop(true);
        tlsConfig.setStopReceivingAfterFatal(true);
        tlsConfig.setStopActionsAfterFatal(true);
        tlsConfig.setHttpsParsingEnabled(true);
        tlsConfig.setWorkflowTraceType(WorkflowTraceType.HTTPS);
        tlsConfig.setStopActionsAfterIOException(true);
        tlsConfig.setAddECPointFormatExtension(true);
        tlsConfig.setAddEllipticCurveExtension(true);
        tlsConfig.setAddSignatureAndHashAlgorithmsExtension(true);
        tlsConfig.setAddRenegotiationInfoExtension(true);
        tlsConfig.setDefaultClientNamedGroups(NamedGroup.getImplemented());
        return tlsConfig;
    }

    private List<CipherSuite> getCipherSuites() {
        List<CipherSuite> cipherSuites = new LinkedList<>(Arrays.asList(CipherSuite.values()));
        cipherSuites.remove(CipherSuite.TLS_FALLBACK_SCSV);
        cipherSuites.remove(CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV);
        return cipherSuites;
    }

    @Override
    protected ProbeRequirement getRequirements(SiteReport report) {
        return new ProbeRequirement(report).requireAnalyzedProperties(AnalyzedProperty.SUPPORTS_HTTPS);
    }

    @Override
    public void adjustConfig(SiteReport report) {
    }

    @Override
    public ProbeResult getCouldNotExecuteResult() {
        return new HttpFalseStartResult(TestResults.COULD_NOT_TEST);
    }
}
