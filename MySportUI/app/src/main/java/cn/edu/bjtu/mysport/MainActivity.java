package cn.edu.bjtu.mysport;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import user.LoginActivity;
import user.LoginService;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private TextView EmailView = null;
    private TextView UsernameView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {

            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
                    new DashBoardFragment()).commit();

        }

        View temp = navigationView.getHeaderView(0);
        EmailView = (TextView) temp.findViewById(R.id.email);
        UsernameView = (TextView) temp.findViewById(R.id.username);
    }

    protected void onStart() {
        update();
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            logout();
            Toast.makeText(MainActivity.this, "Log out successfully", Toast.LENGTH_SHORT).show();
            //return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.home) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
                    new DashBoardFragment()).commit();
        } else if (id == R.id.time) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
                    new MapsFragment()).commit();
        } else if (id == R.id.news) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
                    new NewsFragment()).commit();
        } else if (id == R.id.share) {
            Toast.makeText(MainActivity.this, "Successfully shared!", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.info) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
                    new InfoFragment()).commit();
        } else if (id == R.id.login) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(intent, 1);


        } else if (id == R.id.course) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
                    new CourseFragment()).commit();
        } else if (id == R.id.coach) {
//            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
//                    new CoachFragment()).commit();
//            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
//                    new CoachList()).commit();
            Intent intent = new Intent(MainActivity.this, CoachList.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    String email = data.getStringExtra("email");
                    saveLoginStatus(true, email);
                    update();
                }
                break;
            default:
        }
    }

    private void logout(){
        SharedPreferences sp = getSharedPreferences("loginInfo", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sp.edit();
        editor.remove("email");
        editor.remove("username");
        editor.putBoolean("isLogin",false);
        editor.commit();
        update();
    }
    private void update() {
        SharedPreferences sp = getSharedPreferences("loginInfo", MODE_PRIVATE);
//        boolean status = sp.getBoolean("isLogin", false);
//        if (!status){
//            EmailView.setText("");
//        }
        String email = sp.getString("email", "Please login first");
        EmailView.setText(email);
        String username = sp.getString("username", "BJTU");
        UsernameView.setText(username);
    }


    private void saveLoginStatus(boolean status, String email) {
        SharedPreferences sp = getSharedPreferences("loginInfo", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("isLogin", status);
        editor.putString("email", email);
        editor.commit();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        LoginService service = retrofit.create(LoginService.class);
        Call<ResponseBody> call = service.getUsername(email);
        call.enqueue(
                new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                        try {
                            JsonObject json = (JsonObject) new JsonParser().parse(response.body().string());
                            SharedPreferences sp = getSharedPreferences("loginInfo", MODE_PRIVATE);
                            final SharedPreferences.Editor editor = sp.edit();
                            String username = json.get("username").toString();
                            editor.putString("username", username.substring(1,username.length()-1)) ;
                            editor.commit();
                            update();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        t.printStackTrace();
                    }
                }
        );



    }

}
