package data;

public class Grant {
    private String id;
    private String geographyId; // Already String from previous fix
    private String activityAreaId; // Changed from int to String
    private String periodStartDate;
    private String periodEndDate;

    public Grant() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGeographyId() {
        return geographyId;
    }

    public void setGeographyId(String geographyId) {
        this.geographyId = geographyId;
    }

    public String getActivityAreaId() { // Updated return type
        return activityAreaId;
    }

    public void setActivityAreaId(String activityAreaId) { // Updated parameter type
        this.activityAreaId = activityAreaId;
    }

    public String getPeriodStartDate() {
        return periodStartDate;
    }

    public void setPeriodStartDate(String periodStartDate) {
        this.periodStartDate = periodStartDate;
    }

    public String getPeriodEndDate() {
        return periodEndDate;
    }

    public void setPeriodEndDate(String periodEndDate) {
        this.periodEndDate = periodEndDate;
    }
}