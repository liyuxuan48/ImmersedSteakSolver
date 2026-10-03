const assert=require('node:assert/strict');
const {category,thresholds}=require('../../ios-web/dist/doneness.js');
const {Solver,defaults}=require('../../ios-web/dist/solver.js');
assert.equal(category(5),0);thresholds.forEach((t,i)=>{assert.equal(category(t-1e-7),i);assert.equal(category(t),i+1);assert.equal(category(t+1e-7),i+1);});assert.equal(category(NaN),-1);
const c={...defaults(),across:8,end:30,flipTimes:[7.123,19.456]};const s=new Solver(c),expected=new Float64Array(s.size).fill(c.initial);
while(s.time<c.end){s.advanceTo(c.end,1);const t=s.temperatures();for(let i=0;i<s.size;i++)if(s.inside[i])expected[i]=Math.max(expected[i],t[i]);}
assert.deepEqual(s.peakTemperature,expected);
// Uniform hot steak cooling into cold air: categories must retain the initial maximum.
const cool=new Solver({...c,initial:75,contactH:0,airTemperature:5,airH:100});cool.advanceTo(c.end,10000);let colder=0;const t=cool.temperatures();for(let i=0;i<cool.size;i++)if(cool.inside[i]){assert(cool.peakTemperature[i]>=75);assert(cool.peakTemperature[i]>=t[i]);assert.equal(category(cool.peakTemperature[i]),5);if(t[i]<74)colder++;}assert(colder>0);
cool.initializeUniform(20);assert(cool.peakTemperature.every(t=>t===20));assert(new Solver(c).peakTemperature.every(t=>t===c.initial));
console.log('PASS category thresholds, every-step per-cell maxima, cooling retention, flips and reset');
