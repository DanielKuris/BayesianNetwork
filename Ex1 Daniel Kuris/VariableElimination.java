import java.util.*;

public class VariableElimination {
    public static double calculateCPT(List<BayesianNetworkElement> network, String request_left, String request_right, String eliminationOrder, MathematicalOperationsCounter counter) {
        // format: B=T|J=T,M=T
        List<String> evidence = new ArrayList<>(Arrays.asList(request_right.split(",")));
        List<String> usedVariables = parseUsedVariables(request_left, request_right);
    
        // Call the updated keepRelevantElements
        List<BayesianNetworkElement> relevant_elements = keepRelevantElements(network, usedVariables);
    
        List<Factor> factors = new ArrayList<>();
        for (BayesianNetworkElement element: relevant_elements) {
            factors.add(new Factor(relevant_elements, element, evidence));
        }
        removeEmptyFactors(factors);
    
        evidence.add(request_left);
    
        List<BayesianNetworkElement> free_variables = BayesianNetworkTools.getUnusedElements(relevant_elements, evidence);
        List<String> free_variable_names = new ArrayList<>();
        for (BayesianNetworkElement variable: free_variables) {
            free_variable_names.add(variable.name);
        }
    
        // Ensure the elimination order respects the provided list
        List<String> eliminationOrderList = Arrays.asList(eliminationOrder.split("-"));
        free_variable_names.retainAll(eliminationOrderList);
    
        for (String free_variable: free_variable_names) {
            joinFactors(relevant_elements, factors, free_variable, counter);
            eliminateVariable(relevant_elements, factors, free_variable, counter);
            removeEmptyFactors(factors);
        }
    
        joinFactors(relevant_elements, factors, request_left.split("=")[0], counter);
    
        double numerator = 0;
        double denominator = 0;
    
        Factor final_factor = filterFactorsByValue(factors, request_left.split("=")[0], false).get(0);
    
        for (String outcome: final_factor.factor_variables.get(0).outcomes) {
            List<String> outcome_l = new ArrayList<>();
            outcome_l.add(outcome);
            double cpt_value = final_factor.getValue(outcome_l);
    
            if (Objects.equals(outcome, request_left.split("=")[1])) {
                numerator = cpt_value;
            }
    
            denominator += cpt_value;
            if (final_factor.factor_variables.get(0).outcomes.indexOf(outcome) != 0)
                counter.addition();
        }
    
        return numerator/denominator;
    }
      

    private static void eliminateVariable(List<BayesianNetworkElement> network, List<Factor> factors, String free_variable, MathematicalOperationsCounter counter) {
        Factor relevant_factor = filterFactorsByValue(factors, free_variable, true).get(0);

        Factor clean_factor = new Factor(network, relevant_factor, free_variable, counter);

        factors.add(clean_factor);
    }

    public static void joinFactors(List<BayesianNetworkElement> network, List<Factor> factors, String free_variable, MathematicalOperationsCounter counter) {
        List<Factor> relevant_factors = filterFactorsByValue(factors, free_variable, true);

        while (relevant_factors.size() > 1) {
            Factor a = getSmallestFactor(relevant_factors, true);
            Factor b = getSmallestFactor(relevant_factors, true);

            Factor joint = new Factor(network, a, b, counter);
            relevant_factors.add(joint);
        }

        factors.add(relevant_factors.get(0));
    }

    public static List<Factor> filterFactorsByValue(List<Factor> factors, String variable, boolean remove) {
        List<Factor> relevant_factors = new ArrayList<>();
        for (Factor factor: factors) {
            if (factor.getVariables().contains(variable)) {
                relevant_factors.add(factor);
            }
        }

        if (remove) {
            for (Factor factor : relevant_factors) {
                factors.remove(factor);
            }
        }

        return relevant_factors;
    }

    /*
     * Returns the smallest factor by value size.
     */
    public static Factor getSmallestFactor(List<Factor> factors, boolean remove) {
        Factor smallest = factors.get(0);
        for (Factor factor: factors) {
            if (factor.values.size() < smallest.values.size())
                smallest = factor;
        }

        if (remove)
            factors.remove(smallest);

        return smallest;
    }

    public static void removeEmptyFactors(List<Factor> factors) {
        List<Factor> factors_to_be_removed = new ArrayList<>();

        for (Factor factor: factors) {
            if (factor.values.size() <= 1)
                factors_to_be_removed.add(factor);
        }

        for (Factor factor: factors_to_be_removed) {
            factors.remove(factor);
        }
    }

    /*
     * Keeps only the elements that are relevant to the query.
     * Eliminating variables further using Bayes Ball algorithm
     */
    /*
     * Keeps only the elements that are relevant to the query.
     */
    public static List<BayesianNetworkElement> keepRelevantElements(List<BayesianNetworkElement> network, List<String> used_variables) {
        List<BayesianNetworkElement> relevant_elements = new ArrayList<>();
        for (String variable: used_variables) {
            relevant_elements.add( BayesianNetworkTools.searchElementByName(network, variable) );
        }

        addAllParents(relevant_elements);

        Collections.sort(relevant_elements, new Comparator<BayesianNetworkElement>() {
            public int compare(BayesianNetworkElement left, BayesianNetworkElement right) {
                return Integer.compare(network.indexOf(left), network.indexOf(right));
            }
        });

        return relevant_elements;
    }
    
    

    /*
     * Adds the parents of all elements in the list recursively.
     */
    public static void addAllParents(List<BayesianNetworkElement> relevant_elements) {
        for (BayesianNetworkElement element: relevant_elements) {
            for (BayesianNetworkElement parent: element.given) {
                if (!relevant_elements.contains(parent)) {
                    relevant_elements.add(parent);
                    addAllParents(relevant_elements);
                    return;
                }
            }
        }
    }

    /*
     * Returns the names of all variables used in the query.
     */
    public static List<String> parseUsedVariables(String request_left, String request_right) {
        List<String> variables = new ArrayList<>();

        for (String variable: request_right.split(",")) {
            variables.add(variable.split("=")[0]);
        }
        variables.add(request_left.split("=")[0]);

        return variables;
    }
}
