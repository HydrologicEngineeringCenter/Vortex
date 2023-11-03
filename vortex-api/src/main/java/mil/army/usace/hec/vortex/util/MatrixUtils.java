package mil.army.usace.hec.vortex.util;

public class MatrixUtils {

    private MatrixUtils(){}

    public static float[][] arrayToMatrix(float[] array, int nx, int ny){
        int count = 0;
        float[][] matrix = new float[ny][nx];
        for (int j = ny-1; j>=0; j--){
            for (int i = 0; i<nx; i++){
                matrix[j][i] = array[count];
                count++;
            }
        }
        return matrix;
    }

    public static float[] matrixToArray(float[][] matrix, int nx, int ny){
        int count = 0;
        float[] array = new float[ny*nx];
        for (int j = 0; j<ny; j++){
            for (int i = 0; i<nx; i++){
                array[count] = matrix[j][i];
                count++;
            }
        }
        return array;
    }
}
