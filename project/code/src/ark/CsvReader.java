package ark;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CsvReader {
    public static final List<DataRow> DATA = new ArrayList<>();

        public static void readCsvFile() {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream("Officers.csv"), "GBK"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    DATA.add(new DataRow(line.split(",")));
                }
            } catch (IOException e) {
                System.out.println("文件读取错误：" + e.getMessage());
            }
        }
    }

