package org.omnetpp.experimental.animation.widgets;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class AnimationCanvas extends Canvas {
	protected IFigure contents;
	protected LightweightSystem lws;

	public AnimationCanvas(Composite parent, int style) {
		super(parent, style);

		init();
	}

	protected void init() {
		XYLayout contentsLayout = new XYLayout();

		contents = new Figure();
		contents.setLayoutManager(contentsLayout);

		lws = new LightweightSystem(this);
		lws.setContents(contents);
	}
	
	public IFigure getRootFigure() {
		return contents;
	}

	public void reset() {
		contents.erase();
	}
}
