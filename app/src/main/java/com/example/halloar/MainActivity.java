package com.example.halloar;


import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
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

import java.util.Objects;


public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener {

    private ArFragment arFragment;

    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String TAG = MainActivity.class.getSimpleName();

    private float[] _lengthWidthHeight = new float[3];

    private DimensionState DIMENSION_STATE = DimensionState.NONE;

    private Boolean FIRST_NODE_EXISTS = false;
    private Boolean SECOND_NODE_EXISTS = false;
    private AnchorNode _firstCurrentAnchorNode;
    private AnchorNode _secondCurrentAnchorNode;
    private TextView tvDistance;
    private TextView tvLength;
    private TextView tvWidth;
    private TextView tvHeight;
    private ModelRenderable cubeRenderable;





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

            createNode(hitResult);


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
                            cubeRenderable = ShapeFactory.makeCylinder(0.01f,0.04f, Vector3.zero(), material);
                            cubeRenderable.setShadowCaster(false);
                            cubeRenderable.setShadowReceiver(false);
                        });
    }

    private void clearAnchors() {

        arFragment.getArSceneView().getScene().removeChild(_firstCurrentAnchorNode);
        _firstCurrentAnchorNode.getAnchor().detach();
        _firstCurrentAnchorNode = null;


        arFragment.getArSceneView().getScene().removeChild(_secondCurrentAnchorNode);
        _secondCurrentAnchorNode.getAnchor().detach();
        _secondCurrentAnchorNode = null;


        FIRST_NODE_EXISTS = false;
        SECOND_NODE_EXISTS = false;


    }

    public void lineBetweenPoints(AnchorNode _firstCurrentAnchorNode, AnchorNode _secondCurrentAnchorNode) {

    /*
        First, find the vector extending between the two points and define a look rotation
        in terms of this Vector.
    */
            final Vector3 difference = Vector3.subtract(_secondCurrentAnchorNode.getWorldPosition(), _firstCurrentAnchorNode.getWorldPosition());
            final Vector3 directionFromTopToBottom = difference.normalized();
            final Quaternion rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());

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
                                createNodeForLineBetweenPoints(model , rotationFromAToB);

                            }
                    );
    }

    private void createNodeForLineBetweenPoints(ModelRenderable model , Quaternion rotationFromAToB) {
        Node node = new Node();
        node.setParent(_firstCurrentAnchorNode);
        node.setRenderable(model);
        node.setWorldPosition(Vector3.add(_secondCurrentAnchorNode.getWorldPosition(), _firstCurrentAnchorNode.getWorldPosition()).scaled(.5f));
        node.setWorldRotation(rotationFromAToB);
    }


    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        Log.d("API123", "onUpdateframe... current anchor node " + (_firstCurrentAnchorNode == null));

        if(FIRST_NODE_EXISTS && !SECOND_NODE_EXISTS){
            setInformationText("Select second point");
        }else if(FIRST_NODE_EXISTS && SECOND_NODE_EXISTS){

          float distanceMeters = calculateDistanceBetweenTwoAnchorNodes(_firstCurrentAnchorNode , _secondCurrentAnchorNode);
            setInformationText("Difference: " + distanceMeters + " metres");

            setMeasurementInTextView(distanceMeters);

        }else{
            setInformationText("Select a point");
        }
    }


    public void tappedToSetDimensionStateToLength(View v){
        DIMENSION_STATE = DimensionState.LENGTH;
        tvLength.setBackgroundColor(android.graphics.Color.BLUE);
    }

    public void tappedToSetDimensionStateToWidth(View v){
        DIMENSION_STATE = DimensionState.WIDTH;
        tvWidth.setBackgroundColor(android.graphics.Color.BLUE);

    }

    public void tappedToSetDimensionStateToHeight(View v){
        DIMENSION_STATE = DimensionState.HEIGHT;
        tvHeight.setBackgroundColor(android.graphics.Color.BLUE);

    }

    public void setInformationText(String text){
        tvDistance.setText(text);
    }

    public void createNode(HitResult hitResult){
        Anchor anchor = hitResult.createAnchor();
        AnchorNode node = new AnchorNode(anchor);
        node.setParent(arFragment.getArSceneView().getScene());

       if(!(FIRST_NODE_EXISTS && SECOND_NODE_EXISTS)){

           if(!FIRST_NODE_EXISTS){
               setFirstNode(node);
           }else {
               setSecondNode(node);
           }

           node.setRenderable(cubeRenderable);
           arFragment.getArSceneView().getScene().addOnUpdateListener(this);
           arFragment.getArSceneView().getScene().addChild(node);
       }else{
           clearAnchors();
       }
    }


    private void setFirstNode(AnchorNode node) {
        _firstCurrentAnchorNode = node;
        FIRST_NODE_EXISTS = true;
    }
    private void setSecondNode(AnchorNode node) {
        _secondCurrentAnchorNode = node;
        SECOND_NODE_EXISTS = true;
        lineBetweenPoints(_firstCurrentAnchorNode,_secondCurrentAnchorNode);
    }

    private float calculateDistanceBetweenTwoAnchorNodes(AnchorNode firstNode , AnchorNode secondNode){
        float distanceX, distanceY, distanceZ ,distance , roundedDistance;

        Pose firstPose = firstNode.getAnchor().getPose();
        Pose secondPose = secondNode.getAnchor().getPose();

        distanceX = firstPose.tx() - secondPose.tx();
        distanceY = firstPose.ty() - secondPose.ty();
        distanceZ = firstPose.tz() - secondPose.tz();

        distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);
        roundedDistance = (float) ( Math.round(distance * 1000)) / 1000;

        return roundedDistance;

    }

    private void setMeasurementInTextView(float distance){

        if(DIMENSION_STATE == DimensionState.LENGTH){
            _lengthWidthHeight[0] =  distance;
            tvLength.setText("L: " + _lengthWidthHeight[0]);
            DIMENSION_STATE = DimensionState.NONE;
            tvLength.setBackgroundColor(android.graphics.Color.BLUE);
        }else if (DIMENSION_STATE == DimensionState.WIDTH) {
            _lengthWidthHeight[1] =  distance;
            tvWidth.setText("B: " +_lengthWidthHeight[1]);
            DIMENSION_STATE = DimensionState.NONE;
            tvWidth.setBackgroundColor(android.graphics.Color.BLUE);
        }else if (DIMENSION_STATE == DimensionState.HEIGHT){
            _lengthWidthHeight[2] = distance;
            tvHeight.setText("H: " + _lengthWidthHeight[2]);
            DIMENSION_STATE = DimensionState.NONE;
            tvHeight.setBackgroundColor(android.graphics.Color.BLUE);
        }
    }
}
