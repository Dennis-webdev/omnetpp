/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting;

import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_ANTIALIAS;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_BACKGROUND_COLOR;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_CANVAS_CACHING;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_DISPLAY_LEGEND;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_FOREGROUND_COLOR;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_INSETS_BACKGROUND_COLOR;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_INSETS_LINE_COLOR;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_LEGEND_ANCHOR;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_LEGEND_BORDER;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_LEGEND_FONT;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_LEGEND_POSITION;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_TITLE;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_TITLE_FONT;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_ANTIALIAS;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_BACKGROUND_COLOR;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_CACHING;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_DISPLAY_LEGEND;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_GRAPH_TITLE;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_GRAPH_TITLE_FONT;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_LEGEND_ANCHORING;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_LEGEND_BORDER;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_LEGEND_FONT;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_LEGEND_POSITION;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_Y_AXIS_MAX;
import static org.omnetpp.scave.charting.properties.ChartProperties.PROP_Y_AXIS_MIN;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.common.Debug;
import org.omnetpp.common.canvas.ICoordsMapping;
import org.omnetpp.common.canvas.RectangularArea;
import org.omnetpp.common.canvas.ZoomableCachingCanvas;
import org.omnetpp.common.canvas.ZoomableCanvasMouseSupport;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.common.ui.SizeConstraint;
import org.omnetpp.common.util.Converter;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.scave.ScavePlugin;
import org.omnetpp.scave.actions.ZoomChartAction;
import org.omnetpp.scave.charting.dataset.IDataset;
import org.omnetpp.scave.charting.properties.ChartProperties.LegendAnchor;
import org.omnetpp.scave.charting.properties.ChartProperties.LegendPosition;
import org.omnetpp.scave.editors.ScaveEditorContributor;

/**
 * Base class for all chart widgets.
 *
 * @author tomi, andras
 */
public abstract class ChartCanvas extends ZoomableCachingCanvas {

	private static final boolean debug = false;

	// when displaying confidence intervals, XXX chart parameter?
	protected static final double CONFIDENCE_LEVEL = 0.95;

	protected boolean antialias = DEFAULT_ANTIALIAS;
	protected Color backgroundColor = DEFAULT_BACKGROUND_COLOR;
	protected Title title = new Title(DEFAULT_TITLE, DEFAULT_TITLE_FONT);
	protected String titleText = null;
	protected Legend legend = new Legend(DEFAULT_DISPLAY_LEGEND, DEFAULT_LEGEND_BORDER, DEFAULT_LEGEND_FONT, DEFAULT_LEGEND_POSITION, DEFAULT_LEGEND_ANCHOR);
	protected LegendTooltip legendTooltip;

	private String statusText = "No data available."; // displayed when there's no dataset

	/* bounds specified by the user*/
	protected RectangularArea userDefinedArea =
		new RectangularArea(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
	/* bounds calculated from the dataset */
	protected RectangularArea chartArea;
	/* area to be displayed */
	private RectangularArea zoomedArea;

	private ZoomableCanvasMouseSupport mouseSupport;
	private Color insetsBackgroundColor = DEFAULT_INSETS_BACKGROUND_COLOR;
	private Color insetsLineColor = DEFAULT_INSETS_LINE_COLOR;

	protected IChartSelection selection;
	private ListenerList listeners = new ListenerList();

	private int layoutDepth = 0; // how many layoutChart() calls are on the stack
	private IDataset dataset;

	public ChartCanvas(Composite parent, int style) {
		super(parent, style);
		setCaching(DEFAULT_CANVAS_CACHING);
		setBackground(backgroundColor);
		setToolTipText(null); // XXX prevent "Close" tooltip of the TabItem to come up (Linux only)

		legendTooltip = new LegendTooltip(this);

		mouseSupport = new ZoomableCanvasMouseSupport(this); // add mouse handling; may be made optional

		addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				layoutChart();
			}
		});

		ScaveEditorContributor contributor = ScaveEditorContributor.getDefault();
		if (contributor != null) {
			final IAction zoomOutAction = contributor.getZoomOutAction();
			final IAction zoomToFitAction = contributor.getZoomToFitAction();
			if (zoomOutAction instanceof ZoomChartAction) {
				addPropertyChangeListener(new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (event.getProperty() == ZoomableCachingCanvas.PROP_ZOOM_X ||
								event.getProperty() == ZoomableCachingCanvas.PROP_ZOOM_Y) {
							if (zoomOutAction instanceof ZoomChartAction)
								((ZoomChartAction)zoomOutAction).updateEnabled();
							if (zoomToFitAction instanceof ZoomChartAction)
								((ZoomChartAction)zoomToFitAction).updateEnabled();
						}
					}
				});
			}
		}
	}

	/**
	 * Sets the data to be visualized by the chart.
	 */
	void setDataset(IDataset dataset) {
		if (debug) Debug.println("setDataset()");
		doSetDataset(dataset);
		this.dataset = dataset;
		updateTitle();
		updateZoomedArea();
	}

	abstract void doSetDataset(IDataset dataset);

	abstract String getHoverHtmlText(int x, int y, SizeConstraint outSizeConstraint);

	protected void layoutChart() {
		// prevent nasty infinite layout recursions
		if (layoutDepth>0)
			return;

		// ignore initial invalid layout request
		if (getClientArea().isEmpty())
			return;

		layoutDepth++;
		if (debug) Debug.println("layoutChart(), level "+layoutDepth);
		GC gc = new GC(Display.getCurrent());
		try {
			// preserve zoomed-out state while resizing
			boolean shouldZoomOutX = getZoomX()==0 || isZoomedOutX();
			boolean shouldZoomOutY = getZoomY()==0 || isZoomedOutY();

			for (int pass = 1; pass <= 2; ++pass) {
				Rectangle plotArea = doLayoutChart(gc, pass);
				setViewportRectangle(new org.eclipse.swt.graphics.Rectangle(plotArea.x, plotArea.y, plotArea.width, plotArea.height));

				if (shouldZoomOutX)
					zoomToFitX();
				if (shouldZoomOutY)
					zoomToFitY();
				validateZoom(); //Note: scrollbar.setVisible() triggers Resize too
			}
		}
		catch (Throwable e) {
			ScavePlugin.logError(e);
		}
		finally {
			layoutDepth--;
			gc.dispose();
		}
		// may trigger another layoutChart()
		updateZoomedArea();
	}

	/**
	 * Sets the zoomed area of the chart.
	 * The change will be applied when the chart is layouted next time.
	 */
	public void setZoomedArea(RectangularArea area) {
		if (dataset != null && !getClientArea().isEmpty())
			zoomToArea(area);
		else
			this.zoomedArea = area;
	}

	/**
	 * Applies the zoomed area was set by {@code setZoomedArea}.
	 * To be called after the has an area (calculated from the dataset)
	 * and the virtual size of the canvas is calculated.
	 */
	private void updateZoomedArea() {
		if (zoomedArea != null && !getBounds().isEmpty() && dataset != null) {
			if (debug) Debug.format("Restoring zoomed area: %s%n", zoomedArea);
			RectangularArea area = zoomedArea;
			zoomedArea = null;
			zoomToArea(area);
		}
	}

	/**
	 * Calculate positions of chart elements such as title, legend, axis labels, plot area.
	 * @param pass TODO
	 */
	abstract protected Rectangle doLayoutChart(GC gc, int pass);

	/*-------------------------------------------------------------------------------------------
	 *                                      Drawing
	 *-------------------------------------------------------------------------------------------*/
	private ICoordsMapping coordsMapping;

	@Override
	protected void paintCachableLayer(GC gc) {
		if (debug) {
			Debug.println("paintCachableLayer()");
			Debug.println(String.format("area=%f, %f, %f, %f, zoom: %f, %f", getMinX(), getMaxX(), getMinY(), getMaxY(), getZoomX(), getZoomY()));
			Debug.println(String.format("view port=%s, vxy=%d, %d", getViewportRectangle(), getViewportLeft(), getViewportTop()));
		}
		if (getClientArea().isEmpty())
			return;

		coordsMapping = getOptimizedCoordinateMapper();
		resetDrawingStylesAndColors(gc);
		doPaintCachableLayer(gc, coordsMapping);
	}

	@Override
	protected void paintNoncachableLayer(GC gc) {
		if (debug) Debug.println("paintNoncachableLayer()");
		if (getClientArea().isEmpty())
			return;

		if (coordsMapping == null)
			coordsMapping = getOptimizedCoordinateMapper();
		resetDrawingStylesAndColors(gc);

		doPaintNoncachableLayer(gc, coordsMapping);

		if (coordsMapping.getNumCoordinateOverflows() > 0)
			displayCoordinatesOverflowMessage(gc);

		coordsMapping = null;
	}

	abstract protected void doPaintCachableLayer(GC gc, ICoordsMapping coordsMapping);
	abstract protected void doPaintNoncachableLayer(GC gc, ICoordsMapping coordsMapping);

	private void displayCoordinatesOverflowMessage(GC gc) {
		resetDrawingStylesAndColors(gc);
		gc.drawText("There were coordinate overflows during plotting, and the resulting chart\n"+
				    "may not be accurate. Please decrease zoom level.",
				    getViewportRectangle().x+10, getViewportRectangle().y+10, true);
	}

	/**
	 *
	 * @return
	 */
	protected abstract RectangularArea calculatePlotArea();

	public IChartSelection getSelection() {
		return selection;
	}

	public void setSelection(IChartSelection selection) {
		this.selection = selection;
		chartChanged();
		fireChartSelectionChange(selection);
	}

	public void addChartSelectionListener(IChartSelectionListener listener) {
		listeners.add(listener);
	}

	public void removeChartSelectionListener(IChartSelectionListener listener) {
		listeners.remove(listener);
	}

	protected void fireChartSelectionChange(IChartSelection selection) {
		for (Object listener : listeners.getListeners())
			((IChartSelectionListener)listener).selectionChanged(selection);
	}

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

	/*----------------------------------------------------------------------
	 *                            Properties
	 *----------------------------------------------------------------------*/

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
		else if (PROP_BACKGROUND_COLOR.equals(name))
			setBackgroundColor(ColorFactory.asRGB(value));
		// Axes
		else if (PROP_Y_AXIS_MIN.equals(name))
			setYMin(Converter.stringToDouble(value));
		else if (PROP_Y_AXIS_MAX.equals(name))
			setYMax(Converter.stringToDouble(value));
		else
			ScavePlugin.logError(new RuntimeException("unrecognized chart property: "+name));
	}

	protected String getElementId(String propertyName) {
		int index = propertyName.indexOf('/');
		return index >= 0 ? propertyName.substring(index + 1) : null;
	}

	public boolean getAntialias() {
		return antialias;
	}

	public void setAntialias(Boolean antialias) {
		this.antialias = antialias != null ? antialias : DEFAULT_ANTIALIAS;
		chartChanged();
	}

	public void setCaching(Boolean caching) {
		super.setCaching(caching != null ? caching : DEFAULT_CANVAS_CACHING);
		chartChanged();
	}

	public void setBackgroundColor(RGB rgb) {
		this.backgroundColor = rgb != null ? new Color(null, rgb) : DEFAULT_BACKGROUND_COLOR;
		chartChanged();
	}

	public void setTitle(String value) {
		titleText = value;
		updateTitle();
	}

	private void updateTitle() {
		String newTitle = DEFAULT_TITLE;
		if (dataset != null)
			newTitle = StringUtils.defaultString(dataset.getTitle(titleText), DEFAULT_TITLE);

		if (!ObjectUtils.equals(newTitle, title.getText())) {
			title.setText(newTitle);
			chartChanged();
		}
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

	public void setXMin(Double value) {
		userDefinedArea.minX = value != null ? value : Double.NEGATIVE_INFINITY;
		updateArea();
		chartChanged();
	}

	public void setXMax(Double value) {
		userDefinedArea.maxX = value != null ? value : Double.POSITIVE_INFINITY;
		updateArea();
		chartChanged();
	}

	public void setYMin(Double value) {
		userDefinedArea.minY = value != null ? value : Double.NEGATIVE_INFINITY;
		updateArea();
		chartChanged();
	}

	public void setYMax(Double value) {
		userDefinedArea.maxY = value != null ? value : Double.POSITIVE_INFINITY;
		updateArea();
		chartChanged();
	}

	/**
	 * Sets the area of the zoomable canvas.
	 * This method is called when the area changes because
	 * <ul>
	 * <li> the dataset changed and a new chart area calculated
	 * <li> the user changed the bounds of the are by setting a chart property
	 * </ul>
	 */
	protected void updateArea() {
		if (chartArea == null)
			return;

		RectangularArea area = transformArea(userDefinedArea).intersect(chartArea);

		if (!area.equals(getArea())) {
			if (debug) Debug.format("Update area: %s --> %s%n", getArea(), area);
			setArea(area);
		}
	}

	protected RectangularArea transformArea(RectangularArea area) {
		double minX = transformX(area.minX);
		double minY = transformY(area.minY);
		double maxX = transformX(area.maxX);
		double maxY = transformY(area.maxY);

		return new RectangularArea(
			Double.isNaN(minX) || Double.isInfinite(minX) ? Double.NEGATIVE_INFINITY : minX,
			Double.isNaN(minY) || Double.isInfinite(minY)? Double.NEGATIVE_INFINITY : minY,
			Double.isNaN(maxX) || Double.isInfinite(maxX)? Double.POSITIVE_INFINITY : maxX,
			Double.isNaN(maxY) || Double.isInfinite(maxY) ? Double.POSITIVE_INFINITY : maxY
		);
	}

	protected double transformX(double x) {
		return x;
	}

	protected double transformY(double y) {
		return y;
	}

	protected double inverseTransformX(double x) {
		return x;
	}

	protected double inverseTransformY(double y) {
		return y;
	}


	// make public
	@Override
	public ICoordsMapping getOptimizedCoordinateMapper() {
		return super.getOptimizedCoordinateMapper();
	}

	/**
	 * Resets all GC settings except clipping and transform.
	 */
	public void resetDrawingStylesAndColors(GC gc) {
		gc.setAntialias(antialias ? SWT.ON : SWT.OFF);
		gc.setAlpha(255);
		gc.setBackground(backgroundColor);
		gc.setBackgroundPattern(null);
		//gc.setFillRule();
		gc.setFont(null);
		gc.setForeground(DEFAULT_FOREGROUND_COLOR);
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
		layoutChart();
		clearCanvasCacheAndRedraw();
	}

	protected void paintInsets(GC gc) {
		// draw insets border
		Insets insets = getInsets();
		Rectangle canvasRect = new Rectangle(getClientArea());
		gc.setForeground(insetsBackgroundColor);
		gc.setBackground(insetsBackgroundColor);
		gc.fillRectangle(0, 0, canvasRect.width, insets.top); // top
		gc.fillRectangle(0, canvasRect.bottom()-insets.bottom, canvasRect.width, insets.bottom); // bottom
		gc.fillRectangle(0, 0, insets.left, canvasRect.height); // left
		gc.fillRectangle(canvasRect.right()-insets.right, 0, insets.right, canvasRect.height); // right
		gc.setForeground(insetsLineColor);
		gc.drawRectangle(insets.left, insets.top, getViewportWidth(), getViewportHeight());
	}

	protected void drawStatusText(GC gc) {
		if (getStatusText() != null) {
			resetDrawingStylesAndColors(gc);
			org.eclipse.swt.graphics.Rectangle rect = getViewportRectangle();
			gc.drawText(getStatusText(), rect.x+10, rect.y+10);
		}
	}

	protected void drawRubberband(GC gc) {
		mouseSupport.drawRubberband(gc);
	}

	protected String formatValue(double value, double halfInterval) {
		return !Double.isNaN(halfInterval) && halfInterval > 0.0 ?
				String.format("%.3g\u00b1%.3g", value, halfInterval) :
				String.format("%g", value);
	}
}
