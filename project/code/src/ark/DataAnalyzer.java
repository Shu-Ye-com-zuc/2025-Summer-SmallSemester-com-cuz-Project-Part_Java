package ark;
import java.util.*;

public class DataAnalyzer {
    public static final Map<String, Integer> OCCUPATION_COUNT = new HashMap<>();
    public static final Map<String, Integer> COST_DISTRIBUTION = new HashMap<>();
    public static final String[] COST_INTERVALS = {
            "【5,10)", "【10,15)", "【15,20)", "【20,25)", "【25,30)", "【30,35)"
    };

    public static void analyze() {
        if (CsvReader.DATA.isEmpty()) return;
        for (int i = 1; i < CsvReader.DATA.size(); i++) {// 职业
            String[] arr = CsvReader.DATA.get(i).getData();
            if (arr.length >= 3) {
                String occ = arr[2];
                OCCUPATION_COUNT.put(occ, OCCUPATION_COUNT.getOrDefault(occ, 0) + 1);
            }
        }
        for (int i = 1; i < CsvReader.DATA.size(); i++) {// 费用
            String[] arr = CsvReader.DATA.get(i).getData();
            if (arr.length >= 7) {
                try {
                    int cost = Integer.parseInt(arr[6]);
                    String interval = getCostInterval(cost);
                    COST_DISTRIBUTION.put(interval, COST_DISTRIBUTION.getOrDefault(interval, 0) + 1);
                } catch (NumberFormatException ignore) {
                }
            }
        }
    }

    private static String getCostInterval(int cost) {
        if (cost >= 5 && cost < 10) return "【5,10)";
        if (cost >= 10 && cost < 15) return "【10,15)";
        if (cost >= 15 && cost < 20) return "【15,20)";
        if (cost >= 20 && cost < 25) return "【20,25)";
        if (cost >= 25 && cost < 30) return "【25,30)";
        if (cost >= 30 && cost < 35) return "【30,35)";
        return "【其他】";
    }
}