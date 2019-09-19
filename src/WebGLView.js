//@flow
import React from "react";
import PropTypes from "prop-types";
import {
  Platform,
  View,
  ViewPropTypes,
  requireNativeComponent,
  UIManager,
  findNodeHandle,NativeModules
} from "react-native";
import RNExtension from "./RNExtension";
import wrapGLMethods from "./wrapGLMethods";
import Reactotron from 'reactotron-react-native'


// Get the GL interface from an RNWebGLContextID and do JS-side setup
const getGl = (ctxId: number): ?WebGLRenderingContext => {
  if (!global.__RNWebGLContexts) {
    console.warn(
      "RNWebGL: Can only run on JavaScriptCore! Do you have 'Remote Debugging' enabled in your app's Developer Menu (https://facebook.github.io/react-native/docs/debugging.html)? RNWebGL is not supported while using Remote Debugging, you will need to disable it to use RNWebGL."
    );
    return null;
  }
  const gl = global.__RNWebGLContexts[ctxId];
  gl.__ctxId = ctxId;
  delete global.__RNWebGLContexts[ctxId];
  if (Object.setPrototypeOf) {
    Object.setPrototypeOf(gl, global.WebGLRenderingContext.prototype);
  } else {
    gl.__proto__ = global.WebGLRenderingContext.prototype;
  }
  wrapGLMethods(gl, RNExtension.createWithContext(gl, ctxId));

  gl.canvas = null;

  const viewport = gl.getParameter(gl.VIEWPORT);
  gl.drawingBufferWidth = viewport[2];
  gl.drawingBufferHeight = viewport[3];

  return gl;
};

export default class WebGLView extends React.Component {
  props: {
    onContextCreate: (gl: WebGLRenderingContext) => void,
    onContextFailure: (e: Error) => void,
    msaaSamples: number
  };
  static propTypes = {
    onContextCreate: PropTypes.func,
    onContextFailure: PropTypes.func,
    msaaSamples: PropTypes.number,
    ...ViewPropTypes
  };

  componentDidMount() {
    this.props.onRef && this.props.onRef(this)
    this.requestMap = null
  }

  static defaultProps = {
    msaaSamples: 1
  };

  takeSnapshot( media ) {    
    const { height, width } = media
    let node = findNodeHandle( this.native );
    let command = UIManager.getViewManagerConfig('RNWebGLView').Commands.capture
    const promise = new Promise( ( resolve, reject ) => {
      this.requestMap = { resolve, reject };
          UIManager.dispatchViewManagerCommand(
      node,
      command,
      [ height, width ]
    );
    } ).catch( error => { Reactotron.log( 'caught', error ); } );
    return promise;
}
  
  onDataReturned = ({
    nativeEvent: { url }
  }: {
    nativeEvent: { url: string }
  }) => {
    Reactotron.log("onDataReturned event")
    Reactotron.log(url)
    // Reactotron.log(event.nativeEvent.url)
    // Reactotron.log("this.ctx")
    Reactotron.log(this.requestMap)
    //   // We grab the relevant data out of our event.
    // let url = event.url
    // Reactotron.log("url")
    // Reactotron.log(url)
    const {resolve, reject } = this.requestMap
    if ( url ) {
      Reactotron.log("current yes")
      Reactotron.log(url)
        resolve(url)
    } else {
      Reactotron.log("error error")
      Reactotron.log(url)
        reject("error")
      }
      this.requestMap = null
    }

  render() {
    const {
      onContextCreate, // eslint-disable-line no-unused-vars
      onContextFailure, // eslint-disable-line no-unused-vars
      msaaSamples,
      ...viewProps
    } = this.props;
    return (
        <WebGLView.NativeView
          style={ { flex: 1 } }
          ref={ref => this.native = ref}
           onSurfaceCreate={this.onSurfaceCreate}
           onDataReturned={this.onDataReturned}
           msaaSamples={Platform.OS === "ios" ? msaaSamples : undefined}
        />
    );
  }

  onSurfaceCreate = ({
    nativeEvent: { ctxId }
  }: {
    nativeEvent: { ctxId: number }
  }) => {
    let gl, error;
    try {
      gl = getGl(ctxId);
      if (!gl) {
        error = new Error("RNWebGL context creation failed");
      }
    } catch (e) {
      error = e;
    }
    if (error) {
      if (this.props.onContextFailure) {
        this.props.onContextFailure(error);
      } else {
        throw error;
      }
    } else if ( gl && this.props.onContextCreate ) {
      this.props.onContextCreate( gl );
    }
  };

  static NativeView = requireNativeComponent("RNWebGLView", WebGLView, {
    nativeOnly: { onSurfaceCreate: true, onDataReturned: true  }
  });
}
