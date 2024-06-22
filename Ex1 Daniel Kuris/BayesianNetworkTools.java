import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BayesianNetworkTools {
    public static BayesianNetworkElement searchElementByName(List<BayesianNetworkElement> network, String name) {
        for (BayesianNetworkElement element : network) {
            if (Objects.equals(element.name, name))
                return element;
        }

        return null;
    }

    public static List<BayesianNetworkElement> parseNetwork(String network_file) throws ParserConfigurationException, IOException, SAXException {
        List<BayesianNetworkElement> network_elements = new ArrayList<>();

        File xmlFile = new File(network_file);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        // Find all elements and their outcomes: (handles 'VARIABLE')
        NodeList elements = doc.getElementsByTagName("VARIABLE");
        for (int i = 0; i < elements.getLength(); i++) {
            Node network_element = elements.item(i);
            if (network_element.getNodeType() == Node.ELEMENT_NODE) {
                Element network_element_e = (Element) network_element;
                String element_name = network_element_e.getElementsByTagName("NAME").item(0).getTextContent();

                NodeList element_outcome_list = network_element_e.getElementsByTagName("OUTCOME");
                List<String> element_outcomes = new ArrayList<>();
                for (int j = 0; j < element_outcome_list.getLength(); j++) {
                    Node outcome = element_outcome_list.item(j);
                    if (outcome.getNodeType() == Node.ELEMENT_NODE) {
                        Element outcome_e = (Element) outcome;
                        element_outcomes.add(outcome_e.getTextContent());
                    }
                }

                network_elements.add(new BayesianNetworkElement(element_name, element_outcomes));
            }
        }

        // Find the tables and relations of all elements: (handles 'DEFINITION')
        NodeList definitions = doc.getElementsByTagName("DEFINITION");
        for (int i = 0; i < definitions.getLength(); i++) {
            Node element_definition = definitions.item(i);
            if (element_definition.getNodeType() == Node.ELEMENT_NODE) {
                Element element_definition_e = (Element) element_definition;
                String element_name = element_definition_e.getElementsByTagName("FOR").item(0).getTextContent();
                String element_table = element_definition_e.getElementsByTagName("TABLE").item(0).getTextContent();

                NodeList element_parents_list = element_definition_e.getElementsByTagName("GIVEN");
                String[] element_parents = new String[element_parents_list.getLength()];
                for (int j = 0; j < element_parents_list.getLength(); j++) {
                    Node parent = element_parents_list.item(j);
                    if (parent.getNodeType() == Node.ELEMENT_NODE) {
                        Element parent_e = (Element) parent;
                        element_parents[j] = parent_e.getTextContent();
                    }
                }

                // Adding the new data from 'DEFINITION' to the corresponding 'VARIABLE':
                searchElementByName(network_elements, element_name).parseCptTable(element_table);
                searchElementByName(network_elements, element_name).parseGiven(element_parents, network_elements);
            }
        }

        return network_elements;
    }

    public static double calculateCPT(List<BayesianNetworkElement> network, String request_left, String request_right, MathematicalOperationsCounter counter) {
        // format: B=T|J=T,M=T
        String left_name = request_left.split("=")[0];
        String left_value = request_left.split("=")[1];

        double numerator = 0;
        double denominator = 0;

        for (String outcome: searchElementByName(network, left_name).outcomes) {
            List<String> variables = Arrays.asList((left_name+"="+outcome + "," + request_right).split(","));
            List<String> outcome_combinations = defineOutcomes(network, variables);

            double probability = 0;
            for (String outcome_combination : outcome_combinations) {
                probability += P(network, outcome_combination.split(","), counter);

                // Counting additions of probabilities for all possible input combinations where the desired variable's (the variable to the left of '|') value is 'outcome':
                if (outcome_combinations.indexOf(outcome_combination) != 0)
                    counter.addition();
            }

            denominator += probability;
            // Counting the addition of the current probability to the denominator:
            if (searchElementByName(network, left_name).outcomes.indexOf(outcome) != 0)
                counter.addition();

            if (Objects.equals(outcome, left_value))
                numerator = probability;
        }

        return numerator/denominator;
    }

    /*
     * Returns all valid outcomes for the given variables.
     */
    public static List<String> defineOutcomes(List<BayesianNetworkElement> network, List<String> variables) {
        // get list of free variables:
        List<BayesianNetworkElement> unused_elements = getUnusedElements(network, variables);

        // get all possible outcome combinations:
        List<String> outcomes = new ArrayList<>();
        generatePermutations(network, outcomes, 0, "");

        // keep only the combinations that have the set outcomes for our variables:
        int i = 0;
        for (BayesianNetworkElement element: network) {
            if (!unused_elements.contains(element)) {
                String variable_outcome = "";
                for (String variable: variables) {
                    if (Objects.equals(variable.split("=")[0], element.name)) {
                        variable_outcome = variable.split("=")[1];
                        break;
                    }
                }

                List<String> temp = new ArrayList<>();
                for (String outcome: outcomes) {
                    if (Objects.equals(outcome.split(",")[i], variable_outcome))
                        temp.add(outcome);
                }
                outcomes = temp;
            }

            i++;
        }

        return outcomes;
    }

    public static void generatePermutations(List<BayesianNetworkElement> lists, List<String> result, int depth, String current) {
        if (depth == lists.size()) {
            // 'substring' removes the ',' at the start.
            result.add(current.substring(1));
            return;
        }

        for (int i = 0; i < lists.get(depth).outcomes.size(); i++) {
            generatePermutations(lists, result, depth + 1, current + "," + lists.get(depth).outcomes.get(i));
        }
    }

    /*
     * Calculate the probability of a specific outcome combination.
     */
    public static double P(List<BayesianNetworkElement> network, String[] outcomes, MathematicalOperationsCounter counter) {
        double probability = 1;

        for (BayesianNetworkElement element: network) {
            int offset = 0;
            int modifier = element.table.length;
            for (BayesianNetworkElement parent: element.given) {
                modifier /= parent.outcomes.size();
                offset += modifier * parent.outcomes.indexOf(outcomes[network.indexOf(parent)]);
            }

            modifier /= element.outcomes.size();
            offset += modifier * element.outcomes.indexOf(outcomes[network.indexOf(element)]);

            probability *= element.table[offset];

            if (network.indexOf(element) != 0)
                counter.multiplication();
        }

        return probability;
    }

    public static List<BayesianNetworkElement> getUnusedElements(List<BayesianNetworkElement> network, List<String> variables) {
        List<BayesianNetworkElement> unused_elements = new ArrayList<>();

        for (BayesianNetworkElement element: network) {
            boolean used = false;
            for (String variable: variables) {
                String name = variable.split("=")[0];
                if (Objects.equals(name, element.name)) {
                    used = true;
                    break;
                }
            }

            if (!used)
                unused_elements.add(element);
        }

        return unused_elements;
    }

}
