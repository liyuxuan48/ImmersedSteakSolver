package org.heatlab;

import java.util.Arrays;

/** 3D regularized immersed layers with Robin contact/convection and exact flip events.
 * u is masked temperature excess relative to initial; S is the interior surface excess.
 * u_t = alpha (L u + D Rn S) + R beta(E-S), beta=h_transfer/(rho cp).
 * I u = (I H) S imposes a zero auxiliary exterior temperature trace.
 * The Robin flux is evaluated from this reconstructed surface trace at the same time level.
 * A compatible normal-operator correction preserves the discrete uniform equilibrium.
 */
public final class ImmersedSteakSolver {
    public static final class Config {
        public double length=.12,width=.08,thickness=.025,k=.45,rho=1050,cp=3500;
        public double initial=5,panTemperature=180,airTemperature=25,contactH=500,airH=15,contactDepth=.004;
        public double end=600,exponent=4,asymmetry=.1;
        public double[] flipTimes={300};
        public int across=12;
        public Config copy() {
            Config c=new Config();c.length=length;c.width=width;c.thickness=thickness;c.k=k;c.rho=rho;c.cp=cp;
            c.initial=initial;c.panTemperature=panTemperature;c.airTemperature=airTemperature;c.contactH=contactH;c.airH=airH;c.contactDepth=contactDepth;
            c.end=end;c.exponent=exponent;c.asymmetry=asymmetry;c.across=across;c.flipTimes=flipTimes.clone();return c;
        }
        public void validate() {
            for(double v:new double[]{length,width,thickness,k,rho,cp,end})if(!Double.isFinite(v)||v<=0)throw new IllegalArgumentException("Dimensions, properties and duration must be positive and finite.");
            if(length<.005||length>.5||width<.005||width>.5||thickness<.005||thickness>Math.min(length,width))throw new IllegalArgumentException("Use 5–500 mm dimensions, with thickness no larger than length or width.");
            if(across<8||across>32)throw new IllegalArgumentException("Use 8–32 cells through thickness.");
            for(double v:new double[]{initial,panTemperature,airTemperature})if(!Double.isFinite(v)||Math.abs(v)>1000)throw new IllegalArgumentException("Temperatures must be between −1000 and 1000 °C.");
            for(double v:new double[]{contactH,airH})if(!Double.isFinite(v)||v<0||v>20000)throw new IllegalArgumentException("Heat-transfer coefficients must be 0–20000 W/m²K.");
            if(!Double.isFinite(contactDepth)||contactDepth<0||contactDepth>thickness/2)throw new IllegalArgumentException("Contact band must be 0 to half the thickness; 0 disables pan contact.");
            if(exponent<2||exponent>4||!Double.isFinite(exponent)||!Double.isFinite(asymmetry)||asymmetry<0||asymmetry>.15)throw new IllegalArgumentException("Shape exponent must be 2–4 and asymmetry 0–0.15.");
            if(end<1e-6||end>86400)throw new IllegalArgumentException("Duration must be 1e-6–86400 seconds.");
            if(flipTimes==null||flipTimes.length>100)throw new IllegalArgumentException("Use at most 100 flip times.");
            double prev=0;for(double t:flipTimes){if(!Double.isFinite(t)||t<=prev||t>86400)throw new IllegalArgumentException("Flip times must be positive, strictly increasing and at most 86400 s.");prev=t;}
        }
    }
    /** Area/cell-volume weighted regularized surface spreading and its adjoint. */
    private static final class SurfaceOperator {
        final int[][] ids;final double[][] weights;
        SurfaceOperator(int[][] ids,double[][] weights){this.ids=ids;this.weights=weights;}
        void spread(double[] surface,double[] grid){Arrays.fill(grid,0);for(int s=0;s<ids.length;s++)for(int j=0;j<ids[s].length;j++)grid[ids[s][j]]+=weights[s][j]*surface[s];}
        void sample(double[] grid,double[] surface){for(int s=0;s<ids.length;s++){double v=0;for(int j=0;j<ids[s].length;j++)v+=weights[s][j]*grid[ids[s][j]];surface[s]=v;}}
    }
    private final Config c;
    public final SteakGeometry geometry;
    public final int nx,ny,nz,size;
    private final int fx,fy,faceSize;
    public final double h,alpha,stableDt,x0,y0,z0;
    public final boolean[] inside,core;
    public final int[] coreIndices;
    private final SurfaceOperator scalar,normal;
    private final double[] scaling,unit,correction,mask,u,work,gridTmp,faces,gradient;
    private final double[] surface,traceMask,robin,environment,flux;
    private final double[][] contact;
    private final double capacity;
    public final double volume;
    public double time,maskResidual,panPower,airPower,boxPower,inputEnergy,boxEnergy;
    private double initialEnergy;
    public long steps;
    public ImmersedSteakSolver(Config config){
        config.validate();c=config.copy();capacity=c.rho*c.cp;h=c.thickness/c.across;alpha=c.k/capacity;
        double maxBeta=Math.max(c.contactDepth>0?c.contactH:0,c.airH)/capacity;
        stableDt=.4/(6*alpha/(h*h)+2*maxBeta/h);
        if(!Double.isFinite(stableDt)||stableDt<=0||c.end/stableDt>20000)throw new IllegalArgumentException("Too many time steps. Reduce duration, resolution or transfer coefficients.");
        nx=even((int)Math.ceil(c.length*(1+c.asymmetry)/h)+10);ny=even((int)Math.ceil(c.width*(1+c.asymmetry)/h)+10);nz=c.across+10;
        if((long)nx*ny*nz>250000)throw new IllegalArgumentException("Grid exceeds 250,000 cells. Reduce aspect ratio or resolution.");
        size=nx*ny*nz;fx=(nx+1)*ny*nz;fy=nx*(ny+1)*nz;faceSize=fx+fy+nx*ny*(nz+1);x0=-nx*h/2;y0=-ny*h/2;z0=-nz*h/2;
        geometry=new SteakGeometry(c.length,c.width,c.thickness,c.exponent,c.asymmetry,2*h);int m=geometry.markers.length;
        if(m>4000)throw new IllegalArgumentException("Surface exceeds 4,000 markers. Reduce resolution.");
        scaling=new double[m];unit=new double[m];traceMask=new double[m];surface=new double[m];robin=new double[m];environment=new double[m];flux=new double[m];
        correction=new double[faceSize];faces=new double[faceSize];gradient=new double[faceSize];mask=new double[size];u=new double[size];work=new double[size];gridTmp=new double[size];
        inside=new boolean[size];core=new boolean[size];int count=0;
        for(int z=0;z<nz;z++)for(int y=0;y<ny;y++)for(int x=0;x<nx;x++){int i=index(x,y,z);inside[i]=geometry.level(x(x),y(y),z(z))<1;core[i]=inside[i]&&geometry.interiorDistance(x(x),y(y),z(z))>=2*h;if(core[i])count++;}
        if(count==0)throw new IllegalArgumentException("No resolved core. Increase thickness resolution.");
        coreIndices=new int[count];count=0;for(int i=0;i<size;i++)if(core[i])coreIndices[count++]=i;
        int[][] si=new int[m][64],ni=new int[m][192];double[][] sw=new double[m][64],nw=new double[m][192];
        contact=new double[2][m];int[] contacts=new int[2];
        for(int s=0;s<m;s++){
            double[] q=geometry.markers[s];scaling[s]=Math.sqrt(q[3]/(h*h*h));
            for(int type=-1;type<3;type++){
                double gx=(q[0]-x0)/h-(type==0?0:.5),gy=(q[1]-y0)/h-(type==1?0:.5),gz=(q[2]-z0)/h-(type==2?0:.5);int at=0;
                for(int k=(int)Math.floor(gz)-1;k<=(int)Math.floor(gz)+2;k++)for(int j=(int)Math.floor(gy)-1;j<=(int)Math.floor(gy)+2;j++)for(int i=(int)Math.floor(gx)-1;i<=(int)Math.floor(gx)+2;i++){
                    double w=phi(i-gx)*phi(j-gy)*phi(k-gz)*scaling[s];
                    if(type<0){si[s][at]=index(i,j,k);sw[s][at]=w;}else{ni[s][type*64+at]=faceIndex(type,i,j,k);nw[s][type*64+at]=w*q[4+type];}at++;
                }
            }
            for(int parity=0;parity<2;parity++){double sign=parity==0?1:-1;double depth=c.thickness/2+sign*q[2];contact[parity][s]=c.contactDepth>0?smooth(1-depth/c.contactDepth)*smooth(-sign*q[6]):0;if(contact[parity][s]>1e-6)contacts[parity]++;}
        }
        if(c.contactDepth>0&&c.contactH>0&&(contacts[0]==0||contacts[1]==0))throw new IllegalArgumentException("No resolved contact markers. Increase the contact band or grid resolution.");
        scalar=new SurfaceOperator(si,sw);normal=new SurfaceOperator(ni,nw);
        normal.spread(scaling,faces);divergence(faces,gridTmp);solveMask(gridTmp);
        double v=0;for(double a:mask)v+=a*h*h*h;volume=v;
        // Rank-one correction: Bn_hat * scaling = -G H. Its divergence vanishes
        // to the mask-Poisson tolerance; spreading/interpolation remain adjoints.
        gradient(mask,gradient);double norm=Math.sqrt(dot(scaling,scaling));
        for(int s=0;s<m;s++)unit[s]=scaling[s]/norm;
        for(int i=0;i<faceSize;i++)correction[i]=(-gradient[i]-faces[i])/norm;
        scalar.sample(mask,traceMask);for(int s=0;s<m;s++){traceMask[s]/=scaling[s];if(traceMask[s]<.1)throw new IllegalStateException("Invalid surface mask trace");}
        updateBoundary();solveSurface();
    }
    private static int even(int n){return (n+1)/2*2;}
    public Config config(){return c.copy();}
    public int index(int x,int y,int z){return(z*ny+y)*nx+x;}
    private int faceIndex(int axis,int x,int y,int z){return axis==0?(z*ny+y)*(nx+1)+x:axis==1?fx+(z*(ny+1)+y)*nx+x:fx+fy+(z*ny+y)*nx+x;}
    public double x(int i){return x0+(i+.5)*h;}public double y(int j){return y0+(j+.5)*h;}public double z(int k){return z0+(k+.5)*h;}
    public int markerCount(){return scaling.length;}
    public int flipsAt(double t){int n=0;for(double f:c.flipTimes)if(f<=t)n++;else break;return n;}
    private static double smooth(double x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
    public double contactWeight(int marker,double t){return contact[flipsAt(t)%2][marker];}
    public boolean isContact(int marker,double t){return contactWeight(marker,t)>0;}
    public double nextFlip(){for(double t:c.flipTimes)if(t>time&&t<c.end)return t;return Double.POSITIVE_INFINITY;}
    public double contactArea(double t){double a=0;for(int s=0;s<markerCount();s++)a+=contactWeight(s,t)*geometry.markers[s][3];return a;}
    public double[] temperatures(){double[] t=new double[size];for(int i=0;i<size;i++)t[i]=c.initial+(inside[i]?u[i]/mask[i]:0);return t;}
    public double[] maskedField(){return u.clone();}
    public double[] surfaceTemperatures(){double[] t=new double[markerCount()];for(int s=0;s<t.length;s++)t[s]=c.initial+surface[s]/scaling[s];return t;}
    public double[] inwardFluxes(){double[] q=new double[markerCount()];for(int s=0;s<q.length;s++)q[s]=capacity*2*robin[s]*(environment[s]-surface[s]/scaling[s]);return q;}
    public double[] coreStats(){double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY,sum=0;int cold=-1;for(int i:coreIndices){double t=c.initial+u[i]/mask[i];if(t<min){min=t;cold=i;}max=Math.max(max,t);sum+=t;}return new double[]{min,max,sum/coreIndices.length,cold};}
    public double energy(){double sum=0;for(double v:u)sum+=v;return capacity*h*h*h*sum;}
    public double energyBalanceError(){return energy()-initialEnergy-inputEnergy-boxEnergy;}
    public void initializeUniform(double temperature){if(!Double.isFinite(temperature))throw new IllegalArgumentException("Non-finite initial field");for(int i=0;i<size;i++)u[i]=mask[i]*(temperature-c.initial);time=0;steps=0;inputEnergy=boxEnergy=0;initialEnergy=energy();updateBoundary();solveSurface();}
    public static double phi(double r){double a=Math.abs(r);if(a>=2)return 0;if(a<=1)return(3-2*a+Math.sqrt(1+4*a-4*a*a))/8;return(5-2*a-Math.sqrt(-7+12*a-4*a*a))/8;}
    private static double dot(double[] a,double[] b){double s=0;for(int i=0;i<a.length;i++)s+=a[i]*b[i];return s;}
    private void gradient(double[] v,double[] out){
        for(int z=0;z<nz;z++)for(int y=0;y<ny;y++)for(int x=0;x<=nx;x++)out[faceIndex(0,x,y,z)]=((x<nx?v[index(x,y,z)]:0)-(x>0?v[index(x-1,y,z)]:0))/h;
        for(int z=0;z<nz;z++)for(int y=0;y<=ny;y++)for(int x=0;x<nx;x++)out[faceIndex(1,x,y,z)]=((y<ny?v[index(x,y,z)]:0)-(y>0?v[index(x,y-1,z)]:0))/h;
        for(int z=0;z<=nz;z++)for(int y=0;y<ny;y++)for(int x=0;x<nx;x++)out[faceIndex(2,x,y,z)]=((z<nz?v[index(x,y,z)]:0)-(z>0?v[index(x,y,z-1)]:0))/h;
    }
    private void divergence(double[] v,double[] out){for(int z=0;z<nz;z++)for(int y=0;y<ny;y++)for(int x=0;x<nx;x++)out[index(x,y,z)]=(v[faceIndex(0,x+1,y,z)]-v[faceIndex(0,x,y,z)]+v[faceIndex(1,x,y+1,z)]-v[faceIndex(1,x,y,z)]+v[faceIndex(2,x,y,z+1)]-v[faceIndex(2,x,y,z)])/h;}
    private void negativeLaplacian(double[] v,double[] out){for(int z=0;z<nz;z++)for(int y=0;y<ny;y++)for(int x=0;x<nx;x++){int i=index(x,y,z);out[i]=6*v[i]-(x>0?v[i-1]:0)-(x+1<nx?v[i+1]:0)-(y>0?v[i-nx]:0)-(y+1<ny?v[i+nx]:0)-(z>0?v[i-nx*ny]:0)-(z+1<nz?v[i+nx*ny]:0);}}
    private void solveMask(double[] doubleLayer){
        double[] r=new double[size],p=new double[size],ap=new double[size];for(int i=0;i<size;i++)p[i]=r[i]=h*h*doubleLayer[i];double rr=dot(r,r),initial=rr,tol=rr*1e-20;
        for(int it=0;it<1000&&rr>tol;it++){if(Thread.currentThread().isInterrupted())throw new IllegalStateException("Cancelled");negativeLaplacian(p,ap);double a=rr/dot(p,ap);for(int i=0;i<size;i++){mask[i]+=a*p[i];r[i]-=a*ap[i];}double next=dot(r,r),b=next/rr;rr=next;for(int i=0;i<size;i++)p[i]=r[i]+b*p[i];}
        maskResidual=Math.sqrt(rr/initial);if(rr>tol)throw new IllegalStateException("Mask solve did not converge");for(int i:coreIndices)if(mask[i]<.5||mask[i]>1.5)throw new IllegalStateException("Unresolved geometry mask");
    }
    private void spreadNormal(double[] v,double[] out){normal.spread(v,out);double sum=dot(unit,v);for(int i=0;i<faceSize;i++)out[i]+=correction[i]*sum;}
    private void sampleNormal(double[] v,double[] out){normal.sample(v,out);double sum=dot(correction,v);for(int s=0;s<out.length;s++)out[s]+=unit[s]*sum;}
    private void updateBoundary(){int parity=flipsAt(time)%2;for(int s=0;s<markerCount();s++){double w=contact[parity][s],hc=w*c.contactH,ha=(1-w)*c.airH,ht=hc+ha;robin[s]=ht/(2*capacity);environment[s]=ht>0?(hc*(c.panTemperature-c.initial)+ha*(c.airTemperature-c.initial))/ht:0;}}
    private void solveSurface(){
        // Zero auxiliary exterior trace gives Iu = (IH) S. Eliminate S locally,
        // then evaluate the Robin law; no prescribed-temperature projection.
        gradient(u,gradient);scalar.sample(u,surface);panPower=airPower=0;
        for(int s=0;s<markerCount();s++){
            surface[s]/=traceMask[s];
            double temperature=c.initial+surface[s]/scaling[s],w=contactWeight(s,time),area=geometry.markers[s][3];
            flux[s]=scaling[s]*2*robin[s]*(environment[s]-surface[s]/scaling[s]);
            panPower+=w*c.contactH*(c.panTemperature-temperature)*area;
            airPower+=(1-w)*c.airH*(c.airTemperature-temperature)*area;
        }
    }
    public boolean advanceTo(double target,int maxSteps){
        if(!Double.isFinite(target)||target<time||target>c.end||maxSteps<1)throw new IllegalArgumentException("Invalid target time");
        for(int n=0;n<maxSteps&&time<target;n++){
            double limit=Math.min(target,nextFlip()),remaining=limit-time,dt=Math.min(stableDt,remaining);if(time+dt==time)throw new IllegalStateException("Time step below floating-point precision");
            // All diffusion, double-layer and Robin flux terms are evaluated at the same old time.
            spreadNormal(surface,faces);for(int i=0;i<faceSize;i++)faces[i]+=gradient[i];divergence(faces,work);scalar.spread(flux,gridTmp);
            double sum=0;for(int i=0;i<size;i++){sum+=work[i];u[i]+=dt*(alpha*work[i]+gridTmp[i]);}
            boxPower=capacity*alpha*h*h*h*sum;inputEnergy+=dt*(panPower+airPower);boxEnergy+=dt*boxPower;
            for(int i:coreIndices)if(!Double.isFinite(u[i]))throw new IllegalStateException("Non-finite solution");
            time=dt==remaining?limit:time+dt;steps++;updateBoundary();solveSurface();
        }
        return time>=target;
    }
    public double adjointError(){double[] s=new double[markerCount()],g=new double[faceSize],rs=new double[faceSize],jg=new double[s.length];for(int i=0;i<s.length;i++)s[i]=Math.sin(i*.7);for(int i=0;i<g.length;i++)g[i]=Math.cos(i*.3);spreadNormal(s,rs);sampleNormal(g,jg);return Math.abs(dot(g,rs)-dot(s,jg))/Math.max(1,Math.abs(dot(g,rs)));}
    public double compatibilityError(){spreadNormal(scaling,faces);gradient(mask,gradient);double sum=0,denom=0;for(int i=0;i<faceSize;i++){sum+=Math.pow(faces[i]+gradient[i],2);denom+=gradient[i]*gradient[i];}gradient(u,gradient);return Math.sqrt(sum/denom);}
}
