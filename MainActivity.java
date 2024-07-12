
package com.dln.tunnel;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageInfo;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.airbnb.lottie.LottieAnimationView;
import com.dln.tunnel.R;
import com.dln.tunnel.fragments.LayoutCustom;
import com.dln.tunnel.fragments.Utils;
import com.dln.tunnel.fragments.RetrieveData;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.slipkprojects.ultrasshservice.logger.SkStatus;
import com.slipkprojects.ultrasshservice.logger.ConnectionStatus;
import com.slipkprojects.ultrasshservice.config.Settings;
import com.slipkprojects.ultrasshservice.StatisticGraphData;
import com.slipkprojects.ultrasshservice.tunnel.TunnelManagerHelper;
import com.slipkprojects.ultrasshservice.LaunchVpn;
import com.dln.tunnel.adapter.LogsAdapter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
//OPENVPN


import com.dln.openvpn.OpenVPNService;
import com.dln.openvpn.conf.ConfigUtil;
import com.dln.openvpn.VPNService;
import net.openvpn.openvpn.PrefUtil;
import androidx.preference.PreferenceManager;
import java.io.File;
import java.net.URLEncoder;
import java.io.OutputStream;
import java.io.FileOutputStream;
import com.dln.openvpn.conf.ConfigParser;
import com.dln.openvpn.conf.VpnProfile;
import com.slipkprojects.ultrasshservice.util.FileUtils;
import java.io.StringReader;
import android.os.RemoteException;

public class MainActivity extends BaseGT implements VPNService.InjectorListener,SkStatus.StateListener {
    MainActivity context;
    private Handler mHandler;
    Timer timerTask;
    Settings mConfig;
    private TextView status;
    private Button starterButton;
    TextView dTrasferDow, dTrasferUp;
    
    NavigationView navView, navLog;
    DrawerLayout drawLatout;
    ViewPager2 viewPager;
    
    private static final String UPDATE_VIEWS = "MainUpdate";
    public static final String OPEN_LOGS = "com.dln.tunnel:openLogs";
    
    Utils mUtils;
    LinearLayout spiner_outside, open_spiner;
    LinearLayout window_outside;
    TextView noty_msg;
    
    @SuppressLint("CutPasteId")
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
       Thread.setDefaultUncaughtExceptionHandler(new com.dln.tunnel.fragments.ExceptionHandler(this));
      setContentView(R.layout.activity_main);
        
      mConfig =new Settings(this);
      mUtils = new Utils(this);  
      mHandler = new Handler();
      context = this;   
         
     starterButton = new Button(this);
     status= (TextView)findViewById(R.id.connection_status);
     dTrasferDow=(TextView)findViewById(R.id.TransferDow);
     dTrasferUp=(TextView)findViewById(R.id.TransferUp);
        
     starterButton.setOnClickListener(new View.OnClickListener(){
     public void onClick(View v){
     startOrStopTunnel(context);    
     }});
     starterButton.setOnLongClickListener(new View.OnLongClickListener(){
     public boolean onLongClick(View v){
     SkStatus.updateStateString(SkStatus.SSH_DESCONECTADO, "Desconectado"); 
     TunnelManagerHelper.stopSocksHttp(context);            
     return true;    
     }});
        
     IntentFilter filter = new IntentFilter();
     filter.addAction(UPDATE_VIEWS);
     filter.addAction(OPEN_LOGS);
     filter.addAction("closeSpiner");
     filter.addAction("openLogin");
     filter.addAction("updateNoty");  
     filter.addAction("updateConf");      
     LocalBroadcastManager.getInstance(context).registerReceiver(mActivityReceiver, filter);
        
      drawLatout =(DrawerLayout)findViewById(R.id.draw_layout);
      navView =(NavigationView)findViewById(R.id.nav_view); 
      navLog =(NavigationView)findViewById(R.id.nav_log); 
      viewPager = (ViewPager2) findViewById(R.id.viewPager);
        
      window_outside = (LinearLayout)findViewById(R.id.window_outside);
      window_outside.setOnClickListener(new View.OnClickListener() { public void onClick(View v){ v.setVisibility(View.GONE); }});
      
      noty_msg=  (TextView)findViewById(R.id.noty_msg);
        
      RelativeLayout paren = (RelativeLayout)findViewById(R.id.draw_curve_head);
      LinearLayout   xn =  new LayoutCustom(this, 0, new int[]{  0xdd55aaff,0x1100aaff,0x0200aaff}, new float[]{10,0},60f, 4); //2,2,40,40, 2,2}, 55f);
      //paren.addView(xn);
      xn =  new LayoutCustom(this, 0, new int[]{  0x445577ff,0x110055ff,0x010055ff}, new float[]{10,0},1.01f, 2); //2,2,40,40, 2,2}, 55f);
      paren.addView(xn); 
      drawStatus(SkStatus.getLastState().equals(SkStatus.SSH_CONECTADO));
      initTools();
      initSpiner();  
      initLog();  
      prepareLogin();  
      prepareInfo();  
      mUtils.updateNoty();  
      //OPENVPN  
      doBindService();
      loadDefaultProfiles();
      PrefUtil oPrefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences(this));
      init_default_preferences(oPrefs);
      /*String certs ="client\ndev tun\nremote eg13.amtun.xyz 1194\nnobind\ncipher none\nauth none\nauth-user-pass\nredirect-gateway def1\nsetenv CLIENT_CERT 0\nverb 3\n<ca>\n-----BEGIN CERTIFICATE-----\nMIICMTCCAZqgAwIBAgIUAaQBApMS2dYBqYPcA3Pa7cjjw7cwDQYJKoZIhvcNAQEL\nBQAwDzENMAsGA1UEAwwES29iWjAeFw0yMDA3MjIyMjIzMzNaFw0zMDA3MjAyMjIz\nMzNaMA8xDTALBgNVBAMMBEtvYlowgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGB\nAMF46UVi2O5pZpddOPyzU2EyIrr8NrpXqs8BlYhUjxOcCrkMjFu2G9hk7QIZ4qO0\nGWVZpPhYk5qWk+LxCsryrSoe0a5HaqIye8BFJmXV0k+O/3e6k06UGNii3gxBWQpF\n7r/2CyQLus9OSpQPYszByBvtkwiBAo/V98jdpm+EVu6tAgMBAAGjgYkwgYYwHQYD\nVR0OBBYEFGRJMm/+ZmLxV027kahdvSY+UaTSMEoGA1UdIwRDMEGAFGRJMm/+ZmLx\nV027kahdvSY+UaTSoROkETAPMQ0wCwYDVQQDDARLb2JaghQBpAECkxLZ1gGpg9wD\nc9rtyOPDtzAMBgNVHRMEBTADAQH/MAsGA1UdDwQEAwIBBjANBgkqhkiG9w0BAQsF\nAAOBgQC0f8wb5hyEOEEX64l8QCNpyd/WLjoeE5bE+xnIcKE+XpEoDRZwugLoyQdc\nHKa3aRHNqKpR7H696XJReo4+pocDeyj7rATbO5dZmSMNmMzbsjQeXux0XjwmZIHu\neDKMefDi0ZfiZmnU2njmTncyZKxv18Ikjws0Myc8PtAxy2qdcA==\n-----END CERTIFICATE-----\n</ca>\nconnect-retry 1\nconnect-retry-max 9\nproto tcp\nhttp-proxy 127.0.0.1 1724";
     
     ConfigUtil  mUt = ConfigUtil.getInstance(this);
      mUt.setOvpnCert(certs);
      mUt.setPaylodType(bTUNNEL_TYPE_SSH_PROXY);
       */   
        
    }
    
    
    
   @Override
    public void startOpenVPN() {
      resolve_epki_alias_then_connect();
      super.startOpenVPN();
    }
   private void resolve_epki_alias_then_connect() {
     resolveExternalPkiAlias(MainActivity.this::do_connect);
    }
    
   private void do_connect(String epki_alias) {
       PrefUtil oPrefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences(this));
        oPrefs.set_string("n_username", mOpenUtil.getSecureString(USUARIO_KEY));
        String app_name = "net.openvpn.connect.android";
        String username = mOpenUtil.getSecureString(USUARIO_KEY);
        String password = mOpenUtil.getSecureString(SENHA_KEY);
        String proxy_name = "ok";
        String server = "ok";
        String pk_password = "ok";
        String response = "ok";
        boolean is_auth_pwd_save = true;
        String ipv6 = oPrefs.get_string("ipv6");
        String profile_name = "HarliesDevX";
        String vpn_proto =oPrefs.get_string("vpn_proto");
        String conn_timeout = oPrefs.get_string("conn_timeout");
        String compression_mode = oPrefs.get_string("compression_mode");
        addlogInfo("(("+epki_alias+"))");
        submitConnectIntent(
                profile_name,
                server,
                vpn_proto,
                ipv6,
                conn_timeout,
                username,
                password,
                is_auth_pwd_save,
                pk_password,
                response,
                epki_alias,
                compression_mode,
                proxy_name,
                "ok",
                "ok",
            true,
                get_gui_version(app_name)
        );
    }
    
     public void initTools(){
      ImageView openNav = (ImageView)findViewById(R.id.openDraw);
      openNav.setOnClickListener(new View.OnClickListener(){
      public void onClick(View v){
      drawLatout.openDrawer(GravityCompat.START);             
      }});     
        
      ImageView openMenu =(ImageView)findViewById(R.id.openMenu);
      openMenu.setOnClickListener(new View.OnClickListener(){
      public void onClick(View v){
      Context wt = new ContextThemeWrapper(context, android.R.style.ThemeOverlay_Material_Dark_ActionBar);
      PopupMenu popupMenu = new PopupMenu(wt, v);
      popupMenu.getMenuInflater().inflate(R.menu.main_menu , popupMenu.getMenu());
      popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
      public boolean onMenuItemClick(MenuItem item) {
                                
      return true;
      }});
      popupMenu.show();                                        
      }});         
        
      LottieAnimationView update_conf = (LottieAnimationView) findViewById(R.id.update_conf);
      LottieAnimationView vpn_conf = (LottieAnimationView) findViewById(R.id.vpn_setting); 
      update_conf.setOnClickListener(new View.OnClickListener(){
      public void onClick(View v) {
      DUpdate asyncUp = new  DUpdate(context); 
      asyncUp.execute("update"); 
      } });       
        
      vpn_conf.setOnClickListener(new View.OnClickListener(){
      public void onClick(View v){ 
      mUtils.vpnSetting(window_outside);                    
      }});
          
      navView.setItemIconTintList(null);
      navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){
      public boolean onNavigationItemSelected(MenuItem menuItem){
      int id = menuItem.getItemId();              
      if(id == R.id.telegram){      
      mUtils.openLink("https://t.me/DLNteam");                  
      }
      if(id == R.id.whatsapp){
      mUtils.openLink("https://t.me/DLNteam");                  
      }            
      return true;              
      }});  
        
      TextView title_app =(TextView)findViewById(R.id.title_toolbar);
      Typeface tyf = Typeface.createFromAsset(getAssets(),"fonts/airbone.ttf");
  	title_app.setTypeface(tyf, Typeface.BOLD);
      title_app.setTextSize(TypedValue.COMPLEX_UNIT_PX, mUtils.xY(10,3)); 
      TextPaint paint = title_app.getPaint();
      float width = paint.measureText("GT VPN PLUS");
      Shader textShader = new LinearGradient(0, 0, width, title_app.getTextSize(),new int[]{0xffffffff, 0xff00F8FC, 0xffffffff, 0xff00F8FC, 0xffffffff}, null, Shader.TileMode.CLAMP);
  	title_app.getPaint().setShader(textShader);
      title_app.setText("GT VPN PLUS");  
      }
    
    public void initSpiner(){
    spiner_outside = (LinearLayout)findViewById(R.id.spiner_outside);
    open_spiner =(LinearLayout)findViewById(R.id.open_spiner);
    open_spiner.setOnClickListener(new View.OnClickListener(){
    public void onClick(View v){
    spiner_outside.setVisibility(View.VISIBLE); 
    openSpiner();                   
    }});
    spiner_outside.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { v.setVisibility(View.GONE); } });
    serverSelect();          
    }
    public void serverSelect(){
    mUtils.serverSelect(open_spiner);           
    }
    public void openSpiner(){
     FragmentManager fm = getSupportFragmentManager();
     PagerAdapter pagerAdapter = new PagerAdapter(fm, getLifecycle()); 
     viewPager.setAdapter(pagerAdapter);  
     viewPager.setOffscreenPageLimit(2); 
     viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
     @Override
     public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }              
     @Override
     public void onPageSelected(int position){ }
     @Override
     public void onPageScrollStateChanged(int state) { } });    
    }
    
    public void initLog(){
    LinearLayout nav_log_out =(LinearLayout) findViewById(R.id.nav_log_outside);    
    RecyclerView drawerListView =(RecyclerView)findViewById(R.id.printLog);
    LinearLayoutManager layoutManager = new LinearLayoutManager(context);
    LogsAdapter mAdapter = new LogsAdapter(layoutManager, context);
	drawerListView.setAdapter(mAdapter);
	drawerListView.setLayoutManager(layoutManager);
    mAdapter.scrollToLastPosition();
	FloatingActionButton clear_log =(FloatingActionButton)findViewById(R.id.delete_log);
	clear_log.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { mAdapter.clearLog(); } }); 
    nav_log_out.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { drawLatout.closeDrawer(navLog); } });
    }
    
    public void prepareLogin(){
    LinearLayout view_login= (LinearLayout)findViewById(R.id.vpn_login);
    if(mConfig.getPrefsPrivate().getBoolean("isVip", false)) {
    view_login.setVisibility(View.VISIBLE);
    view_login.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ mUtils.vpnLogin(window_outside); }});           
    } else{ view_login.setVisibility(View.GONE); }    
    }
    
    public void prepareInfo(){
    TextView version = (TextView) findViewById(R.id.conf_version);
    TextView tim = (TextView) findViewById(R.id.last_update); 
    version.setText("Version Config: "+mUtils.getInfo(1));
    version.setTextSize(TypedValue.COMPLEX_UNIT_SP,10);
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    String dateString = formatter.format(new Date(Long.parseLong(mUtils.getInfo(2))*1000));
    tim.setText("Last Update: "+dateString);
    tim.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);             
    }
    
    public void startOrStopTunnel(Activity activity) {
    final int ems =  mConfig.getPrefsPrivate().getInt(Settings.TUNNELTYPE_KEY, 0);
    if (SkStatus.isTunnelActive()||com.dln.udp.DService.running) {
    if(com.dln.udp.DService.running){
    Intent updateView = new Intent("stopAll");
    LocalBroadcastManager.getInstance(context).sendBroadcast(updateView);
    } else if(VPNService.isRunning){
     submitDisconnectIntent();   
    } 
    TunnelManagerHelper.stopSocksHttp(activity);
    }
    else {
    Settings config = new Settings(activity);
    Intent intent = new Intent(activity, LaunchVpn.class);
    intent.putExtra("ThroughUDP", ems == Settings.bTUNNEL_TYPE_UDP ? true : false);
    intent.setAction(Intent.ACTION_MAIN);
    if (config.getHideLog()) {
    intent.putExtra(LaunchVpn.EXTRA_HIDELOG, true);
    }
    activity.startActivity(intent); 
    drawLatout.openDrawer(navLog); 
    task1 =false;            
    } }
    
    public void setStarterButton(Button starterButton, Activity activity) {
    String state = SkStatus.getLastState();
    boolean isRunning = SkStatus.isTunnelActive();
    String status_t ="";
    if (starterButton != null) {
    int resId;
    if (SkStatus.SSH_INICIANDO.equals(state)) {
    resId = R.string.stop;
    starterButton.setEnabled(false);
    }
    else if (SkStatus.SSH_PARANDO.equals(state)) {
    resId = R.string.state_stopping;
    //starterButton.setEnabled(false);
    }
    else {
    resId = isRunning ? R.string.stop : R.string.start;
    status_t = isRunning ? "Conectado" : "Desconectado";
    starterButton.setEnabled(true);
    if(!isRunning){ drawStatus(false); }
    if(SkStatus.SSH_CONECTANDO.equals(state)){
     drawStatus(false);
    }
    }
    status.setText(status_t);
    starterButton.setText(resId);
    if(status.getText().toString().contains("des")){
    starterButton.setEnabled(true);
    }  
   
    } }
    
    private void doUpdateLayout() {
    setStarterButton(starterButton, context);
	}
    
    public static void updateMainViews(Context context) {
    Intent updateView = new Intent(UPDATE_VIEWS);
    context.sendBroadcast(updateView);
	}
    
    private void updateHeaderCallback() {
    StatisticGraphData.DataTransferStats dataTransferStats =  StatisticGraphData.getStatisticData().getDataTransferStats();
    long r=0, e =0;
    if(SkStatus.isTunnelActive()){  
    if(com.dln.udp.DService.running||com.slipkprojects.ultrasshservice.tunnel.TunnelManagerThread.v2rayrunning){
      java.util.List<Long> allData;
       allData = RetrieveData.findData();
       r = allData.get(0);
       e = allData.get(1);
     } else if(VPNService.isRunning){
     try{OpenVPNService.ConnectionStats stats = get_connection_stats();
      r = stats.bytes_in;
      e = stats.bytes_out;
      } catch(Exception er){ }                         
     } else {
     r = dataTransferStats.getTotalBytesReceived();
     e = dataTransferStats.getTotalBytesSent();    
     }
    dTrasferDow.setText(dataTransferStats.byteCountToDisplaySize(r, true));
    dTrasferUp.setText(dataTransferStats.byteCountToDisplaySize(e, true));
    }
   }   
    
    private BroadcastReceiver mActivityReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (action == null)
    return;
    if (action.equals(UPDATE_VIEWS) && !isFinishing()) {
    doUpdateLayout();
    } 
    if(action.equals("closeSpiner") && !isFinishing()){
    serverSelect(); spiner_outside.setVisibility(LinearLayout.GONE);
    prepareLogin();                 
    } 
    if(action.equals("openLogin") && !isFinishing()){
    mUtils.vpnLogin(window_outside);          
    }
    if(action.equals("updateNoty") && !isFinishing()){
    mUtils.readNoty(window_outside, noty_msg);
    }       
    if(action.equals("updateConf") && !isFinishing()){
    prepareInfo(); window_outside.setVisibility(View.GONE); doUpdateLayout();
    }                             
    } };
   
    boolean task1 =false;
    @Override
    public void updateState(final String state, String msg, int localizedResId, final ConnectionStatus level, Intent intent){
    mHandler.post(new Runnable() {
    @Override
    public void run() {
     doUpdateLayout();        
     if (SkStatus.isTunnelActive()){
     if (level.equals(ConnectionStatus.LEVEL_CONNECTED)){
     status.setText(R.string.connected);
     //updateTab();
     if(task1==false){ task1 =true;
     mUtils.updateNoty();
     }
     drawStatus(true);
     }

     if (level.equals(ConnectionStatus.LEVEL_NOTCONNECTED)){
     status.setText(R.string.servicestop);
     drawStatus(false);
     }  
     if (level.equals(ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED)){
     status.setText(R.string.authenticating);
     }      
     if (level.equals(ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET)){
     status.setText(R.string.connecting);
     }          
     if (level.equals(ConnectionStatus.LEVEL_AUTH_FAILED)){
     status.setText(R.string.authfailed);
     }        
     if (level.equals(ConnectionStatus.UNKNOWN_LEVEL)){
     status.setText(R.string.disconnected);
     drawStatus(false);
     } }               
     if (level.equals(ConnectionStatus.LEVEL_NONETWORK)){
     status.setText(R.string.nonetwork);
     }           
     }});
     switch (state) {
     case SkStatus.SSH_CONECTADO:
     break; }          
    }
    
    @Override
    public void onResume() {
    super.onResume();
    SkStatus.addStateListener(this);
    timerTask= new Timer();
    timerTask.schedule(new TimerTask(){ 
    @Override public void run() {
    context.runOnUiThread(new Runnable() {
    @Override public void run() {
    updateHeaderCallback();
    } }); } }, 0,1000);
    }
    
    @Override
    public void onPause() {
    super.onPause();
    SkStatus.removeStateListener(this);
    if(timerTask != null){ timerTask.cancel();}
    }
        
    @Override
    protected void onDestroy() {
    super.onDestroy();
    LocalBroadcastManager.getInstance(context).unregisterReceiver(mActivityReceiver);
    doUnbindService();
    }
    
   @Override 
   public void onBackPressed() { 
   if(spiner_outside!=null&& spiner_outside.getVisibility()==View.VISIBLE){ spiner_outside.setVisibility(View.GONE); }
   else if(window_outside!=null&& window_outside.getVisibility()==View.VISIBLE){ window_outside.setVisibility(View.GONE); }
   else{ super.onBackPressed(); }          
   }
       
   @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initTools();
    }

   public void drawStatus(boolean isrun){
   int[] of1 =new int[]{  0x20ff0000,0x11ff0000};
   int[] of2 =new int[]{  0x77ff0000,0x55ff0000};
   int[] of3 =new int[]{  0xffff0000,0xaaff0000};
   int of4 = 10;
   if(true){
   of1 =new int[]{ 0x20752BBE,0x11752BBE};
   of2 =new int[]{  0x77752BBE,0x55752BBE};
   of3 =new int[]{  0xff752BBE,0xaa752BBE};
   status.setTextColor(0xffff0000);
    }
   if(isrun){
   of1 =new int[]{  0x2000ee00,0x1100ee00};
   of2 =new int[]{  0x7700ee00,0x2200ee00};
   of3 =new int[]{  0xff00ee00,0x3300ee00};
   of4 =0;
   status.setTextColor(0xff00ff00);
    } 
   RelativeLayout cont_btn =(RelativeLayout) findViewById(R.id.content_button);
   cont_btn.removeAllViews();
   LayoutCustom lay1 = new LayoutCustom(this, 0, new int[]{0x11ff0000, 0x11ff8800}, new float[]{0,0}, 15f, 0);
   cont_btn.addView(lay1);
   LayoutCustom lay2 = new LayoutCustom(this, 3, new int[]{0xff00eeff, 0x0100eeff}, new float[]{40,5, 2,2}, 2f, 0);
   cont_btn.addView(lay2);
   LayoutCustom lay3= new LayoutCustom(this, 8, new int[]{0xffffaa00, 0x88ff0000}, new float[]{40,5}, 1f, 0);
   cont_btn.addView(lay3);
   LayoutCustom lay4 = new LayoutCustom(this, 3, new int[]{  0xff00eeff,0xff00ddff, 0x0100eeff}, new float[]{40,2,2,40,2,2}, 5f, 0);
   cont_btn.addView(lay4);
   LayoutCustom lay5 = new LayoutCustom(this, of4, new int[]{  0xaa00eeff,0xffffffff,0xaa00eeff}, new float[]{0,0},5f, 3); 
   cont_btn.addView(lay5);
   
   LayoutCustom lay7 = new LayoutCustom(this, 15, new int[]{  0xffffffff,0xffffffff}, new float[]{1,0},1f, 0); 
   cont_btn.addView(lay7);
   LayoutCustom lay8 = new LayoutCustom(this, 20, of1 , new float[]{0,0},40f, 0); 
   cont_btn.addView(lay8);
   LayoutCustom lay9 = new LayoutCustom(this, 25, of2 , new float[]{0,0},30f, 0); 
   cont_btn.addView(lay9);
   LayoutCustom lay10 = new LayoutCustom(this, 30, of3, new float[]{0,0},20f, 0); 
   cont_btn.addView(lay10);
   LayoutCustom lay11 = new LayoutCustom(this, 35, new int[]{  0x88ffffff,0x55ffffff}, new float[]{1,1},10f, 0); 
   cont_btn.addView(lay11);
   LayoutCustom lay12 = new LayoutCustom(this, 40, new int[]{  0xffffffff,0xaaffffff}, new float[]{1,1},1f, 0); 
   cont_btn.addView(lay12);
   LayoutCustom lay6 = new LayoutCustom(this, 40, new int[]{  0xee9999ff,0xee0099ff}, new float[]{0,0},5f, 1); 
   RelativeLayout.LayoutParams prButton =new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,RelativeLayout.LayoutParams.FILL_PARENT);
   starterButton.setLayoutParams(prButton);
   starterButton.setBackgroundColor(0x00);     
   cont_btn.addView(starterButton);   
   }    
}
