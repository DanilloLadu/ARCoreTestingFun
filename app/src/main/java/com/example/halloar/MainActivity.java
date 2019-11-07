package com.example.halloar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Objects;


public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener {

    private ArFragment arFragment;

    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String TAG = MainActivity.class.getSimpleName();

    private float[] _lengthWidthHeight = new float[3];

    private DimentionState DIMENTION_STATE = DimentionState.NONE;

    private Boolean FIRST_NODE_EXISTS = false;
    private Boolean SECOND_NODE_EXISTS = false;
    private AnchorNode _firstCurrentAnchorNode;
    private AnchorNode _secondCurrentAnchorNode;
    private TextView tvDistance;
    private TextView tvLength;
    private TextView tvWidth;
    private TextView tvHeight;
    ModelRenderable cubeRenderable;
    private Anchor _firstCurrentAnchor = null;
    private Anchor _secondCurrentAnchor = null;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        }

        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        tvDistance = findViewById(R.id.tvDistance);
        tvHeight = findViewById(R.id.tvHeight);
        tvWidth = findViewById(R.id.tvWidth);
        tvLength = findViewById(R.id.tvLength);
        initModel();

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (cubeRenderable == null)
                return;

            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            if(!(FIRST_NODE_EXISTS && SECOND_NODE_EXISTS)) {

                if (!FIRST_NODE_EXISTS && !SECOND_NODE_EXISTS) {
                    _firstCurrentAnchor = anchor;
                    _firstCurrentAnchorNode = anchorNode;
                    FIRST_NODE_EXISTS = true;

                } else if (FIRST_NODE_EXISTS && !SECOND_NODE_EXISTS) {
                    _secondCurrentAnchor = anchor;
                    _secondCurrentAnchorNode = anchorNode;
                    lineBetweenPoints(_firstCurrentAnchor, _secondCurrentAnchor);
                    SECOND_NODE_EXISTS = true;
                }

                TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
                node.getTranslationController().setEnabled(false); //makes sure you can't move the anchors by dragging
                node.getScaleController().setEnabled(false);//makes sure you can't scale the anchors by dragging
                node.setRenderable(cubeRenderable);
                node.setParent(anchorNode);
                arFragment.getArSceneView().getScene().addOnUpdateListener(this);
                arFragment.getArSceneView().getScene().addChild(anchorNode);
                node.select();

            }else{
                clearAnchors();
            }
        });
    }

    public boolean checkIsSupportedDeviceOrFinish(final Activity activity) {

        String openGlVersionString =
                ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE)))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    private void initModel() {
        MaterialFactory.makeTransparentWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            Vector3 vector3 = new Vector3(0.05f, 0.01f, 0.01f);
                            cubeRenderable = ShapeFactory.makeCylinder(0.01f,0.04f, Vector3.zero(), material);
                            cubeRenderable.setShadowCaster(false);
                            cubeRenderable.setShadowReceiver(false);
                        });
    }

    private void clearAnchors() {

        arFragment.getArSceneView().getScene().removeChild(_firstCurrentAnchorNode);
        _firstCurrentAnchorNode.getAnchor().detach();
        _firstCurrentAnchorNode.setParent(null);
        _firstCurrentAnchorNode = null;
        _firstCurrentAnchor = null;

        arFragment.getArSceneView().getScene().removeChild(_secondCurrentAnchorNode);
        _secondCurrentAnchorNode.getAnchor().detach();
        _secondCurrentAnchorNode.setParent(null);
        _secondCurrentAnchorNode = null;
        _secondCurrentAnchor = null;

        FIRST_NODE_EXISTS = false;
        SECOND_NODE_EXISTS = false;


    }

    public void lineBetweenPoints(Anchor _firstCurrentAnchor, Anchor _secondCurrentAnchor) {
        AnchorNode _firstCurrentAnchorNode = new AnchorNode(_firstCurrentAnchor);
        AnchorNode _secondCurrentAnchorNode = new AnchorNode(_secondCurrentAnchor);
        if (_secondCurrentAnchorNode != null) {
            _firstCurrentAnchorNode.setParent(arFragment.getArSceneView().getScene());
            Vector3 point1, point2;
            point1 = _secondCurrentAnchorNode.getWorldPosition();
            point2 = _firstCurrentAnchorNode.getWorldPosition(); //eventueel niet normalized maken
    /*
        First, find the vector extending between the two points and define a look rotation
        in terms of this Vector.
    */
            final Vector3 difference = Vector3.subtract(point1, point2);
            final Vector3 directionFromTopToBottom = difference.normalized();
            final Quaternion rotationFromAToB =
                    Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
            MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(0, 255, 244))
                    .thenAccept(
                            material -> {
                            /* Then, create a rectangular prism, using ShapeFactory.makeCube() and use the difference vector
                                   to extend to the necessary length.  */
                                ModelRenderable model = ShapeFactory.makeCube(
                                        new Vector3(.01f, .01f, difference.length()),
                                        Vector3.zero(), material);
                            /* Last, set the world rotation of the node to the rotation calculated earlier and set the world position to
                                   the midpoint between the given points . */
                                Node node = new Node();
                                node.setParent(_firstCurrentAnchorNode);
                                node.setRenderable(model);
                                node.setWorldPosition(Vector3.add(point1, point2).scaled(.5f));
                                node.setWorldRotation(rotationFromAToB);
                            }
                    );
            _secondCurrentAnchorNode = _firstCurrentAnchorNode;
        }
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        Log.d("API123", "onUpdateframe... current anchor node " + (_firstCurrentAnchorNode == null));

        if(FIRST_NODE_EXISTS && !SECOND_NODE_EXISTS){
            setInformationText("Select second point");
        }else if(FIRST_NODE_EXISTS && SECOND_NODE_EXISTS){

            Pose firstPose = _firstCurrentAnchor.getPose();
            Pose secondPose = _secondCurrentAnchor.getPose();

            float dx = firstPose.tx() - secondPose.tx();
            float dy = firstPose.ty() - secondPose.ty();
            float dz = firstPose.tz() - secondPose.tz();

            ///Compute the straight-line distance.
            float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            setInformationText("Difference: " + distanceMeters + " metres");

            if(DIMENTION_STATE == DimentionState.LENGTH ){
                _lengthWidthHeight[0] =  distanceMeters;
                tvLength.setText("L: " +String.valueOf(_lengthWidthHeight[0]));
                DIMENTION_STATE = DimentionState.NONE;
                tvLength.setBackgroundColor(android.graphics.Color.BLUE);

            }else if(DIMENTION_STATE == DimentionState.WIDTH  ){
                _lengthWidthHeight[1] =  distanceMeters;
                tvWidth.setText("B: " +String.valueOf(_lengthWidthHeight[1]));
                DIMENTION_STATE = DimentionState.NONE;
                tvWidth.setBackgroundColor(android.graphics.Color.BLUE);

            }else if(DIMENTION_STATE == DimentionState.HEIGHT ) {
                _lengthWidthHeight[2] = distanceMeters;
                tvHeight.setText("H: " + String.valueOf(_lengthWidthHeight[2]));
                DIMENTION_STATE = DimentionState.NONE;
                tvHeight.setBackgroundColor(android.graphics.Color.BLUE);

            }


        }else{
            setInformationText("Select a point");
        }




    }


    public void tappedToSetDimentionStateToLength(View v){
        DIMENTION_STATE = DimentionState.LENGTH;
        tvLength.setBackgroundColor(android.graphics.Color.BLUE);
    }

    public void tappedToSetDimentionStateToWidth(View v){
        DIMENTION_STATE = DimentionState.WIDTH;
        tvWidth.setBackgroundColor(android.graphics.Color.BLUE);

    }

    public void tappedToSetDimentionStateToHeight(View v){
        DIMENTION_STATE = DimentionState.HEIGHT;
        tvHeight.setBackgroundColor(android.graphics.Color.BLUE);

    }

    public void setInformationText(String text){
        tvDistance.setText(text);
    }

}