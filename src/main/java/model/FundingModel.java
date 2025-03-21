package model;

import data.Grant;
import data.FinancialIndicator;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.regression.GradientTreeBoost;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class FundingModel {
    private GradientTreeBoost model;
    private List<String> features = Arrays.asList("geographyId", "activityAreaId", "periodStartDate", "periodEndDate");
    private String target = "totalDisbursedAmount_ReferenceRate";
    private StructType schema;

    private Object[] convertMapToArray(Map<String, Object> rowData) {
        return new Object[] {
                rowData.get("geographyId"),
                rowData.get("activityAreaId"),
                rowData.get("periodStartDate"),
                rowData.get("periodEndDate"),
                rowData.get(target)
        };
    }

    public DataFrame prepareData(List<Grant> grants, List<FinancialIndicator> finIndicators) {
        Map<String, FinancialIndicator> finMap = finIndicators.stream()
                .collect(Collectors.toMap(FinancialIndicator::getGrantId, fi -> fi));

        List<Tuple> rows = new ArrayList<>();

        schema = new StructType(
                new StructField("geographyId", DataTypes.DoubleType),
                new StructField("activityAreaId", DataTypes.DoubleType),
                new StructField("periodStartDate", DataTypes.DoubleType),
                new StructField("periodEndDate", DataTypes.DoubleType),
                new StructField(target, DataTypes.DoubleType)
        );

        for (Grant grant : grants) {
            FinancialIndicator fi = finMap.getOrDefault(grant.getId(), null);
            Double targetValue = (fi != null && fi.getTotalDisbursedAmountReferenceRate() != null)
                    ? fi.getTotalDisbursedAmountReferenceRate() : null;
            if (targetValue != null) {
                int startYear = grant.getPeriodStartDate() != null
                        ? LocalDate.parse(grant.getPeriodStartDate()).getYear() : 0;
                int endYear = grant.getPeriodEndDate() != null
                        ? LocalDate.parse(grant.getPeriodEndDate()).getYear() : 0;

                Map<String, Object> rowData = new HashMap<>();
                double geoIdNumeric = grant.getGeographyId() != null
                        ? (double) grant.getGeographyId().hashCode() : 0.0;
                double activityIdNumeric = grant.getActivityAreaId() != null
                        ? (double) grant.getActivityAreaId().hashCode() : 0.0; // Convert String to double
                rowData.put("geographyId", geoIdNumeric);
                rowData.put("activityAreaId", activityIdNumeric);
                rowData.put("periodStartDate", (double) startYear);
                rowData.put("periodEndDate", (double) endYear);
                rowData.put(target, targetValue);

                rows.add(Tuple.of(convertMapToArray(rowData), schema));
            }
        }

        if (rows.isEmpty()) {
            throw new IllegalStateException("No valid data rows with target values found");
        }
        return DataFrame.of(rows, schema);
    }

    private DataFrame[] splitData(DataFrame data, double testSize) {
        int n = data.size();
        int testN = (int) (n * testSize);
        Random rand = new Random(42);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) indices.add(i);
        Collections.shuffle(indices, rand);

        int[] trainIndices = indices.subList(testN, n).stream().mapToInt(Integer::intValue).toArray();
        int[] testIndices = indices.subList(0, testN).stream().mapToInt(Integer::intValue).toArray();

        DataFrame train = data.select(trainIndices);
        DataFrame test = data.select(testIndices);
        return new DataFrame[] {train, test};
    }

    public void train(DataFrame data) {
        DataFrame[] split = splitData(data, 0.2);
        DataFrame train = split[0];
        DataFrame test = split[1];

        Formula formula = Formula.lhs(target);

        Properties props = new Properties();
        props.setProperty("smile.gbt.ntrees", "100");
        props.setProperty("smile.gbt.max.depth", "10");
        props.setProperty("smile.gbt.max.nodes", "4");
        props.setProperty("smile.gbt.shrinkage", "0.1");
        props.setProperty("smile.gbt.sampling.rate", "0.05");

        model = GradientTreeBoost.fit(formula, train, props);

        double[] y_test = test.doubleVector(target).toDoubleArray();
        double[] y_pred = new double[test.size()];
        for (int i = 0; i < test.size(); i++) {
            Tuple testTuple = test.get(i);
            y_pred[i] = model.predict(testTuple);
        }

        double sumSquaredError = 0.0;
        for (int i = 0; i < y_test.length; i++) {
            double error = y_test[i] - y_pred[i];
            sumSquaredError += error * error;
        }
        double rmse = y_test.length > 0 ? Math.sqrt(sumSquaredError / y_test.length) : 0.0;
        System.out.println("\nRMSE: " + rmse);

        System.out.println("Feature Importance:");
        Object importanceObj = model.importance();
        if (importanceObj instanceof int[]) {
            int[] importance = (int[]) importanceObj;
            for (int i = 0; i < features.size() && i < importance.length; i++) {
                System.out.println(features.get(i) + ": " + importance[i]);
            }
        } else if (importanceObj instanceof double[]) {
            double[] importance = (double[]) importanceObj;
            for (int i = 0; i < features.size() && i < importance.length; i++) {
                System.out.println(features.get(i) + ": " + importance[i]);
            }
        } else {
            System.out.println("Unexpected importance type: " + importanceObj.getClass());
        }
    }

    public double predict(double[] input) {
        if (input.length != features.size()) {
            throw new IllegalArgumentException("Input array length (" + input.length + ") must match number of features (" + features.size() + ")");
        }
        Map<String, Object> rowData = new HashMap<>();
        for (int i = 0; i < features.size(); i++) {
            rowData.put(features.get(i), input[i]);
        }
        Tuple tuple = Tuple.of(convertMapToArray(rowData), schema);
        return model.predict(tuple);
    }
}