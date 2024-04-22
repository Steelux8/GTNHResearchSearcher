import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ResearchAnalyzer {

    static Map<String, Integer> complexities = new HashMap<>();
    static Map<String, Map<String, Integer>> aspects = new HashMap<>();
    static Map<String, Integer> warps = new HashMap<>();
    static Map<String, String> mods = new HashMap<>();
    static Set<String> uniqueAspects = new TreeSet<>();

    public static void main(String[] args) {

        String csvOutput = "";
        String researchItemsText = "";

        String folderLocation = "src\\ResearchTextFiles\\";
        String[] fileNameList = {
                "TC4_Research.txt",
                "Gadomancy_Research.txt",
                "ThaumicExploration_Research.txt",
                "ThaumicBoots_Research.txt",
                "ThaumicTinkerer_Research.txt"
        };

        Map<String, String> modNameMap = new HashMap<>();
        modNameMap.put("TC4_Research.txt", "Thaumcraft");
        modNameMap.put("Gadomancy_Research.txt", "Gadomancy");
        modNameMap.put("ThaumicExploration_Research.txt", "Thaumic_Exploration");
        modNameMap.put("ThaumicBoots_Research.txt", "Thaumic_Boots");
        modNameMap.put("ThaumicTinkerer_Research.txt", "Thaumic_Tinkerer");

        // Research formats for different syntax across mods for defining ResearchItems:
        // 1 - Definition in 1 line - TC4
        // 2 - Definition in multiple lines - Gadomancy, Thaumic Exploration
        // 3 - Same as 2, but aspects come before complexity - Thaumic Boots

        // Missing - Electro-Magic Tools (defines aspects elsewhere)
        Map<String, Integer> researchFormatMap = new HashMap<>();
        researchFormatMap.put("TC4_Research.txt", 1);
        researchFormatMap.put("Gadomancy_Research.txt", 2);
        researchFormatMap.put("ThaumicExploration_Research.txt", 3);
        researchFormatMap.put("ThaumicBoots_Research.txt", 3);
        researchFormatMap.put("ThaumicTinkerer_Research.txt", 3);

        for (String fileName : fileNameList) {
            researchItemsText = readFromFile(folderLocation + fileName);

            if (researchItemsText != null) {
                populateInformationMaps(researchItemsText, modNameMap.get(fileName), researchFormatMap.get(fileName));
            } else {
                System.out.println("Failed to read researchItems.txt");
            }
        }

        csvOutput = generateCSVFromMaps();

        System.out.println(csvOutput);
    }

    public static String readFromFile(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void populateInformationMaps(String researchItemsText, String modName, int researchFormat) {
        if (researchItemsText == null) {
            return;
        }

        // Regex pattern to match ResearchItem definitions and addWarpToResearch values
        Pattern pattern;
        Matcher matcher = null;

        if (researchFormat == 1) {
            pattern = Pattern.compile("\\(\"([A-Z]+)\", \"[A-Z]+\", \\(new AspectList\\(\\)\\)((?:\\.add\\(Aspect\\.[A-Z]+, [-]?\\d+\\))++), ([-]?\\d+), (\\d+), (\\d+), .*?\\)|ThaumcraftApi\\.addWarpToResearch\\(\"([A-Z]+)\", (\\d+)\\)");
            matcher = pattern.matcher(researchItemsText);
        } else if (researchFormat == 2) {
            pattern = Pattern.compile("ResearchItem[I]*\\(\\s*\"([a-zA-Z_]+)\"[\\s\\S]*?(?:\\s*\\-?\\d+\\s*,){2}\\s*(\\-?\\d+),[\\s\\S]*?new AspectList\\(\\)((?:\\.add\\(Aspect\\.[A-Z]+, \\d+\\)\\s*)+).*?\\)|ThaumcraftApi\\.addWarpToResearch\\(.*?\"([a-zA-Z_]+(?:\\.[a-zA-Z_]+)?)\", (\\d+)\\)");
            matcher = pattern.matcher(researchItemsText);
        } else {
            if (modName.equals("Thaumic_Exploration")) {
                pattern = Pattern.compile("TXResearchItem\\(\\s*\"([a-zA-Z_]+)\"[\\s\\S]*?new AspectList\\(\\)((?:\\.add\\([TB_]*Aspect\\.[A-Z]+, \\d+\\)\\s*)+)[\\s\\S]*?\\d+,[\\s\\S]*?\\d+,[\\s\\S]*?(\\d+),[\\s\\S]*?\\)|ThaumcraftApi\\.addWarpToResearch\\(.*?\"([a-zA-Z_]+(?:\\.[a-zA-Z_]+)?)\", (\\d+)\\)");
            } else {
                pattern = Pattern.compile("ResearchItem\\(\\s*\"([a-zA-Z_]+)\"[\\s\\S]*?new AspectList\\(\\)((?:\\.add\\([TB_]*Aspect\\.[A-Z]+, \\d+\\)\\s*)+)[\\s\\S]*?\\d+,[\\s\\S]*?\\d+,[\\s\\S]*?(\\d+),[\\s\\S]*?\\)|ThaumcraftApi\\.addWarpToResearch\\(.*?\"([a-zA-Z_]+(?:\\.[a-zA-Z_]+)?)\", (\\d+)\\)");
            }
            matcher = pattern.matcher(researchItemsText);
        }

        while (matcher.find()) {
            if (matcher.group(1) != null) { // ResearchItem definition
                String key = matcher.group(1);
                int complexity = 0;
                if (researchFormat == 1) {
                    complexity = Integer.parseInt(matcher.group(5));
                } else if (researchFormat == 2) {
                    complexity = Integer.parseInt(matcher.group(2));
                } else {
                    complexity = Integer.parseInt(matcher.group(3));
                }
                complexities.put(key, complexity);
                mods.put(key, modName);

                Map<String, Integer> aspectValues = new HashMap<>();

                Pattern aspectPattern;
                Matcher aspectMatcher = null;

                if (researchFormat == 1) {
                    aspectPattern = Pattern.compile("\\.add\\(Aspect\\.([A-Z]+), (\\d+)\\)");
                    aspectMatcher = aspectPattern.matcher(matcher.group(2));
                } else {
                    aspectPattern = Pattern.compile("\\.add\\([TB_]*Aspect\\.([A-Z]+), (\\d+)\\)");
                    aspectMatcher = aspectPattern.matcher(matcher.group(0));
                }

                while (aspectMatcher.find()) {
                    String aspectName = aspectMatcher.group(1);
                    int aspectValue = Integer.parseInt(aspectMatcher.group(2));
                    aspectValues.put(aspectName, aspectValue);
                    uniqueAspects.add(aspectName);
                }
                aspects.put(key, aspectValues);
            } else if (researchFormat == 1) { // addWarpToResearch value
                if (matcher.group(6) != null) {
                    String key = matcher.group(6);
                    int warp = Integer.parseInt(matcher.group(7));
                    warps.put(key, warp);
                }
            } else if (matcher.group(4) != null) { // addWarpToResearch value
                String key = matcher.group(4);
                int warp = Integer.parseInt(matcher.group(5));
                warps.put(key, warp);
            }
        }
    }

    public static String generateCSVFromMaps() {

        // Create StringBuilder to construct .csv content
        StringBuilder csvContent = new StringBuilder();

        // Write header
        csvContent.append("ResearchItem,Mod,Complexity,Warp,");

        // Add aspects to header
        for (String aspect : uniqueAspects) {
            csvContent.append(aspect).append(",");
        }

        csvContent.append("Total Aspect Value\n");

        // Build .csv content
        for (String key : complexities.keySet()) {
            csvContent.append(key).append(",");
            csvContent.append(mods.getOrDefault(key, "Unknown")).append(",");
            csvContent.append(complexities.getOrDefault(key, 1)).append(",");
            csvContent.append(warps.getOrDefault(key, 0)).append(",");

            Map<String, Integer> aspectValues = aspects.get(key);
            for (String aspect : uniqueAspects) {
                int value = aspectValues.getOrDefault(aspect, 0);
                // int complexity = complexities.get(key);
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