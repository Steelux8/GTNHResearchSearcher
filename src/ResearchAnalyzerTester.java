import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ResearchAnalyzerTester {

    public static void main(String[] args) {
        String folderLocation = "src\\ResearchTextFiles\\";
        String GadomancyFilename = folderLocation + "ThaumicTinkerer_Research.txt";

        String researchItemsText = readFromFile(GadomancyFilename);

        if (researchItemsText != null) {
            String csvOutput = calculateMultipliedAspectValuesAndReturnCSV(researchItemsText);
            System.out.println(csvOutput);
        } else {
            System.out.println("Failed to read researchItems.txt");
        }
    }

    public static String readFromFile(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String calculateMultipliedAspectValuesAndReturnCSV(String researchItemsText) {
        if (researchItemsText == null) {
            System.out.println("researchItemsText is null");
            return "";
        } else {
            System.out.println("researchItemsText length: " + researchItemsText.length());
        }


        Map<String, Integer> complexities = new HashMap<>();
        Map<String, Map<String, Integer>> aspects = new HashMap<>();
        Map<String, Integer> warps = new HashMap<>();

        // Updated Regex pattern to match SimpleResearchItem definitions
        Pattern pattern = Pattern.compile("ResearchItem\\(\\s*\"([a-zA-Z_]+)\"[\\s\\S]*?new AspectList\\(\\)((?:\\.add\\([TB_]*Aspect\\.[A-Z]+, \\d+\\)\\s*)+)[\\s\\S]*?\\d+,[\\s\\S]*?\\d+,[\\s\\S]*?(\\d+),[\\s\\S]*?\\)|ThaumcraftApi\\.addWarpToResearch\\(.*?\"([a-zA-Z_]+(?:\\.[a-zA-Z_]+)?)\", (\\d+)\\)");
        Matcher matcher = pattern.matcher(researchItemsText);

        Set<String> uniqueAspects = new TreeSet<>();

        while (matcher.find()) {
            /*System.out.println("Group 1 (ResearchItem key): " + matcher.group(1));
            System.out.println("Group 2 (Complexity): " + matcher.group(3));
            System.out.println("Aspect List: " + matcher.group(2).replaceAll("\\s+", ""));
            System.out.println("Group 4 (Warp key): " + matcher.group(4));
            System.out.println("Group 5 (Warp value): " + matcher.group(5));*/
            if (matcher.group(1) != null) { // SimpleResearchItem definition
                String key = matcher.group(1);
                int complexity = Integer.parseInt(matcher.group(3));
                complexities.put(key, complexity);

                Map<String, Integer> aspectValues = new HashMap<>();
                Pattern aspectPattern = Pattern.compile("\\.add\\([TB_]*Aspect\\.([A-Z]+), (\\d+)\\)");
                Matcher aspectMatcher = aspectPattern.matcher(matcher.group(0));

                while (aspectMatcher.find()) {
                    String aspectName = aspectMatcher.group(1);
                    int aspectValue = Integer.parseInt(aspectMatcher.group(2));
                    aspectValues.put(aspectName, aspectValue);
                    uniqueAspects.add(aspectName);
                }

                aspects.put(key, aspectValues);
            } else if (matcher.group(4) != null) { // addWarpToResearch value
                String key = matcher.group(4);
                int warp = Integer.parseInt(matcher.group(5));
                warps.put(key, warp);
            }
        }

        // Create StringBuilder to construct .csv content
        StringBuilder csvContent = new StringBuilder();

        // Write header
        csvContent.append("ResearchItem,Warp,");

        // Add aspects to header
        for (String aspect : uniqueAspects) {
            csvContent.append(aspect).append(",");
        }

        csvContent.append("Total Aspect Value\n");

        // Build .csv content
        for (String key : complexities.keySet()) {
            csvContent.append(key).append(",");
            csvContent.append(warps.getOrDefault(key, 0)).append(",");

            Map<String, Integer> aspectValues = aspects.get(key);
            for (String aspect : uniqueAspects) {
                int value = aspectValues.getOrDefault(aspect, 0);
                int complexity = complexities.get(key);
                // int multipliedValue = value * complexity;
                csvContent.append(value).append(",");
            }

            // Calculate total aspect value
            int totalValue = 0;
            for (int value : aspectValues.values()) {
                totalValue += value;
            }
            csvContent.append(totalValue).append("\n");
        }

        return csvContent.toString();
    }
}