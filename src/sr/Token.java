package sr;

import java.io.Serializable;

public class Token implements Serializable {

	private static final long serialVersionUID = 446420401467497873L;
	private final int key;
	private Role role;
	
	public Token(int key) {
		this.key = key;
	}
	
	public int getKey() {
		return key;
	}

	public Role getRole() {
		return role;
	}
	
	public void setRole(Role role) {
		this.role = role;
	}

}
