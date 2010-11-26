package itu.spvc;

import java.util.ArrayList;

import org.mt4j.MTApplication;
import org.mt4j.input.inputSources.MacTrackpadSource;
import org.mt4j.util.math.Vector3D;

public class MTDragDropApplication extends MTApplication {

	private static final long serialVersionUID = 1L;
	private CreateTemplateScene templateCreatorScene = null;

	// ArrayList for holding the touch points that make up a custom gesture
	private ArrayList<Vector3D> points = null;

	public static void main(String[] args) {
		initialize();
	}

	@Override
	public void startUp() {
		// uncomment the following line if you have multitouch apple trackpad
		// getInputManager().registerInputSource(new MacTrackpadSource(this));

		// Start with one scene or the other not the two of them at the same
		// time

		// Create the template creator scene
		templateCreatorScene = new CreateTemplateScene(this, "Template creator");
		addScene(templateCreatorScene);

		// OR

		// Create the basic Drag and drop scene
		// addDragDropScene();
	}

	public void setPoints(ArrayList<Vector3D> p) {
		points = p;
	}

	public void addDragDropScene() {
		DragDropScene ddScene = new DragDropScene(this,
				"Multitouch drag & drop");
		ddScene.initGestureProcessors(points);

		// If we have two scene, we need to handle the transition
		// from one to the next
		if (templateCreatorScene != null) {
			// we store the old scene, because
			// we can't destroy it until another one is active
			this.pushScene();
			templateCreatorScene.shutDown();
		}

		// We add the new Drag and Drop scene to the application
		addScene(ddScene);

		if (templateCreatorScene != null) {
			// Now that we added the old scene
			// we don't need the old one anymore
			this.changeScene(ddScene);
			templateCreatorScene.destroy();
		}
	}
}
