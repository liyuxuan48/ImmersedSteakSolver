package org.heatlab;

public final class ImmersedSteakSolverTest {
    static int assertions;
    static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    static void finish(ImmersedSteakSolver s){while(!s.advanceTo(s.config().end,100)) {}}
    static double[] roots(double bi){double[] out=new double[60];for(int n=0;n<out.length;n++){double a=n*Math.PI+1e-9,b=(n+1)*Math.PI-1e-9;for(int j=0;j<80;j++){double m=(a+b)/2;if(1-m/Math.tan(m)-bi>0)b=m;else a=m;}out[n]=(a+b)/2;}return out;}
    static double exact(double r,double[] roots){double theta=0;for(double l:roots){double v=l*r/.02;theta+=4*(Math.sin(l)-l*Math.cos(l))/(2*l-Math.sin(2*l))*(v==0?1:Math.sin(v)/v)*Math.exp(-l*l*.1);}return 1-theta;}
    static double sphere(int n,double bi){
        ImmersedSteakSolver.Config c=new ImmersedSteakSolver.Config();c.length=c.width=c.thickness=.04;c.exponent=2;c.asymmetry=0;c.across=n;c.k=c.rho=c.cp=1;c.initial=0;c.airTemperature=1;c.airH=bi/.02;c.contactDepth=0;c.flipTimes=new double[0];c.end=.00004;
        ImmersedSteakSolver s=new ImmersedSteakSolver(c);finish(s);double[] f=s.temperatures(),roots=roots(bi);double err=0;int count=0;
        for(int i:s.coreIndices){double r=Math.sqrt(Math.pow(s.x(i%s.nx),2)+Math.pow(s.y(i/s.nx%s.ny),2)+Math.pow(s.z(i/(s.nx*s.ny)),2));if(r>.01)continue;err+=Math.pow(f[i]-exact(r,roots),2);count++;}
        double rms=Math.sqrt(err/count);check(count>0&&rms<.065,"Robin sphere accuracy");check(s.maskResidual<1e-9,"Mask solve");check(s.panPower==0,"No pan in oven case");check(Math.abs(s.energyBalanceError())<1e-10,"Sphere energy accounting");
        System.out.printf("Robin sphere Bi=%.1f n=%d RMS(r<=R/2)=%.9f%n",bi,n,rms);return rms;
    }
    static void bounds(ImmersedSteakSolver s,double lo,double hi){for(double t:s.surfaceTemperatures())check(Double.isFinite(t)&&t>=lo-1e-6&&t<=hi+1e-6,"Surface bounds");double[] st=s.coreStats();check(st[0]>=lo-1e-6&&st[1]<=hi+1e-6,"Core bounds");}
    public static void main(String[] args){
        for(double shift:new double[]{0,.1,.5,.99}){double sum=0,moment=0;for(int i=-3;i<4;i++){double w=ImmersedSteakSolver.phi(i-shift);sum+=w;moment+=(i-shift)*w;}check(Math.abs(sum-1)<1e-12,"Delta mass");check(Math.abs(moment)<1e-12,"Delta moment");}
        for(double bi:new double[]{.1,1,10}){double e12=sphere(12,bi),e20=sphere(20,bi),e32=sphere(32,bi);check(e32<e20&&e32<e12,"Fine-grid error reduction (coarse trend need not be monotonic)");}
        ImmersedSteakSolver.Config c=new ImmersedSteakSolver.Config();ImmersedSteakSolver s=new ImmersedSteakSolver(c);
        check(s.adjointError()<1e-11,"Normal operator adjoint");check(s.compatibilityError()<1e-12,"Compatible equilibrium");
        double[] normal=new double[3];double area=0;for(double[] m:s.geometry.markers){area+=m[3];for(int d=0;d<3;d++)normal[d]+=m[3]*m[4+d];}for(double v:normal)check(Math.abs(v)<area*1e-12,"Closed surface");
        for(int j=1;j<=20;j++){s.advanceTo(c.end*j/20,10000);bounds(s,5,180);check(Math.abs(s.energyBalanceError())<1e-6,"Pan-air-box accounting");}
        check(s.flipsAt(s.time)==1&&s.time==600,"Default flip and end");double[] st=s.coreStats();System.out.printf("Default steak: min %.6f max %.6f mean %.6f; box %.6f J; residual %.3g J%n",st[0],st[1],st[2],s.boxEnergy,s.energyBalanceError());
        c=new ImmersedSteakSolver.Config();c.end=50;c.flipTimes=new double[]{17.123,40.987};s=new ImmersedSteakSolver(c);ImmersedSteakSolver.Config nc=c.copy();nc.flipTimes=new double[0];ImmersedSteakSolver no=new ImmersedSteakSolver(nc);
        s.advanceTo(17.123,10000);no.advanceTo(17.123,10000);double[] a=s.maskedField(),b=no.maskedField();for(int i=0;i<a.length;i++)check(a[i]==b[i],"Flip preserves instantaneous field");check(s.flipsAt(s.time)==1,"Exact noninteger event");
        double bottomBefore=0,bottomAfter=0;for(int i=0;i<s.markerCount();i++){double z=s.geometry.markers[i][2];bottomBefore+=z*s.contactWeight(i,0);bottomAfter+=z*s.contactWeight(i,s.time);}check(bottomBefore<0&&bottomAfter>0,"Contact changes body face");
        s.advanceTo(40.987,10000);check(s.flipsAt(s.time)==2,"Second exact flip");finish(s);finish(no);check(Math.abs(s.coreStats()[2]-no.coreStats()[2])>1e-4,"Flip changes subsequent heating");
        c=new ImmersedSteakSolver.Config();c.end=20;c.airTemperature=c.panTemperature=37;s=new ImmersedSteakSolver(c);s.initializeUniform(37);finish(s);st=s.coreStats();check(Math.abs(st[0]-37)<1e-8&&Math.abs(st[1]-37)<1e-8,"Uniform nonzero equilibrium");check(Math.abs(s.boxEnergy)<1e-8,"Equilibrium box cancellation");
        c.contactH=c.airH=0;s=new ImmersedSteakSolver(c);s.initializeUniform(60);finish(s);st=s.coreStats();check(Math.abs(st[0]-60)<1e-8&&Math.abs(st[1]-60)<1e-8,"Insulated uniform equilibrium");
        c.contactH=-1;boolean bad=false;try{c.validate();}catch(IllegalArgumentException e){bad=true;}check(bad,"Negative coefficient rejected");c.contactH=1;c.flipTimes=new double[]{10,9};bad=false;try{c.validate();}catch(IllegalArgumentException e){bad=true;}check(bad,"Unsorted flips rejected");
        System.out.println("PASS: "+assertions+" assertions");
    }
}
