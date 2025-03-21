package data;

public class FinancialIndicator {
    private String grantId;
    private Double totalDisbursedAmountReferenceRate;

    // Constructors
    public FinancialIndicator() {}

    // Getters and Setters
    public String getGrantId() {
        return grantId;
    }

    public void setGrantId(String grantId) {
        this.grantId = grantId;
    }

    public Double getTotalDisbursedAmountReferenceRate() {
        return totalDisbursedAmountReferenceRate;
    }

    public void setTotalDisbursedAmountReferenceRate(Double totalDisbursedAmountReferenceRate) {
        this.totalDisbursedAmountReferenceRate = totalDisbursedAmountReferenceRate;
    }
}