const assert=require('node:assert/strict');
const {Solver,defaults}=require('../../ios-web/dist/solver.js');
for(const exponent of [2,4])for(const across of [8,12]){
 const c={...defaults(),exponent,across,end:20,flipTimes:[10]};const s=new Solver(c);let area=[0,0];
 for(let parity=0;parity<2;parity++)for(let i=0;i<s.geometry.markers.length;i++){
  const w=s.contact[parity][i],q=s.geometry.markers[i];assert(w===0||w===1);
  if(w){area[parity]+=q[3];assert.equal(s.contact[1-parity][i],0);for(const v of s.geometry.quads[i])assert(Math.abs(s.geometry.vertices[v][2]-(parity?1:-1)*c.thickness/2)<1e-12);assert(Math.abs(q[4])+Math.abs(q[5])<1e-10);}
 }
 assert(area[0]>0);assert(Math.abs(area[0]-area[1])<1e-12);
 const before=s.contact.map(w=>Array.from(w));s.advanceTo(10,10000);assert.equal(s.flipsAt(s.time),1);for(let i=0;i<s.surface.length;i++)assert.equal(s.contactWeight(i),before[1][i]);s.advanceTo(20,10000);assert(s.coreStats().every(Number.isFinite));assert(Math.abs(s.energyBalanceError())<1e-6);
 console.log('Flat-contact case',exponent,across,'area m²',area[0]);
}
const c={...defaults(),across:8,contactH:0,end:20};const oven=new Solver(c);oven.advanceTo(20,10000);assert.equal(oven.panPower,0);assert(oven.contact.every(a=>a.every(w=>w===0)));assert(oven.airPower>0);
console.log('PASS planar-only pan contact, no side heating, flip exchange, finite evolution, oven convection');
