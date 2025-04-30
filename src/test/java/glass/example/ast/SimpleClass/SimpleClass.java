package glass.example.ast.SimpleClass;

public class SimpleClass {
	
	private int count;
	private String name = "Class name";
	private char id;
	
	public SimpleClass(char c) {
		this.id = c;
	}
	
	public void printId() {
		System.out.println(this.id);
	}
	
	public int getCount() {
		return this.count;
	}
	
	private void compute(int a, String c) {
		// do nothing
	}

}
