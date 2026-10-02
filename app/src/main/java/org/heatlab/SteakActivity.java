package org.heatlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native 3D geometry inspection and slices for the independent immersed-layer solver. */
public final class SteakActivity extends Activity {
    private static final int INK=0xff172b30,TEAL=0xff007e80,MUTED=0xff5b6d6f;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Map<String,EditText> inputs=new LinkedHashMap<>();
    private final ArrayList<double[]> history=new ArrayList<>();
    private volatile boolean running;
    private volatile int generation;
    private boolean inFlight,building;
    private ImmersedSteakSolver solver;
    private ImmersedSteakSolver.Config config=new ImmersedSteakSolver.Config();
    private double[] field,stats;
    private double time;
    private double[] diagnostics=new double[6],surfaceTemperatures,fluxes;
    private long steps;
    private LinearLayout explore,setup,method;
    private ScrollView scroll;
    private TextView status,temperature,details,sliceLabel;
    private Button run;
    private ShapeView shapeView;
    private SliceView sliceView;
    private Spinner plane;
    private SeekBar slice;
    private String pendingExport;
    private int pendingSlice=-1;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=column();root.setBackgroundColor(0xfff4f6f3);root.setPadding(dp(16),dp(10),dp(16),0);
        root.addView(text("HEAT LAB / 3D IMMERSED LAYERS",11,TEAL));
        TextView title=text("Inside the steak.",28,INK);title.setTypeface(null,1);root.addView(title);
        LinearLayout nav=row();
        for(String name:new String[]{"Explore","Setup","Method"})nav.addView(button(name,()->show(name)),new LinearLayout.LayoutParams(0,dp(48),1));
        root.addView(nav);scroll=new ScrollView(this);LinearLayout pages=column();scroll.addView(pages);
        explore=column();setup=column();method=column();pages.addView(explore);pages.addView(setup);pages.addView(method);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        makeExplore();makeSetup();makeMethod();setContentView(root);restore();show("Explore");
        try{config=readConfig();}catch(Exception e){config=new ImmersedSteakSolver.Config();fill(config);}
        build(config);
    }
    int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);return l;}
    TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(5),0,dp(5));return t;}
    Button button(String s,Runnable f){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(TEAL);b.setTextSize(13);b.setOnClickListener(v->f.run());return b;}
    void section(LinearLayout p,String name){TextView t=text(name,18,INK);t.setTypeface(null,1);t.setPadding(0,dp(16),0,dp(6));p.addView(t);}
    void show(String page){explore.setVisibility(page.equals("Explore")?View.VISIBLE:View.GONE);setup.setVisibility(page.equals("Setup")?View.VISIBLE:View.GONE);method.setVisibility(page.equals("Method")?View.VISIBLE:View.GONE);scroll.post(()->scroll.scrollTo(0,0));}
    void makeExplore(){
        status=text("Building surface…",13,TEAL);explore.addView(status);
        LinearLayout controls=row();run=button("Run 3D",this::toggle);
        controls.addView(run,new LinearLayout.LayoutParams(0,dp(48),1));controls.addView(button("Reset",()->build(config)),new LinearLayout.LayoutParams(0,dp(48),1));
        explore.addView(controls);
        shapeView=new ShapeView();explore.addView(shapeView,new LinearLayout.LayoutParams(-1,dp(225)));
        explore.addView(text("Drag to rotate • white cross = coldest resolved core cell\nAmber = pan contact · teal = exposed surface.\nSlices and cold-point coordinates follow the steak.",11,MUTED));
        temperature=text("",17,INK);explore.addView(temperature);
        section(explore,"Inside the volume");
        plane=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,new String[]{"XY · through thickness z","XZ · along width y","YZ · along length x"});
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);plane.setAdapter(a);explore.addView(plane);
        sliceLabel=text("",12,MUTED);explore.addView(sliceLabel);
        slice=new SeekBar(this);slice.setContentDescription("Slice position");explore.addView(slice);
        slice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){updateSlice();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        plane.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){configureSlice();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        sliceView=new SliceView();explore.addView(sliceView,new LinearLayout.LayoutParams(-1,dp(225)));
        explore.addView(button("Show coldest slice",()->{
            if(solver==null||building)return;int k=(int)stats[3]/(solver.nx*solver.ny);
            if(plane.getSelectedItemPosition()==0)slice.setProgress(k);else{pendingSlice=k;plane.setSelection(0);}
        }));
        explore.addView(text("Grey = regularized surface band (within 2 grid spacings). Colours and cold-point search use only the resolved interior. Fixed colour limits are shown below the slice.",12,MUTED));
        details=text("",12,MUTED);explore.addView(details);
        LinearLayout exports=row();exports.addView(button("Core CSV",()->export(0)),new LinearLayout.LayoutParams(0,dp(48),1));
        exports.addView(button("History",()->export(1)),new LinearLayout.LayoutParams(0,dp(48),1));exports.addView(button("Mesh STL",()->export(2)),new LinearLayout.LayoutParams(0,dp(48),1));explore.addView(exports);explore.addView(button("Boundary CSV",()->export(3)));
        explore.addView(text("Research model: finite pan contact and air convection, illustrative material properties, no evaporation, crust, fat/bone interfaces or food-safety prediction.",12,MUTED));
    }
    void input(String key,String name,double initial){
        LinearLayout r=row();r.setGravity(Gravity.CENTER_VERTICAL);r.addView(text(name,14,INK),new LinearLayout.LayoutParams(0,-2,1));
        EditText e=new EditText(this);e.setText(Double.toString(initial));e.setSingleLine(true);e.setTextSize(15);e.setContentDescription(name);
        e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        inputs.put(key,e);r.addView(e,new LinearLayout.LayoutParams(dp(112),dp(52)));setup.addView(r);
    }
    void makeSetup(){
        setup.addView(text("An adjustable approximation, not a scanned steak. Applying creates a new surface/grid and clears results.",13,MUTED));
        LinearLayout presets=row();
        presets.addView(button("Rounded steak",()->fill(new ImmersedSteakSolver.Config())),new LinearLayout.LayoutParams(0,dp(48),1));
        presets.addView(button("Ellipsoid",()->{ImmersedSteakSolver.Config c=new ImmersedSteakSolver.Config();c.exponent=2;c.asymmetry=0;fill(c);}),new LinearLayout.LayoutParams(0,dp(48),1));setup.addView(presets);
        setup.addView(button("Apply 3D case",this::apply));
        section(setup,"Pan, air & flipping");
        input("initial","Initial interior (°C)",5);input("panTemperature","Pan temperature (°C)",180);input("airTemperature","Air temperature (°C)",25);
        input("contactH","Pan coefficient (W/m²K)",500);input("airH","Air coefficient (W/m²K)",15);input("contactDepth","Contact band (mm; 0 = off)",4);
        input("flipTimes","Flip times (s; comma separated)",300);inputs.get("flipTimes").setInputType(InputType.TYPE_CLASS_TEXT);
        setup.addView(text("Blank flip times = no flipping. Each event rotates the steak 180° about its length; temperature stays with the meat. Contact fades smoothly across the lower band. Pan temperature is constant; contact deformation is approximated.",12,MUTED));
        setup.addView(button("Oven only",()->{ImmersedSteakSolver.Config c=new ImmersedSteakSolver.Config();c.contactDepth=0;c.airTemperature=180;c.airH=25;c.flipTimes=new double[0];fill(c);}));
        section(setup,"Shape · centred at x = y = z = 0");
        input("length","Nominal length (mm)",120);input("width","Nominal width (mm)",80);input("thickness","Thickness (mm)",25);
        input("exponent","Roundness exponent (2–4)",4);input("asymmetry","Outline variation (0–0.15)",.1);
        setup.addView(text("Exponent 2 gives an ellipsoid; 4 gives flatter faces. Outline variation can extend nominal length/width by up to its fractional value.",12,MUTED));
        section(setup,"Grid & duration");input("across","Cells through thickness",12);input("end","Duration (s)",600);
        setup.addView(text("Start with 12 cells through thickness; compare 8 and 16 to assess resolution. Up to 250,000 grid cells and 4,000 surface markers. This is a coarse model.",12,MUTED));
        section(setup,"Constant material properties");
        input("k","Conductivity (W/m·K)",.45);input("rho","Density (kg/m³)",1050);input("cp","Heat capacity (J/kg·K)",3500);
        setup.addView(text("These are illustrative inputs, not measurements of your steak.",12,MUTED));
        setup.addView(button("Apply 3D case",this::apply));
    }
    double value(String key){try{return Double.parseDouble(inputs.get(key).getText().toString().trim());}catch(Exception e){throw new IllegalArgumentException("Enter a valid number for "+inputs.get(key).getContentDescription());}}
    ImmersedSteakSolver.Config readConfig(){
        ImmersedSteakSolver.Config c=new ImmersedSteakSolver.Config();c.length=value("length")/1000;c.width=value("width")/1000;c.thickness=value("thickness")/1000;
        c.exponent=value("exponent");c.asymmetry=value("asymmetry");double n=value("across");if(n!=Math.rint(n))throw new IllegalArgumentException("Cell count must be a whole number");c.across=(int)n;
        c.end=value("end");c.k=value("k");c.rho=value("rho");c.cp=value("cp");c.initial=value("initial");c.panTemperature=value("panTemperature");c.airTemperature=value("airTemperature");c.contactH=value("contactH");c.airH=value("airH");c.contactDepth=value("contactDepth")/1000;String times=inputs.get("flipTimes").getText().toString().trim();String[] parts=times.isEmpty()?new String[0]:times.split(",",-1);c.flipTimes=new double[parts.length];try{for(int i=0;i<parts.length;i++)c.flipTimes[i]=Double.parseDouble(parts[i].trim());}catch(Exception e){throw new IllegalArgumentException("Use comma-separated flip times, e.g. 120, 240");}c.validate();return c;
    }
    void put(String k,double v){inputs.get(k).setText(String.format(Locale.US,"%.8g",v));}
    void fill(ImmersedSteakSolver.Config c){put("length",c.length*1000);put("width",c.width*1000);put("thickness",c.thickness*1000);put("exponent",c.exponent);put("asymmetry",c.asymmetry);put("across",c.across);put("end",c.end);put("k",c.k);put("rho",c.rho);put("cp",c.cp);put("initial",c.initial);put("panTemperature",c.panTemperature);put("airTemperature",c.airTemperature);put("contactH",c.contactH);put("airH",c.airH);put("contactDepth",c.contactDepth*1000);StringJoiner times=new StringJoiner(", ");for(double t:c.flipTimes)times.add(Double.toString(t));inputs.get("flipTimes").setText(times.toString());}
    void restore(){android.content.SharedPreferences p=getSharedPreferences("steak-v3",0);for(String key:inputs.keySet())if(p.contains(key))inputs.get(key).setText(p.getString(key,""));}
    void save(){android.content.SharedPreferences.Editor p=getSharedPreferences("steak-v3",0).edit();for(String key:inputs.keySet())p.putString(key,inputs.get(key).getText().toString());p.apply();}
    void apply(){try{ImmersedSteakSolver.Config c=readConfig();build(c);show("Explore");}catch(Exception e){error(e.getMessage());}}
    void build(ImmersedSteakSolver.Config candidate){
        running=false;building=true;inFlight=false;int token=++generation;run.setEnabled(false);status.setText("Building 3D surface & immersed operators…");
        worker.execute(()->{
            try{ImmersedSteakSolver created=new ImmersedSteakSolver(candidate);double[] f=created.temperatures(),s=created.coreStats();
                runOnUiThread(()->{if(token!=generation||isDestroyed())return;solver=created;config=candidate.copy();field=f;stats=s;time=0;steps=0;diagnostics=snapshot(created);surfaceTemperatures=created.surfaceTemperatures();fluxes=created.inwardFluxes();building=false;history.clear();record();pendingSlice=-1;configureSlice();refresh();fill(config);save();});
            }catch(Exception e){runOnUiThread(()->{if(token!=generation||isDestroyed())return;building=false;refresh();error(e.getMessage());});}
        });
    }
    void toggle(){if(building||solver==null)return;if(time>=config.end){build(config);return;}running=!running;refresh();if(running)schedule();}
    void schedule(){
        if(!running||building||inFlight||solver==null)return;inFlight=true;int token=generation;ImmersedSteakSolver active=solver;
        worker.execute(()->{
            try{double target=Math.min(config.end,active.time+config.end/100);
                while(!active.advanceTo(target,1))if(!running||token!=generation||Thread.currentThread().isInterrupted())break;
                double[] f=active.temperatures(),s=active.coreStats();double t=active.time;double[] d=snapshot(active),st=active.surfaceTemperatures(),q=active.inwardFluxes();long n=active.steps;
                runOnUiThread(()->{if(token!=generation||isDestroyed())return;inFlight=false;field=f;stats=s;time=t;diagnostics=d;surfaceTemperatures=st;fluxes=q;steps=n;record();if(time>=config.end)running=false;refresh();if(running)shapeView.postDelayed(this::schedule,16);});
            }catch(Exception e){runOnUiThread(()->{if(token!=generation||isDestroyed())return;inFlight=false;running=false;refresh();error(e.getMessage());});}
        });
    }
    double[] snapshot(ImmersedSteakSolver s){return new double[]{s.panPower,s.airPower,s.energy(),s.inputEnergy,s.boxEnergy,s.energyBalanceError()};}
    void record(){int cold=(int)stats[3];history.add(new double[]{time,stats[0],stats[1],stats[2],solver.x(cold%solver.nx),solver.y(cold/solver.nx%solver.ny),solver.z(cold/(solver.nx*solver.ny)),solver.flipsAt(time),diagnostics[0],diagnostics[1],diagnostics[2],diagnostics[3],diagnostics[4],diagnostics[5]});
        if(history.size()>2000)for(int i=history.size()-2;i>0;i-=2)history.remove(i);
    }
    void refresh(){
        run.setEnabled(!building&&solver!=null);if(building)return;
        if(solver==null){status.setText("No 3D case. Edit Setup and apply.");return;}
        status.setText(String.format(Locale.US,"%s · %.1f / %.1f s",running?"Running":time>=config.end?"Complete":time==0?"Ready":"Paused",time,config.end));
        run.setText(running?"Pause":time>=config.end?"New run":time>0?"Resume":"Run 3D");
        int i=(int)stats[3];temperature.setText(time==0?String.format(Locale.US,"Initial core: %.2f °C everywhere",config.initial):String.format(Locale.US,"Coldest resolved core: %.2f °C\nx %.1f · y %.1f · z %.1f mm",stats[0],solver.x(i%solver.nx)*1000,solver.y(i/solver.nx%solver.ny)*1000,solver.z(i/(solver.nx*solver.ny))*1000));
        double next=Double.POSITIVE_INFINITY;for(double f:config.flipTimes)if(f>time&&f<config.end){next=f;break;}
        status.append(String.format(Locale.US,"\nSide %s down · %d flips · next %s\nPan %+.1f W · air %+.1f W",solver.flipsAt(time)%2==0?"A":"B",solver.flipsAt(time),Double.isFinite(next)?String.format(Locale.US,"%.1f s",next):"none",diagnostics[0],diagnostics[1]));
        details.setText(String.format(Locale.US,"%d × %d × %d grid · %,d cells · %,d surface markers\nΔ = %.3f mm · Δt ≤ %.3g s · %,d steps\nCore mean %.2f °C · core max %.2f °C\nMasked energy %.1f J · boundary input %.1f J\nNumerical box exchange %.1f J · accounting residual %.2g J\nBox exchange is numerical error, not physical heat loss.",solver.nx,solver.ny,solver.nz,solver.size,solver.markerCount(),solver.h*1000,solver.stableDt,steps,stats[2],stats[1],diagnostics[2],diagnostics[3],diagnostics[4],diagnostics[5]));
        shapeView.invalidate();updateSlice();
    }
    void configureSlice(){if(solver==null)return;int n=plane.getSelectedItemPosition()==0?solver.nz:plane.getSelectedItemPosition()==1?solver.ny:solver.nx;slice.setMax(n-1);slice.setProgress(pendingSlice>=0?pendingSlice:n/2);pendingSlice=-1;updateSlice();}
    void updateSlice(){if(solver==null||sliceView==null)return;int p=plane.getSelectedItemPosition(),s=slice.getProgress();double v=p==0?solver.z(s):p==1?solver.y(s):solver.x(s);
        sliceLabel.setText(String.format(Locale.US,"%s = %.2f mm · body coordinates · slice %d",p==0?"z":p==1?"y":"x",v*1000,s));sliceView.invalidate();}
    void makeMethod(){
        section(method,"3D surface + volume");method.addView(text("An adjustable closed superellipsoid mesh carries surface areas and normals. A separate Cartesian volume grid evolves the temperature. This Java implementation uses immersed single and double layers; it does not run JuliaIBPM's Julia package.",15,INK));
        section(method,"Contact, convection and flips");method.addView(text("Inward flux = w hc (Tpan − Ts) + (1 − w) ha (Tair − Ts). The contact fraction w smoothly combines depth in the lower contact band and downward normal direction. A zero band disables contact. Each scheduled flip rotates the body 180° about x: the contacting face changes instantly, while the temperature field remains continuous in body coordinates. Time steps end exactly at each flip.",15,INK));
        section(method,"Immersed layers");method.addView(text("With u = masked temperature excess and S = surface temperature excess:\n\nu_t = α (L u + D Rₙ S) + R(qin / ρcp)\nIu = (IH) S\nLH = −D Rₙ 1\n\nR and I use a four-point regularized delta; D Rₙ is a staggered-face double layer. A compatible normal-operator correction preserves uniform equilibrium. The surface trace is reconstructed from Iu/(IH), then used in the Robin flux. This is a calibrated discrete trace closure, not the reference Julia Neumann-constraint algorithm.",15,INK));
        section(method,"Numerical limits");method.addView(text("Explicit Euler evaluates diffusion, double layer and Robin flux at the same time level. Δt ≤ 0.4 / (6α/Δ² + 2βmax/Δ). Interior temperature is Tinitial + u/H. Core statistics exclude a 2Δ surface band. The finite auxiliary box can exchange numerical energy; this is reported separately from pan and air input. A small accounting residual does not imply a physically exact solution.",15,INK));
        method.addView(text("Sphere Robin analytical comparisons, uniform equilibrium and exact flip-event tests check the solver. Refinement is not monotonic on all coarse grids. Compare resolutions before interpreting results. There is no radiation, evaporation, crust, fluid flow, deformation or food-safety prediction.",15,INK));
        section(method,"Coordinates and exports");method.addView(text("Slices and core coordinates stay attached to the steak; the mesh rotates after a flip. Boundary CSV includes body/world positions, contact fraction, surface temperature and inward flux. History includes powers and energy accounting. STL uses millimetres. Settings persist; simulations pause in the background and results are not restored after process destruction.",15,INK));
    }
    void error(String s){new AlertDialog.Builder(this).setTitle("3D case").setMessage(s).setPositiveButton("OK",null).show();}
    String metadata(){return String.format(Locale.US,"# Heat Lab 3.0; 3D immersed layers; calibrated trace closure; body coordinates; core excludes 2h band\n# length_m=%s; width_m=%s; thickness_m=%s; exponent=%s; asymmetry=%s\n# k=%s; rho=%s; cp=%s; initial_C=%s; pan_C=%s; air_C=%s; hc=%s; ha=%s; contact_band_m=%s; flips_s=%s; h_m=%s; time_s=%s\n",config.length,config.width,config.thickness,config.exponent,config.asymmetry,config.k,config.rho,config.cp,config.initial,config.panTemperature,config.airTemperature,config.contactH,config.airH,config.contactDepth,Arrays.toString(config.flipTimes),solver.h,time);}
    void export(int kind){
        if(solver==null||building)return;StringBuilder b=new StringBuilder();
        if(kind==2){b.append("solid steak_mm\n");for(int[] q:solver.geometry.quads)for(int k=1;k<=2;k++){
            double[] a=solver.geometry.vertices[q[0]],v=solver.geometry.vertices[q[k]],w=solver.geometry.vertices[q[k+1]];
            double[] n=SteakGeometry.cross(SteakGeometry.sub(v,a),SteakGeometry.sub(w,a));double norm=SteakGeometry.norm(n);
            b.append(" facet normal ").append(n[0]/norm).append(' ').append(n[1]/norm).append(' ').append(n[2]/norm).append("\n  outer loop\n");
            for(double[] x:new double[][]{a,v,w})b.append("   vertex ").append(x[0]*1000).append(' ').append(x[1]*1000).append(' ').append(x[2]*1000).append('\n');b.append("  endloop\n endfacet\n");
        }b.append("endsolid steak_mm\n");}else{
            b.append(metadata());if(kind==0){b.append("x_m,y_m,z_m,temperature_C,time_s\n");for(int i:solver.coreIndices)b.append(solver.x(i%solver.nx)).append(',').append(solver.y(i/solver.nx%solver.ny)).append(',').append(solver.z(i/(solver.nx*solver.ny))).append(',').append(field[i]).append(',').append(time).append('\n');}
            else if(kind==3){b.append("body_x_m,body_y_m,body_z_m,world_x_m,world_y_m,world_z_m,area_m2,contact_fraction,surface_C,inward_flux_W_m2,time_s\n");double sign=solver.flipsAt(time)%2==0?1:-1;for(int m=0;m<solver.markerCount();m++){double[] a=solver.geometry.markers[m];double[] row={a[0],a[1],a[2],a[0],sign*a[1],sign*a[2],a[3],solver.contactWeight(m,time),surfaceTemperatures[m],fluxes[m],time};for(int j=0;j<row.length;j++){if(j>0)b.append(',');b.append(row[j]);}b.append('\n');}}
            else{b.append("time_s,core_min_C,core_max_C,core_mean_C,cold_x_m,cold_y_m,cold_z_m,flip_count,pan_W,air_W,masked_energy_J,boundary_input_J,numerical_box_J,accounting_residual_J\n");for(double[] row:history){for(int i=0;i<row.length;i++){if(i>0)b.append(',');b.append(row[i]);}b.append('\n');}}
        }
        pendingExport=b.toString();Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType(kind==2?"application/octet-stream":"text/csv");
        intent.putExtra(Intent.EXTRA_TITLE,kind==2?"steak-surface-mm.stl":kind==0?"steak-core.csv":kind==3?"steak-boundary.csv":"steak-history.csv");
        try{startActivityForResult(intent,63);}catch(Exception e){pendingExport=null;error("No document provider available");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=63)return;String content=pendingExport;pendingExport=null;
        if(result!=RESULT_OK||data==null||data.getData()==null||content==null)return;android.net.Uri uri=data.getData();
        worker.execute(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new java.io.IOException("No destination");out.write(content.getBytes(StandardCharsets.UTF_8));runOnUiThread(()->Toast.makeText(this,"Export saved",Toast.LENGTH_SHORT).show());}
            catch(Exception e){runOnUiThread(()->{if(!isDestroyed())error("Export failed: "+e.getMessage());});}});
    }
    @Override protected void onPause(){super.onPause();running=false;if(status!=null)refresh();}
    @Override protected void onDestroy(){running=false;generation++;worker.shutdownNow();super.onDestroy();}
    static int colour(double t){double f=Math.max(0,Math.min(1,t))*4;int i=Math.min(3,(int)f);double a=f-i;int[] c={0xff193e68,0xff148f9a,0xffa5d0a0,0xfff1cf70,0xffb83d38};return Color.rgb((int)(Color.red(c[i])*(1-a)+Color.red(c[i+1])*a),(int)(Color.green(c[i])*(1-a)+Color.green(c[i+1])*a),(int)(Color.blue(c[i])*(1-a)+Color.blue(c[i+1])*a));}
    final class ShapeView extends View{
        final Paint p=new Paint(3);float yaw=.55f,pitch=.65f,lastX,lastY;
        ShapeView(){super(SteakActivity.this);setContentDescription("Rotatable 3D steak surface mesh with cold-core marker");}
        float[] project(double[] body){double sign=solver.flipsAt(time)%2==0?1:-1;double[] v={body[0],body[1]*sign,body[2]*sign};double x=Math.cos(yaw)*v[0]-Math.sin(yaw)*v[1],y=Math.sin(yaw)*v[0]+Math.cos(yaw)*v[1];double z=Math.sin(pitch)*y+Math.cos(pitch)*v[2],depth=Math.cos(pitch)*y-Math.sin(pitch)*v[2];
            double scale=Math.min(getWidth(),getHeight())*.83/Math.max(config.length*(1+config.asymmetry),config.width*(1+config.asymmetry));return new float[]{getWidth()/2+(float)(x*scale),getHeight()/2-(float)(z*scale),(float)depth};}
        @Override protected void onDraw(Canvas canvas){if(solver==null)return;SteakGeometry g=solver.geometry;float[][] points=new float[g.vertices.length][];for(int i=0;i<points.length;i++)points[i]=project(g.vertices[i]);
            Integer[] order=new Integer[g.quads.length];for(int i=0;i<order.length;i++)order[i]=i;Arrays.sort(order,(a,b)->Float.compare(depth(g.quads[b],points),depth(g.quads[a],points)));
            for(int qi:order){int[] q=g.quads[qi];Path path=new Path();path.moveTo(points[q[0]][0],points[q[0]][1]);for(int k=1;k<4;k++)path.lineTo(points[q[k]][0],points[q[k]][1]);path.close();
                p.setStyle(Paint.Style.FILL);double w=solver.contactWeight(qi,time);p.setColor(Color.rgb((int)(70+175*w),(int)(161+16*w),(int)(164-104*w)));canvas.drawPath(path,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.45f));p.setColor(0xff865e56);canvas.drawPath(path,p);
            }
            if(stats!=null&&time>0){int i=(int)stats[3];float[] c=project(new double[]{solver.x(i%solver.nx),solver.y(i/solver.nx%solver.ny),solver.z(i/(solver.nx*solver.ny))});p.setStrokeWidth(dp(3));p.setColor(Color.WHITE);canvas.drawLine(c[0]-dp(6),c[1],c[0]+dp(6),c[1],p);canvas.drawLine(c[0],c[1]-dp(6),c[0],c[1]+dp(6),p);}
            p.setStyle(Paint.Style.FILL);p.setColor(MUTED);p.setTextSize(dp(11));canvas.drawText(String.format(Locale.US,"%.0f × %.0f × %.0f mm nominal",config.length*1000,config.width*1000,config.thickness*1000),dp(4),getHeight()-dp(6),p);
        }
        float depth(int[] q,float[][] p){return(p[q[0]][2]+p[q[1]][2]+p[q[2]][2]+p[q[3]][2])/4;}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();getParent().requestDisallowInterceptTouchEvent(true);return true;}
            if(e.getAction()==MotionEvent.ACTION_MOVE){yaw+=(e.getX()-lastX)/180;pitch+=(e.getY()-lastY)/180;pitch=Math.max(-1.4f,Math.min(1.4f,pitch));lastX=e.getX();lastY=e.getY();invalidate();return true;}
            if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){getParent().requestDisallowInterceptTouchEvent(false);performClick();return true;}return super.onTouchEvent(e);}
        @Override public boolean performClick(){super.performClick();return true;}
    }
    final class SliceView extends View{
        final Paint p=new Paint(3);SliceView(){super(SteakActivity.this);setContentDescription("Interior temperature slice; grey cells are the unresolved surface band");}
        @Override protected void onDraw(Canvas canvas){if(solver==null||field==null)return;int axis=plane.getSelectedItemPosition(),at=slice.getProgress();int n1=axis==2?solver.ny:solver.nx,n2=axis==0?solver.ny:solver.nz;
            float scale=Math.min((getWidth()-dp(34))/(float)n1,(getHeight()-dp(62))/(float)n2);float left=(getWidth()-n1*scale)/2,top=(getHeight()-dp(48)-n2*scale)/2;
            double low=Math.min(config.initial,Math.min(config.airTemperature,config.contactDepth>0?config.panTemperature:config.airTemperature)),high=Math.max(config.initial,Math.max(config.airTemperature,config.contactDepth>0?config.panTemperature:config.airTemperature));if(high==low)high=low+1;float coldU=-1,coldV=-1;
            for(int v=0;v<n2;v++)for(int u=0;u<n1;u++){int i=axis==0?solver.index(u,v,at):axis==1?solver.index(u,at,v):solver.index(at,u,v);
                if(!solver.inside[i])continue;p.setColor(solver.core[i]?colour((field[i]-low)/(high-low)):0xffbfcac6);canvas.drawRect(left+u*scale,top+(n2-1-v)*scale,left+(u+1)*scale+.4f,top+(n2-v)*scale+.4f,p);
                if(stats!=null&&i==(int)stats[3]&&time>0){coldU=u+.5f;coldV=n2-v-.5f;}
            }
            if(coldU>=0){p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));canvas.drawCircle(left+coldU*scale,top+coldV*scale,Math.max(dp(4),scale),p);p.setStyle(Paint.Style.FILL);}
            p.setColor(MUTED);p.setTextSize(dp(11));canvas.drawText(axis==2?"y →":"x →",left,top+n2*scale+dp(14),p);
            canvas.drawText(axis==0?"↑ y":"↑ z",dp(2),dp(13),p);
            float y=getHeight()-dp(30),w=getWidth()-dp(20);for(int i=0;i<100;i++){p.setColor(colour(i/99.));canvas.drawRect(dp(10)+i*w/100,y,dp(10)+(i+1)*w/100+1,y+dp(8),p);}
            p.setColor(MUTED);canvas.drawText(String.format(Locale.US,"%.1f °C",low),dp(10),getHeight()-dp(5),p);p.setTextAlign(Paint.Align.RIGHT);canvas.drawText(String.format(Locale.US,"%.1f °C",high),getWidth()-dp(10),getHeight()-dp(5),p);p.setTextAlign(Paint.Align.LEFT);
        }
    }
}
