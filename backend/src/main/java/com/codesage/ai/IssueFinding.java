package com.codesage.ai;

import com.codesage.model.ReviewIssue;

public record IssueFinding(
        ReviewIssue.IssueType type,
        ReviewIssue.IssueSeverity severity,
        String filePath,
        Integer lineNumber,
        String title,
        String description,
        String suggestion) {
}
