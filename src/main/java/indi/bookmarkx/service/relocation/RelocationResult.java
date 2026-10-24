package indi.bookmarkx.service.relocation;

public final class RelocationResult {

    private final int line;
    private final double confidence;
    private final String strategyName;

    public RelocationResult(int line, double confidence, String strategyName) {
        this.line = line;
        this.confidence = confidence;
        this.strategyName = strategyName;
    }

    public int getLine() {
        return line;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getStrategyName() {
        return strategyName;
    }
}
