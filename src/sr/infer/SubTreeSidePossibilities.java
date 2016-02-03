package sr.infer;

public class SubTreeSidePossibilities {

	private short from;
	private short to;
	private SubTree[] left;
	private SubTree[] right;
	
	public SubTreeSidePossibilities(short from, short to, int roles) {
		this.from = from;
		this.to = to;
		this.left = new SubTree[roles];
		this.right = new SubTree[roles];
	}
	
	@Override
	public int hashCode() {
		return this.from * this.to;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SubTreeSidePossibilities)) return false;
		SubTreeSidePossibilities r = (SubTreeSidePossibilities) o;
		if (r.from != from) return false;
		if (r.to != to) return false;
		return true;
	}
	
	public String toString() {
		return from+"-"+to;
	}

	public short getFrom() {
		return from;
	}

	public void setFrom(short from) {
		this.from = from;
	}

	public short getTo() {
		return to;
	}

	public void setTo(short to) {
		this.to = to;
	}

	public SubTree[] getLeft() {
		return left;
	}

	public void setLeft(SubTree[] left) {
		this.left = left;
	}

	public SubTree[] getRight() {
		return right;
	}

	public void setRight(SubTree[] right) {
		this.right = right;
	}
	
}
