package org.heatlab;
import java.io.*;
public class Reference {
 public static void main(String[] args)throws Exception{
  ImmersedSteakSolver.Config c=new ImmersedSteakSolver.Config();c.across=8;c.end=60;c.flipTimes=new double[]{17.123,40.987};
  ImmersedSteakSolver s=new ImmersedSteakSolver(c);for(double t:new double[]{17.123,40.987,60})s.advanceTo(t,10000);
  try(DataOutputStream out=new DataOutputStream(new FileOutputStream(args[0]))){out.writeInt(s.size);for(double v:s.temperatures())out.writeDouble(v);for(double v:s.surfaceTemperatures())out.writeDouble(v);out.writeDouble(s.panPower);out.writeDouble(s.airPower);out.writeDouble(s.boxEnergy);}
 }
}
