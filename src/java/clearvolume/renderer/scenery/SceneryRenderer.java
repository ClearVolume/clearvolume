package clearvolume.renderer.scenery;

import cleargl.*;
import clearvolume.renderer.AdaptiveLODController;
import clearvolume.renderer.ClearVolumeRendererBase;
import clearvolume.renderer.cleargl.overlay.Overlay;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.opengl.GLAutoDrawable;
import coremem.ContiguousMemoryInterface;
import coremem.buffers.ContiguousBuffer;
import coremem.offheap.OffHeapMemory;
import coremem.types.NativeTypeEnum;
import scenery.*;
import scenery.controls.ClearGLInputHandler;
import scenery.rendermodules.opengl.DeferredLightingRenderer;

import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * <Description>
 *
 * @author Ulrik Günther <hello@ulrik.is>
 */
public class SceneryRenderer extends ClearVolumeRendererBase implements ClearGLEventListener {
  protected ClearGLWindow clearGLWindow;
  protected ClearGLInputHandler inputHandler;

  protected DeferredLightingRenderer dlr;
  protected Scene scene;
  protected Camera cam;

  protected long frameCount;

  protected String windowName;

  public AdaptiveLODController getAdaptiveLODController() {
    return alc;
  }

  public void setAlc(AdaptiveLODController alc) {
    this.alc = alc;
  }

  protected AdaptiveLODController alc = new AdaptiveLODController();


  public SceneryRenderer(final String pWindowName,
                              final Integer pWindowWidth,
                              final Integer pWindowHeight,
                              final NativeTypeEnum pNativeTypeEnum,
                              final Integer pMaxTextureWidth,
                              final Integer pMaxTextureHeight,
                              final Integer pNumberOfRenderLayers,
                              final Boolean useInCanvas) {
    super(pNumberOfRenderLayers);

    clearGLWindow = new ClearGLWindow(pWindowName,
            pWindowWidth,
            pWindowHeight,
            this);

    windowName = pWindowName;

    // this is where init() is called
    clearGLWindow.setVisible(true);
    clearGLWindow.setFPS(60);

    inputHandler = new ClearGLInputHandler(scene, dlr, clearGLWindow);
    inputHandler.useDefaultBindings(System.getProperty("user.home") + "/.ClearVolume.bindings");

    clearGLWindow.start();
  }

  @Override
  public void addOverlay(Overlay pOverlay) {

  }

  @Override
  public void disableClose() {

  }

  @Override
  public NewtCanvasAWT getNewtCanvasAWT() {
    return clearGLWindow.getNewtCanvasAWT();
  }

  @Override
  public Collection<Overlay> getOverlays() {
    return null;
  }

  @Override
  public int getWindowHeight() {
    return clearGLWindow.getHeight();
  }

  @Override
  public String getWindowName() {
    return null;
  }

  @Override
  public int getWindowWidth() {
    return clearGLWindow.getWidth();
  }

  @Override
  public boolean isFullScreen() {
    return clearGLWindow.isFullscreen();
  }

  @Override
  public boolean isShowing() {
    return clearGLWindow.isVisible();
  }

  @Override
  public void setVisible(boolean pVisible) {

  }

  @Override
  public void toggleBoxDisplay() {

  }

  @Override
  public void toggleFullScreen() {

  }

  @Override
  public void toggleRecording() {

  }

  @Override
  public void requestDisplay() {

  }

  @Override
  public void setClearGLWindow(ClearGLWindow clearGLWindow) {
    this.clearGLWindow = clearGLWindow;
  }

  @Override
  public ClearGLDisplayable getClearGLWindow() {
    return clearGLWindow;
  }

  @Override
  public void init(GLAutoDrawable drawable) {
    scene = new Scene();
    dlr = new DeferredLightingRenderer(drawable.getGL().getGL4(),
            clearGLWindow.getWidth(),
            clearGLWindow.getHeight());

    cam = new DetachedHeadCamera();
    cam.setPosition(new GLVector(0.0f, 0.0f, 0.0f));

    GLMatrix projection = new GLMatrix();
    projection.setPerspectiveProjectionMatrix(
            50.0f / 180.0f * (float)Math.PI,
            drawable.getSurfaceWidth()/drawable.getSurfaceHeight(),
            0.1f, 10000.0f
    );
    GLMatrix view = new GLMatrix();
    view.setCamera(cam.getPosition(), cam.getPosition().plus(cam.getForward()), cam.getUp());

    cam.setView(view);
    cam.setProjection(projection);
    cam.setActive(true);

    Box hullbox = new Box(new GLVector(50.0f, 50.0f, 50.0f));
    Material hullboxMaterial = new Material();
    hullboxMaterial.setAmbient(new GLVector(0.16f, 0.16f, 0.18f));
    hullboxMaterial.setDiffuse(new GLVector(0.16f, 0.16f, 0.18f));
    hullboxMaterial.setSpecular(new GLVector(0.1f, 0.1f, 0.0f));
    hullboxMaterial.setDoubleSided(true);
    hullbox.setMaterial(hullboxMaterial);

    dlr.getSettings().set("hdr.Exposure", 5.0f);

    PointLight pl = new PointLight();
    pl.setPosition(new GLVector(10.0f, 10.0f, -10.0f));
    pl.setIntensity(1500.0f);
    pl.setEmissionColor(GLVector.getOneVector(3));
    pl.setLinear(1f);
    pl.setQuadratic(0f);
    pl.showLightBox();

    scene.addChild(hullbox);
    scene.addChild(pl);
    scene.addChild(cam);

    VolumeNode vn = new VolumeNode();
    vn.setName("DefaultVolumeNode");

    scene.addChild(vn);
  }

  public Scene getScene() {
    return this.scene;
  }

  @Override
  public void dispose(GLAutoDrawable drawable) {
    clearGLWindow.stop();
  }

  @Override
  public void display(GLAutoDrawable drawable) {
    dlr.render(scene);

    /*if(dlr.getWantsFullscreen() == false && dlr.isFullscreen() == false) {
      clearGLWindow.setFullscreen(true);
      dlr.setWantsFullscreen(true);
    }

    if(dlr.getWantsFullscreen() == false && dlr.isFullscreen() == false) {
      dlr.setWantsFullscreen(false);
    }*/

    //clearGLWindow.setWindowTitle("ClearVolume, scenery renderer - " + windowName + " - " + String.format("$.1f fps", drawable.getAnimator().getLastFPS()));

    frameCount++;
  }

  @Override
  public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
                                       ByteBuffer pByteBuffer,
                                       long pVolumeSizeX,
                                       long pVolumeSizeY,
                                       long pVolumeSizeZ) {
    final VolumeNode vn = (VolumeNode)this.scene.find("DefaultVolumeNode");
    final ContiguousMemoryInterface cmi = OffHeapMemory.allocateBytes(pVolumeSizeX*pVolumeSizeY*pVolumeSizeZ);
    final ContiguousBuffer cb = new ContiguousBuffer(cmi);

    for(int i = 0; i < pVolumeSizeX*pVolumeSizeY*pVolumeSizeZ; i++) {
      cb.writeByte(pByteBuffer.get());
    }

    vn.getVolumeData().add(cmi);

    return true;
  }

  @Override
  public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
                                       ContiguousMemoryInterface pBuffer,
                                       long pVolumeSizeX,
                                       long pVolumeSizeY,
                                       long pVolumeSizeZ) {

    final VolumeNode vn = (VolumeNode)this.scene.find("DefaultVolumeNode");
    vn.getVolumeData().add(pBuffer);

    return true;
  }

  @Override
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    dlr.reshape(width, height);
  }
}
