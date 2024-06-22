/*
 * This class will be used to count the number of mathematical operators used during CPT calculation.
 */
public class MathematicalOperationsCounter {
    public int addition_counter;
    public int multiplication_counter;

    public MathematicalOperationsCounter () {
        addition_counter = 0;
        multiplication_counter = 0;
    }

    public void addition() {
        addition_counter++;
    }

    public void multiplication() {
        multiplication_counter++;
    }
}
