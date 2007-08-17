package org.omnetpp.ned.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ui.internal.texteditor.quickdiff.compare.rangedifferencer.IRangeComparator;
import org.eclipse.ui.internal.texteditor.quickdiff.compare.rangedifferencer.RangeDifference;
import org.eclipse.ui.internal.texteditor.quickdiff.compare.rangedifferencer.RangeDifferencer;
import org.omnetpp.common.editor.text.TextDifferenceUtils;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.engine.NEDErrorStore;
import org.omnetpp.ned.model.ex.NEDElementFactoryEx;
import org.omnetpp.ned.model.pojo.CommentNode;
import org.omnetpp.ned.model.pojo.ConnectionNode;
import org.omnetpp.ned.model.pojo.NEDElementFactory;
import org.omnetpp.ned.model.pojo.NEDElementTags;

public class NEDTreeDifferenceUtils {
	public interface INEDTreeDifferenceApplier {
		public void replaceElements(INEDElement parent, int start, int end, int offset, INEDElement[] replacement);

		public void replaceAttribute(INEDElement element, String name, String value);
	}

	@SuppressWarnings("unchecked")
	public static void applyTreeDifferences(INEDElement original, INEDElement target, INEDTreeDifferenceApplier applier) {
		applyAttributeDifferences(original, target, applier);

		NEDElementChildrenComparator comparatorOriginal = new NEDElementChildrenComparator(original);
		NEDElementChildrenComparator comparatorTarget = new NEDElementChildrenComparator(target);
		List<RangeDifference> differences = RangeDifferencer.findRanges(comparatorOriginal, comparatorTarget);

		Collections.sort(differences, new Comparator<RangeDifference>() {
			public int compare(RangeDifference o1, RangeDifference o2) {
				return o1.leftStart() - o2.leftStart();
			}}
		);

		int offset = 0;
		for (RangeDifference difference : differences) {
//			System.out.println(difference);

			int leftStart = difference.leftStart();
			int leftEnd = difference.leftEnd();
			int rightStart = difference.rightStart();
			int rightEnd = difference.rightEnd();

			if (difference.kind() == RangeDifference.NOCHANGE) {
				for (int i = leftStart; i < leftEnd; i++)
					applyTreeDifferences(original.getChild(i) , target.getChild(rightStart + i - leftStart), applier);
			}
			else if (difference.kind() == RangeDifference.CHANGE) {
				INEDElement[] replacement = comparatorTarget.getElementRange(rightStart, rightEnd);
				applier.replaceElements(original, leftStart, leftEnd, offset, replacement);
				offset += difference.rightLength() - difference.leftLength();
			}
			else
				throw new RuntimeException("Unkown difference kind");
		}
	}

	private static void applyAttributeDifferences(INEDElement original, INEDElement target, INEDTreeDifferenceApplier applier) {
		int num = original.getNumAttributes();
		for (int j = 0; j < num; j++) {
			String originalAttribute = original.getAttribute(j);
			String targetAttribute = target.getAttribute(j);

			if (!StringUtils.equals(originalAttribute, targetAttribute))
				applier.replaceAttribute(original, target.getAttributeName(j), targetAttribute);
		}
	}

	public static class NEDTreeDifferenceApplier implements NEDTreeDifferenceUtils.INEDTreeDifferenceApplier {
		final ArrayList<Runnable> runnables = new ArrayList<Runnable>();
		
		public void replaceAttribute(final INEDElement element, final String name, final String value) {
			runnables.add(new Runnable() {
				public void run() {
					element.setAttribute(name, value);
				}
			});
		}

		public void replaceElements(final INEDElement parent, final int start, final int end, final int offset, final INEDElement[] replacement) {
			runnables.add(new Runnable() {
				public void run() {
					for (int i = start; i < end; i++)
						parent.getChild(offset + start).removeFromParent();

					INEDElement startChild = parent.getChild(offset + start);

					for (INEDElement element : replacement)
						parent.insertChildBefore(startChild, element);
				}
			});
		}
		
		public void apply() {
			for (Runnable runnable : runnables)
				runnable.run();
		}

		public boolean hasDifferences() {
			return !runnables.isEmpty();
		}
	}

	public static void main(String[] args) {
		new NEDTreeDifferenceTest().run();
	}
}

class NEDElementChildrenComparator implements IRangeComparator {
	private INEDElement parent;

	public NEDElementChildrenComparator(INEDElement parent) {
		this.parent = parent;
	}

	public int getRangeCount() {
		return parent.getNumChildren();
	}

	public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
		INEDElement thisElement = getElementAt(thisIndex);
		INEDElement otherElement = ((NEDElementChildrenComparator)other).getElementAt(otherIndex);

		if (thisElement == null || otherElement == null || thisElement.getTagCode() != otherElement.getTagCode())
			return false;
		
		if (thisElement.getTagCode() == NEDElementTags.NED_CONNECTION) {
			ConnectionNode thisConnection = (ConnectionNode)thisElement;
			ConnectionNode otherConnection = (ConnectionNode)otherElement;
			
			String thisId = getConnectionId(thisConnection);
			String otherId = getConnectionId(otherConnection);

			return thisId.equals(otherId);
		}
		else {
			String thisName = thisElement.getAttribute("name");
			String otherName = otherElement.getAttribute("name");
			
			return StringUtils.equals(thisName, otherName);
		}
	}

	private String getConnectionId(ConnectionNode connection) {
		return connection.getSrcModule() + connection.getSrcGate() + connection.getSrcGateIndex() +
			connection.getDestModule() + connection.getDestGate() + connection.getDestGateIndex();
	}

	public INEDElement getElementAt(int index) {
		return parent.getChild(index);
	}

	public INEDElement[] getElementRange(int start, int end) {
		INEDElement[] range = new INEDElement[end - start];
		for (int i = 0; i < range.length; i++)
			range[i] = getElementAt(start + i);
		return range;
	}

	public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
		return false;
	}
}

class NEDTreeDifferenceTest {
	private static Random random = new Random(1);
	
	public void run() {
		NEDElementFactoryEx.setInstance(new NEDElementFactoryEx());

		for (int i = 0; i < 100; i++) {
			INEDElement original = NEDTreeUtil.loadNedSource("C:\\Workspace\\Repository\\trunk\\omnetpp\\samples\\queuenet\\CallCenter.ned", new NEDErrorStore());
			test(original);
		}
	}

	private void test(INEDElement original) {
		INEDElement target = original.deepDup();
		doRandomTreeChanges(target);
		
		String targetNED = NEDTreeUtil.generateNedSource(target, false);
		String targetXML = NEDTreeUtil.generateXmlFromPojoElementTree(target, "", false);

		NEDTreeDifferenceUtils.NEDTreeDifferenceApplier applier = new NEDTreeDifferenceUtils.NEDTreeDifferenceApplier();
		NEDTreeDifferenceUtils.applyTreeDifferences(original, target, applier);
		applier.apply();
		
		final String originalNED = NEDTreeUtil.generateNedSource(original, false);
		final String originalXML = NEDTreeUtil.generateXmlFromPojoElementTree(original, "", false);

		System.out.println("Original NED: " + originalNED);
		System.out.println("Target NED: " + targetNED);

		TextDifferenceUtils.applyTextDifferences(originalNED, targetNED, new TextDifferenceUtils.ITextDifferenceApplier() {
			public void replace(int start, int end, String replacement) {
				System.out.println("Original first line: >>>" + StringUtils.getLines(originalNED, start, end) + "<<<");
				System.out.println("Target start: " + start + " end: " + end + " difference: >>>" + replacement + "<<<");
			}
		});

		System.out.println("Original XML: " + originalXML);
		System.out.println("Target XML: " + targetXML);

		TextDifferenceUtils.applyTextDifferences(originalXML, targetXML, new TextDifferenceUtils.ITextDifferenceApplier() {
			public void replace(int start, int end, String replacement) {
				System.out.println("Original: >>>" + StringUtils.getLines(originalXML, start, end) + "<<<");
				System.out.println("Target start: " + start + " end: " + end + " difference: >>>" + replacement + "<<<");
			}
		});

		Assert.isTrue(originalNED.equals(targetNED));
		Assert.isTrue(originalXML.equals(targetXML));
	}
	
	private void doRandomTreeChanges(INEDElement element) {
		dupRandomNodes(element);
		deleteRandomChildren(element);
		insertRandomCommentNodes(element);
		changeRandomAttributes(element);

		for (INEDElement child : element)
			doRandomTreeChanges(child);
	}
	
	private void deleteRandomChildren(INEDElement element) {
		for (int i = 0; i < element.getNumChildren(); i++)
			if (random.nextDouble() < 0.1)
				element.getChild(i).removeFromParent();
	}

	private void insertRandomCommentNodes(INEDElement element) {
		if (!(element instanceof CommentNode)) {
			for (int i = 0; i < random.nextInt(3); i++) {
				CommentNode comment = (CommentNode)NEDElementFactory.getInstance().createNodeWithTag("comment");
				comment.setLocid("banner");
				comment.setContent("// Comment: " + String.valueOf(random.nextInt(10000)));
				element.insertChildBefore(element.getChild(random.nextInt(element.getNumChildren() + 1)), comment);
			}
		}
	}
	
	private void dupRandomNodes(INEDElement element) {
		if (element.getNumChildren() > 0)
			while (random.nextDouble() < 0.1) {
				INEDElement child = element.getChild(random.nextInt(element.getNumChildren()));
				element.insertChildBefore(element.getChild(random.nextInt(element.getNumChildren() + 1)), child.deepDup());
			}
	}

	private static void changeRandomAttributes(INEDElement element) {
		for (int i = 0; i < element.getNumAttributes(); i++)
			if (random.nextDouble() < 0.3) {
				try {
					element.setAttribute(i, String.valueOf(random.nextInt(100)));
				}
				catch (RuntimeException e) {
					// ignore
				}
			}	
	}
}
