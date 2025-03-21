package model;

import Api.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.DoubleVector;
import smile.regression.LinearModel;
import smile.regression.OLS;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FundingPrediction {
    private ApiClient apiClient;
    private List<double[]> trainingData;
    private static final double USD_TO_KES_RATE = 129.0;
    private LinearModel model;

    public FundingPrediction() {
        this.apiClient = new ApiClient();
        this.trainingData = new ArrayList<>();
    }

    public void processGrantsData(String grantsJson) {
        JSONObject json = new JSONObject(grantsJson);
        JSONArray valueArray = json.getJSONArray("value");
        System.out.println("Processing Grants data... Value array size: " + valueArray.length());
        int validInstances = 0;
        for (int i = 0; i < valueArray.length(); i++) {
            JSONObject grant = valueArray.getJSONObject(i);
            String grantId = grant.getString("id");
            double committedUSD = grant.optDouble("totalCommitmentAmount_ReferenceRate", 0.0);
            double requestedUSD = grant.optDouble("totalBoardApprovedAmount_ReferenceRate", 0.0);
            double committedKES = committedUSD * USD_TO_KES_RATE;
            double requestedKES = requestedUSD * USD_TO_KES_RATE;
            System.out.printf("Entry: grantId=%s, committed=%,.2f KES, requested=%,.2f KES%n",
                    grantId, committedKES, requestedKES);
            if (committedKES > 0 && requestedKES > 0) {
                validInstances++;
                trainingData.add(new double[]{requestedKES, committedKES}); // [feature, target]
            }
        }
        System.out.println("Loaded " + validInstances + " valid instances from Grants");
    }

    public void trainModel() {
        if (trainingData.isEmpty()) {
            System.out.println("Insufficient data to train the model. Total data points: 0");
            return;
        }

        double[] requestedValues = new double[trainingData.size()];
        double[] committedValues = new double[trainingData.size()];
        for (int i = 0; i < trainingData.size(); i++) {
            requestedValues[i] = trainingData.get(i)[0];
            committedValues[i] = trainingData.get(i)[1];
        }

        DataFrame df = DataFrame.of(
                DoubleVector.of("requested", requestedValues),
                DoubleVector.of("committed", committedValues)
        );

        Formula formula = Formula.lhs("committed");
        model = OLS.fit(formula, df);

        System.out.println("Model trained. Coefficients: " + java.util.Arrays.toString(model.coefficients()));
        System.out.printf("Intercept: %,.2f KES%n", model.intercept());
    }

    public double predict(double requestedKES) {
        if (model == null) {
            System.out.println("Model not trained yet!");
            return 0.0;
        }
        return model.predict(new double[]{requestedKES});
    }

    public void plotModel() {
        if (model == null || trainingData.isEmpty()) {
            System.out.println("Cannot plot: Model not trained or no data available.");
            return;
        }

        // Create dataset for scatter plot
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries dataSeries = new XYSeries("Grants Data");
        for (double[] point : trainingData) {
            dataSeries.add(point[0], point[1]); // requested vs committed
        }
        dataset.addSeries(dataSeries);

        // Add regression line
        XYSeries regressionSeries = new XYSeries("Regression Line");
        double minRequested = trainingData.stream().mapToDouble(p -> p[0]).min().getAsDouble();
        double maxRequested = trainingData.stream().mapToDouble(p -> p[0]).max().getAsDouble();
        regressionSeries.add(minRequested, predict(minRequested));
        regressionSeries.add(maxRequested, predict(maxRequested));
        dataset.addSeries(regressionSeries);

        // Create chart
        JFreeChart chart = ChartFactory.createScatterPlot(
                "Funding Disbursement Prediction",
                "Requested Amount (KES)",
                "Committed Amount (KES)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        // Display chart in a frame
        ChartFrame frame = new ChartFrame("Prediction Model", chart);
        frame.pack();
        frame.setVisible(true);
    }

    public void run() throws Exception {
        Map<String, String> data = apiClient.fetchAllData();
        String grantsJson = data.get("https://fetch.theglobalfund.org/v4.2/odata/Grants");
        if (grantsJson != null) {
            processGrantsData(grantsJson);
        }
        if (trainingData.isEmpty()) {
            System.out.println("Insufficient data to train the model. Total data points: 0");
        } else {
            System.out.println("Training model with " + trainingData.size() + " data points in KES...");
            trainModel();
            double testRequested = 1.0E9;
            double predictedCommitted = predict(testRequested);
            System.out.printf("Predicted committed for requested=%,.2f KES: %,.2f KES%n",
                    testRequested, predictedCommitted);
            plotModel(); // Add the plot after training and prediction
        }
    }

    public static void main(String[] args) throws Exception {
        FundingPrediction predictor = new FundingPrediction();
        predictor.run();
    }
}