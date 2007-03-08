package org.omnetpp.scave.charting;

import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_ANTIALIAS;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_CANVAS_CACHING;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_DISPLAY_LEGEND;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_LEGEND_ANCHOR;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_LEGEND_BORDER;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_LEGEND_FONT;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_LEGEND_POSITION;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_TITLE;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_TITLE_FONT;
import static org.omnetpp.scave.charting.ChartProperties.PROP_ANTIALIAS;
import static org.omnetpp.scave.charting.ChartProperties.PROP_CACHING;
import static org.omnetpp.scave.charting.ChartProperties.PROP_DISPLAY_LEGEND;
import static org.omnetpp.scave.charting.ChartProperties.PROP_GRAPH_TITLE;
import static org.omnetpp.scave.charting.ChartProperties.PROP_GRAPH_TITLE_FONT;
import static org.omnetpp.scave.charting.ChartProperties.PROP_LEGEND_ANCHORING;
import static org.omnetpp.scave.charting.ChartProperties.PROP_LEGEND_BORDER;
import static org.omnetpp.scave.charting.ChartProperties.PROP_LEGEND_FONT;
import static org.omnetpp.scave.charting.ChartProperties.PROP_LEGEND_POSITION;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.jfree.data.general.Dataset;
import org.omnetpp.common.canvas.ZoomableCachingCanvas;
import org.omnetpp.common.canvas.ZoomableCanvasMouseSupport;
import org.omnetpp.common.image.ImageConverter;
import org.omnetpp.common.util.Converter;
import org.omnetpp.scave.ScavePlugin;
import org.omnetpp.scave.charting.ChartProperties.LegendAnchor;
import org.omnetpp.scave.charting.ChartProperties.LegendPosition;

/**
 * Base class for all chart widgets.
 * 
 * @author tomi, andras
 */
public abstract class ChartCanvas extends ZoomableCachingCanvas implements ICoordsMapping {

	protected boolean antialias = DEFAULT_ANTIALIAS;
	protected Title title = new Title(DEFAULT_TITLE, DEFAULT_TITLE_FONT);
	protected Legend legend = new Legend(DEFAULT_DISPLAY_LEGEND, DEFAULT_LEGEND_BORDER, DEFAULT_LEGEND_FONT, DEFAULT_LEGEND_POSITION, DEFAULT_LEGEND_ANCHOR);
	
	private String statusText = "No data available."; // displayed when there's no dataset 

	private ZoomableCanvasMouseSupport mouseSupport;
	
	public ChartCanvas(Composite parent, int style) {
		super(parent, style);
		setCaching(DEFAULT_CANVAS_CACHING);
		setBackground(ColorConstants.white);

		mouseSupport = new ZoomableCanvasMouseSupport(this); // add mouse handling; may be made optional
		
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				layoutChart();
			}
		});
	}

	/**
	 * Sets the data to be visualized by the chart.
	 */
	abstract void setDataset(Dataset dataset);
	
	/**
	 * Calculate positions of chart elements such as title, legend, axis labels, plot area. 
	 */
	abstract protected void layoutChart();

	/**
	 * Switches between zoom and pan mode. 
	 * @param mouseMode should be ZoomableCanvasMouseSupport.PAN_MODE or ZoomableCanvasMouseSupport.ZOOM_MODE
	 */
	public void setMouseMode(int mouseMode) {
		mouseSupport.setMouseMode(mouseMode);
	}

	public int getMouseMode() {
		return mouseSupport.getMouseMode();
	}

	public String getStatusText() {
		return statusText;
	}

	public void setStatusText(String statusText) {
		this.statusText = statusText;
		chartChanged();
	}
	
	public void setProperty(String name, String value) {
		// Titles
		if (PROP_GRAPH_TITLE.equals(name))
			setTitle(value);
		else if (PROP_GRAPH_TITLE_FONT.equals(name))
			setTitleFont(Converter.stringToSwtfont(value));
		// Legend
		else if (PROP_DISPLAY_LEGEND.equals(name))
			setDisplayLegend(Converter.stringToBoolean(value));
		else if (PROP_LEGEND_BORDER.equals(name))
			setLegendBorder(Converter.stringToBoolean(value));
		else if (PROP_LEGEND_FONT.equals(name))
			setLegendFont(Converter.stringToSwtfont(value));
		else if (PROP_LEGEND_POSITION.equals(name))
			setLegendPosition(Converter.stringToEnum(value, LegendPosition.class));
		else if (PROP_LEGEND_ANCHORING.equals(name))
			setLegendAnchor(Converter.stringToEnum(value, LegendAnchor.class));
		// Plot
		else if (PROP_ANTIALIAS.equals(name))
			setAntialias(Converter.stringToBoolean(value));
		else if (PROP_CACHING.equals(name))
			setCaching(Converter.stringToBoolean(value));
		else
			ScavePlugin.logError(new RuntimeException("unrecognized chart property: "+name));
	}
	
	public boolean getAntialias() {
		return antialias;
	}

	public void setAntialias(Boolean antialias) {
		this.antialias = antialias != null && antialias.booleanValue();
		chartChanged();
	}
	
	public void setCaching(Boolean caching) {
		super.setCaching(caching != null && caching.booleanValue());
		chartChanged();
	}
	
	public void setTitle(String value) {
		if (value == null)
			value = DEFAULT_TITLE;
		title.setText(value);
		chartChanged();
	}

	public void setTitleFont(Font value) {
		if (value == null)
			value = DEFAULT_TITLE_FONT;
		title.setFont(value);
		chartChanged();
	}
	
	public void setDisplayLegend(Boolean value) {
		if (value == null)
			value = DEFAULT_DISPLAY_LEGEND;
		legend.setVisible(value);
		chartChanged();
	}
	
	public void setLegendBorder(Boolean value) {
		if (value == null)
			value = DEFAULT_LEGEND_BORDER;
		legend.setDrawBorder(value);
		chartChanged();
	}
	
	public void setLegendFont(Font value) {
		if (value == null)
			value = DEFAULT_LEGEND_FONT;
		legend.setFont(value);
		chartChanged();
	}
	
	public void setLegendPosition(LegendPosition value) {
		if (value == null)
			value = DEFAULT_LEGEND_POSITION;
		legend.setPosition(value);
		chartChanged();
	}
	
	public void setLegendAnchor(LegendAnchor value) {
		if (value == null)
			value = DEFAULT_LEGEND_ANCHOR;
		legend.setAnchor(value);
		chartChanged();
	}
	
	/**
	 * Resets all GC settings except clipping and transform.
	 */
	public static void resetDrawingStylesAndColors(GC gc) {
		gc.setAntialias(SWT.DEFAULT);
		gc.setAlpha(255);
		gc.setBackground(ColorConstants.white);
		gc.setBackgroundPattern(null);
		//gc.setFillRule();
		gc.setFont(null);
		gc.setForeground(ColorConstants.black);
		gc.setForegroundPattern(null);
		gc.setInterpolation(SWT.DEFAULT);
		//gc.setLineCap();
		gc.setLineDash(null);
		//gc.setLineJoin();
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		//gc.setXORMode(false);
		gc.setTextAntialias(SWT.DEFAULT);
	}
	
	protected void chartChanged() {
		clearCanvasCacheAndRedraw();
	}
	
	/**
	 * Returns an object for efficient plot-to-canvas coordinate mapping. The returned 
	 * object is intended for *one-time* plotting the chart: it SHOULD BE DISCARDED
	 * at the end of the paint() method, because it captures chart geometry in "final" 
	 * variables which become obsolete once the user scrolls/resizes the chart.
	 */
	protected ICoordsMapping getOptimizedCoordinateMapper() {
		// Unoptimized version (for testing): 
		// return this;
		
		// how this method was created (also hints for maintenance): copy the corresponding
		// methods from ZoomableCachingCanvas, and create "final" variables for all 
		// member accesses and method calls in it.
		final double zoomX = getZoomX();
		final double zoomY = getZoomY();
		final double minX = this.getMinX();
		final double maxX = this.getMaxX();
		final double minY = this.getMinY();
		final double maxY = this.getMaxY();
		final long viewportLeftMinusLeftInset = getViewportLeft() - getLeftInset();
		final long viewportTopMinusTopInset = getViewportTop() - getTopInset();

		ICoordsMapping mapping = new ICoordsMapping() {
			private int numOverflows; // counts pixel coordinate overflows
			
			public double fromCanvasX(int x) {
				Assert.isTrue(-MAXPIX<x && x<MAXPIX);
				return (x + viewportLeftMinusLeftInset) / zoomX + minX;
			}

			public double fromCanvasY(int y) {
				Assert.isTrue(-MAXPIX<y && y<MAXPIX);
				return maxY - (y + viewportTopMinusTopInset) / zoomY;
			}

			public double fromCanvasDistX(int x) {
				return x / zoomX;
			}

			public double fromCanvasDistY(int y) {
				return y / zoomY;
			}
			
			public int toCanvasX(double xCoord) {
				double x = (xCoord - minX)*zoomX - viewportLeftMinusLeftInset;
				return toInt(x);
			}

			public int toCanvasY(double yCoord) {
				double y = (maxY - yCoord)*zoomY - viewportTopMinusTopInset;
				return toInt(y);
			}

			public int toCanvasDistX(double xCoord) {
				double x = xCoord * zoomX;
				return toInt(x);
			}

			public int toCanvasDistY(double yCoord) {
				double y = yCoord * zoomY;
				return toInt(y);
			}
			
			private int toInt(double c) {
				return c<-MAXPIX ? -largeValue(c) : c>MAXPIX ? largeValue(c) : Double.isNaN(c) ? NANPIX : (int)c;
			}
			
			private int largeValue(double c) {
				if (!Double.isInfinite(c)) // infinite is OK and does not count as coordinate overflow
					numOverflows++;
				return MAXPIX; 
			}

			public int getNumCoordinateOverflows() {
				return numOverflows;
			}

			public void resetCoordinateOverflowCount() {
				numOverflows = 0;				
			}
		};
		
		// run a mini regression test before we return it
		Assert.isTrue(toCanvasX(minX)==mapping.toCanvasX(minX) && toCanvasX(maxX)==mapping.toCanvasX(maxX));
		Assert.isTrue(toCanvasY(minY)==mapping.toCanvasY(minY) && toCanvasY(maxY)==mapping.toCanvasY(maxY));
		Assert.isTrue(toCanvasDistX(maxX-minX)==mapping.toCanvasDistX(maxX-minX));
		Assert.isTrue(toCanvasDistY(maxY-minY)==mapping.toCanvasDistY(maxY-minY));
		Rectangle r = getViewportRectangle();
		Assert.isTrue(fromCanvasX(r.x)==mapping.fromCanvasX(r.x) && fromCanvasX(r.x+r.width)==mapping.fromCanvasX(r.x+r.width));
		Assert.isTrue(fromCanvasY(r.y)==mapping.fromCanvasY(r.y) && fromCanvasY(r.y+r.height)==mapping.fromCanvasY(r.y+r.height));
		Assert.isTrue(fromCanvasDistX(r.width)==mapping.fromCanvasDistX(r.width));
		Assert.isTrue(fromCanvasDistY(r.height)==mapping.fromCanvasDistY(r.height));

		return mapping;
	}

	
	/**
	 * Copies the image of the chart to the clipboard.
	 * Uses AWT functionality, because SWT does not support ImageTransfer yet.
	 * See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=78856.
	 */
	public void copyToClipboard() {
		Clipboard cp = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
		ClipboardOwner owner = new java.awt.datatransfer.ClipboardOwner() {
			public void lostOwnership(Clipboard clipboard, Transferable contents) {
			}
		};
		
		class ImageTransferable implements Transferable {
			public java.awt.Image image;

			public ImageTransferable(java.awt.Image image) {
				this.image = image;
			}
			
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				if (flavor == DataFlavor.imageFlavor)
					return image;
				else
					throw new UnsupportedFlavorException(flavor);
			}

			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[] {DataFlavor.imageFlavor};
			}

			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return flavor == DataFlavor.imageFlavor;
			}
		};
		
		cp.setContents(new ImageTransferable(ImageConverter.convertToAWT(getImage())), owner);
	}
	
	/**
	 * Returns the image of the chart.
	 */
	public Image getImage() {
		Image image = new Image(getDisplay(), getClientArea().width, getClientArea().height);
		GC gc = new GC(image);
		paint(gc);
		gc.dispose();
		return image;
	}
	
	protected static class PlotArea {
		public double minX;
		public double maxX;
		public double minY;
		public double maxY;

		public PlotArea(double minX, double maxX, double minY, double maxY) {
			this.minX = minX;
			this.maxX = maxX;
			this.minY = minY;
			this.maxY = maxY;
		}
		
	}

}
