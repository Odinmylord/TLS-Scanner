/*
 * TLS-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tlsscanner.core.passive;

import de.rub.nds.scanner.core.passive.StatExtractor;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.crypto.keys.CustomDhPublicKey;
import de.rub.nds.tlsattacker.core.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.core.protocol.message.DHClientKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.protocol.message.DHEServerKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;

import java.math.BigInteger;
import java.util.List;

public class DhPublicKeyExtractor extends StatExtractor<CustomDhPublicKey> {

    public DhPublicKeyExtractor() {
        super(TrackableValueType.DHE_PUBLICKEY);
    }

    @Override
    public void extract(State state) {
        WorkflowTrace trace = state.getWorkflowTrace();
        List<ProtocolMessage> allReceivedMessages =
                WorkflowTraceUtil.getAllReceivedMessages(trace, ProtocolMessageType.HANDSHAKE);
        if (state.getRunningMode() == RunningModeType.CLIENT) {
            for (ProtocolMessage message : allReceivedMessages) {
                if (message instanceof DHEServerKeyExchangeMessage) {
                    put(
                            new CustomDhPublicKey(
                                    new BigInteger(
                                            1,
                                            ((DHEServerKeyExchangeMessage) message)
                                                    .getModulus()
                                                    .getValue()),
                                    new BigInteger(
                                            1,
                                            ((DHEServerKeyExchangeMessage) message)
                                                    .getGenerator()
                                                    .getValue()),
                                    new BigInteger(
                                            1,
                                            ((DHEServerKeyExchangeMessage) message)
                                                    .getPublicKey()
                                                    .getValue())));
                }
            }
        } else {
            for (ProtocolMessage message : allReceivedMessages) {
                if (message instanceof DHClientKeyExchangeMessage) {
                    CustomDhPublicKey publicKey = new CustomDhPublicKey();
                    publicKey.setModulus(state.getTlsContext().getChooser().getServerDhModulus());
                    publicKey.setGenerator(
                            state.getTlsContext().getChooser().getServerDhGenerator());
                    publicKey.setPublicKey(
                            new BigInteger(
                                    1,
                                    ((DHClientKeyExchangeMessage) message)
                                            .getPublicKey()
                                            .getValue()));
                    put(publicKey);
                }
            }
        }
    }
}
