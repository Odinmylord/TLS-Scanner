/**
 * TLS-Client-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.clientscanner.report;

import de.rub.nds.scanner.core.constants.ScannerDetail;
import de.rub.nds.scanner.core.report.ScanReport;
import de.rub.nds.scanner.core.report.container.ReportContainer;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class ClientReport extends ScanReport {

    public ClientReport() {
        super();
    }

    @Override
    public String getFullReport(ScannerDetail detail, boolean printColorful) {
        ClientContainerReportCreator creator = new ClientContainerReportCreator();
        ReportContainer createReport = creator.createReport(this);
        StringBuilder builder = new StringBuilder();
        createReport.print(builder, 0, printColorful);
        return builder.toString();
    }
}
