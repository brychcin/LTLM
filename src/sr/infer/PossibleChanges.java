package sr.infer;

import java.util.HashSet;
import java.util.Set;

import sr.Role;
import sr.Sentence;

public class PossibleChanges {

	private Sentence sentence;
	//private Set<Role>[] nodeChanges;
	
	public PossibleChanges(Sentence sentence) {
		this.sentence = sentence;
		//this.nodeChanges = new Set[sentence.size()]; 
	}
	/*
	public Set<Role>[] getNodeChanges() {
		return nodeChanges;
	}
	
	public void calculateChanges() {
		//System.out.println("Pocitam zmeny u "+sentence.size()+" roli");
		for (int pos=0; pos<sentence.size(); pos++) {
			getNodeChanges()[pos] = findPossibleChanges(sentence.getTokens()[pos].getRole());
		}
	}
	
	private Set<Role> findPossibleChanges2(Role role) {
		Set<Role> changes = new HashSet<Role>();
		changes.add(role.getParent());
		
		for (int pos=0; pos<sentence.size(); pos++) {
			Role target = sentence.getTokens()[pos].getRole();
			if (target.getPosition() == role.getPosition()) continue;
			if (target.isDescendantOf(role)) continue;
			
			if (role.isNextTo(target)) {
				changes.add(target);
				if (target.getParent() != null) changes.add(target.getParent());
			}
		}
		
		return changes;
	}
	*/
	
	public Set<Role> findPossibleChanges(Role role) {
		Set<Role> changes = new HashSet<Role>();
		//predchudci role
		Role last = role.getParent();
		if (last == null) return changes;
		changes.add(role.getParent());
		
		//System.out.println("Pocitam rodice u "+role.getPosition());
		while (last.getParent() != null) {
			if (role.isOnSideOf(last)) {
				changes.add(last.getParent());
			} else break;
			last = last.getParent();
		}
		
		//System.out.println("Pocitam rodice leveho u "+role.getPosition());
		//pokud to neni prvni prvek
		if (role.getPosition() > 0) {
			Role left = sentence.getTokens()[role.getPosition()-1].getRole();
			//pokud to neni potomek
			if (!left.isDescendantOf(role)) {
				findChanges(left, changes);
			}
		}
		
		//System.out.println("Pocitam rodice praveho u "+role.getPosition());
		//pokud to neni posledni prvek
		if (role.getPosition() < sentence.size()-1) {
			Role right = sentence.getTokens()[role.getPosition()+1].getRole();
			//pokud to neni potomek
			if (!right.isDescendantOf(role)) {
				findChanges(right, changes);
			}
		}
				
		for (int pos=0; pos<sentence.size(); pos++) {
			Role target = sentence.getTokens()[pos].getRole();
			if (target.getPosition() == role.getPosition()) continue;
			if (target.isDescendantOf(role)) continue;
			
			if (role.isNextTo(target)) {
				changes.add(target);
				if (target.getParent() != null) changes.add(target.getParent());
			}
		}
		
		changes.remove(role);
		return changes;
	}
	
	private void findChanges(Role role, Set<Role> knownChanges) {
		Role last = role;

		while (last != null) {
			if (knownChanges.contains(last)) return;
				
			//if (last.getChildrens().size() < last.MAX_CHILDRENS) {
				knownChanges.add(last);
			//}
			last = last.getParent();
		}
	}
	
	/*
	public String toString() {
		String s = "";
		
		for (int pos=0; pos<nodeChanges.length; pos++) {
			Role role = sentence.getTokens()[pos].getRole();
			System.out.print("Role "+role.toGraphVizString()+": ");
			for (Role target : nodeChanges[pos]) {
				System.out.print(target.toGraphVizString()+", ");
			}
			System.out.println();
		}
		
		return s;
	}*/
}
