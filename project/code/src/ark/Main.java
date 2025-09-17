package ark;

public class Main {
    public static void main(String[] args) {
        CsvReader.readCsvFile();
        DataAnalyzer.analyze();
        GuiBuilder.create();
    }
}