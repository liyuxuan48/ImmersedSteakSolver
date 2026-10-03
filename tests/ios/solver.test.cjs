const assert=require('node:assert/strict');
const {Solver,defaults,validate}=require('../../ios-web/dist/solver.js');
let c,s;
c=defaults();c.end=50;c.across=8;c.flipTimes=[17.123,40.987];s=new Solver(c);let no=new Solver({...c,flipTimes:[]});s.advanceTo(17.123,10000);no.advanceTo(17.123,10000);assert.deepEqual(s.u,no.u);assert.equal(s.flipsAt(s.time),1);s.advanceTo(40.987,10000);assert.equal(s.flipsAt(s.time),2);assert(Math.abs(s.energyBalanceError())<1e-6);
c=defaults();c.across=8;c.end=20;c.panTemperature=c.airTemperature=37;s=new Solver(c);s.initializeUniform(37);s.advanceTo(20,10000);assert(Math.abs(s.coreStats()[0]-37)<1e-8);assert(Math.abs(s.coreStats()[1]-37)<1e-8);
function roots(bi){return Array.from({length:60},(_,n)=>{let a=n*Math.PI+1e-9,b=(n+1)*Math.PI-1e-9;for(let j=0;j<80;j++){const m=(a+b)/2;if(1-m/Math.tan(m)-bi>0)b=m;else a=m;}return(a+b)/2;});}
// Fixed physical sampling region for all resolutions. This replaces the old
// calibrated-closure single-grid check with a constrained-method refinement check.
for(const bi of [.1,1,10]){const errors=[];for(const across of [12,24,32]){
 c={...defaults(),length:.04,width:.04,thickness:.04,exponent:2,asymmetry:0,across,k:1,rho:1,cp:1,initial:0,airTemperature:1,airH:bi/.02,contactH:0,flatFaces:false,flipTimes:[],end:.00004};s=new Solver(c);s.advanceTo(c.end,20000);assert.equal(s.time,c.end);let error=0,count=0;const f=s.temperatures(),lambdas=roots(bi);
 for(let i=0;i<s.size;i++){const r=Math.hypot(s.x(i%s.nx),s.y(Math.floor(i/s.nx)%s.ny),s.z(Math.floor(i/(s.nx*s.ny))));if(r>.01)continue;let theta=0;for(const l of lambdas){const v=l*r/.02;theta+=4*(Math.sin(l)-l*Math.cos(l))/(2*l-Math.sin(2*l))*(v===0?1:Math.sin(v)/v)*Math.exp(-l*l*.1);}error+=(f[i]-(1-theta))**2;count++;}
 const rms=Math.sqrt(error/count);errors.push(rms);assert(s.maxBoundaryResidual<1e-4);console.log('Constrained Robin sphere Bi',bi,'across',across,'RMS',rms);
 }assert(errors[1]<errors[0]);assert(errors[2]<errors[1]);assert(errors[2]<errors[0]*.5);assert(errors[2]<({'.1':.002,'0.1':.002,'1':.015,'10':.05}[bi]));}
assert.throws(()=>validate({...defaults(),across:NaN}));assert.throws(()=>validate({...defaults(),flipTimes:[10,9]}));console.log('PASS web solver planar geometry, exact flips, equilibrium, sphere checks');
