package org.heatlab;

import java.util.ArrayList;

/** Closed cubed-sphere surface, mapped to an elliptical superellipsoid. SI units. */
public final class SteakGeometry {
    public final double a,b,c,p,asymmetry;
    public final double[][] vertices;
    public final int[][] quads;
    // Quadrature markers: x,y,z, area, outward nx,ny,nz.
    public final double[][] markers;
    public SteakGeometry(double length,double width,double thickness,double exponent,double asymmetry,double spacing) {
        a=length/2; b=width/2; c=thickness/2; p=exponent; this.asymmetry=asymmetry;
        ArrayList<double[]> points=new ArrayList<>(),samples=new ArrayList<>();
        ArrayList<int[]> faces=new ArrayList<>();
        double[] radii={a,b,c};
        for(int axis=0;axis<3;axis++) for(int sign:new int[]{-1,1}) {
            int d1=(axis+1)%3,d2=(axis+2)%3;
            int n1=Math.max(2,(int)Math.ceil(2*radii[d1]/spacing)),n2=Math.max(2,(int)Math.ceil(2*radii[d2]/spacing));
            int base=points.size();
            for(int j=0;j<=n2;j++) for(int i=0;i<=n1;i++) {
                double[] v=new double[3]; v[axis]=sign*radii[axis];
                v[d1]=radii[d1]*(2.*i/n1-1); v[d2]=radii[d2]*(2.*j/n2-1); points.add(project(v));
            }
            for(int j=0;j<n2;j++) for(int i=0;i<n1;i++) {
                int v0=base+j*(n1+1)+i,v1=v0+1,v3=v0+n1+1,v2=v3+1;
                int[] q=sign>0?new int[]{v0,v1,v2,v3}:new int[]{v0,v3,v2,v1}; faces.add(q);
                double[] normal=new double[3],centre=new double[3];
                double area=0;
                for(int k=0;k<4;k++) for(int d=0;d<3;d++) centre[d]+=points.get(q[k])[d]/4;
                for(int k=1;k<=2;k++) {
                    double[] u=sub(points.get(q[k]),points.get(q[0])),v=sub(points.get(q[k+1]),points.get(q[0]));
                    double[] cross=cross(u,v); double magnitude=norm(cross); area+=magnitude/2;
                    for(int d=0;d<3;d++) normal[d]+=cross[d]/2;
                }
                centre=project(centre);
                // Area-weighted vector normal preserves the closed mesh's integral of n dA.
                samples.add(new double[]{centre[0],centre[1],centre[2],area,normal[0]/area,normal[1]/area,normal[2]/area});
            }
        }
        vertices=points.toArray(new double[0][]); quads=faces.toArray(new int[0][]); markers=samples.toArray(new double[0][]);
    }
    private double outline(double x,double y) {
        double angle=Math.atan2(y/b,x/a);
        return 1+asymmetry*(.65*Math.cos(3*angle)+.35*Math.sin(angle));
    }
    public double level(double x,double y,double z) {
        double xy=Math.hypot(x/a,y/b)/outline(x,y);
        return Math.pow(Math.pow(xy,p)+Math.pow(Math.abs(z/c),p),1/p);
    }
    private double[] project(double[] v) {
        double s=level(v[0],v[1],v[2]); return new double[]{v[0]/s,v[1]/s,v[2]/s};
    }
    /** First-order signed-distance estimate; positive inside. Used only to flag the diffuse band. */
    public double interiorDistance(double x,double y,double z) {
        double f=level(x,y,z),eps=Math.min(a,Math.min(b,c))*1e-4;
        double gx=(level(x+eps,y,z)-level(x-eps,y,z))/(2*eps);
        double gy=(level(x,y+eps,z)-level(x,y-eps,z))/(2*eps);
        double gz=(level(x,y,z+eps)-level(x,y,z-eps))/(2*eps);
        double g=Math.sqrt(gx*gx+gy*gy+gz*gz);
        return g<1e-12?Math.min(a,Math.min(b,c)):(1-f)/g;
    }
    static double[] sub(double[] a,double[] b) { return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]}; }
    static double[] cross(double[] a,double[] b) { return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]}; }
    static double norm(double[] a) { return Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]); }
}
