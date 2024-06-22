import java.util.*;
import java.util.stream.Collectors;
/*
 * The following class represents
 */
public class Factor {
    List<BayesianNetworkElement> factor_variables;
    List<Double> values;

    /*
     * Creates a completely new factor that 'represents' and element.
     */
    public Factor(List<BayesianNetworkElement> network, BayesianNetworkElement element, List<String> evidence) {
        // Indicate up all free variables that are used in this factor:
        List<BayesianNetworkElement> variables = new ArrayList<>(element.given);
        variables.add(element);

        this.factor_variables = new ArrayList<>();

        List<String> evidence_name = new ArrayList<>();
        for (String variable: evidence) {
            evidence_name.add(variable.split("=")[0]);
        }
        for (BayesianNetworkElement variable: variables) {
            if (!evidence_name.contains(variable.name)) {
                factor_variables.add(variable);
            }
        }

        // Sorting variables by network order for consistent order:
        Collections.sort(this.factor_variables, new Comparator<BayesianNetworkElement>() {
            public int compare(BayesianNetworkElement left, BayesianNetworkElement right) {
                return Integer.compare(network.indexOf(left), network.indexOf(right));
            }
        });

        // Get all possible outcomes:
        List<String> outcomes = BayesianNetworkTools.defineOutcomes(network, evidence);
        List<List<String>> variable_outcomes = new ArrayList<>();
        for (String outcome: outcomes) {
            List<String> variable_outcome = new ArrayList<>();
            for (BayesianNetworkElement variable: variables) {
                variable_outcome.add(outcome.split(",")[network.indexOf(variable)]);
            }
            variable_outcomes.add(variable_outcome);
        }

        // removes duplicates:
        variable_outcomes = variable_outcomes.stream().distinct().collect(Collectors.toList());

        // Get appropriate values for the factor's table based on the outcomes:
        this.values = new ArrayList<>();
        for (List<String> outcome: variable_outcomes) {
            this.values.add(element.getCptValue(outcome));
        }
    }

    /*
     * Creates a factor by joining two existing factors.
     */
    public Factor(List<BayesianNetworkElement> network, Factor a, Factor b, MathematicalOperationsCounter counter) {
        this.factor_variables = new ArrayList<>(a.factor_variables);
        for (BayesianNetworkElement variable: b.factor_variables) {
            if (!a.factor_variables.contains(variable))
                this.factor_variables.add(variable);
        }

        // Sorting variables by network order for consistent order:
        Collections.sort(this.factor_variables, new Comparator<BayesianNetworkElement>() {
            public int compare(BayesianNetworkElement left, BayesianNetworkElement right) {
                return Integer.compare(network.indexOf(left), network.indexOf(right));
            }
        });

        // Get all possible outcomes:
        List<String> outcomes = new ArrayList<>();
        BayesianNetworkTools.generatePermutations(network, outcomes, 0, "");

        List<List<String>> variable_outcomes = new ArrayList<>();
        for (String outcome: outcomes) {
            List<String> variable_outcome = new ArrayList<>();
            for (BayesianNetworkElement variable: this.factor_variables) {
                variable_outcome.add(outcome.split(",")[network.indexOf(variable)]);
            }
            variable_outcomes.add(variable_outcome);
        }

        // removes duplicates:
        variable_outcomes = variable_outcomes.stream().distinct().collect(Collectors.toList());

        // Get appropriate values for the factor's table based on the outcomes:
        this.values = new ArrayList<>();
        for (List<String> outcome: variable_outcomes) {
            List<String> outcome_a = new ArrayList<>();
            List<String> outcome_b = new ArrayList<>();
            int i = 0;
            for (String value: outcome) {
                if (a.factor_variables.contains(this.factor_variables.get(i))) {
                    outcome_a.add(value);
                }
                if (b.factor_variables.contains(this.factor_variables.get(i))) {
                    outcome_b.add(value);
                }
                i++;
            }

            double a_val = a.getValue(outcome_a);
            double b_val = b.getValue(outcome_b);
            this.values.add(a_val*b_val);
            counter.multiplication();
        }
    }

    /*
     * Creates a factor from another factor but removes a variable.
     */
    public Factor(List<BayesianNetworkElement> network, Factor a, String variable_to_eliminate, MathematicalOperationsCounter counter) {
        this.factor_variables = new ArrayList<>(a.factor_variables);

        BayesianNetworkElement eliminated_variable = null;
        for (BayesianNetworkElement element: this.factor_variables) {
            if (Objects.equals(element.name, variable_to_eliminate)) {
                eliminated_variable = element;
                break;
            }
        }
        this.factor_variables.remove(eliminated_variable);
        int eliminated_variable_index = a.factor_variables.indexOf(eliminated_variable);

        // Get all possible outcomes:
        List<String> outcomes = new ArrayList<>();
        BayesianNetworkTools.generatePermutations(network, outcomes, 0, "");

        List<List<String>> variable_outcomes = new ArrayList<>();
        for (String outcome: outcomes) {
            List<String> variable_outcome = new ArrayList<>();
            for (BayesianNetworkElement variable: this.factor_variables) {
                variable_outcome.add(outcome.split(",")[network.indexOf(variable)]);
            }
            variable_outcomes.add(variable_outcome);
        }

        // removes duplicates:
        variable_outcomes = variable_outcomes.stream().distinct().collect(Collectors.toList());

        // Get appropriate values for the factor's table based on the outcomes:
        this.values = new ArrayList<>();
        for (List<String> outcome: variable_outcomes) {
            double cpt_value = 0;

            int i = 0;
            for (String eliminated_variable_outcome: eliminated_variable.outcomes) {
                outcome.add(eliminated_variable_index, eliminated_variable_outcome);

                cpt_value += a.getValue(outcome);
                if (i != 0)
                    counter.addition();
                i++;

                outcome.remove(eliminated_variable_index);
            }

            this.values.add(cpt_value);
        }
    }

    public List<String> getVariables() {
        List<String> variables = new ArrayList<>();
        for (BayesianNetworkElement element: this.factor_variables) {
            variables.add(element.name);
        }
        return variables;
    }

    /*
     * Returns factor's value for specific outcome combination.
     */
    public double getValue(List<String> outcomes) {
        int offset = 0;
        int modifier = this.values.size();

        int i = 0;
        for (BayesianNetworkElement parent: this.factor_variables) {
            modifier /= parent.outcomes.size();
            offset += modifier * parent.outcomes.indexOf(outcomes.get(i));
            i++;
        }

        return this.values.get(offset);
    }
}
