// Reproducible sensitivity report; run separately from the quick test suite.
const fs=require('node:fs'),assert=require('node:assert/strict');
const {Solver,defaults}=require('../../ios-web/dist/solver.js');
const report=[];
for(const factor of [25,50,100]){const s=new Solver({...defaults(),exteriorRobinFactor:factor});let surfaceMin=Infinity,surfaceMax=-Infinity;const start=Date.now();do{surfaceMin=Math.min(surfaceMin,s.surfaceMin);surfaceMax=Math.max(surfaceMax,s.surfaceMax);if(s.time>=s.c.end)break;s.advanceTo(s.c.end,1);}while(true);
 const row={exteriorRobinFactor:factor,across:s.c.across,time:s.time,steps:s.steps,core:s.coreStats().slice(0,3),surfaceRangeOverRun:[surfaceMin,surfaceMax],maxConstraintResidualWm2:s.maxBoundaryResidual,physicalBoundaryEnergyJ:s.inputEnergy,auxiliaryExchangeEnergyJ:s.boxEnergy,energyAccountingResidualJ:s.energyBalanceError(),elapsedMs:Date.now()-start};report.push(row);assert(s.maxBoundaryResidual<.001);assert(surfaceMin>4);assert(surfaceMax<181);console.log(row);}
assert(Math.max(...report.map(r=>r.core[2]))-Math.min(...report.map(r=>r.core[2]))<.3);
fs.mkdirSync('build/ios-tests',{recursive:true});fs.writeFileSync('build/ios-tests/constraint-report.json',JSON.stringify(report,null,2)+'\n');
