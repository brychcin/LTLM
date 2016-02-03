package sr;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;

public class Role implements Comparable<Role>, Serializable {

	private static final long serialVersionUID = -6539040868282520887L;
	private short key;
	private Token token;
	private Role parent;
	//serazeny vzestupne podle pozice
	private SortedSet<Role> childrens;
	private short maxPosition;
	private short minPosition;
	private final short position;

	public Role(short key, Token token, short position) {
		this.key = key;
		this.token = token;
		this.position = position;
		this.childrens = new TreeSet<Role>();
	}
	
	public short getPosition() {
		return position;
	}

	public short getKey() {
		return key;
	}

	public void setKey(short key) {
		this.key = key;
	}

	public Token getToken() {
		return token;
	}

	public void setToken(Token token) {
		this.token = token;
	}

	public Role getParent() {
		return parent;
	}

	public void setParent(Role parent) {
		this.parent = parent;
	}

	public short getMaxPosition() {
		return maxPosition;
	}

	public void setMaxPosition(short maxPosition) {
		this.maxPosition = maxPosition;
	}

	public short getMinPosition() {
		return minPosition;
	}

	public void setMinPosition(short minPosition) {
		this.minPosition = minPosition;
	}
	
	public void setChild(Role role) {
		childrens.add(role);
		role.setParent(this);
	}

	public SortedSet<Role> getChildrens() {
		return childrens;
	}

	public void setChildrens(SortedSet<Role> childrens) {
		this.childrens = childrens;
	}

	public boolean isOnLeft() {
		return getPosition() < parent.getPosition();
	}
	
	public String toGraphVizString(Vocabulary vocabulary) {
		return vocabulary.getWordByKey(getToken().getKey())+"["+position+"]("+key+")("+getMinPosition()+"-"+getPosition()+"-"+getMaxPosition()+")";
		//return vocabulary.getWordByKey(getToken().getKey())+"["+position+"]("+key+")";
	}
	
	
	public void calculateMinMaxPositions() {
		minPosition = position;
		maxPosition = position;
		
		if (childrens.size() == 0) return;
		
		for (Role child : childrens) {
			child.calculateMinMaxPositions();
		}
		
		minPosition = childrens.first().getMinPosition();
		if (minPosition > position) minPosition = position;
		maxPosition = childrens.last().getMaxPosition();	
		if (maxPosition < position) maxPosition = position;
	}
	
	//jestli je na strane
	public boolean isOnSideOf(Role role) {
		return (minPosition==role.getMinPosition()) || (maxPosition==role.getMaxPosition());
	}
	
	//jestli je potomek
	public boolean isDescendantOf(Role role) {
		return (getMinPosition() >= role.getMinPosition() && getMaxPosition() <= role.getMaxPosition());
	}
	
	//jestli je to hned vedle
	public boolean isNextTo(Role role) {
		return (getMaxPosition()+1==role.getMinPosition()) || (role.getMaxPosition()+1==getMinPosition());		
	}
	
	@Override
	public int hashCode() {
		return this.position;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Role)) return false;
		Role r = (Role) o;
		if (r.getPosition() != position) return false;
		return true;
	}
	
	public String toString() {
		String s = "";
		s += "position="+position+" childrens: ";
		for (Role child : childrens) {
			s += child.getPosition()+", ";
		}
		
		return s;
	}

	@Override
	public int compareTo(Role r) {
		return getPosition() - r.getPosition();
	}
	
	public String toPennFormat() {
		String s = "("+this.key;
		
		for (Role role : this.childrens) {
			s += role.toPennFormat();
		}
		
		s+=")";
		return s;
	}

}
