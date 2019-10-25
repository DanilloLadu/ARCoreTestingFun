package com.example.halloar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.ar.sceneform.Scene;
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

    private int[] _lengthWidthHeight = new int[3];


    private Boolean FIRST_NODE_EXISTS = false;
    private Boolean SECOND_NODE_EXISTS = false;
    private AnchorNode _firstCurrentAnchorNode;
    private AnchorNode _secondCurrentAnchorNode;
    private TextView tvDistance;
    private TextView tvLength;
    ModelRenderable cubeRenderable;
    private Anchor _firstCurrentAnchor = null;
    private Anchor _secondCurrentAnchor = null;


    private android.graphics.Point getScreenCenter() {
        View vw = findViewById(android.R.id.content);
        return new android.graphics.Point(vw.getWidth()/2, vw.getHeight()/2);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        }

        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        tvDistance = findViewById(R.id.tvDistance);


        initModel();



        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (cubeRenderable == null)
                return;

            Anchor anchor = hitResult.createAnchor();

            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            //clearAnchor();

            if(!FIRST_NODE_EXISTS && !SECOND_NODE_EXISTS)
            {
                _firstCurrentAnchor = anchor;
                _firstCurrentAnchorNode = anchorNode;

                FIRST_NODE_EXISTS = true;

            }else if(FIRST_NODE_EXISTS && !SECOND_NODE_EXISTS){
                _secondCurrentAnchor = anchor;
                _secondCurrentAnchorNode = anchorNode;

                SECOND_NODE_EXISTS = true;
            }else if(FIRST_NODE_EXISTS && SECOND_NODE_EXISTS){

                clearAnchors();
                _firstCurrentAnchor = anchor;
                _firstCurrentAnchorNode = anchorNode;
                FIRST_NODE_EXISTS = true;

            }




            TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
            node.setRenderable(cubeRenderable);
            node.setParent(anchorNode);
            arFragment.getArSceneView().getScene().addOnUpdateListener(this);
            arFragment.getArSceneView().getScene().addChild(anchorNode);
            node.select();


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
                            cubeRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
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



        /**
        if(SECOND_NODE_EXISTS) {
            _firstCurrentAnchor = null;


            if (_firstCurrentAnchor != null) {
                arFragment.getArSceneView().getScene().removeChild(_firstCurrentAnchorNode);
                _firstCurrentAnchorNode.getAnchor().detach();
                _firstCurrentAnchorNode.setParent(null);
                _firstCurrentAnchorNode = null;
            }
        }else {
            _secondCurrentAnchor = null;


            if (_secondCurrentAnchorNode != null) {
                arFragment.getArSceneView().getScene().removeChild(_secondCurrentAnchorNode);
                _secondCurrentAnchorNode.getAnchor().detach();
                _secondCurrentAnchorNode.setParent(null);
                _secondCurrentAnchorNode = null;
            }
        }

         */
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        Log.d("API123", "onUpdateframe... current anchor node " + (_firstCurrentAnchorNode == null));

        tvDistance.setText("No point selected");
        if(FIRST_NODE_EXISTS && !SECOND_NODE_EXISTS){
            tvDistance.setText("Select second point");


        }else if(FIRST_NODE_EXISTS && SECOND_NODE_EXISTS){

                Pose firstPose = _firstCurrentAnchor.getPose();
                Pose secondPose = _secondCurrentAnchor.getPose();

                float dx = firstPose.tx() - secondPose.tx();
                float dy = firstPose.ty() - secondPose.ty();
                float dz = firstPose.tz() - secondPose.tz();

                ///Compute the straight-line distance.
                float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                tvDistance.setText("Difference: " + distanceMeters + " metres");


            /*float[] distance_vector = currentAnchor.getPose().inverse()
                    .compose(cameraPose).getTranslation();
            float totalDistanceSquared = 0;
            for (int i = 0; i < 3; ++i)
                totalDistanceSquared += distance_vector[i] * distance_vector[i];*/



        }




    }

}
