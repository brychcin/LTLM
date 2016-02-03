package sr.utils;

import java.io.Serializable;

public class MutableDouble implements Serializable {

	private static final long serialVersionUID = -7626575704692199011L;
	private double value;
   
	public MutableDouble(double value) {
        this.value = value;
    }
    public void set(double value) {
        this.value = value;
    }
    public double get() {
        return value;
    }
	
}
