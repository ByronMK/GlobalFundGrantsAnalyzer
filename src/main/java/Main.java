import Api.ApiClient;
import data.Grant;
import data.FinancialIndicator;
import model.FundingModel;
import smile.data.DataFrame;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            // Fetch data
            ApiClient client = new ApiClient();
            Map<String, String> allData = client.fetchAllData();

            if (allData.isEmpty()) {
                System.out.println("No data loaded successfully");
                return;
            }

            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            String grantsJson = allData.get("/v4.2/odata/Grants");
            String finJson = allData.get("/v4.2/odata/FinancialIndicators(indicatorName='Disbursement Amount - Implementation Period Currency',financialDatasetName='Disbursement_ImplementationPeriodCurrency')");

            List<Grant> grants = mapper.readValue(mapper.readTree(grantsJson).get("value").toString(),
                    new TypeReference<List<Grant>>() {});
            List<FinancialIndicator> finIndicators = mapper.readValue(mapper.readTree(finJson).get("value").toString(),
                    new TypeReference<List<FinancialIndicator>>() {});

            // Data summary
            System.out.println("\nStored Data Summary:");
            System.out.println("/v4.2/odata/Grants: " + grants.size() + " rows");
            System.out.println("/v4.2/odata/FinancialIndicators: " + finIndicators.size() + " rows");

            // Train model
            FundingModel model = new FundingModel();
            DataFrame data = model.prepareData(grants, finIndicators);
            model.train(data);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}