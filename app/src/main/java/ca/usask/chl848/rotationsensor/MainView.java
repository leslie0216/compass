package ca.usask.chl848.rotationsensor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

/**
 * Created by chl848 on 05/06/2015.
 */
public class MainView extends View {
    Paint m_paint;

    private String m_id;
    private String m_name;
    private int m_color;

    private class RotationVector {
        float m_x;
        float m_y;
        float m_z;
    }

    private Queue<RotationVector> m_rotationVectorQueue;
    private static final int m_filterSize = 15;
    private static final int m_textSize = 70;
    private static final int m_messageTextSize = 50;
    private static final int m_textStrokeWidth = 2;
    private static final int m_boundaryStrokeWidth = 10;

    private String m_message;

    Bitmap m_pic;

    public class RemotePhoneInfo {
        String m_name;
        int m_color;
        float m_x;
        float m_y;
        float m_z;
    }

    private ArrayList<RemotePhoneInfo> m_remotePhones;
    private float m_remotePhoneRadius;

    private class Ball {
        public int m_ballColor;
        public float m_ballX;
        public float m_ballY;
        public boolean m_isTouched;
        public String m_id;
        public String m_name;

    }
    private ArrayList<Ball> m_balls;
    private int m_touchedBallId;

    private float m_ballRadius;
    private float m_ballBornX;
    private float m_ballBornY;

    private float m_localCoordinateCenterX;
    private float m_localCoordinateCenterY;
    private float m_localCoordinateRadius;

    private boolean m_showRemoteNames;
    final Handler handler = new Handler();
    Runnable mLongPressed = new Runnable() {
        @Override
        public void run() {
            setShowRemoteNames(true);
            m_numberOfLongPress++;
            invalidate();
        }
    };

    /**
     * experiment begin
     */

    private ArrayList<String> m_ballNames;
    private long m_trailStartTime;
    private int m_numberOfDrops;
    private int m_numberOfErrors;
    private int m_numberOfTouch;
    private int m_numberOfTouchBall;
    private int m_numberOfLongPress;
    private int m_numberOfRelease;
    private String m_receiverName;
    private int m_maxBlocks;
    private int m_maxTrails;
    private int m_currentBlock;
    private int m_currentTrail;
    private static final int m_experimentPhoneNumber = 1;
    private MainLogger m_logger;
    private MainLogger m_angleLogger;
    private boolean m_isStarted;
    private boolean m_isExperimentInitialised;
    // calibration
    private boolean m_isAccurate = true;
    /**
     * experiment end
     */

    public MainView (Context context) {
        super(context);

        m_paint = new Paint();
        m_rotationVectorQueue = new LinkedList<>();
        m_remotePhones = new ArrayList<>();

        setBackgroundColor(Color.WHITE);
        m_pic = BitmapFactory.decodeResource(this.getResources(), R.drawable.arrow);

        m_message = "No Message";

        m_id = ((MainActivity)(context)).getUserId();
        m_name = ((MainActivity)(context)).getUserName();
        Random rnd = new Random();
        //m_color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        m_color = ((MainActivity)(context)).getUserColor();

        m_touchedBallId = -1;
        m_balls = new ArrayList<>();
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        m_ballRadius = displayMetrics.widthPixels * 0.08f;
        m_ballBornX = displayMetrics.widthPixels * 0.5f;
        m_ballBornY = displayMetrics.heightPixels * 0.75f - m_ballRadius * 2.0f;

        m_localCoordinateCenterX = displayMetrics.widthPixels * 0.5f;
        m_localCoordinateCenterY = displayMetrics.heightPixels * 0.5f;
        m_localCoordinateRadius = displayMetrics.widthPixels * 0.5f;

        m_remotePhoneRadius = displayMetrics.widthPixels * 0.05f;

        setShowRemoteNames(false);

        resetCounters();

        m_isStarted = false;

        m_isExperimentInitialised = false;
    }

    private void setShowRemoteNames(boolean show) {
        m_showRemoteNames = show;
    }

    private boolean getShowRemoteNames() {
        return m_showRemoteNames;
    }

    @Override
    protected  void onDraw(Canvas canvas) {
        showAccuracy(canvas);
        showArrow(canvas);
        showBoundary(canvas);
        showLocalCircleCoordinate(canvas);
        showMessage(canvas);
        showRotationVector(canvas);
        showBalls(canvas);
        //showProgress(canvas);
    }

    public void showAccuracy(Canvas canvas) {
        if (!m_isAccurate) {
            m_paint.setTextSize(m_messageTextSize);
            m_paint.setColor(Color.RED);
            m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

            String output = "Inaccurate";
            canvas.drawText(output, (int) (displayMetrics.widthPixels * 0.05), (int) (displayMetrics.heightPixels * 0.1), m_paint);
        }
    }

    public void showBoundary(Canvas canvas) {
        m_paint.setColor(Color.RED);
        m_paint.setStrokeWidth(m_boundaryStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawLine(0, displayMetrics.heightPixels * 0.75f, displayMetrics.widthPixels, displayMetrics.heightPixels * 0.75f, m_paint);
    }

    public void showBalls(Canvas canvas) {
        if (!m_balls.isEmpty()) {
            for (Ball ball : m_balls) {
                m_paint.setColor(ball.m_ballColor);
                m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawCircle(ball.m_ballX, ball.m_ballY, m_ballRadius, m_paint);

                /**
                 * experiment begin
                 */

                m_paint.setStrokeWidth(m_textStrokeWidth);
                m_paint.setTextSize(m_textSize);
                float textX = ball.m_ballX - m_ballRadius;
                float textY = ball.m_ballY - m_ballRadius;
                if (ball.m_name.length() > 5) {
                    textX = ball.m_ballX - m_ballRadius * 2.0f;
                }
                canvas.drawText(ball.m_name, textX, textY, m_paint);
                /**
                 * experiment end
                 */
            }
        }
    }

    public void showArrow(Canvas canvas) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        float picWidth = m_pic.getScaledWidth(displayMetrics);
        float picHeight = m_pic.getScaledHeight(displayMetrics);

        float left = displayMetrics.widthPixels * 0.5f - picWidth * 0.25f;
        float top = displayMetrics.heightPixels * 0.02f;
        float right = displayMetrics.widthPixels * 0.5f + picWidth * 0.25f;
        float bottom = displayMetrics.heightPixels * 0.01f + picHeight*0.3f;
        RectF disRect = new RectF(left, top, right, bottom );
        canvas.drawBitmap(m_pic, null, disRect, m_paint);
    }

    public void showCircleCoordinate(Canvas canvas) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        float centerX = displayMetrics.widthPixels * 0.5f;
        float centerY = displayMetrics.heightPixels * 0.5f;
        float radius = 250.0f;

        m_paint.setColor(Color.BLUE);
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(10);
        canvas.drawCircle(centerX, centerY, radius, m_paint);

        // local
        float angle = 90.0f - getRotationVector().m_z + 180.0f;
        float pointX = centerX + radius * (float)Math.cos(Math.toRadians(angle));
        float pointY = centerY - radius * (float) Math.sin(Math.toRadians(angle));
        m_paint.setColor(Color.YELLOW);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(pointX, pointY, 20, m_paint);

        // remote
        /*
        float angle_remote = 90.0f - m_remote_rotationVector[0];
        float pointX_remote = centerX + radius * (float)Math.cos(Math.toRadians(angle_remote));
        float pointY_remote = centerY - radius * (float) Math.sin(Math.toRadians(angle_remote));
        m_paint.setColor(Color.YELLOW);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(pointX_remote, pointY_remote, 20, m_paint);
        */
    }

    public void showLocalCircleCoordinate(Canvas canvas) {
        MainActivity ma = (MainActivity)getContext();

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        float left = 0.0f;
        float top = displayMetrics.heightPixels * 0.5f - m_localCoordinateRadius;
        float right = displayMetrics.widthPixels;
        float bottom = displayMetrics.heightPixels * 0.5f + m_localCoordinateRadius;
        RectF disRect = new RectF(left, top, right, bottom);

        m_paint.setColor(Color.BLUE);
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(m_boundaryStrokeWidth);
        //canvas.drawCircle(centerX, centerY, radius, m_paint);
        canvas.drawArc(disRect, 160.0f, 220.0f, false, m_paint);

        if ((ma != null) && ma.isConnected()) {
            showRemotePhones(canvas);
        }
    }

    public void showRemotePhones(Canvas canvas) {
        if (!m_remotePhones.isEmpty()) {
            for (RemotePhoneInfo info : m_remotePhones) {
                float angle_remote = calculateRemoteAngleInLocalCoordinate(info.m_z);
                float pointX_remote = m_localCoordinateCenterX + m_localCoordinateRadius * (float) Math.cos(Math.toRadians(angle_remote));
                float pointY_remote = m_localCoordinateCenterY - m_localCoordinateRadius * (float) Math.sin(Math.toRadians(angle_remote));
                m_paint.setColor(info.m_color);
                m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawCircle(pointX_remote, pointY_remote, m_remotePhoneRadius, m_paint);

                if (getShowRemoteNames()) {
                    m_paint.setTextSize(m_textSize);
                    m_paint.setStrokeWidth(m_textStrokeWidth);
                    float textX = pointX_remote - m_remotePhoneRadius;
                    float textY = pointY_remote - m_remotePhoneRadius * 1.5f;
                    if (info.m_name.length() > 5) {
                        textX = pointX_remote - m_remotePhoneRadius * 2.0f;
                    }
                    canvas.drawText(info.m_name, textX, textY, m_paint);
                }
            }
        }
    }

    public void showRotationVector(Canvas canvas) {
        m_paint.setTextSize(m_messageTextSize);
        m_paint.setColor(Color.RED);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        RotationVector rotationVector = getRotationVector();

        //String output = "Z = " + String.format("%.3f",rotationVector.m_z) + " X = " + String.format("%.3f", rotationVector.m_x) + " Y = " + String.format("%.3f", rotationVector.m_y);
        String output = "Azimuth = " + String.format("%.3f",rotationVector.m_z);
        canvas.drawText(output, (int) (displayMetrics.widthPixels * 0.3), (int) (displayMetrics.heightPixels * 0.85), m_paint);

        /*
        if (!m_remotePhones.isEmpty()){
            float remote = m_remotePhones.get(0).m_z;
            float angle = calculateRemoteAngleInLocalCoordinate(remote);
            float angle_2 = (getRotationVector().m_z - remote)/2.0f;
            if (angle_2 < 0){
                angle_2 = 180.0f+angle_2;
            }

            String output2 = "a1 = " + String.format("%.3f",angle);
            String output3 = "a2 = " + String.format("%.3f",angle_2);
            canvas.drawText(output2, (int) (displayMetrics.widthPixels * 0.7), (int) (displayMetrics.heightPixels * 0.1), m_paint);
            canvas.drawText(output3, (int) (displayMetrics.widthPixels * 0.7), (int) (displayMetrics.heightPixels * 0.15), m_paint);
        }
        */

        /*
        //String output1 = "Z axis (Yaw) = " + String.format("%.4f", m_rotationVector[0]);
        String output1 = "Z = " + String.format("%.4f", m_rotationVector[0]);

        canvas.drawText(output1, (int) (displayMetrics.widthPixels * 0.05), (int) (displayMetrics.heightPixels * 0.75), m_paint);

        //String output2 = "X axis (Pitch) = " + String.format("%.4f", m_rotationVector[1]);
        String output2 = "X = " + String.format("%.4f", m_rotationVector[1]);
        canvas.drawText(output2, (int) (displayMetrics.widthPixels * 0.05), (int) (displayMetrics.heightPixels * 0.8), m_paint);

        //String output3 = "Y axis (Roll) = " + String.format("%.4f", m_rotationVector[2]);
        String output3 = "Y = " + String.format("%.4f", m_rotationVector[2]);
        canvas.drawText(output3, (int) (displayMetrics.widthPixels * 0.05), (int) (displayMetrics.heightPixels * 0.85), m_paint);
        */
    }

    public void showMessage(Canvas canvas) {
        m_paint.setTextSize(m_messageTextSize);
        m_paint.setColor(Color.GREEN);
        m_paint.setStrokeWidth(m_textStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawText(m_message, displayMetrics.widthPixels * 0.3f, displayMetrics.heightPixels * 0.8f, m_paint);
    }

    public void showProgress(Canvas canvas) {
        m_paint.setTextSize(m_messageTextSize);
        m_paint.setColor(Color.BLUE);
        m_paint.setStrokeWidth(m_textStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        String block = "Block: " + m_currentBlock +"/" + m_maxBlocks;
        canvas.drawText(block, (int) (displayMetrics.widthPixels * 0.75), (int) (displayMetrics.heightPixels * 0.1), m_paint);

        String trial = "Trial: " + m_currentTrail +"/" + m_maxTrails;
        canvas.drawText(trial, (int) (displayMetrics.widthPixels * 0.75), (int) (displayMetrics.heightPixels * 0.15), m_paint);
    }

    private float calculateRemoteAngleInLocalCoordinate(float raw_angle) {
        float angle = 90.0f - getRotationVector().m_z + 180.0f;
        float angle_remote = 90.0f - raw_angle + 180.0f;

        float included_angle = Math.abs(angle - angle_remote);
        boolean greaterThan180 = false;

        if (included_angle > 180.0f) {
            included_angle = 360.0f - included_angle;
            greaterThan180 = true;
        }

        float new_remote_angle;

        float intersect_angle = (180.0f - included_angle) / 2.0f;

        if (angle > angle_remote) {
            if (!greaterThan180) {
                new_remote_angle = 90.0f + intersect_angle;
            } else {
                new_remote_angle = 90.0f - intersect_angle;
            }
        } else {
            if (!greaterThan180) {
                new_remote_angle = 90.0f - intersect_angle;
            } else {
                new_remote_angle = 90.0f + intersect_angle;
            }
        }

        return new_remote_angle;
    }

    public void setRotation(float[] values, boolean isAccurate) {
        // log raw angle
        if (m_isStarted) {
            if (m_angleLogger != null) {
                //<participantID> <participantName> <condition> <block#> <trial#> <azimuth(Z)> <pitch(X)> <roll(Y)> <isAccurate> <timestamp>
                //m_angleLogger.write(m_id + "," + m_name + "," + getResources().getString(R.string.app_name) + "," + m_currentBlock + "," + m_currentTrail + "," + values[0] + "," + values[1] + "," + values[2] + "," + (isAccurate?1:0) + "," + System.currentTimeMillis(), false);
            }
        }


        RotationVector rotationVector = new RotationVector();
        rotationVector.m_x = values[1];
        rotationVector.m_y = values[2];
        rotationVector.m_z = values[0];

        int size = m_rotationVectorQueue.size();
        if (size >= m_filterSize) {
            m_rotationVectorQueue.poll();
        }

        m_rotationVectorQueue.offer(rotationVector);
        this.invalidate();
    }

    public void setIsAccurate (boolean isAccurate) {
        m_isAccurate = isAccurate;
    }

    private RotationVector getRotationVector() {
        RotationVector rotationVector = new RotationVector();
        float x, y, z;
        x = y = z = 0.0f;
        int size = m_rotationVectorQueue.size();
        for (RotationVector rv : m_rotationVectorQueue) {
            x += rv.m_x;
            y += rv.m_y;
            z += rv.m_z;
        }
        rotationVector.m_x = x/size;
        rotationVector.m_y = y/size;
        rotationVector.m_z = z/size;

        return rotationVector;
    }

    public void setMessage (String msg) {
        m_message = msg;
    }

    public void updateRemotePhone(String name, int color, float x, float y, float z){
        if (name.isEmpty() || name.equalsIgnoreCase(m_name)) {
            return;
        }

        int size = m_remotePhones.size();
        boolean isFound = false;
        for (int i = 0; i<size; ++i) {
            RemotePhoneInfo info = m_remotePhones.get(i);
            if (info.m_name.equalsIgnoreCase(name)) {
                info.m_color = color;
                info.m_x = x;
                info.m_y = y;
                info.m_z = z;

                isFound = true;
                break;
            }
        }

        if (!isFound) {
            RemotePhoneInfo info = new RemotePhoneInfo();
            info.m_name = name;
            info.m_color = color;
            info.m_x = x;
            info.m_y = y;
            info.m_z = z;

            m_remotePhones.add(info);

            /**
             * experiment end
             */
            if (m_remotePhones.size() == m_experimentPhoneNumber && !m_isExperimentInitialised) {
                initExperiment();
            }
            /**
             * experiment end
             */
        }
    }

    public ArrayList<RemotePhoneInfo> getRemotePhones() {
        return m_remotePhones;
    }

    public void removePhones(ArrayList<RemotePhoneInfo> phoneInfos) {
        m_remotePhones.removeAll(phoneInfos);
    }

    public void clearRemotePhoneInfo() {
        m_remotePhones.clear();
    }

    public int getBallCount() {
        return m_balls.size();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        float X = event.getX();
        float Y = event.getY();
        float touchRadius = event.getTouchMajor();

        int ballCount = m_balls.size();
        switch (eventaction) {
            case MotionEvent.ACTION_DOWN:
                m_numberOfTouch++;
                m_touchedBallId = -1;
                for (int i = 0; i < ballCount; ++i){
                    Ball ball = m_balls.get(i);
                    ball.m_isTouched = false;

                    double dist;
                    dist = Math.sqrt(Math.pow((X - ball.m_ballX), 2) + Math.pow((Y - ball.m_ballY), 2));
                    if (dist <= (touchRadius + m_ballRadius)) {
                        ball.m_isTouched = true;
                        m_touchedBallId = i;

                        boolean isOverlap = false;
                        for (int j = 0; j < ballCount; ++j) {
                            if (j != m_touchedBallId) {
                                Ball ball2 = m_balls.get(j);

                                double dist2 = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                                if (dist2 <= m_ballRadius * 2) {
                                    isOverlap = true;
                                }
                            }
                        }

                        if (!isOverlap && !isBoundary(X, Y)) {
                            ball.m_ballX = X;
                            ball.m_ballY = Y;
                            this.invalidate();
                        }
                    }

                    if (m_touchedBallId > -1)
                    {
                        break;
                    }
                }

                if (m_touchedBallId == -1) {

                    boolean show = false;

                    for (RemotePhoneInfo remotePhone : m_remotePhones) {
                        float angle_remote = calculateRemoteAngleInLocalCoordinate(remotePhone.m_z);
                        float pointX_remote = m_localCoordinateCenterX + m_localCoordinateRadius * (float) Math.cos(Math.toRadians(angle_remote));
                        float pointY_remote = m_localCoordinateCenterY - m_localCoordinateRadius * (float) Math.sin(Math.toRadians(angle_remote));

                        double dist = Math.sqrt(Math.pow((X - pointX_remote),2) + Math.pow((Y - pointY_remote), 2));

                        if (dist <= (touchRadius + m_remotePhoneRadius)) {
                            show = true;
                            break;
                        }
                    }

                    if (show) {
                        handler.postDelayed(mLongPressed, 500);
                    }
                } else {
                    m_numberOfTouchBall++;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (getShowRemoteNames()) {
                    boolean show = false;

                    for (RemotePhoneInfo remotePhone : m_remotePhones) {
                        float angle_remote = calculateRemoteAngleInLocalCoordinate(remotePhone.m_z);
                        float pointX_remote = m_localCoordinateCenterX + m_localCoordinateRadius * (float) Math.cos(Math.toRadians(angle_remote));
                        float pointY_remote = m_localCoordinateCenterY - m_localCoordinateRadius * (float) Math.sin(Math.toRadians(angle_remote));

                        double dist = Math.sqrt(Math.pow((X - pointX_remote),2) + Math.pow((Y - pointY_remote), 2));

                        if (dist <= (touchRadius + m_remotePhoneRadius)) {
                            show = true;
                            break;
                        }
                    }

                    if (!show) {
                        handler.removeCallbacks(mLongPressed);
                        setShowRemoteNames(false);
                        invalidate();
                    }
                }

                if (m_touchedBallId > -1) {
                    Ball ball = m_balls.get(m_touchedBallId);
                    if (ball.m_isTouched) {
                        boolean isOverlap = false;

                        for (int j = 0; j < ballCount; ++j) {
                            if (j != m_touchedBallId) {
                                Ball ball2 = m_balls.get(j);

                                double dist = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                                if (dist <= m_ballRadius * 2) {
                                    isOverlap = true;
                                }
                            }
                        }

                        if (!isOverlap & !isBoundary(X, Y)) {
                            ball.m_ballX = X;
                            ball.m_ballY = Y;
                            this.invalidate();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(mLongPressed);
                if (getShowRemoteNames()) {
                    setShowRemoteNames(false);
                    invalidate();
                }
                m_numberOfRelease++;

                if (m_touchedBallId > -1) {
                    m_numberOfDrops += 1;
                    Ball ball = m_balls.get(m_touchedBallId);
                    if (ball.m_isTouched) {
                        boolean isOverlap = false;

                        for (int j = 0; j < ballCount; ++j) {
                            if (j != m_touchedBallId) {
                                Ball ball2 = m_balls.get(j);

                                double dist = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                                if (dist <= m_ballRadius * 2) {
                                    isOverlap = true;
                                }
                            }
                        }

                        if (!isOverlap) {
                            String name = isSending(ball.m_ballX, ball.m_ballY);
                            if (!ball.m_name.isEmpty() && !name.isEmpty()) {
                                if (name.equalsIgnoreCase(ball.m_name)) {
                                    //((MainActivity) getContext()).showToast("send ball to : " + name);
                                    //sendBall(ball, id);
                                    removeBall(ball.m_id);
                                    this.invalidate();
                                    endTrail();
                                } else {
                                    m_numberOfErrors += 1;
                                }
                            }
                        }
                    }
                }

                for (Ball ball : m_balls) {
                    ball.m_isTouched = false;
                }
                break;
        }

        return  true;
    }

    private boolean isBoundary(float x, float y) {
        boolean rt = false;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        while (true) {
            if (y > m_localCoordinateCenterY) {
                // check bottom
                if ((y + m_ballRadius) >= (displayMetrics.heightPixels * 0.75f)) {
                    rt = true;
                    break;
                }

                // check left
                if (x - m_ballRadius <= 0.0f) {
                    rt = true;
                    break;
                }

                // check right
                if (x + m_ballRadius >= displayMetrics.widthPixels) {
                    rt = true;
                    break;
                }
            } else {
                //check top
                double dist = Math.sqrt(Math.pow((x - m_localCoordinateCenterX), 2) + Math.pow((y - m_localCoordinateCenterY), 2));
                if (dist + m_ballRadius >= m_localCoordinateRadius) {
                    rt = true;
                }
                break;
            }
            break;
        }

        return rt;
    }

    private boolean canTouch(float x, float y) {
        boolean rt = true;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        while (true) {
            if (y > m_localCoordinateCenterY) {
                // check bottom
                if (y >= (displayMetrics.heightPixels * 0.75f)) {
                    rt = false;
                    break;
                }

                // check left
                if (x <= 0.0f) {
                    rt = false;
                    break;
                }

                // check right
                if (x >= displayMetrics.widthPixels) {
                    rt = false;
                    break;
                }
            } else {
                //check top
                double dist = Math.sqrt(Math.pow((x - m_localCoordinateCenterX), 2) + Math.pow((y - m_localCoordinateCenterY), 2));
                if (dist >= m_localCoordinateRadius) {
                    rt = false;
                }
                break;
            }
            break;
        }

        return rt;
    }

    private String isSending(float x, float y) {
        String receiverName = "";
        float rate = 10000.0f;
        if (!m_remotePhones.isEmpty()) {
            for (RemotePhoneInfo remotePhoneInfo : m_remotePhones) {
                float angle_remote = calculateRemoteAngleInLocalCoordinate(remotePhoneInfo.m_z);
                float pointX_remote = m_localCoordinateCenterX + m_localCoordinateRadius * (float) Math.cos(Math.toRadians(angle_remote));
                float pointY_remote = m_localCoordinateCenterY - m_localCoordinateRadius * (float) Math.sin(Math.toRadians(angle_remote));

                double dist = Math.sqrt(Math.pow((x - pointX_remote), 2) + Math.pow((y - pointY_remote), 2));
                if (dist < (m_remotePhoneRadius + m_ballRadius)){
                    if (dist < rate) {
                        receiverName = remotePhoneInfo.m_name;
                        rate = (float)dist;
                    }
                }
            }
        }

        return receiverName;
    }

    public void addBall() {
        Ball ball = new Ball();
        Random rnd = new Random();
        ball.m_ballColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        ball.m_ballX = m_ballBornX;
        ball.m_ballY = m_ballBornY;
        ball.m_isTouched = false;
        ball.m_id = UUID.randomUUID().toString();
        ball.m_name = getBallName();
        m_balls.add(ball);
        m_receiverName = ball.m_name;
        this.invalidate();
    }

    public  void removeBall(String id) {
        for (Ball ball : m_balls) {
            if (ball.m_id.equalsIgnoreCase(id)) {
                m_balls.remove(ball);
                m_touchedBallId = -1;
                break;
            }
        }
    }

    public void receivedBall(String id, int color) {
        boolean isReceived = false;
        for (Ball ball : m_balls) {
            if (ball.m_id.equalsIgnoreCase(id)) {
                isReceived = true;
                break;
            }
        }

        if (!isReceived) {
            Ball ball = new Ball();
            ball.m_id = id;
            ball.m_ballColor = color;
            ball.m_isTouched = false;

            ball.m_ballX = m_ballBornX;
            ball.m_ballY = m_ballBornY;

            m_balls.add(ball);
        }
    }

    public void sendBall(Ball ball, String receiverName ) {
        JSONObject jsonObject = new JSONObject();
        RotationVector rotationVector = getRotationVector();
        try {
            jsonObject.put("ballId", ball.m_id);
            jsonObject.put("ballColor", ball.m_ballColor);
            jsonObject.put("receiverName", receiverName);
            jsonObject.put("isSendingBall", true);
            jsonObject.put("name", m_name);
            jsonObject.put("z", rotationVector.m_z);
            jsonObject.put("x", rotationVector.m_z);
            jsonObject.put("y", rotationVector.m_z);
            jsonObject.put("color", m_color);
        } catch (JSONException e){
            e.printStackTrace();
        }

        MainActivity ma = (MainActivity)getContext();
        if (ma != null) {
            ma.addMessage(jsonObject.toString());
        }
    }


    public void sendRotation(){
        JSONObject msg = new JSONObject();
        RotationVector rotationVector = getRotationVector();
        try {
            msg.put("name", m_name);
            msg.put("z", rotationVector.m_z);
            msg.put("x", rotationVector.m_x);
            msg.put("y", rotationVector.m_y);
            msg.put("color", m_color);
            msg.put("isSendingBall", false);
        } catch (JSONException e){
            e.printStackTrace();
        }

        MainActivity ma = (MainActivity)getContext();
        if (ma != null) {
            ma.addMessage(msg.toString());
        }
    }

    /**
     * experiment begin
     */
    private void initExperiment() {
        m_isExperimentInitialised = true;

        // init ball names
        m_ballNames = new ArrayList<>();

        m_maxBlocks = 5;
        m_maxTrails = 9;

        m_currentBlock = 0;
        m_currentTrail = 0;

        m_isStarted = false;

        resetBlock();

        m_logger = null;
        //m_logger = new MainLogger(getContext(), m_id+"_"+m_name+"_"+getResources().getString(R.string.app_name));
        //<participantID> <participantName> <condition> <block#> <trial#> <receiver name> <elapsed time for this trial> <number of errors for this trial> <number of release for this trial> <number of drops for this trial> <number of touch for this trial> <number of touch ball for this trial> <number of long press for this trial> <timestamp>
        //m_logger.writeHeaders("participantID" + "," + "participantName" + "," + "condition" + "," + "block" + "," + "trial" + "," + "receiverName" + "," + "elapsedTime" + "," + "errors" + "," + "release" + "," + "drops" + "," + "touch" + "," + "touchBall" + "," + "longPress" + "," + "timestamp");

        m_angleLogger = null;
        //m_angleLogger = new MainLogger(getContext(), m_id+"_"+m_name+"_"+getResources().getString(R.string.app_name)+"_orientation");
        //<participantID> <participantName> <condition> <block#> <trial#> <azimuth(Z)> <pitch(X)> <roll(Y)> <isAccurate> <timestamp>
        //m_angleLogger.writeHeaders("participantID" + "," + "participantName" + "," + "condition" + "," + "block" + "," + "trial" + "," + "azimuth(Z)" + "," + "pitch(X)" + "," + "roll(Y)" + "," + "isAccurate" + "," + "timestamp");

        ((MainActivity) getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((MainActivity) getContext()).setStartButtonEnabled(true);
                ((MainActivity) getContext()).setContinueButtonEnabled(false);
            }
        });
    }

    private String getBallName() {
        if (m_ballNames.isEmpty()) {
            return "";
        }

        Random rnd = new Random();
        int index = rnd.nextInt(m_ballNames.size());
        String name = m_ballNames.get(index);
        m_ballNames.remove(index);
        return name;
    }

    public boolean isFinished() {
        return m_currentBlock == m_maxBlocks;
    }

    public void nextBlock() {
        ((MainActivity)getContext()).setStartButtonEnabled(true);
        ((MainActivity)getContext()).setContinueButtonEnabled(false);
    }

    public void resetBlock() {
        // reset ball names
        m_ballNames.clear();
        for (RemotePhoneInfo remotePhoneInfo : m_remotePhones){
            for(int i=0; i<3; i++){
                m_ballNames.add(remotePhoneInfo.m_name);
            }
        }

        // reset self phone color
        //Random rnd = new Random();
        //m_color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        resetCounters();
    }

    public void startBlock() {
        m_currentBlock += 1;
        m_currentTrail = 0;
        m_isStarted = true;

        resetBlock();
        startTrial();
        ((MainActivity)getContext()).setStartButtonEnabled(false);
        ((MainActivity)getContext()).setContinueButtonEnabled(false);
    }

    public void endBlock() {
        m_isStarted = false;

        if (isFinished()) {
            closeLogger();
        }

        new AlertDialog.Builder(getContext()).setTitle("Warning").setMessage("You have completed block " + m_currentBlock + ", please wait for other participants.").setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();

        ((MainActivity) getContext()).setContinueButtonEnabled(true);
        ((MainActivity)getContext()).setStartButtonEnabled(false);
        m_currentTrail = 0;
    }

    public void startTrial() {
        m_trailStartTime = System.currentTimeMillis();
        m_currentTrail += 1;
        resetCounters();
        addBall();
    }

    public void endTrail() {
        long trailEndTime = System.currentTimeMillis();
        long timeElapse = trailEndTime - m_trailStartTime;

        if (m_currentBlock == 0) {
            ++m_currentBlock;
        }

        if (m_currentTrail == 0) {
            ++m_currentTrail;
        }

        //<participantID> <participantName> <condition> <block#> <trial#> <receiver name> <elapsed time for this trial> <number of errors for this trial> <number of release for this trial> <number of drops for this trial> <number of touch for this trial> <number of touch ball for this trial> <number of long press for this trial> <timestamp>
        if (m_logger != null) {
            //m_logger.write(m_id + "," + m_name + "," + getResources().getString(R.string.app_name) + "," + m_currentBlock + "," + m_currentTrail + "," + m_receiverName + "," + timeElapse + "," + m_numberOfErrors + "," + m_numberOfRelease + "," + m_numberOfDrops + "," + m_numberOfTouch + "," + m_numberOfTouchBall + "," + m_numberOfLongPress + "," + trailEndTime, true);
        }

        if (m_angleLogger != null) {
            m_angleLogger.flush();
        }

        if (m_currentTrail < m_maxTrails) {
            startTrial();
        } else {
            endBlock();
        }
    }

    public void closeLogger() {
        if (m_logger != null) {
            m_logger.close();
        }

        if (m_angleLogger != null) {
            m_angleLogger.close();
        }
    }

    private void resetCounters() {
        m_numberOfDrops = 0;
        m_numberOfErrors = 0;
        m_numberOfTouch = 0;
        m_numberOfTouchBall = 0;
        m_numberOfLongPress = 0;
        m_numberOfRelease = 0;
        m_receiverName = "";
    }
    /**
     * experiment end
     */
}
