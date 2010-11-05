package itu.spvc;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.mt4j.MTApplication;
import org.mt4j.components.visibleComponents.font.FontManager;
import org.mt4j.components.visibleComponents.shapes.MTRectangle;
import org.mt4j.components.visibleComponents.shapes.MTRoundRectangle;
import org.mt4j.components.visibleComponents.shapes.MTRectangle.PositionAnchor;
import org.mt4j.components.visibleComponents.widgets.MTImage;
import org.mt4j.components.visibleComponents.widgets.MTTextArea;
import org.mt4j.input.gestureAction.DefaultDragAction;
import org.mt4j.input.gestureAction.DefaultRotateAction;
import org.mt4j.input.gestureAction.DefaultScaleAction;
import org.mt4j.input.inputProcessors.IGestureEventListener;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.DragProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.rotateProcessor.RotateProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.scaleProcessor.ScaleProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.tapAndHoldProcessor.TapAndHoldProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.unistrokeProcessor.UnistrokeEvent;
import org.mt4j.input.inputProcessors.componentProcessors.unistrokeProcessor.UnistrokeProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.unistrokeProcessor.UnistrokeUtils.Direction;
import org.mt4j.input.inputProcessors.componentProcessors.unistrokeProcessor.UnistrokeUtils.UnistrokeGesture;
import org.mt4j.input.inputProcessors.globalProcessors.CursorTracer;
import org.mt4j.sceneManagement.AbstractScene;
import org.mt4j.sceneManagement.AddNodeActionThreadSafe;
import org.mt4j.util.MTColor;
import org.mt4j.util.math.Vector3D;

import processing.core.PImage;

public class DragDropScene extends AbstractScene {
	// The multitouch application
	private MTApplication mtApp;

	// Foldername where images are located
	private String imageFolder = "images/";

	// Multitouch rectangle representing the drag and drop zone
	private MTRoundRectangle mtrr;

	// For dispatching unistroke events
	private UnistrokeProcessor usp;

	// List of displayed images
	private ArrayList<MTImage> images;

	// Store the last image added
	private MTImage lastImageAdded;

	MTRectangle dragDropZone;
	Server server;

	/**
	 * Constructor
	 * 
	 * @param mtApplication
	 *            , mt application
	 * @param name
	 *            of application
	 */
	public DragDropScene(MTApplication mtApplication, String name) {
		super(mtApplication, name);
		this.mtApp = mtApplication;
		images = new ArrayList<MTImage>();

		// Set scene background color
		this.setClearColor(new MTColor(146, 150, 188, 255));

		// Add mouse input
		this.registerGlobalInputProcessor(new CursorTracer(mtApp, this));

		// Start new server
		server = new Server(this);
	}

	/**
	 * Draw a scene with images, drag and drop area and relevant processors and
	 * listeners
	 */
	private void initImages() {
		// Remove previous components
		this.getCanvas().removeAllChildren();

		// Add images
		// Image image1 = Toolkit.getDefaultToolkit().createImage(
		// imageFolder + "Mountain_Pass.jpg");
		// Image image2 = Toolkit.getDefaultToolkit().createImage(
		// imageFolder + "sea-turtle.jpg");
		// addImage(image1);
		// addImage(image2);

		// Add drag-drop area
		dragDropZone = new MTRectangle(0, 0, 420, 768, mtApp);
		dragDropZone.setFillColor(new MTColor(255, 255, 255, 255));
		dragDropZone.setNoFill(true);
		dragDropZone.setPickable(false);
		this.getCanvas().addChild(dragDropZone);

		// Add text to the drag-drop area
		MTTextArea text = new MTTextArea(mtApp);
		text.setNoFill(true);
		text.setNoStroke(true);
		text.setPickable(false);
		text.setText("Drag and Drop");
		text.setPositionRelativeToOther(dragDropZone, dragDropZone
				.getCenterPointGlobal());
		dragDropZone.addChild(text);

	}

	/**
	 * init the gesture processors
	 */
	public void initGestureProcessors(ArrayList<Vector3D> points) {

		// Define unistroke processor
		usp = new UnistrokeProcessor(mtApp);

		// Add templates to process by the unistroke processor
		usp.addTemplate(UnistrokeGesture.V, Direction.CLOCKWISE);

		// If we have defined a gesture we add it
		if (points != null && points.size() > 0) {
			// # update MT4J code before going further
			usp.getUnistrokeUtils().getRecognizer()
					.addTemplate(UnistrokeGesture.CUSTOMGESTURE, points,
							Direction.CLOCKWISE);
		}
	}

	/**
	 * Register a unistroke processor and listen for gesture events. The
	 * templates to match gestures against are the ones we specified the
	 * 'initImages()'method
	 * 
	 * @param mti
	 *            , the image in the drag and drop zone
	 */
	private void addUnistrokeProcessor(final MTImage mti) {
		mti.registerInputProcessor(usp);
		mti.addGestureListener(UnistrokeProcessor.class,
				new IGestureEventListener() {
					@Override
					/**
					 * If the gesture is being recognized and a finger is pressed in the send the image
					 */
					public boolean processGestureEvent(MTGestureEvent ge) {
						UnistrokeEvent ust = (UnistrokeEvent) ge;
						if (ust.hasTarget()
								&& ust.getId() == MTGestureEvent.GESTURE_ENDED
								&& !ust.getGesture().equals(
										UnistrokeGesture.NOGESTURE)) {
							// TODO: Send the image using the server
							System.out.println("Sending image...");
							Image img = mti.getImage().getTexture().getImage();
							int h = img.getHeight(mtApp);
							int w = img.getWidth(mtApp);
							BufferedImage bi = new BufferedImage(w, h,
									BufferedImage.TYPE_INT_RGB);
							Graphics2D g = bi.createGraphics();
							g.drawImage(img, 0, 0, mtApp);
							g.dispose();
							ByteArrayOutputStream bas = new ByteArrayOutputStream();
							try {
								ImageIO.write(bi, "jpg", bas);
								byte[] bytes = bas.toByteArray();
								synchronized (server.out) {
									server.sendMessage("1");
									server.out.writeInt(bytes.length);
									System.out.println(bytes.length);
									server.out.write(bytes);
									System.out.println(bytes);
									server.out.flush();
								}
								mti.removeFromParent();
								images.add(mti);
								setFingerUp();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						return false;
					}
				});
	}

	private void removeUnistrokeProcessor(MTImage mti) {
		mti.removeAllGestureEventListeners(UnistrokeProcessor.class);
		mti.removeInputListener(usp);
	}

	/**
	 * Called when a touch is registered in the drag and drop area. A unistroke
	 * processor is added to and move/scale and resize processors are removed
	 * from all images in the area so unistroke gestures can be detected without
	 * the images moving.
	 */
	private void setFingerDown() {
		for (MTImage mti : images) {
			mti.removeAllGestureEventListeners(DragProcessor.class);
			mti.removeAllGestureEventListeners(RotateProcessor.class);
			mti.removeAllGestureEventListeners(ScaleProcessor.class);
			addUnistrokeProcessor(mti);
		}
	}

	/**
	 * When no finger is down in the drag and drop zone we remove the unistroke
	 * processor and add default processors to images again
	 */
	private void setFingerUp() {
		for (MTImage mti : images) {
			removeUnistrokeProcessor(mti);
			mti.removeAllGestureEventListeners(TapAndHoldProcessor.class);
			addImageDragGestureListener(mti);
		}
	}

	public void addImage(Image awtImage) {
		PImage img = loadImageMT(awtImage);
		MTImage mti1 = new MTImage(img, mtApp);
		// TODO: should the following line be commented out
		// mti1.setFillColor(randomMTColor());

		// Edit image
		mti1.scale(0.5f, 0.5f, 1, new Vector3D(0, 0, 0));
		mti1.setAnchor(PositionAnchor.UPPER_LEFT);
		Random rand = new Random();
		mti1.setPositionGlobal(new Vector3D((500 + rand.nextInt(100)),
				(100 + rand.nextInt(100))));
		mti1.rotateZ(new Vector3D(0, 0), rand.nextInt(20));

		// sets the last image
		lastImageAdded = mti1;
		images.add(mti1);

		// Add gesture
		addImageDragGestureListener(mti1);

		// Add the downloaded image
		this.registerPreDrawAction(new AddNodeActionThreadSafe(mti1, this
				.getCanvas()));
	}

	private void addImageDragGestureListener(final MTImage mti) {
		mti.addGestureListener(DragProcessor.class,
				new IGestureEventListener() {
					@Override
					public boolean processGestureEvent(MTGestureEvent ge) {
						if (ge.getId() == MTGestureEvent.GESTURE_ENDED) {
							if (dragDropZone.containsPointGlobal(mti
									.getCenterPointGlobal())) {
								addImageHoldGestureListener(mti);
								addUnistrokeProcessor(mti);
							}
							return true;
						} else {
							removeUnistrokeProcessor(mti);
							return false;
						}
					}
				});
	}

	private void addImageHoldGestureListener(final MTImage mti) {
		TapAndHoldProcessor tahp = new TapAndHoldProcessor(mtApp);
		tahp.setMaxFingerUpDist(1000);
		mti.registerInputProcessor(tahp);
		mti.addGestureListener(TapAndHoldProcessor.class,
				new IGestureEventListener() {
					@Override
					public boolean processGestureEvent(MTGestureEvent ge) {
						if (MTGestureEvent.GESTURE_DETECTED == ge.getId()) {
							setFingerDown();
							return true;
						} else if (MTGestureEvent.GESTURE_ENDED == ge.getId()) {
							setFingerUp();
							return true;
						} else {
							return false;
						}
					}
				});
	}

	public void moveImage(float x, float y) {
		// the the w/h of the window
		int w = mtApp.getWidth();
		int h = mtApp.getHeight();

		// tx and ty identifies where the image should be translated
		float tx, ty;

		tx = x * w;
		ty = y * h;
		System.out.println("translate " + lastImageAdded + " to " + tx + ", "
				+ ty);

		// get the center of the image
		Vector3D imgPos = lastImageAdded.getCenterPointGlobal();

		// translate the new coords in the image space
		tx -= imgPos.x;
		ty -= imgPos.y;

		// get the length of the vector, i.e. how much the image should move

		System.out.println("translate " + lastImageAdded + " to local coords: "
				+ tx + ", " + ty);

		lastImageAdded.translate(new Vector3D(tx, ty));
		mtApp.repaint();
	}

	/**
	 * Transform a basic java image into a Processing image uses the
	 * MediaTracker to load it.
	 * 
	 * @param awtImage
	 *            basic java image
	 * @return the same image as a Processing image object
	 */
	private PImage loadImageMT(Image awtImage) {
		MediaTracker tracker = new MediaTracker(mtApp);
		tracker.addImage(awtImage, 0);
		try {
			tracker.waitForAll();
		} catch (InterruptedException e) {
			// e.printStackTrace(); // non-fatal, right?
		}

		PImage image = new PImage(awtImage);
		return image;
	}

	/**
	 * Start drawing the scene
	 */
	@Override
	public void init() {
		initImages();
	};

	@Override
	public void shutDown() {
	}

}
