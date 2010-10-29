package itu.spvc;

import java.util.ArrayList;

import org.mt4j.components.visibleComponents.shapes.MTEllipse;
import org.mt4j.components.visibleComponents.shapes.MTRoundRectangle;
import org.mt4j.components.visibleComponents.widgets.MTTextArea;
import org.mt4j.input.IMTInputEventListener;
import org.mt4j.input.inputData.MTFingerInputEvt;
import org.mt4j.input.inputData.MTInputEvent;
import org.mt4j.input.inputProcessors.IGestureEventListener;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.tapProcessor.TapProcessor;
import org.mt4j.input.inputProcessors.globalProcessors.RawFingerProcessor;
import org.mt4j.sceneManagement.AbstractScene;
import org.mt4j.util.MTColor;
import org.mt4j.util.math.Vector3D;

public class CreateTemplateScene extends AbstractScene {
	MTDragDropApplication mtApp=null;

	//Save if the start/stop button is clicked
	private boolean buttonClicked;

	//ArrayList for holding the touch points that make up a custom gesture
	private ArrayList<Vector3D> points;


	public CreateTemplateScene (MTDragDropApplication mtApplication, String name) {
		super(mtApplication, name);
		mtApp = mtApplication;
	}

	/**
	 * Draw a scene to record custom gestures
	 */
	public void initDefineGesture() {
		//Area within to define gestures



    /* OPTIONAL
    Add start/stop button     
    Add text to button
    Add tap functionality to the button; when pressed recording is on, when pressed again off
    
    Add clear button
    Add text to button
    Add tap functionality to the button; the drawing area is cleared
		OPTIONAL */

		//Add done/save button
		 
		//Add text to button
		
		//Add tap functionality to the button; when pressed the scene is cleared and images added
		// Use the following when tapped
		// mtApp.setPoints(points);
		// mtApp.addDragDropScene();
		

	}
	
	/**
	 * Add a raw finger processor to get raw data for user-defined gestures
	 * 
	 * <note>The RawFingerProcessor cannot be added to an MTComponent, so we add it
	 * to the scene and check if touches are within a specified component before saving
	 * the touch coordinates</note>
	 * 
	 * @param rectComp, the component that gestures should be defined within
	 * @param rfp, a raw finger processor
	 */
	private void addRawFingerProcessor(MTRoundRectangle comp, RawFingerProcessor rfp) {
		final MTRoundRectangle rectComp = comp;
		points = new ArrayList<Vector3D>();
		rfp.addProcessorListener(new IMTInputEventListener() {
			@Override
			public boolean processInputEvent(MTInputEvent inEvt) {
			  //TODO save the points 
			  //points.add()

			  //TODO draw the points from the raw finger events
			  
				return false;
			}
		});
		this.registerGlobalInputProcessor(rfp);
	}
	
	/**
	 * Remove raw finger processor
	 * @param comp, the component to remove the processor from
	 * @param rfp, the processor to remove
	 */
	private void removeRawFingerProcessor(MTRoundRectangle comp, RawFingerProcessor rfp) {
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
}
