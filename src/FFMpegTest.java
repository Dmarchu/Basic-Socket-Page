import java.io.BufferedReader;
import java.io.InputStreamReader;

public class FFMpegTest {
    public static void main(String[] args) {
        try {
            // Ejecuta el comando para obtener la versión de ffmpeg
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            process.waitFor();

            // Imprimir la salida
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
