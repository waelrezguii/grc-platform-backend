package com.project.grcplatform.seed;

import com.project.grcplatform.model.*;
import com.project.grcplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(2)
public class ReferenceDataSeeder implements CommandLineRunner {

    private final KriCategoryRepository kriCategoryRepository;
    private final AuditTypeRepository auditTypeRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final ThreatTypeRepository threatTypeRepository;
    private final ControlCategoryRepository controlCategoryRepository;
    private final ControlTypeRepository controlTypeRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final ResponsibilityTypeRepository responsibilityTypeRepository;

    @Override
    public void run(String... args) {
        seedKriCategories();
        seedAuditTypes();
        seedAssetTypes();
        seedAssetCategories();
        seedThreatTypes();
        seedControlCategories();
        seedControlTypes();
        seedPolicyTypes();
        seedDocumentTypes();
        seedResponsibilityTypes();
    }

    private void seedKriCategories() {
        List<String[]> categories = List.of(
                new String[]{"VULNERABILITY_EXPOSURE", "Tracks exposure level based on open or unresolved vulnerabilities"},
                new String[]{"CONTROL_EFFECTIVENESS",  "Measures how effectively controls reduce risk"},
                new String[]{"RISK_LEVEL",             "Overall risk level across assessed scenarios"},
                new String[]{"COMPLIANCE_RATE",        "Percentage of compliance requirements met"},
                new String[]{"INCIDENT_RATE",          "Frequency of security or operational incidents"},
                new String[]{"TREATMENT_PROGRESS",     "Progress of treatment plans and associated actions"},
                new String[]{"ASSET_CRITICALITY",      "Criticality score of assets under management"}
        );
        for (String[] entry : categories) {
            if (!kriCategoryRepository.existsByName(entry[0])) {
                kriCategoryRepository.save(KriCategory.builder()
                        .name(entry[0]).description(entry[1]).build());
            }
        }
    }

    private void seedAuditTypes() {
        List<String[]> types = List.of(
                new String[]{"INTERNAL",     "Audit conducted internally by the organisation's own team"},
                new String[]{"EXTERNAL",     "Audit conducted by an independent external party"},
                new String[]{"REGULATORY",   "Audit required by a regulatory body or legal obligation"},
                new String[]{"SURVEILLANCE", "Ongoing monitoring audit between full certification cycles"}
        );
        for (String[] entry : types) {
            if (!auditTypeRepository.existsByName(entry[0])) {
                auditTypeRepository.save(AuditType.builder()
                        .name(entry[0]).description(entry[1]).build());
            }
        }
    }

    private void seedAssetTypes() {
        List<String[]> types = List.of(
                new String[]{"PROCESS",     "Business process or procedure"},
                new String[]{"INFORMATION", "Data or information asset"},
                new String[]{"EQUIPMENT",   "Physical hardware or equipment"},
                new String[]{"LOCAL",       "Physical facility or location"},
                new String[]{"DOCUMENT",    "Document or documentation asset"},
                new String[]{"HR",          "Human resource"},
                new String[]{"SUPPLIER",    "External supplier or vendor"},
                new String[]{"APPLICATION", "Software application or system"}
        );
        for (String[] entry : types) {
            if (!assetTypeRepository.existsByName(entry[0])) {
                assetTypeRepository.save(AssetType.builder()
                        .name(entry[0]).description(entry[1]).build());
            }
        }
    }

    private void seedAssetCategories() {
        List<String[]> categories = List.of(
                new String[]{"PRIMARY", "Primary asset directly supporting business objectives"},
                new String[]{"SUPPORT", "Support asset enabling primary assets"}
        );
        for (String[] entry : categories) {
            if (!assetCategoryRepository.existsByName(entry[0])) {
                assetCategoryRepository.save(AssetCategory.builder()
                        .name(entry[0]).description(entry[1]).build());
            }
        }
    }

    private void seedThreatTypes() {
        List<String[]> types = List.of(
                new String[]{"OPERATIONAL",    "Threats arising from operational activities"},
                new String[]{"ORGANIZATIONAL", "Threats arising from organizational factors"},
                new String[]{"ENVIRONMENTAL",  "Threats from physical or environmental sources"},
                new String[]{"TECHNOLOGICAL",  "Threats from technology or technical failures"}
        );
        for (String[] entry : types) {
            if (!threatTypeRepository.existsByName(entry[0])) {
                threatTypeRepository.save(ThreatType.builder()
                        .name(entry[0]).description(entry[1]).build());
            }
        }
    }

    private void seedControlCategories() {
        List<String[]> categories = List.of(
                new String[]{"ORGANISATIONAL", "Organisational controls — ISO 27001 Annex A.5"},
                new String[]{"PEOPLE",         "People controls — ISO 27001 Annex A.6"},
                new String[]{"PHYSICAL",       "Physical controls — ISO 27001 Annex A.7"},
                new String[]{"TECHNOLOGICAL",  "Technological controls — ISO 27001 Annex A.8"}
        );
        for (String[] entry : categories) {
            if (!controlCategoryRepository.existsByName(entry[0])) {
                controlCategoryRepository.save(ControlCategory.builder()
                        .name(entry[0]).description(entry[1]).build());
            }
        }
    }

    private void seedControlTypes() {
        List<String[]> types = List.of(
                new String[]{"PREVENTIVE",   "Prevents a risk event from occurring"},
                new String[]{"DETECTIVE",    "Detects risk events after they occur"},
                new String[]{"CORRECTIVE",   "Corrects the impact of a risk event"},
                new String[]{"DETERRENT",    "Discourages risk events from occurring"},
                new String[]{"COMPENSATING", "Compensates when primary controls are not feasible"}
        );
        for (String[] entry : types) {
            if (!controlTypeRepository.existsByName(entry[0])) {
                controlTypeRepository.save(ControlType.builder()
                        .name(entry[0]).description(entry[1]).build());
            }
        }
    }

    private void seedPolicyTypes() {
        List<String[]> types = List.of(
                new String[]{"SECURITY",    "Information security policy"},
                new String[]{"RISK",        "Risk management policy"},
                new String[]{"COMPLIANCE",  "Compliance and regulatory policy"},
                new String[]{"PRIVACY",     "Data privacy and protection policy"},
                new String[]{"OPERATIONAL", "Operational procedures policy"}
        );
        for (String[] entry : types) {
            if (!policyTypeRepository.existsByName(entry[0])) {
                policyTypeRepository.save(PolicyType.builder()
                        .name(entry[0]).description(entry[1]).build());
            }
        }
    }

    private void seedDocumentTypes() {
        List<String[]> types = List.of(
                new String[]{"POLICY",     "Policy document"},
                new String[]{"CHARTER",    "Governance charter"},
                new String[]{"PROCEDURE",  "Operational procedure"},
                new String[]{"REGULATION", "Regulatory document"},
                new String[]{"MINUTES",    "Meeting minutes"},
                new String[]{"FRAMEWORK",  "Framework document"},
                new String[]{"GUIDELINE",  "Guideline or best practice"}
        );
        for (String[] entry : types) {
            if (!documentTypeRepository.existsByName(entry[0])) {
                documentTypeRepository.save(DocumentType.builder()
                        .name(entry[0]).description(entry[1]).build());
            }
        }
    }

    private void seedResponsibilityTypes() {
        List<String[]> types = List.of(
                new String[]{"RISK_OWNER",              "Owns and manages identified risks"},
                new String[]{"CONTROL_OWNER",           "Owns and manages controls"},
                new String[]{"COMPLIANCE_OFFICER",      "Ensures regulatory compliance"},
                new String[]{"AUDITOR",                 "Conducts audits and assessments"},
                new String[]{"DATA_PROTECTION_OFFICER", "Manages data protection obligations"},
                new String[]{"CISO",                    "Chief Information Security Officer"},
                new String[]{"EXECUTIVE",               "Executive-level accountability"}
        );
        for (String[] entry : types) {
            if (!responsibilityTypeRepository.existsByName(entry[0])) {
                responsibilityTypeRepository.save(ResponsibilityType.builder()
                        .name(entry[0]).description(entry[1]).build());
            }
        }
    }
}
