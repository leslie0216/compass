package ca.usask.chl848.rotationsensor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Locale;


public class LoginActivity extends Activity {

    private static final HashMap<String, Integer> m_ColorNameMap;

    static {
        m_ColorNameMap = new HashMap<>();
        m_ColorNameMap.put("black", Color.BLACK);
        m_ColorNameMap.put("darkgray", Color.DKGRAY);
        m_ColorNameMap.put("gray", Color.GRAY);
        m_ColorNameMap.put("lightgray", Color.LTGRAY);
        m_ColorNameMap.put("white", Color.WHITE);
        m_ColorNameMap.put("red", Color.RED);
        m_ColorNameMap.put("green", Color.GREEN);
        m_ColorNameMap.put("blue", Color.BLUE);
        m_ColorNameMap.put("yellow", Color.YELLOW);
        m_ColorNameMap.put("cyan", Color.CYAN);
        m_ColorNameMap.put("magenta", Color.MAGENTA);
        m_ColorNameMap.put("aqua", 0xFF00FFFF);
        m_ColorNameMap.put("fuchsia", 0xFFFF00FF);
        m_ColorNameMap.put("darkgrey", Color.DKGRAY);
        m_ColorNameMap.put("grey", Color.GRAY);
        m_ColorNameMap.put("lightgrey", Color.LTGRAY);
        m_ColorNameMap.put("lime", 0xFF00FF00);
        m_ColorNameMap.put("maroon", 0xFF800000);
        m_ColorNameMap.put("navy", 0xFF000080);
        m_ColorNameMap.put("olive", 0xFF808000);
        m_ColorNameMap.put("purple", 0xFF800080);
        m_ColorNameMap.put("silver", 0xFFC0C0C0);
        m_ColorNameMap.put("teal", 0xFF008080);
        m_ColorNameMap.put("pink", 0xFFFF69B4);
        m_ColorNameMap.put("orange", 0xFFFF4500);
        m_ColorNameMap.put("darkgreen", 0xFF006400);

    }

    private static final String[] Colors = {"Black", "DarkGrey", "Grey", "LightGrey", "White", "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta", "Aqua", "Fuchsia", "Lime", "Maroon", "Navy", "Olive", "Purple", "Silver", "Teal", "Pink", "Orange", "DarkGreen"};
    private TextView m_spinnerTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        m_spinnerTextView = (TextView)findViewById(R.id.txt_color);
        Spinner spinner = (Spinner) findViewById(R.id.spinner_color);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Colors);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new SpinnerSelectedListener());
        spinner.setVisibility(View.VISIBLE);

        Button btnPrev=(Button)this.findViewById(R.id.btn_go);
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = getUserName();
                String id = getUserId();
                if (!user.isEmpty()) {
                    Intent intent = new Intent();
                    intent.setClass(LoginActivity.this, MainActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("user", user);
                    bundle.putString("id", id);
                    bundle.putInt("color", m_spinnerTextView.getCurrentTextColor());
                    intent.putExtras(bundle);
                    LoginActivity.this.startActivity(intent);
                    LoginActivity.this.finish();
                }
            }
        });
    }

    class SpinnerSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {
            String color = Colors[arg2];
            m_spinnerTextView.setText("Color : "+color);
            m_spinnerTextView.setTextColor(m_ColorNameMap.get(color.toLowerCase(Locale.ROOT)));
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    public String getUserName() {
        TextView user = (TextView)this.findViewById(R.id.txt_userName);
        return  user.getText().toString();
    }

    public String getUserId() {
        TextView user = (TextView)this.findViewById(R.id.txt_userId);
        return  user.getText().toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void exit() {
        new AlertDialog.Builder(LoginActivity.this).setTitle("Warning").setMessage("Do you want to exit?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                System.exit(0);
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
            }
        }).show();
    }
}
