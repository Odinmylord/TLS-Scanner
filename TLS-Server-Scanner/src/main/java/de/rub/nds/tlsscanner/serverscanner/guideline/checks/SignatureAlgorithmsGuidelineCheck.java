/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.guideline.checks;

import de.rub.nds.scanner.core.constants.ListResult;
import de.rub.nds.scanner.core.constants.TestResult;
import de.rub.nds.scanner.core.constants.TestResults;
import de.rub.nds.tlsattacker.core.constants.SignatureAlgorithm;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlsscanner.core.guideline.GuidelineCheck;
import de.rub.nds.tlsscanner.core.guideline.GuidelineCheckCondition;
import de.rub.nds.tlsscanner.core.guideline.GuidelineCheckResult;
import de.rub.nds.tlsscanner.core.guideline.RequirementLevel;
import de.rub.nds.tlsscanner.serverscanner.guideline.results.SignatureAlgorithmsGuidelineCheckResult;
import de.rub.nds.tlsscanner.serverscanner.report.ServerReport;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SignatureAlgorithmsGuidelineCheck extends GuidelineCheck<ServerReport> {

    private List<SignatureAlgorithm> recommendedAlgorithms;

    private SignatureAlgorithmsGuidelineCheck() {
        super(null, null);
    }

    public SignatureAlgorithmsGuidelineCheck(String name, RequirementLevel requirementLevel,
        List<SignatureAlgorithm> recommendedAlgorithms) {
        super(name, requirementLevel);
        this.recommendedAlgorithms = recommendedAlgorithms;
    }

    public SignatureAlgorithmsGuidelineCheck(String name, RequirementLevel requirementLevel,
        GuidelineCheckCondition condition, List<SignatureAlgorithm> recommendedAlgorithms) {
        super(name, requirementLevel, condition);
        this.recommendedAlgorithms = recommendedAlgorithms;
    }

    @SuppressWarnings("unchecked")
    @Override
    public GuidelineCheckResult evaluate(ServerReport report) {

        TestResult samResult_cert =
            report.getResultMap().get(TlsAnalyzedProperty.LIST_SUPPORTED_SIGNATUREANDHASH_ALGORITHMS_CERT.name());
        TestResult samResult_ske =
            report.getResultMap().get(TlsAnalyzedProperty.LIST_SUPPORTED_SIGNATUREANDHASH_ALGORITHMS_SKE.name());
        if (samResult_cert != null || samResult_ske != null) {
            Set<SignatureAlgorithm> notRecommended = new HashSet<>();
            List<SignatureAndHashAlgorithm> algorithms = new LinkedList<>();
            if (samResult_cert != null && ((ListResult<SignatureAndHashAlgorithm>) samResult_cert).getList() != null)
                algorithms.addAll(((ListResult<SignatureAndHashAlgorithm>) samResult_cert).getList());
            if (samResult_ske != null && ((ListResult<SignatureAndHashAlgorithm>) samResult_ske).getList() != null)
                algorithms.addAll(((ListResult<SignatureAndHashAlgorithm>) samResult_ske).getList());
            // can algorithm be empty? check required? TODO
            for (SignatureAndHashAlgorithm alg : algorithms) {
                if (!this.recommendedAlgorithms.contains(alg.getSignatureAlgorithm())) {
                    notRecommended.add(alg.getSignatureAlgorithm());
                }
            }
            return new SignatureAlgorithmsGuidelineCheckResult(TestResults.of(notRecommended.isEmpty()),
                notRecommended);
        } else
            return new SignatureAlgorithmsGuidelineCheckResult(TestResults.UNCERTAIN, null);
    }

    @Override
    public String getId() {
        return "SignatureAlgorithms_" + getRequirementLevel() + "_" + recommendedAlgorithms;
    }

    public List<SignatureAlgorithm> getRecommendedAlgorithms() {
        return recommendedAlgorithms;
    }
}
