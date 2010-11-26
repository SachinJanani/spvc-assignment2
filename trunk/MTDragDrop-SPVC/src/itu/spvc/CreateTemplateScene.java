package itu.spvc;

import java.util.ArrayList;

import org.mt4j.components.visibleComponents.font.FontManager;
import org.mt4j.components.visibleComponents.font.IFont;
import org.mt4j.components.visibleComponents.shapes.MTEllipse;
import org.mt4j.components.visibleComponents.shapes.MTRectangle;
import org.mt4j.components.visibleComponents.shapes.MTRoundRectangle;
import org.mt4j.components.visibleComponents.widgets.MTTextArea;
import org.mt4j.input.IMTInputEventListener;
import org.mt4j.input.gestureAction.DefaultButtonClickAction;
import org.mt4j.input.inputData.AbstractCursorInputEvt;
import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputData.MTInputEvent;
import org.mt4j.input.inputProcessors.IGestureEventListener;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.tapProcessor.TapProcessor;
import org.mt4j.input.inputProcessors.globalProcessors.RawFingerProcessor;
import org.mt4j.sceneManagement.AbstractScene;
import org.mt4j.util.MTColor;
import org.mt4j.util.math.Vector3D;

public class CreateTemplateScene extends AbstractScene {

	MTDragDropApplication mtApp = null;

	// Save if the start/stop button is clicked
	private boolean buttonClicked;

	// ArrayList for holding the touch points that make up a custom gesture
	private ArrayList<Vector3D> points;

	//
	private MTRoundRectangle gestureDefinitionZone;
	private MTRoundRectangle saveBtn;
	private RawFingerProcessor rfp;

	public CreateTemplateScene(MTDragDropApplication mtApplication, String name) {
		super(mtApplication, name);
		mtApp = mtApplication;
	}

	/**
	 * Draw a scene to record custom gestures
	 */
	public void initDefineGesture() {
		this.setClearColor(new MTColor(146, 150, 188, 255));
		// Area within to define gestures
		gestureDefinitionZone = new MTRoundRectangle(50, 50, 0, 600, 450, 1, 1,
				1, mtApp);
		gestureDefinitionZone.setFillColor(new MTColor(255, 255, 255, 255));
		gestureDefinitionZone.setNoFill(true);
		gestureDefinitionZone.setPickable(false);
		this.getCanvas().addChild(gestureDefinitionZone);

		/*
		 * OPTIONAL Add start/stop button Add text to button Add tap
		 * functionality to the button; when pressed recording is on, when
		 * pressed again off
		 * 
		 * Add clear button Add text to button Add tap functionality to the
		 * button; the drawing area is cleared OPTIONAL
		 */

		// Add done/save button
		saveBtn = getRoundRectWithText(250, 550, 120, 35, "Save Gesture");
		saveBtn.registerInputProcessor(new TapProcessor(getMTApplication()));
		saveBtn.addGestureListener(TapProcessor.class,
				new DefaultButtonClickAction(saveBtn));
		saveBtn.addGestureListener(TapProcessor.class,
				new IGestureEventListener() {
					public boolean processGestureEvent(MTGestureEvent ge) {
						if (ge.getId() == MTGestureEvent.GESTURE_ENDED) {
							mtApp.setPoints(points);
							mtApp.addDragDropScene();
							return true;
						} else {
							return false;
						}
					}
				});
		this.getCanvas().addChild(saveBtn);

		// Add raw finger processor
		rfp = new RawFingerProcessor();
		this.addRawFingerProcessor(gestureDefinitionZone, rfp);

	}

	/**
	 * Add a raw finger processor to get raw data for user-defined gestures
	 * 
	 * <note>The RawFingerProcessor cannot be added to an MTComponent, so we add
	 * it to the scene and check if touches are within a specified component
	 * before saving the touch coordinates</note>
	 * 
	 * @param rectComp
	 *            , the component that gestures should be defined within
	 * @param rfp
	 *            , a raw finger processor
	 */
	private void addRawFingerProcessor(MTRoundRectangle comp,
			RawFingerProcessor rfp) {
		final MTRoundRectangle rectComp = comp;
		points = new ArrayList<Vector3D>();
		rfp.addProcessorListener(new IMTInputEventListener() {
			@Override
			public boolean processInputEvent(MTInputEvent inEvt) {
				// TODO save the points
				final AbstractCursorInputEvt posEvt = (AbstractCursorInputEvt) inEvt;
				Vector3D pos = new Vector3D(posEvt.getPosX(), posEvt.getPosY(),
						0);
				points.add(pos);

				// TODO draw the points from the raw finger events
				MTEllipse ellipse = new MTEllipse(mtApp, pos, 5, 5);
				rectComp.addChild(ellipse);
				return false;
			}
		});
		this.registerGlobalInputProcessor(rfp);
	}

	/**
	 * Remove raw finger processor
	 * 
	 * @param comp
	 *            , the component to remove the processor from
	 * @param rfp
	 *            , the processor to remove
	 */
	private void removeRawFingerProcessor(MTRoundRectangle comp,
			RawFingerProcessor rfp) {
		this.unregisterGlobalInputProcessor(rfp);
	}

	@Override
	public void init() {
		initDefineGesture();
	}

	@Override
	public void shutDown() {
		// TODO Auto-generated method stub

	}

	private MTRoundRectangle getRoundRectWithText(float x, float y,
			float width, float height, String text) {
		MTRoundRectangle r = new MTRoundRectangle(x, y, 0, width, height, 12,
				12, getMTApplication());
		r.unregisterAllInputProcessors();
		r.setFillColor(MTColor.BLACK);
		r.setStrokeColor(MTColor.BLACK);
		MTTextArea rText = new MTTextArea(getMTApplication());
		rText.unregisterAllInputProcessors();
		rText.setPickable(false);
		rText.setNoFill(true);
		rText.setNoStroke(true);
		rText.setText(text);
		r.addChild(rText);
		rText.setPositionRelativeToParent(r.getCenterPointLocal());
		return r;
	}
}
