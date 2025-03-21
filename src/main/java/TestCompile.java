import smile.data.DataFrame;
import smile.data.vector.DoubleVector;

public class TestCompile {
    public static void main(String[] args) {
        DataFrame df = DataFrame.of(new double[][]{{1.0}, {2.0}}, "col");
        DoubleVector vec = df.doubleVector("col");
        double[] arr = vec.toDoubleArray(); // Should compile fine
        System.out.println(arr[0]);
    }
}