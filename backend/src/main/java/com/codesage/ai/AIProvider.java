package com.codesage.ai;

public interface AIProvider {
    String name();
    String model();
    boolean isAvailable();
    AnalysisResult analyze(String codeDiff);
}
