package com.codesage.ai;

import com.codesage.model.ReviewIssue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DeterministicAIProvider implements AIProvider {

    private final boolean enabled;

    public DeterministicAIProvider(@Value("${codesage.demo.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String name() {
        return "Demo";
    }

    @Override
    public String model() {
        return "deterministic-v1";
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    public AnalysisResult analyze(String codeDiff) {
        List<IssueFinding> findings = new ArrayList<>();
        if (codeDiff.contains("SELECT *") || codeDiff.contains("+ input")) {
            findings.add(new IssueFinding(
                    ReviewIssue.IssueType.SECURITY,
                    ReviewIssue.IssueSeverity.HIGH,
                    "src/main/java/Example.java",
                    12,
                    "Potential SQL injection",
                    "The query is assembled from untrusted input.",
                    "Use a parameterized query."));
        }
        if (codeDiff.contains("TODO") || codeDiff.contains("catch (Exception")) {
            findings.add(new IssueFinding(
                    ReviewIssue.IssueType.CODE_QUALITY,
                    ReviewIssue.IssueSeverity.MEDIUM,
                    "src/main/java/Example.java",
                    20,
                    "Broad or unfinished error handling",
                    "The change contains a broad catch or unfinished TODO.",
                    "Handle the expected exception explicitly."));
        }

        double score = Math.max(4.0, 9.5 - findings.size() * 1.5);
        return new AnalysisResult(
                score,
                findings.isEmpty() ? "No material issues found in the deterministic demo analysis."
                        : "The deterministic demo analysis found " + findings.size() + " actionable issue(s).",
                List.copyOf(findings),
                Math.max(1, codeDiff.length() / 4),
                120,
                0.0);
    }
}
