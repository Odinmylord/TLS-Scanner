/*
 * TLS-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe;

import de.rub.nds.scanner.core.constants.TestResult;
import de.rub.nds.scanner.core.constants.TestResults;
import de.rub.nds.scanner.core.probe.requirements.Requirement;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.layer.context.TlsContext;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.HelloVerifyRequestMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloDoneMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveTillAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsattacker.transport.TransportHandlerType;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlsscanner.core.constants.TlsProbeType;
import de.rub.nds.tlsscanner.serverscanner.report.ServerReport;
import de.rub.nds.tlsscanner.serverscanner.selector.ConfigSelector;

/**
 * Determines whether the server uses the client IP address for the DTLS cookie generation. It requires a proxy so we
 * limit the probe.
 */
public class DtlsIpAddressInCookieProbe extends TlsServerProbe<ConfigSelector, ServerReport> {

    private static final String PROXY_CONTROL_HOSTNAME = "195.37.190.89";
    private static final int PROXY_CONTROL_PORT = 5555;
    private static final String PROXY_DATA_HOSTNAME = "195.37.190.89";
    private static final int PROXY_DATA_PORT = 4444;

    private TestResult usesIpAdressInCookie;

    public DtlsIpAddressInCookieProbe(ConfigSelector configSelector, ParallelExecutor parallelExecutor) {
        super(parallelExecutor, TlsProbeType.DTLS_IP_ADDRESS_IN_COOKIE, configSelector);
        register(TlsAnalyzedProperty.USES_IP_ADDRESS_FOR_COOKIE);
    }

    @Override
    public void executeTest() {
        Config config = configSelector.getBaseConfig();
        config.getDefaultClientConnection().setTransportHandlerType(TransportHandlerType.UDP_PROXY);
        config.getDefaultClientConnection().setProxyControlHostname(PROXY_CONTROL_HOSTNAME);
        config.getDefaultClientConnection().setProxyControlPort(PROXY_CONTROL_PORT);
        config.getDefaultClientConnection().setProxyDataHostname(PROXY_DATA_HOSTNAME);
        config.getDefaultClientConnection().setProxyDataPort(PROXY_DATA_PORT);
        WorkflowTrace trace =
            new WorkflowConfigurationFactory(config).createTlsEntryWorkflowTrace(config.getDefaultClientConnection());
        trace.addTlsAction(new SendAction(new ClientHelloMessage(config)));
        trace.addTlsAction(new ReceiveAction(new HelloVerifyRequestMessage()));
        State state = new State(config, trace);
        TlsContext oldContext = state.getTlsContext();
        executeState(state);
        if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.HELLO_VERIFY_REQUEST, state.getWorkflowTrace())) {
            config = configSelector.getBaseConfig();
            config.getDefaultClientConnection().setSourcePort(3333);
            trace = new WorkflowConfigurationFactory(config)
                .createTlsEntryWorkflowTrace(config.getDefaultClientConnection());
            trace.addTlsAction(new SendAction(new ClientHelloMessage(config)));
            trace.addTlsAction(new ReceiveTillAction(new ServerHelloDoneMessage()));
            state = new State(config, trace);
            state.getTlsContext().setClientRandom(oldContext.getClientRandom());
            state.getTlsContext().setDtlsCookie(oldContext.getDtlsCookie());
            executeState(state);
            usesIpAdressInCookie =
                WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO, state.getWorkflowTrace())
                    ? TestResults.TRUE : TestResults.FALSE;
        } else {
            usesIpAdressInCookie = TestResults.CANNOT_BE_TESTED;
        }
    }

    @Override
    public void adjustConfig(ServerReport report) {
    }

    @Override
    protected void mergeData(ServerReport report) {
        put(TlsAnalyzedProperty.USES_IP_ADDRESS_FOR_COOKIE, usesIpAdressInCookie);
    }

    @Override
    protected Requirement getRequirements() {
        return Requirement.NO_REQUIREMENT;
    }
}
