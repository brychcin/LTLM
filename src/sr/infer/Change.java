package sr.infer;

import sr.Role;

public class Change {

	private Role nodeToChange = null;
	private Role targetNode = null;
	private short newKey = 0;
	private double score = Double.NEGATIVE_INFINITY;
	
	public Role getNodeToChange() {
		return nodeToChange;
	}
	public void setNodeToChange(Role nodeToChange) {
		this.nodeToChange = nodeToChange;
	}
	public Role getTargetNode() {
		return targetNode;
	}
	public void setTargetNode(Role targetNode) {
		this.targetNode = targetNode;
	}
	public short getNewKey() {
		return newKey;
	}
	public void setNewKey(short newKey) {
		this.newKey = newKey;
	}
	
	public String toString() {
		return "Sampled change from "+nodeToChange.getPosition()+" to "+targetNode.getPosition();
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	
}
