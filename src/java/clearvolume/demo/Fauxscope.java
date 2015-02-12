package clearvolume.demo;

import clearvolume.renderer.ClearVolumeRendererInterface;
import io.scif.Reader;
import io.scif.SCIFIO;

import javax.swing.*;
import java.io.IOException;


/**
 * Created by ulrik on 12/02/15.
 */
public class Fauxscope {

  protected final ClearVolumeRendererInterface mRenderer;
  protected final SCIFIO scifio;
  protected Reader reader;

  public Fauxscope(boolean randomDrift, ClearVolumeRendererInterface renderer) {
    mRenderer = renderer;
    scifio = new SCIFIO();
  }

  public void use4DStack(String filename) {

  }

  public void use3DStack(String filename) {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    int returnVal = chooser.showOpenDialog(null);
    if(returnVal == JFileChooser.APPROVE_OPTION) {
      System.out.println("You chose to open this file: " +
              chooser.getSelectedFile().getName());
    } else {
      return;
    }

    final String hugeImage = chooser.getSelectedFile().getName();

    // We initialize a reader as we did before
    try {
      reader = scifio.initializer().initializeReader(hugeImage);
    } catch (io.scif.FormatException | IOException e) {
      e.printStackTrace();
    }
  }

  public void sendVolume() {

  }
}
