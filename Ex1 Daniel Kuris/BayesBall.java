import java.util.*;

public class BayesBall {

    public static boolean runBayesBall(List<BayesianNetworkElement> network, String variables, String evidenceString) {
        String[] variableNames = variables.split("-");
        String[] evidenceVariables = evidenceString.split(",");

        // Parse evidence variables into a list of names only
        List<String> evidenceNames = new ArrayList<>();
        for (String evidenceVar : evidenceVariables) {
            if (!evidenceVar.trim().isEmpty()) {
                evidenceNames.add(evidenceVar.split("=")[0].trim());
            }
        }

        // Retrieve BayesianNetworkElement for each variable
        if (variableNames.length != 2) {
            return false;
        }

        String var1Name = variableNames[0].trim();
        String var2Name = variableNames[1].trim();

        BayesianNetworkElement var1 = searchElementByName(network, var1Name);
        BayesianNetworkElement var2 = searchElementByName(network, var2Name);

        if (var1 == null || var2 == null) {
            return false;
        }


        // Determine independence using parsed evidence names
        boolean independent = isIndependent(var1, var2, evidenceNames, network);




        return independent;
    }

    public static boolean isIndependent(BayesianNetworkElement source, BayesianNetworkElement destination,List<String> evidenceNames, List<BayesianNetworkElement> network) {
        if(source.name.equals(destination.name))
            return false;

        // Mark evidence nodes if there are any
        if (!evidenceNames.isEmpty()) {
            markEvidences(evidenceNames, network);
        }

        BayesianNetworkElement result = canReach(source, destination, network);

        // Reset the network state
        resetVars(network);

        if(result == null)
            return true;
        return !result.name.equals(destination.name);
    }

    private static BayesianNetworkElement canReach(BayesianNetworkElement source, BayesianNetworkElement target, List<BayesianNetworkElement> network) {
        Queue<BayesianNetworkElement> toVisit = new LinkedList<>();
        source.visit = BayesianNetworkElement.VISIT_FROM_CHILD;
        toVisit.add(source);

        while (!toVisit.isEmpty()) {
            BayesianNetworkElement curr = toVisit.remove();
            curr.updateChildren(network);

            if (curr.equals(target)) {
                return target;
            }

            if (curr.color == BayesianNetworkElement.UNCOLORED && curr.visit == BayesianNetworkElement.VISIT_FROM_CHILD) {
                if (curr.hasChild()) {
                    for (BayesianNetworkElement child : curr.children) {
                        if (child.visit == BayesianNetworkElement.UNVISITED) {
                            child.visit = BayesianNetworkElement.VISIT_FROM_PARENT;
                            toVisit.add(child);
                        }
                    }
                }
                if (curr.hasParent()) {
                    for (BayesianNetworkElement parent : curr.given) {
                        if (parent.visit == BayesianNetworkElement.UNVISITED) {
                            parent.visit = BayesianNetworkElement.VISIT_FROM_CHILD;
                            toVisit.add(parent);
                        }
                    }
                }
            } else if (curr.color == BayesianNetworkElement.UNCOLORED && curr.visit == BayesianNetworkElement.VISIT_FROM_PARENT) {
                if (curr.hasChild()) {
                    for (BayesianNetworkElement child : curr.children) {
                        if (child.visit != BayesianNetworkElement.VISIT_FROM_PARENT) {
                            child.visit = BayesianNetworkElement.VISIT_FROM_PARENT;
                            toVisit.add(child);
                        }
                    }
                }
            } else if (curr.color == BayesianNetworkElement.COLORED && curr.visit == BayesianNetworkElement.VISIT_FROM_PARENT) {
                if (curr.hasParent()) {
                    for (BayesianNetworkElement parent : curr.given) {
                        if (parent.visit != BayesianNetworkElement.VISIT_FROM_CHILD) {
                            parent.visit = BayesianNetworkElement.VISIT_FROM_CHILD;
                            toVisit.add(parent);
                        }
                    }
                }
            }
            // Case 4 (color == COLORED && visit == VISIT_FROM_CHILD) - DO NOTHING!
        }

        return null;
    }

    private static void markEvidences(List<String> evidenceNames, List<BayesianNetworkElement> network) {
        for (String name : evidenceNames) {
            BayesianNetworkElement element = searchElementByName(network, name);
            if (element != null) {
                element.color = BayesianNetworkElement.COLORED;
            }
        }
    }

    private static void resetVars(List<BayesianNetworkElement> network) {
        for (BayesianNetworkElement element : network) {
            element.color = BayesianNetworkElement.UNCOLORED;
            element.visit = BayesianNetworkElement.UNVISITED;
        }
    }


    private static BayesianNetworkElement searchElementByName(List<BayesianNetworkElement> network, String name) {
        for (BayesianNetworkElement element : network) {
            if (Objects.equals(element.name, name)) {
                return element;
            }
        }
        return null;
    }
}
