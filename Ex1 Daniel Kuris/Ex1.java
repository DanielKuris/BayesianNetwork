import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class Ex1 {
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        // Load input file:
        FileReader input_file = new FileReader("input.txt");
        Scanner scanner = new Scanner(input_file);

        String network_file = scanner.nextLine();

        List<String> queries = new ArrayList<>();
        while (scanner.hasNextLine()) {
            queries.add(scanner.nextLine().trim()); // Trim to handle any leading/trailing whitespace
        }
        scanner.close();

        // Parse network XML file:
        List<BayesianNetworkElement> network = BayesianNetworkTools.parseNetwork(network_file);

        // Handle queries:
        StringBuilder output = new StringBuilder();
        for (String query : queries) {
            char mode = determineMode(query);

            MathematicalOperationsCounter counter = new MathematicalOperationsCounter();
            double result = 0;
            boolean independent = false;

            if (mode == '1') {
                // VE query handling
                String request_left = extractVELeft(query);
                String request_right = extractVERight(query);
                String eliminationOrder = extractVEEliminationOrder(query);

                result = VariableElimination.calculateCPT(network, request_left, request_right, eliminationOrder, counter);
                output.append(String.format("%.5f,%d,%d\n", result, counter.addition_counter, counter.multiplication_counter));
            } else if (mode == '2') {
                // BayesBall query handling
                String request_left = extractBBLeft(query);
                String request_right = extractBBRight(query);

                // BayesBall method returns a result as String indicating independence
                independent = BayesBall.runBayesBall(network, request_left, request_right);

                // Append the result to output format
                output.append(independent ? "yes\n" : "no\n");
            } else {
                // Handle unknown mode 
                output.append("error\n");
            }
        }

        // Write output to file:
        FileWriter fw = new FileWriter("output.txt");
        fw.write(output.toString().trim()); // trim to remove any trailing newline
        fw.close();
    }

    // Function to determine if query is VE ('1') or BayesBall ('2')
    private static char determineMode(String query) {
        if (query.startsWith("P(")) {
            return '1'; // VE mode
        } else {
            return '2'; // BayesBall mode
        }
    }

    // Function to extract VE left request (before '|')
    private static String extractVELeft(String query) {
        // Extract the content between "P(" and "|"
        int startIndex = query.indexOf("P(") + 2;
        int endIndex = query.indexOf("|");
        return query.substring(startIndex, endIndex);
    }

    // Function to extract VE right request (after '|')
    private static String extractVERight(String query) {
        // Extract the content between "|" and ")"
        int startIndex = query.indexOf("|") + 1;
        int endIndex = query.indexOf(")");
        return query.substring(startIndex, endIndex);
    }

    // Function to extract VE elimination order (after whole query)
    private static String extractVEEliminationOrder(String query) {
        // Extract the content after the closing ")"
        int startIndex = query.indexOf(")") + 1;
        String eliminationOrderString = query.substring(startIndex).trim(); // Trim to remove leading/trailing spaces
        return eliminationOrderString;
    }

    // Function to extract BayesBall left request (before '|')
    private static String extractBBLeft(String query) {
        // Extract the content before "|"
        int endIndex = query.indexOf("|");
        return query.substring(0, endIndex).trim(); // Trim to remove any extra spaces
    }

    // Function to extract BayesBall right request (after '|')
    private static String extractBBRight(String query) {
        // Extract the content after "|"
        int startIndex = query.indexOf("|") + 1;
        return query.substring(startIndex).trim(); // Trim to remove any extra spaces
    }
}
