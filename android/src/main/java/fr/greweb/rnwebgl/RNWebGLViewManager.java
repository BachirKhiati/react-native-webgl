package fr.greweb.rnwebgl;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import java.util.Map;
import javax.annotation.Nullable;

public class RNWebGLViewManager extends SimpleViewManager<RNWebGLView> {
  public static final String REACT_CLASS = "RNWebGLView";
  public static final int COMMAND_CAPTURE_CURRENT_VIEW = 3;

  @Nullable
  @Override
  public Map<String, Integer> getCommandsMap() {
    // You need to implement this method and return a map with the readable
    // name and constant for each of your commands. The name you specify
    // here is what you'll later use to access it in react-native.
    return MapBuilder.of(
            "capture",
            COMMAND_CAPTURE_CURRENT_VIEW
    );
  }



  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public RNWebGLView createViewInstance(ThemedReactContext context) {
    return new RNWebGLView(context);
  }

  @Override
  public @Nullable Map getExportedCustomDirectEventTypeConstants() {
    return MapBuilder.of(
            "surfaceCreate",
            MapBuilder.of("registrationName", "onSurfaceCreate"),
            "dataReturned",
            MapBuilder.of("registrationName", "onDataReturned")
    );
  }


  @Override
  public void receiveCommand(final RNWebGLView root, int commandId, @Nullable ReadableArray args) {
    // This will be called whenever a command is sent from react-native.
    switch (commandId) {
      case COMMAND_CAPTURE_CURRENT_VIEW:
        root.snapShot = true;
        root.widthImage = args.getInt(1);
        root.heightImage = args.getInt(0);
        root.requestRender();
        break;
    }
  }
}
