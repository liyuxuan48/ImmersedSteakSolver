const assert=require('node:assert/strict');
const {Solver,defaults}=require('../../ios-web/dist/solver.js');
const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0);
function residual(s){
 // Reconstruct the physical two-sided boundary equation independently, using
 // fresh operator buffers rather than the solver's cached residual or CG matrix.
 const g=new Float64Array(s.faceSize),layer=new Float64Array(s.faceSize),av=new Float64Array(s.surface.length),trace=new Float64Array(s.surface.length);
 s.grad(s.u,g);s.normal.spread(s.jump,layer);for(let i=0;i<g.length;i++)g[i]+=layer[i];s.normal.sample(g,av);s.scalar.sample(s.u,trace);
 const exported=s.inwardFluxes();let maximum=0;for(let i=0;i<av.length;i++){
  const ti=s.c.initial+(trace[i]+s.jump[i]/2)/s.scaling[i],te=(trace[i]-s.jump[i]/2)/s.scaling[i],w=s.contactWeight(i);
  const qi=w*s.c.contactH*(s.c.panTemperature-ti)+(1-w)*s.c.airH*(s.c.airTemperature-ti),qe=-s.capacity*s.exteriorBeta*te;
  maximum=Math.max(maximum,Math.abs(2*s.capacity*s.alpha*av[i]/s.scaling[i]-qi+qe));
  assert(Math.abs(exported[i]-qi)<1e-8); // physical CSV flux excludes auxiliary exchange
 }return maximum;
}
const s=new Solver({...defaults(),across:8,end:30,flipTimes:[7.123,19.456]});
const m=s.surface.length,v=Float64Array.from({length:m},(_,i)=>Math.sin(i*.37));
const faces=new Float64Array(s.faceSize),ntn=new Float64Array(m),gram=new Float64Array(m),zeros=new Float64Array(m);
s.normal.spread(v,faces);s.normal.sample(faces,ntn);s.constraint.apply(v,gram,1,zeros);for(let i=0;i<m;i++)assert(Math.abs(ntn[i]-gram[i])<1e-10);
const scalarGrid=new Float64Array(s.size),sampled=new Float64Array(m),probe=Float64Array.from({length:s.size},(_,i)=>Math.cos(i*.13));s.scalar.spread(v,scalarGrid);s.scalar.sample(probe,sampled);assert(Math.abs(dot(scalarGrid,probe)-dot(v,sampled))<1e-9);
// Both transpose pairs, including all boundary grid faces.
const f=Float64Array.from({length:s.faceSize},(_,i)=>Math.sin(i*.13));s.normal.sample(f,ntn);assert(Math.abs(dot(v,ntn)-dot(faces,f))<1e-9);
const u=Float64Array.from({length:s.size},(_,i)=>Math.cos(i*.31)),grad=new Float64Array(s.faceSize),div=new Float64Array(s.size);s.grad(u,grad);s.divergence(f,div);assert(Math.abs(dot(grad,f)+dot(u,div))<1e-7);
for(const t of [0,7.123,19.456,30]){s.advanceTo(t,10000);assert(residual(s)<.001);assert(s.boundaryResidual<.001);}
assert(Math.abs(s.energyBalanceError())<1e-6);
// No artificial zero-flux shortcut: the beta+ = 0 exterior-Neumann case uses
// the same coupled Robin/Neumann solve and satisfies the independent equation.
const n=new Solver({...defaults(),across:8,exteriorRobinFactor:0,end:10});n.advanceTo(10,10000);assert(residual(n)<.001);assert.equal(n.auxPower,0);
// Dissipation for homogeneous environments: u dot du/dt <= 0 after elimination.
const e=new Solver({...defaults(),across:8,initial:0,panTemperature:0,airTemperature:0,end:10});for(let i=0;i<e.size;i++)e.u[i]=Math.sin(i*.17);e.solveSurface();e.normal.spread(e.jump,e.faces);for(let i=0;i<e.faceSize;i++)e.faces[i]+=e.gradient[i];e.divergence(e.faces,e.work);e.scalar.spread(e.flux,e.gridTmp);let energyRate=0;for(let i=0;i<e.size;i++)energyRate+=e.u[i]*(e.alpha*e.work[i]+e.gridTmp[i]);assert(energyRate<0);
// Changing time-step size must have a small effect on this fixed-grid trajectory.
const a=new Solver({...defaults(),across:8,end:30,flipTimes:[7.123,19.456]}),b=new Solver(a.c);b.stableDt/=2;a.advanceTo(30,10000);b.advanceTo(30,10000);let delta=0;const ta=a.temperatures(),tb=b.temperatures();for(const i of a.coreIndices)delta=Math.max(delta,Math.abs(ta[i]-tb[i]));assert(delta<1);console.log('Half-step max core change °C',delta);
console.log('PASS Gram and transpose identities, independent Robin/Neumann residuals, flips, energy, Neumann limit and dissipation');
