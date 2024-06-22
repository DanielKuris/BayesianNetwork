import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * The following class will represent an element in a Bayesian network.
 */
public class BayesianNetworkElement {
    public String name;
    public List<String> outcomes;
    public List<BayesianNetworkElement> given;
    public double[] table;

    // Visit and color fields for the Bayes Ball algorithm
    public int visit;
    public int color;

    public static final int UNVISITED = 0;
    public static final int VISIT_FROM_PARENT = 1;
    public static final int VISIT_FROM_CHILD = 2;

    public static final int UNCOLORED = 0;
    public static final int COLORED = 1;

    public BayesianNetworkElement(String name, List<String> outcomes) {
        this.name = name;
        this.outcomes = outcomes;
        this.given = null;
        this.table = null;
        this.visit = UNVISITED;
        this.color = UNCOLORED;
    }

    /*
     * Interprets the <GIVEN> tag and saves it to the element.
     */
    public void parseGiven(String[] given, List<BayesianNetworkElement> network_elements) {
        this.given = new ArrayList<>();

        for(String given_element: given) {
            for (BayesianNetworkElement element: network_elements) {
                if (Objects.equals(element.name, given_element)) {
                    this.given.add(element);
                }
            }
        }
    }

    /*
     * Interprets the <TABLE> tag and saves it to the element.
     */
    public void parseCptTable(String table) {
        this.table = new double[table.split(" ").length];
        for(int i = 0; i < table.split(" ").length; i++) {
            this.table[i] = Double.parseDouble(table.split(" ")[i]);
        }
    }

    /*
     * Returns the CPT of this element's variable given a specific outcome.
     */
    public double getCptValue(List<String> outcomes) {
        int offset = 0;
        int modifier = this.table.length;

        int i = 0;
        for (BayesianNetworkElement parent: this.given) {
            modifier /= parent.outcomes.size();
            offset += modifier * parent.outcomes.indexOf(outcomes.get(i));

            i++;
        }

        modifier /= this.outcomes.size();
        offset += modifier * this.outcomes.indexOf(outcomes.get(i));

        return this.table[offset];
    }

    public boolean hasParent(List<BayesianNetworkElement> network) {
        for (BayesianNetworkElement element : network) {
            if (element.given != null && element.given.contains(this)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasChild() {
        return this.given != null && !this.given.isEmpty();
    }
}
